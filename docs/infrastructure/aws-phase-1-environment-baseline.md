# AWS Phase 1 Environment Baseline

This document defines the minimal environment baseline for #187 and provides an input to the target architecture in #171.

## Region

Use **US East (Ohio), `us-east-2`**, for Phase 1 regional resources.

Ohio matches the pricing baseline used by the [cost preflight](./aws-phase-1-cost-preflight.md) and [compute decision](../architecture/decisions/0034-aws-phase-1-compute-strategy.md). Kartoush has no current availability or data-residency requirement that justifies changing that baseline or adding another Region.

Global services and service-specific certificate requirements may use their required locations. They do not introduce another application environment.

## Environment Scope

Phase 1 has one AWS environment named `demo`.

It supports portfolio demonstrations and development validation. It is not a 24/7 production service. Local development and automated tests remain outside this AWS environment; Phase 1 does not add separate AWS development, staging, or production environments.

Application compute is normally inactive and starts deliberately, following ADR 0034. The AWS environment name does not select a Spring profile: the deployed application uses the production runtime configuration where required for secure credentials and provider behavior.

## Resource Naming

Use `kartoush-demo-<purpose>` for names and `Name` tags where supported.

Examples include:

- `kartoush-demo-vpc`
- `kartoush-demo-app` for the ECS service and task-definition family
- `kartoush-demo-db` for the RDS instance
- `kartoush-demo-app-execution` for the ECS execution role
- `kartoush-demo-app-task` for the application task role

Use `/kartoush/demo/<purpose>` for hierarchical configuration paths and CloudWatch log groups. Add an Availability Zone suffix to subnet and routing names when a resource exists in more than one zone.

Names must comply with each AWS service's character and length restrictions. Use an account or Region suffix only where uniqueness requirements demand it. Do not include issue numbers, credentials, or personal information in names.

## Minimal Tags

Apply these tags where the service supports resource tagging:

| Tag | Value | Purpose |
| --- | --- | --- |
| `Project` | `kartoush` | Identify project resources |
| `Environment` | `demo` | Identify the single Phase 1 environment |
| `Name` | Resource name | Make resources recognizable in the console |

Activate the project and environment tags for cost allocation when configuring billing visibility. Tags support inventory and cost reporting; they do not grant access or implement runtime shutdown behavior.

## Implementation Boundary

This baseline selects the Region, environment scope, names, and minimal tags. The target architecture in #171 selects services, networking, and access flows. No AWS resources are provisioned by this documentation task.
