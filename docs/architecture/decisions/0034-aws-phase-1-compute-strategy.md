# ADR 0034: AWS Phase 1 Compute Strategy

## Status

Accepted

## Context

Kartoush is a portfolio and development environment, not a service that needs
to run 24/7. Its normal state should be inactive, with deliberate manual
startup for development, testing, or demonstrations.

Task `#186` needs a compute decision before AWS architecture and deployment
work proceeds. The application is already moving toward containerizing the
Spring Boot modular monolith.

The [AWS Phase 1 cost preflight](../../infrastructure/aws-phase-1-cost-preflight.md)
from `#185` establishes two constraints:

- Normal recurring spend should target $30 per month or less
- The complete architecture must remain below $50 per month even if the
  runtime is accidentally left running 24/7

Nightly shutdown is an additional safeguard, not a requirement for making the
architecture affordable. Free Tier eligibility and promotional credits must
not be required to meet the ceiling.

The compute decision must support this lifecycle without independently
selecting the final ingress or network architecture owned by `#171`.

## Decision

Kartoush will use Amazon ECS with AWS Fargate for Phase 1 application compute.

Kartoush will apply the following rules:

1. The ECS service may remain defined while inactive, with desired count zero
   and no running Fargate tasks
2. Startup is a deliberate manual action that raises desired count to one
   after required dependencies are ready
3. Shutdown sets desired count back to zero and allows running tasks to stop
4. The application remains inactive until manually started again; deployment
   workflows and scaling policies must not implicitly reactivate it
5. One small application task is the initial direction, with no autoscaling or
   high-availability requirement
6. The complete architecture must satisfy the `#185` cost constraints before
   provisioning, including persistent services and networking

Stopping an individual task is not the shutdown model. An ECS service with
desired count one would replace it.

## Runtime Lifecycle

The later lifecycle task should notify the owner around 10:00 PM local time if
the runtime is still active. Around 10:15 PM, it should shut down the runtime
unless the owner explicitly chooses to keep it running.

There should be no scheduled morning restart or traffic-triggered startup.
After shutdown, desired count remains zero until the next manual startup.

This ADR establishes the lifecycle contract. Notification delivery, the local
time zone, keep-running controls, and automation implementation belong in the
downstream lifecycle task.

RDS PostgreSQL remains the database direction from the preflight. Stopping
application compute does not stop the database or remove persistent costs.
RDS can be stopped temporarily, but AWS automatically starts it after seven
consecutive days stopped. The broader lifecycle work must account for that
behavior rather than assuming the database stays stopped indefinitely.

For long inactive periods, that work should evaluate repeatable create/destroy
behavior where stopping alone leaves significant recurring cost. Database
retention, snapshots, and restoration must be considered separately from
disposable application tasks.

## Cost Comparison

The comparison uses the preflight's Ohio (`us-east-2`) baseline, one application
task or instance, low traffic, and a 730-hour planning month. Part-time use is
illustrated as 120 running hours per month, roughly four hours per day; it is
an estimate, not a requirement to run every day.

For Fargate, the planning candidate is Linux/x86 with 0.25 vCPU, 1 GB memory,
and the included 20 GB ephemeral storage. AWS's Ohio pricing catalog checked
on 2026-10-05 lists $0.04048 per vCPU-hour and $0.004445 per GB-hour. This gives
approximately $0.014565 per task-hour.

For EC2 and Beanstalk, a small Linux instance such as `t3.micro` is an
illustrative candidate at a planning rate of $0.0104 per hour. AWS publishes
that rate for Northern Virginia; the final Ohio instance rate and root-volume
cost must be verified if either alternative is revisited. These are rough
comparisons, not equivalent performance guarantees or final resource sizing.

| Option | Inactive application compute | Part-time compute, 120 hours | 24/7 compute, 730 hours |
| --- | --- | ---: | ---: |
| ECS Fargate | $0 with no running tasks | ~$1.75 | ~$10.63 |
| EC2 | $0 instance compute while stopped; EBS remains billable | ~$1.25 plus EBS | ~$7.59 plus EBS |
| Elastic Beanstalk | $0 instance compute after termination; retained resources remain billable | ~$1.25 plus underlying resources | ~$7.59 plus underlying resources |

Beanstalk has no additional service charge. Its estimate assumes a small
single-instance environment; load balancing and other resources would add
cost. Terminating and recreating an environment is the clearer inactive model
than manually stopping instances that Beanstalk manages.

These compute figures exclude RDS, networking, image retention, logging,
secrets, DNS, and data transfer. EC2 root EBS storage continues billing while
the instance is stopped. Fargate does not require a retained host root volume.

### Selected compute and shared infrastructure

Using the preflight's working estimates, RDS compute and storage total about
$14 per month if the database remains running. Adding a hosted zone and low
CloudWatch usage gives a shared baseline of approximately $14.50-15.50 before
small image, secret, backup, and transfer charges.

On that conservative database assumption, the Fargate candidate gives:

- Inactive application compute: approximately $14.50-15.50 in shared costs,
  plus the remaining persistent charges
