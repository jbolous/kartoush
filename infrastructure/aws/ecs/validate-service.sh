#!/usr/bin/env bash
# Briefly validate the inactive app-only service, then restore desired count zero.
set -euo pipefail

aws_ecs() {
  aws --profile kartoush --region us-east-2 --no-cli-pager ecs "$@"
}

cleanup() {
  local result=$?
  trap - EXIT
  if ! aws_ecs update-service --cluster kartoush-demo-cluster \
      --service kartoush-demo-app --desired-count 0 >/dev/null; then
    echo 'Shutdown failed: restore desired count zero immediately using the runbook.' >&2
    exit 1
  fi
  exit "$result"
}

# Refuse to interrupt an environment somebody intentionally left running.
initial_count=$(aws_ecs describe-services --cluster kartoush-demo-cluster \
  --services kartoush-demo-app --query 'services[0].desiredCount' --output text)
[[ "$initial_count" == 0 ]] || { echo 'Validation requires desired count zero.' >&2; exit 1; }

# Install cleanup before starting: a failed API response may still have changed AWS state.
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
aws_ecs update-service --cluster kartoush-demo-cluster \
  --service kartoush-demo-app --desired-count 1 >/dev/null

deadline=$((SECONDS + 600))
while (( SECONDS < deadline )); do
  ready=$(aws_ecs describe-services --cluster kartoush-demo-cluster \
    --services kartoush-demo-app \
    --query 'services[0].{ready: desiredCount == `1` && runningCount == `1` && pendingCount == `0` && length(deployments) == `1` && deployments[0].rolloutState == `"COMPLETED"`}.ready' \
    --output text)
  if [[ "$ready" == True ]]; then
    task=$(aws_ecs list-tasks --cluster kartoush-demo-cluster \
      --service-name kartoush-demo-app --query 'taskArns[0]' --output text)
    if [[ "$task" != None && -n "$task" ]]; then
      healthy=$(aws_ecs describe-tasks --cluster kartoush-demo-cluster --tasks "$task" \
        --query 'tasks[0].{ready: lastStatus == `"RUNNING"` && healthStatus == `"HEALTHY"`}.ready' \
        --output text)
      if [[ "$healthy" == True ]]; then
        echo 'Application task healthy; service deployment completed. Restoring zero.'
        exit 0
      fi
    fi
  fi
  sleep 15
done

echo 'Startup validation timed out. Restoring desired count zero.' >&2
exit 1
