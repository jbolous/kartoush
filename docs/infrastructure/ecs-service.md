# ECS service foundation

This is the initial service configuration for #177. It uses the approved [Phase 1 architecture](../architecture/aws-phase-1-target-architecture.md): one Fargate service, normally inactive, in an Ohio public application subnet. The original issue's private-subnet assumption is superseded by that architecture. RDS remains private. Public-subnet placement provides HTTPS egress without NAT Gateway or paid interface endpoints.

## Resources

| Resource | Configuration |
| --- | --- |
| Cluster | `kartoush-demo-cluster`; Container Insights disabled |
| Service and task family | `kartoush-demo-app` |
| Capacity | Linux/x86 Fargate, 0.25 vCPU, 1 GiB task memory |
| Application limit | 768 MiB; remaining task capacity reserved for later sidecars/operations |
| Network | Public application subnet, app security group, task public IPv4 when running |
| Runtime | Production profile; five injected managed-secret fields; verified-TLS RDS connection |
| Logging | `/kartoush/demo/app`, seven-day retention |
| Deployment | Minimum healthy 0%, maximum 100%; circuit breaker with rollback |
| Normal state | Desired count zero; no running application task or retained task public address |

The application image is pinned to the same ECR digest validated in #178. Task definitions are immutable revisions: registering a revision stores a new startup recipe; it does not start a container. The service is the controller that maintains the desired number of tasks. A desired count of zero retains configuration without paying for Fargate compute.

## Initial boundary

This revision contains only the application. It binds to `127.0.0.1:8080`; there is no public listener and no DNS publication. The HTTPS proxy, TLS materialization, private-key injection, exact route allowlist, proxy image, and sidecar startup ordering remain #341. Do not change the app binding or open port 8080 as a shortcut to public access.

ECS Exec is disabled until the independent transcript log group and cluster audit configuration are ready in #179. The task image already includes the required `script` and `cat` tools. Email remains disabled with the noop provider until real provider credentials are supplied.

Creating this foundation does not complete full #177 acceptance: the combined application/proxy task and representative request/memory validation are still pending. Keep #177 open until the complete task is stable.

## Provisioning

Confirm the selected Region in AWS Settings > View all projects > Overview > Additional Info > Region. Use profile `kartoush`, project ID `790873128308`, and `us-east-2`.

With Python, boto3, and the AWS login credential dependency available:

```sh
python3 infrastructure/aws/ecs/provision-service.py --profile kartoush
```

The helper refuses a different project, a nonzero desired count, an existing active service, or network resources outside the tagged demo VPC. It validates an active Internet Gateway route before provisioning. On a retry after a partial failure, it reuses an active cluster only when its demo tags and configured settings match. It refuses a cluster in a transitional state or with mismatched configuration. An absent or inactive cluster is created. It registers the task definition and uses the returned revision ARN when creating the service; the historical revision in `service.json` is not assumed to be current.

An expired session requires `aws login --profile kartoush`. Do not use permanent access keys. If provisioning fails, inspect the cluster/task/service state before retrying: creating a cluster or registering a revision can succeed before a later operation fails. The helper does not automatically remove resources or overwrite an existing service.

The preconditions are the existing ECR image, RDS database and application role, managed application secret, execution/task roles, tagged networking, and application log group with finite retention. See [runtime secrets](runtime-secrets.md) for injection and rotation details.

## Deliberate validation and shutdown

For the initial app-only service, a deliberate desired-count-one check may verify startup and container health. It must not publish DNS or claim a public API endpoint. ECS will replace an unhealthy task while desired count is one; a deployment circuit breaker stops repeated failed deployments, but it is not a spending limit or nightly shutdown mechanism. An initial failed deployment has no previous completed deployment to roll back to.

After the service reaches a completed deployment with one healthy task, return desired count to zero. Verify running and pending counts are zero and the task network interface is deleted. Stopping an individual task alone is insufficient: the service will replace it if desired count remains one.

Inspect service state:

```sh
aws ecs describe-services --profile kartoush --region us-east-2 \
  --cluster kartoush-demo-cluster --services kartoush-demo-app
```

Deliberately start one application task for validation:

```sh
aws ecs update-service --profile kartoush --region us-east-2 \
  --cluster kartoush-demo-cluster --service kartoush-demo-app --desired-count 1
```

Return the service to its normal inactive state:

```sh
aws ecs update-service --profile kartoush --region us-east-2 \
  --cluster kartoush-demo-cluster --service kartoush-demo-app --desired-count 0
```

After #341 supplies the proxy, manual startup must verify both containers before publishing DNS. Deployment or task replacement can change the public IP. Follow the architecture's DNS reconciliation and remove the public A record before planned shutdown. No autoscaling or schedule starts this service.

Inactive service deployments must preserve desired count zero. The chosen 0%/100% replacement configuration accepts downtime and limits deployments to one normal task. Fargate compute and task IPv4 charges accrue while a task exists; retained RDS storage/compute, secrets, and logs have their own costs. Nightly lifecycle automation remains a later task.

## Live validation

On 2026-10-06, the service ran one application task using revision `kartoush-demo-app:1`. ECS reported container health `HEALTHY`, deployment state `COMPLETED`, and steady state with zero failed tasks. Startup logs confirmed the RDS connection and successful application startup. See [provisioning evidence](../../infrastructure/aws/ecs/provisioning-evidence.json) for non-secret resource metadata and final shutdown state.

This validates the application-only service. It does not establish public HTTPS availability, proxy behavior, combined-task memory headroom, ECS Exec transcripts, or nightly lifecycle automation. Those acceptance checks remain in the linked follow-up tasks.

## AWS references

- [Fargate task networking](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/fargate-task-networking.html)
- [Deployment circuit breaker](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-circuit-breaker.html)