- Part-time application compute: approximately $16.25-17.25, plus those charges
- 24/7 failure scenario: approximately $25-27 before optional networking and
  the remaining small charges, consistent with the preflight's core estimate

Temporary RDS stops can reduce database compute charges, but storage and
retained backups remain billable. RDS public IPv4 charges also continue while
a publicly accessible database is stopped.

ECR images, retained CloudWatch logs, secrets, DNS, snapshots, and any retained
network infrastructure must be included in the inactive cost model. Desired
count zero means no Fargate runtime charge, not a zero-cost AWS environment.

### Complete architecture cost boundary

The core estimate leaves room within the $30 normal target and $50 ceiling,
but it does not establish the final architecture's total cost.

Task `#171` must include every public IPv4 address and any continuously billed
ingress or networking resource. At $0.005 per address-hour, one address adds
about $0.60 for 120 hours or $3.65 for 730 hours. An ALB or NAT Gateway can
consume much of the remaining budget; neither is assumed to be required.

The final design must be recalculated after networking and the Region are
selected. Any expected normal spend above $30 needs an explicit rationale,
and a design reaching or exceeding $50 in the documented 24/7 scenario must
be rejected. Larger task sizing must trigger the same recalculation.
Successful nightly shutdown must not be needed to pass this check.

Billing alerts and anomaly detection from `#185` must be configured before
billable application infrastructure is provisioned.

## Rationale

ECS/Fargate offers a clean inactive compute model while retaining the service
definition. It avoids EC2 host provisioning, operating-system patching, and
host administration that are not primary learning objectives for Kartoush.

It also gives useful hands-on experience with containers, ECS, Fargate, ECR,
IAM, networking, CloudWatch, and later CI/CD integration. Those services fit
the existing direction toward a containerized Spring Boot application while
keeping the AWS infrastructure visible enough to learn from it.

RDS lifecycle limitations and persistent infrastructure costs affect all
three options. They require broader architecture and lifecycle decisions,
rather than being reasons to reject Fargate.

## Alternatives Considered

### Amazon EC2

EC2 is viable and potentially cheaper for application compute. An EBS-backed
instance supports deliberate manual startup and can remain stopped after a
nightly stop, while its root volume and other retained resources keep billing.

It provides direct infrastructure experience but adds operating-system,
patching, container-runtime, and host administration. That work is not a
primary learning objective, so the modest compute savings do not outweigh
Fargate's simpler runtime model. Burstable-instance CPU credit behavior would
also need consideration in a final cost and sizing assessment.

### AWS Elastic Beanstalk

Beanstalk is viable and potentially inexpensive with a single small instance.
It simplifies application deployment and has no additional platform fee, but
abstracts more of the AWS infrastructure that Kartoush aims to learn directly.

Manual environment creation and nightly termination could support part-time
use, followed by deliberate recreation. This is less natural than retaining
an ECS service at desired count zero, and requires careful handling of
retained databases, deployment artifacts, and environment configuration.

## Consequences

### Positive

This decision:

- Gives downstream architecture work a concrete container compute direction
- Eliminates application compute charges when no tasks are running
- Supports deliberate startup and a persistent inactive service definition
- Avoids host administration while preserving useful AWS learning experience
- Keeps the core 24/7 compute estimate compatible with the cost preflight

### Negative

This decision:

- Requires ECS task definitions, IAM roles, image delivery, and runtime wiring
- Introduces startup delay while dependencies and the application become ready
- Requires validation that the small task can run the Spring Boot application
- Leaves database lifecycle and persistent costs to broader architecture work
- Requires discipline so deployments do not reset an inactive service to one

## Out of Scope

- Final ingress, routing, TLS, and network architecture, owned by `#171`
- Final AWS Region, owned by `#187`
- Container build, deployment configuration, and CI/CD implementation
- Lifecycle notification and shutdown automation
- Database retention and restore implementation

## Follow-Up

- Task [`#171`](https://github.com/jbolous/kartoush/issues/171) should combine
  this compute decision with the `#185` constraints to define and cost the
  complete Phase 1 architecture
- Task [`#187`](https://github.com/jbolous/kartoush/issues/187) should select the
  Region and trigger cost recalculation if it differs from the Ohio baseline
- The later lifecycle task should implement manual startup, nightly
  notification and shutdown, and persistent-resource handling using this
  direction

## References

- [AWS Phase 1 cost and billing-safety preflight](../../infrastructure/aws-phase-1-cost-preflight.md)
- [AWS Fargate pricing](https://aws.amazon.com/fargate/pricing/)
- [AWS Ohio ECS pricing catalog](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonECS/current/us-east-2/index.json)
- [Amazon EC2 T3 pricing examples](https://aws.amazon.com/ec2/instance-types/t3/)
- [Stopping and starting EC2 instances](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Stop_Start.html)
- [AWS Elastic Beanstalk pricing](https://aws.amazon.com/elasticbeanstalk/pricing/)
- [Terminating an Elastic Beanstalk environment](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/using-features.terminating.html)
- [Stopping an RDS instance temporarily](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_StopInstance.html)
- [Amazon VPC pricing](https://aws.amazon.com/vpc/pricing/)
