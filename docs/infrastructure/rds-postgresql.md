# Demo PostgreSQL Database

Issue #174 provisions private RDS PostgreSQL in Ohio (`us-east-2`) for the demo VPC. The application and Flyway integration remain separate work. This database must not be used by the application with its master credentials.

## Configuration

The instance is `kartoush-demo-db`, with database `kartoush` and master username `kartoush_owner`. It uses PostgreSQL `16.15`, the latest available minor release in the same major version as local PostgreSQL and integration tests when checked on October 6, 2026 UTC. Newer major versions are available; this task preserves the validated major instead of introducing an untested upgrade. Recheck version availability and support before recreating it.

The project baseline takes precedence over general production defaults: Single-AZ `db.t4g.micro`, 20 GiB encrypted gp3, no storage autoscaling, one-day automated backups, no public access, and AWS-managed encryption keys. Extended Support enrollment is disabled. Deletion protection and minor-version upgrades are enabled. Performance Insights, enhanced monitoring, and database log exports are not enabled by this task; #179 owns the logging configuration.

The backup window is `03:00-03:30` UTC; maintenance is Sunday `04:00-04:30` UTC. The primary placement is `us-east-2a`. Both isolated database subnets are in the subnet group, satisfying the two-zone subnet requirement without enabling Multi-AZ database compute. The custom `postgres16` parameter group requires `rds.force_ssl=1`.

## Network Boundary

The subnet group uses `subnet-0306b9414236e9dd0` and `subnet-003110dfe19004153` in VPC `vpc-0f7b8f49822de6526`. Their route table contains only the local VPC route. Security group `sg-04aead1737f1d9ceb` permits TCP 5432 only from app group `sg-05304aaebf6d0ea53`. There are no database CIDR ingress rules and no public database address.

The app group permits PostgreSQL egress to the database group and HTTPS egress for image/secret retrieval. Local development does not connect directly to this private endpoint. Use the endpoint hostname, verified TLS, and a controlled client inside the VPC. Do not open public port 5432 for convenience.

## Credential Ownership

RDS generates and rotates the master password in Secrets Manager via `ManageMasterUserPassword=true`. No password is supplied in the creation request, repository, or operator command. The RDS-managed secret has an AWS-generated name rather than `/kartoush/demo/app`; it is the operator-only database master secret in the architecture's three-secret allowance. The application execution-role policy does not grant access to it.

The managed secret inherits the database's baseline inventory tags. Its AWS-generated name contains `!`, which AWS rejected as a tag value; the inherited `Name=kartoush-demo-db` is retained to identify the owning database. RDS rotates the master password every seven days. Only secret metadata is recorded. Do not retrieve the master secret into agent output or copy its password to task-definition plaintext. A controlled bootstrap must later create the separate application role and grant the permissions required by Flyway. #178 owns application-secret wiring, and #180 validates the application connection and migrations. Master credentials must not become the application's runtime connection.

## Cost Gate

The selected Ohio Region was verified from the `kartoush` profile and live identity. Confirm the project Region in AWS Settings > View all projects > Overview > Additional Info > Region before recreating resources. The project is on the active Free plan; `db.t4g.micro` PostgreSQL is supported on that plan. The monthly $50 budgets and service anomaly monitor were present before creation.

