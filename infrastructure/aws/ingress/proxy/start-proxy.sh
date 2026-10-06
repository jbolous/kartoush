#!/usr/bin/env bash
set -euo pipefail
# Let the app finish its measured cold start before opening the HTTPS listener.
deadline=$((SECONDS + 300))
until curl --fail --silent --max-time 4 http://127.0.0.1:8080/actuator/health --output /dev/null; do
  if (( SECONDS >= deadline )); then
    echo 'Application readiness timed out; HTTPS listener remains closed' >&2
    exit 1
  fi
  sleep 5
done
exec nginx -g 'daemon off;'
