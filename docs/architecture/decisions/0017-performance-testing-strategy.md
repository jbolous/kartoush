# Performance Testing Strategy

## Status

Proposed

## Context

Performance characteristics and scalability assumptions must be validated, not assumed.

Following the decision to explicitly define system behavior under load and spike conditions (ADR 0016), Kartoush requires a consistent and repeatable approach to performance testing that:

- Validates throughput and latency expectations
- Exercises spike and burst scenarios
- Detects regressions as the system evolves

Performance testing is treated as a **design validation tool**, not only a pre-release safety check.

This ADR builds on the following prior decisions:

- [ADR 0016: Order throughput and spike handling](./0016-order-throughput-and-spike-handling.md)
- [ADR 0015: Headless and API Strategy](./0015-headless-and-api-strategy.md)

## Decision

Kartoush will adopt a **scenario-driven performance testing strategy** with the following characteristics:

1. Performance tests focus on **critical workflows**, starting with order placement.
2. Tests are designed to model **realistic traffic patterns**, including burst and sustained load.
3. Performance testing is performed:
   - During development of high-impact changes
   - Before major architectural or behavioral changes
4. Performance tests define **explicit success criteria**, including acceptable latency, error rates, and throughput.

JMeter will be used as the initial load testing tool due to its flexibility, maturity, and suitability for HTTP-based APIs.

This decision does not require performance tests to run on every CI build, but it establishes them as a first-class artifact of the system design.

## Alternatives Considered

### Manual or ad-hoc testing

Rely on developers to run occasional local tests without formal scenarios.

- Pros:
  - Low setup cost
- Cons:
  - Inconsistent coverage
  - Poor repeatability
  - Easy to skip under time pressure

### Always-on CI performance testing

Run performance tests on every pull request.

- Pros:
  - Strong regression detection
- Cons:
  - Expensive and slow
  - Difficult to maintain stable baselines
  - High friction early in the project

### Infrastructure-level load testing only

Test only production-like environments.

- Pros:
  - High fidelity
- Cons:
  - Late feedback
  - Harder to isolate causes
  - Slower iteration cycles

## Consequences

### Positive

- Performance expectations are explicit and testable
- System behavior under load is validated early
- Supports ADR 0016 assumptions with empirical data
- Encourages performance-aware design decisions

### Negative

- Requires maintenance of test scenarios
- Baselines must be revisited as the system evolves
- Performance results may vary across environments

## Follow-Up Decisions

- Definition of performance success thresholds
- Test environment standardization
- Integration of performance testing into CI/CD (if and when justified)

## Notes

Performance testing scenarios are expected to evolve alongside the system. This ADR establishes *how* and *when* performance is validated, not fixed numerical targets.
