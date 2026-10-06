# AWS Phase 1 Access Model

Implementation record for #172, checked on 2026-10-05 using profile `kartoush` and selected Region `us-east-2`. Project identifier: `790873128308`.

## Human access

The current operator session uses AWS-managed `arn:aws:iam::790873128308:role/managed/AccountFullAccessRole`, trusted by `account-access.amazonaws.com` and carrying AWS-managed `AdministratorAccess`. AWS Settings manages project owners and team members. Keep this managed role unchanged; no additional administrator IAM user, human role, or long-lived access key was created.

This is bootstrap administrator access, not a least-privilege deployment role. Restrict any future deployment automation to the demo resources, scope `iam:PassRole` to the required execution/task roles and `ecs-tasks.amazonaws.com`, and exclude human access management. Phase 2 owns CI/CD credentials. See the [target architecture](../architecture/aws-phase-1-target-architecture.md).

## Runtime access

| Role | Permissions |
| --- | --- |
| `kartoush-demo-app-execution` | Authenticate to ECR, pull only `kartoush-demo-app` and `kartoush-demo-proxy`, write streams in `/kartoush/demo/app` and `/kartoush/demo/proxy`, inject `/kartoush/demo/app` and `/kartoush/demo/tls` secrets |
| `kartoush-demo-app-task` | ECS Exec Session Manager channels, discover log groups, and write audit streams in `/kartoush/demo/exec` |

Both roles were created with `Project=kartoush`, `Environment=demo`, and their role name as the `Name` tag. Each has one inline policy with the same name as its role. Neither has an attached managed policy.

Both trust only `ecs-tasks.amazonaws.com`, with `aws:SourceAccount=790873128308` and `aws:SourceArn=arn:aws:ecs:us-east-2:790873128308:*`. AWS does not support narrowing ECS task-role source ARN to one cluster; see [ECS task IAM roles](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html).

The policies are stored in [infrastructure/aws/iam](../../infrastructure/aws/iam/). They are project-specific artifacts, not portable templates. ECR authorization, Session Manager channels, and log-group discovery require wildcard resources; all other permissions use named resources. Secret ARN patterns allow exactly the six-character Secrets Manager suffix. The execution role does not grant access to `/kartoush/demo/db-master`; the task role grants no secret retrieval, image access, infrastructure administration, or certificate/DNS management.

Follow-up resource provisioning must use these names or update the policies to exact provisioned ARNs. Log groups must be created separately with seven-day retention. Secrets use AWS-managed encryption keys; a customer-managed key would require a separate scoped KMS policy change. The database maintenance task requires its own execution role before use and must not reuse the normal application execution role for master credentials.

Use the execution-role ARN as `executionRoleArn` and task-role ARN as `taskRoleArn` when wiring ECS in #177. The historical `kartoush-app-role` name in #172 is superseded by the two distinct roles selected in #171/#187.

## Security and plan evidence

- Plan: `FREE`, `ACTIVE`; the project has not upgraded to Paid. No billable runtime resources were created for this task.
- IAM summary: `AccountPasswordPresent=0`, `AccountAccessKeysPresent=0`, `AccountSigningCertificatesPresent=0`, `AccountMFAEnabled=0`, and zero IAM users.
- The owner confirmed that Google 2-Step Verification is On and has been enabled since December 17, 2014. This is owner-reported identity-provider MFA evidence, not an AWS root-MFA result. Root credentials are absent; the legacy root-MFA criterion is represented by MFA on the Google identity used for project access. Do not create root credentials merely to satisfy the legacy issue wording. See [AWS sign-in user types](https://docs.aws.amazon.com/signin/latest/userguide/user-types-list.html) and [root access best practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/root-user-best-practices.html).
- The CLI profile confirms Ohio. The owner should also confirm AWS Settings > View all projects > Overview > Additional Info > Region.
- Both identity policies passed AWS IAM Access Analyzer validation with zero findings. Live policy readback confirmed the saved documents.
- The principal-policy simulator returned an unexpected placeholder-resource `explicitDeny`; a custom-policy simulation returned `InvalidInput`. These results do not establish application/TLS access or an effective master-secret denial. Effective access also depends on AWS-managed SCPs/RCPs. Validate actual image pulls, secret injection, and ECS Exec transcript delivery during the owning deployment tasks before operational acceptance.

## Remaining acceptance

Owner sign-in MFA is confirmed. The legacy root-MFA criterion is adapted to the managed project experience as described above. The AWS-managed administrator role and the two scoped runtime roles are present; the access model is documented here. AWS Settings Region confirmation remains pending; the CLI profile confirms `us-east-2`. Runtime access validation belongs to the subsequent deployment tasks.

The two IAM roles incur no runtime usage charge and are intended for subsequent Phase 1 work. They can be removed if this environment is abandoned.
