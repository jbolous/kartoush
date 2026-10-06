# Managed runtime secrets

The demo uses the `kartoush` profile in `us-east-2`. Confirm the selected Region in AWS Settings > View all projects > Overview > Additional Info > Region before reproducing this setup.

## Application secret

`/kartoush/demo/app` contains the database username/password, internal admin username/password, and a stable base64-encoded 32-byte activation-email encryption key. Its current ARN is `arn:aws:secretsmanager:us-east-2:790873128308:secret:/kartoush/demo/app-giAB5K`. Values belong only in Secrets Manager; never put them in source, images, logs, task-definition environment values, or PR descriptions.

For a new installation, with Python and boto3 available, run:

```sh
python3 infrastructure/aws/secrets/create-app-secret.py --profile kartoush
```

The helper checks the project ID, generates random values, and prints only metadata. It refuses to replace an existing secret because replacing the encryption key would make queued encrypted payloads unreadable. The live secret already exists; do not recreate it. AWS-managed Secrets Manager encryption is used.

## Database bootstrap

The RDS-managed master secret stays separate and rotates every seven days. Normal application roles cannot read it. A disposable bootstrap execution role injects master credentials and the application password into a one-shot PostgreSQL client; the agent does not retrieve either password.

The artifacts in `infrastructure/aws/secrets/bootstrap/` describe that operation. Build the Linux amd64 image using its directory as the context, publish it to a temporary private repository, and replace the historical image digest in the task definition before reuse. Apply the execution policy with the existing ECS trust policy in `infrastructure/aws/iam/ecs-trust.json`. Run the task on the demo app subnet/security group with public egress and no inbound access. The task verifies the RDS hostname and CA, has no log driver, and must exit zero. Remove the bootstrap role, image repository, and task definition afterward.

`create-app-role.sql` creates `kartoush_app` with no superuser, role-management, database-management, replication, or RLS-bypass privileges. It grants connection, temporary-table, and schema-creation rights in the application database because this application runs Flyway and creates the `kartoush` and `jobrunr` schemas. It revokes public database access and public schema creation. This is a fresh-database bootstrap: it refuses an existing role and is not a password-rotation script. The live role has already been created.

## ECS injection and startup

`runtime-secret-fields.json` maps JSON keys to the existing application environment variables. `validation-task-definition.json` supplies these references through ECS `secrets`, using the application execution role. The application task role receives no Secrets Manager permissions. Fargate platform 1.4.0 or later is required for JSON-key injection.

Use the JDBC URL from the task definition, including `currentSchema=kartoush`, `sslmode=verify-full`, and the bundled Ohio CA path. Flyway targets `kartoush`; Hibernate must use that schema too. Credentials are supplied separately by ECS. The application binds to loopback for this startup check, with no public HTTP listener. Email uses the disabled noop provider until real provider credentials are configured. Logs use `/kartoush/demo/app` with seven-day retention.

`master-denial-task-definition.json` is a negative check: using the ordinary application execution role, injection of the RDS master password must fail with `AccessDeniedException` before the container starts. Never broaden that role to make this check pass.

The permanent service configuration belongs to #177. Provider credentials and the `/kartoush/demo/tls` certificate/private-key secret must be supplied when email and the HTTPS proxy are configured; no placeholder credentials or certificates were created. TLS remains tracked by #341.

## Validation evidence

On 2026-10-06, the bootstrap exited zero and the application reached ECS `HEALTHY` with all five managed fields. The master-secret negative check failed before startup as expected. See `infrastructure/aws/secrets/validation-evidence.json` for non-secret metadata. The one-shot compute was stopped after validation; the application secret, database, and seven-day log group are retained.

## Rotation and revocation

ECS reads secrets at task startup. Updating a secret does not update existing containers; start replacement tasks and verify health before stopping the old tasks.

For database password rotation, use a controlled privileged maintenance session to change `kartoush_app`'s password and update only the matching `db_password` field while preserving every other field, especially the encryption key. Coordinate the cutover: during the update window, newly starting tasks can fail authentication. Verify replacement tasks before completing maintenance. Automatic application-secret rotation is not configured.

Rotate internal admin credentials by updating their fields and replacing tasks. Rotate provider credentials with the provider, update their secret fields, and replace tasks. For TLS, replace the certificate/key together and restart the proxy. Keep the activation-email encryption key stable; key changes require a separate migration plan for outstanding encrypted jobs.

For compromise, stop affected tasks, revoke the compromised provider credentials where applicable, disable the database role's login and terminate its sessions through a privileged maintenance session, then provision replacement credentials. Removing IAM access alone does not remove secrets already injected into a running task. Preserve the encryption key until pending jobs have been migrated or explicitly discarded.

## Local development

The existing local environment and container workflow remain available without AWS access. Follow [the container guide](container-local-run.md) and keep local environment files untracked. Production uses ECS injection; it must not rely on development credential defaults.