Live Ohio catalog rates checked on October 6, 2026 UTC were $0.016 per instance-hour and $0.115 per GiB-month for gp3. A script calculated $11.68 compute at 730 hours plus $2.30 for 20 GiB storage, or $13.98/month before Secrets Manager, backup overages, CPU surplus credits, taxes, and other environment resources. These rates match the accepted full-environment architecture estimate; promotional credits are not deducted from that estimate. See [the complete cost model](../architecture/aws-phase-1-target-architecture.md#complete-cost-model).

The master secret occupies the already-budgeted operator secret; do not create a duplicate. Review actual billing and spend status in AWS Settings > Billing. Free-plan credits do not remove the need to control resources or revisit tax and cash costs before a Paid-plan upgrade.

## Recreate from Reviewed Inputs

Inspect existing resources before running creation commands. These are create-once requests, not an idempotent reconciliation script. The files contain this project's subnet and security-group IDs; verify them against the private topology before use in another project.

```bash
set -euo pipefail
aws sts get-caller-identity --profile kartoush --region us-east-2
aws rds create-db-subnet-group --profile kartoush --region us-east-2 \
  --cli-input-json file://infrastructure/aws/rds/subnet-group.json
aws rds create-db-parameter-group --profile kartoush --region us-east-2 \
  --db-parameter-group-name kartoush-demo-postgres16-tls \
  --db-parameter-group-family postgres16 --description 'PostgreSQL 16 with required TLS' \
  --tags Key=Project,Value=kartoush Key=Environment,Value=demo Key=Name,Value=kartoush-demo-postgres16-tls
aws rds modify-db-parameter-group --profile kartoush --region us-east-2 \
  --cli-input-json file://infrastructure/aws/rds/tls-parameters.json
aws rds create-db-instance --profile kartoush --region us-east-2 \
  --cli-input-json file://infrastructure/aws/rds/create-db-instance.json
```

Wait for RDS to become available, then inspect the effective engine, encryption, network, backup, parameter, and secret metadata. Never assume a successful create request establishes availability or connectivity. `rds.force_ssl` must be `1` and the parameter group must be in sync before acceptance.

## Lifecycle

Stopping RDS saves instance compute while preserving storage and backup charges. RDS automatically restarts after at most seven stopped days. Stopping ECS does not stop RDS. Deletion protection blocks accidental deletion; removal requires a deliberate decision about final snapshots and retained backups. Do not delete this persistent database as part of a temporary validation-task cleanup. The follow-up lifecycle work must reconcile the seven-day restart behavior.

## VPC TLS Validation

The credential-free check runs the published Linux/x86 application image as a short-lived Fargate task in the public application subnet with the existing app security group. It has no application task role, injected secrets, application process, or service. It uses the existing execution role only to pull the private ECR image. The command starts PostgreSQL TLS negotiation and requires successful CA-chain and endpoint-hostname validation with the image's bundled Ohio CA certificates. It never authenticates a database user or runs SQL.

The validation cluster and task definition are disposable. A public address is required for image retrieval with this no-NAT topology; the task exposes no application listener. Container Insights is disabled. The task exits after the probe and releases its task network interface. Inspect exit status before cleaning up; a successful task launch is not a successful probe.

To repeat the check, review the pinned image digest, endpoint, subnet, and group in the JSON files first. Wait for the database to be available. Create the disposable cluster with `Project`, `Environment`, and `Name` tags, register the task definition, and select the exact returned revision rather than an unrelated newer revision:

```bash
set -euo pipefail
aws ecs create-cluster --profile kartoush --region us-east-2 \
  --cluster-name kartoush-demo-db-validation \
  --settings name=containerInsights,value=disabled \
  --tags key=Project,value=kartoush key=Environment,value=demo key=Name,value=kartoush-demo-db-validation
probe_definition=$(aws ecs register-task-definition --profile kartoush --region us-east-2 \
  --cli-input-json file://infrastructure/aws/rds/validation-task.json \
  --query taskDefinition.taskDefinitionArn --output text)
aws ecs run-task --profile kartoush --region us-east-2 \
  --cli-input-json file://infrastructure/aws/rds/run-validation-task.json \
  --task-definition "$probe_definition"
```

Save the task ARN, inspect `failures`, wait for it to stop, and inspect the container exit code and stopped reason. Exit code zero establishes a verified-TLS connection from the app network boundary, not SQL authorization or Flyway compatibility. After the task stops, deregister its exact validation revision and delete only the disposable validation cluster. Do not remove database resources or secrets. If the probe remains running, stop that specific task before removing the cluster. #178/#180 must separately verify the application role and SQL integration.

## Provisioning Evidence

On October 6, 2026 UTC, RDS reported the instance available with the expected PostgreSQL version, size, encrypted gp3 storage, private placement, one-day backups, deletion protection, and in-sync TLS parameter group. The Secrets Manager master secret was active with rotation enabled; only its metadata was read. IAM simulation returned `explicitDeny` for master-secret access by the application execution role. This is simulation evidence, not an attempted secret retrieval.

The Fargate probe pulled the exact application ECR digest through the execution role, connected from the app security group, and completed CA-chain and endpoint-hostname verification with exit code zero. ECS reported the task stopped and its network interface deleted. The temporary cluster was deleted and the validation task definition deregistered. [Provisioning evidence](../../infrastructure/aws/rds/provisioning-evidence.json) records the non-secret resource metadata and probe result. No database login, SQL, application startup, or Flyway migration was run.

The database and RDS-managed secret remain available for the next setup steps. Runtime application-role creation and secret wiring remain #178 work; application/Flyway validation remains #180. Request JSON, probe shell syntax, documentation editorial checks, and `git diff --check` passed. Java unit and integration suites were not rerun for this infrastructure-only change.
