# AWS Phase 1 Cost and Billing-Safety Preflight

This document records the cost constraints and billing-safety requirements for the
Kartoush AWS Phase 1 environment. It is the deliverable for #185 and an input to
the compute decision in #186.

## Goals

Kartoush is a portfolio and learning environment, not a production SaaS workload.
The AWS architecture should provide meaningful hands-on experience without
creating unnecessary recurring infrastructure cost.

Phase 1 uses these cost constraints:

- Target normal recurring spend: **$30 per month or less**
- Hard architecture ceiling: **$50 per month**
- Free Tier eligibility and promotional credits may reduce actual spend but must
  not be required to satisfy the architecture ceiling
- No billable Kartoush application infrastructure should be provisioned until
  billing alerts and anomaly detection are configured

## Workload Assumptions

The Phase 1 estimate assumes:

- one AWS environment
- very low portfolio/demo traffic
- one continuously running application instance or task when deployed
- one small Single-AZ PostgreSQL database
- approximately 20 GiB of database storage
- low container-image storage
- low log and metrics volume
- low outbound data transfer
- no autoscaling requirement
- no multi-region or high-availability requirement

These assumptions should be revisited if Kartoush becomes a regularly used public
application.

## Working Cost Model

The following values are planning estimates rather than billing guarantees.
Current AWS pricing must be verified when #186 selects the final compute and
network architecture.

| Component | Working monthly estimate | Notes |
| --- | ---: | --- |
| RDS PostgreSQL compute | ~$11.70 | Small Single-AZ instance such as db.t4g.micro |
| RDS storage | ~$2.30 | Approximately 20 GiB gp3 |
| Small Fargate task | ~$9-11 | Candidate only; #186 selects compute |
| Route 53 hosted zone | ~$0.50 | If Route 53 hosts the domain |
| CloudWatch | ~$0-1 initially | Assumes low log and metric volume |
| ECR | Negligible initially | Assumes a small number of retained images |
| Secrets/configuration | Low | Depends on service selected by the architecture |
| Core candidate total | **~$25-27** | Before optional continuously billed networking |

### Cost-Sensitive Infrastructure

Some conventional AWS components can dominate the cost of a small environment:

| Component | Approximate monthly impact | Concern |
| --- | ---: | --- |
| Application Load Balancer | ~$16+ | Continuous hourly charge plus usage |
| NAT Gateway | ~$33+ | Continuous hourly charge plus data processing |
| NAT public IPv4 address | ~$3.65 | Additional continuous IPv4 charge |

A design containing both an ALB and NAT Gateway can exceed the Phase 1 cost
target before meaningful application traffic exists.

Phase 1 should therefore avoid NAT Gateway and ALB unless #186/#171 establish
that their learning or architecture value justifies the additional recurring
cost while remaining below the $50 ceiling.

## Architecture Constraints

The Phase 1 architecture should:

- preserve RDS PostgreSQL unless the compute analysis finds a compelling reason
  not to; managed database experience is a deliberate learning objective
- avoid paying for production-scale availability that the portfolio workload
  does not require
- minimize continuously billed networking resources
- prefer usage-based or no-additional-charge alternatives where they preserve
  the AWS learning objectives
- document any decision that causes expected recurring spend to exceed $30
- reject a design expected to exceed $50 per month under the stated workload

## Billing Safety

Billing monitoring must be configured before the Kartoush application
environment is provisioned.

### AWS Budget Alerts

Configure actual and forecast budget notifications with these thresholds:

| Threshold | Action |
| --- | --- |
| $10 actual | Early warning; verify charges are expected |
| $20 actual | Review current month spend and major services |
| $30 actual | Normal monthly design target reached |
| $40 forecast | Investigate immediately before reaching the ceiling |
| $50 actual or forecast | Architecture ceiling reached; stop or remove unnecessary resources |

Budget notifications are safeguards, not a hard spending limit. Do not assume
AWS will automatically stop all services when a threshold is crossed.

Initial Phase 1 safety should favor notifications and deliberate teardown over
automatic destructive budget actions.

### Cost Anomaly Detection

Enable AWS Cost Anomaly Detection before application infrastructure is
provisioned.

Configure notifications low enough to identify unexpected portfolio-environment
spend early. Anomaly detection supplements the fixed budget thresholds; it does
not replace them.

## Teardown Checklist

When the environment is no longer needed, verify each applicable resource rather
than assuming stopping the application eliminates all charges.

Check and remove or intentionally retain:

- ECS services and running Fargate tasks, if selected
- EC2 instances, if selected
- RDS database instances
- retained RDS snapshots and backups
- Application Load Balancers and target groups, if selected
- NAT Gateways, if selected
- Elastic IP or other billable public IPv4 addresses
- ECR repositories and retained images
- Secrets Manager secrets, if selected
- CloudWatch log groups and retained logs
- Route 53 hosted zones and records
- other networking or storage resources introduced by #171

After teardown, review AWS billing/cost data to verify that no unexpected
Kartoush resources continue accruing charges.

## Decision for Downstream Work

Issue #186 must evaluate compute options against the following requirement:

> Select an AWS compute and deployment approach that provides meaningful,
> defensible AWS experience while targeting no more than $30 per month in normal
> recurring Phase 1 spend and remaining below a $50 monthly architecture ceiling.

The final estimate should be recalculated after #186 selects compute and #171
defines the complete Phase 1 network and deployment architecture.

## References

- AWS Fargate pricing: https://aws.amazon.com/fargate/pricing/
- Amazon RDS pricing: https://aws.amazon.com/rds/postgresql/pricing/
- Amazon VPC pricing: https://aws.amazon.com/vpc/pricing/
- Elastic Load Balancing pricing: https://aws.amazon.com/elasticloadbalancing/pricing/
- AWS Budgets: https://aws.amazon.com/aws-cost-management/aws-budgets/
- AWS Cost Anomaly Detection: https://aws.amazon.com/aws-cost-management/aws-cost-anomaly-detection/
