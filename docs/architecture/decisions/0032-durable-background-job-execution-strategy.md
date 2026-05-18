# ADR 0032: Durable Background Job Execution Strategy

## Status

Accepted

## Context

Kartoush now has customer-facing work that should not always be executed inline
with the request thread.

Recent implementation work introduced:

- A framework-free background job contract in `platform:platform-jobs`
- A JobRunr-backed adapter in `app`
- PostgreSQL-backed durable job persistence
- Transaction-aware after-commit scheduling
- Durable activation email scheduling

Those changes establish a real background-job direction, but the reasoning and
intended boundary are not yet captured as an architecture decision.

Kartoush needs a documented answer to these questions:

- When durable background jobs should be used
- When inline request handling should remain the default
- Why JobRunr was chosen instead of `@Async`, in-memory async execution, or a
  full message broker
- Where JobRunr is allowed in the codebase
- How job handlers should be structured
- What migration path exists if Kartoush later needs more than the current
  model provides

Without that decision, later work such as recurring cleanup jobs, additional
email jobs, and cloud deployment hardening would risk inconsistent scheduling
patterns and accidental framework leakage into application or domain code.

## Decision

Kartoush will use JobRunr as its initial durable background job mechanism.

Kartoush will apply the following rules:

1. Application code schedules background work through the shared
   `BackgroundJobScheduler` contract rather than directly through JobRunr
2. Background job request and handler contracts live in
   `platform:platform-jobs`
3. JobRunr wiring, serialization, and infrastructure-specific behavior live in
   `app`
4. Background jobs are for durable task execution, not for domain event
   streaming or cross-service messaging
5. Job scheduling that depends on committed database state must occur only
   after the surrounding transaction commits successfully
6. Durable background jobs should be used when the work is non-immediate,
   retryable, and operationally useful to persist across restarts
7. Inline execution should remain the default when the caller needs the result
   immediately as part of the request contract

## Intended Use

Durable background jobs are appropriate for work such as:

- Transactional email delivery
- Conservative recurring cleanup tasks
- Operationally retryable side effects that should survive process restarts

Durable background jobs are not the intended mechanism for:

- Domain event publication as an architectural pattern
- High-throughput stream processing
- Fan-out integration messaging across independent services
- Work whose result must be returned synchronously to the caller

## Boundary and Module Placement

The shared scheduling boundary is:

- `BackgroundJobScheduler`
- `JobRequest`
- `JobHandler`

Those contracts exist so application code can describe background work without
depending on JobRunr types.

The current placement is intentional:

- `platform:platform-jobs`
  - shared job contracts
- `app`
  - JobRunr adapter
  - transaction-aware scheduling wrapper
  - runtime configuration

Customer, auth, notification, and other application modules should not depend
directly on JobRunr APIs.

## Handler Conventions

Job handlers should follow a small set of rules:

1. A job request should contain only the data needed to run the job later
2. Job requests should prefer stable identifiers and intentionally persisted
   values over rich in-memory objects
3. A handler should reload current state when correctness depends on what is
   currently stored in the database
4. A handler should treat terminally invalid work conservatively instead of
   retrying forever
5. Provider-specific and infrastructure-specific details should stay behind
   existing internal boundaries

In practice, that means a job should usually carry things such as:

- customer identifiers
- scheduled timestamps
- intentionally persisted tokens or references when the value must survive
  retries

A job should usually not carry things such as:

- JPA entities
- request-scoped Spring objects
- rich in-memory aggregates captured directly from the request path

These rules keep jobs durable, replayable, and easier to evolve.

## Why JobRunr

JobRunr is a good fit for Kartoush at the current stage because it provides:

- Durable persistence using the existing PostgreSQL database
- Retry support for operationally useful background work
- A lightweight integration model inside a modular monolith
- A local dashboard that helps with development and operational inspection
- A simpler path than adopting a full broker-based architecture too early

Kartoush currently needs durable task execution more than it needs a fuller
distributed messaging platform.

## Alternatives Considered

### `@Async`

Spring `@Async` was rejected as the primary mechanism for this work because it
does not provide durable persistence by default.

If the process restarts or fails after the request returns, in-flight async
work can be lost.

That is not acceptable for workflows such as activation email delivery where
the work should survive application restarts.

### In-memory async execution

Simple in-memory executor usage was rejected for the same durability reason.

It may be acceptable for low-value fire-and-forget work, but it is not strong
enough for the customer-facing flows Kartoush has already introduced.

### Full message broker

A broker such as SQS, RabbitMQ, or Kafka was rejected for the current phase.

Those tools are valuable when the system needs:

- Higher throughput
- Cross-service messaging
- Independent worker fleets
- Richer event-driven integration patterns

Kartoush does not need that complexity yet.

Introducing a broker now would add infrastructure, operational burden, and
conceptual overhead before the current product scope justifies it.

## Relationship to Domain Events

JobRunr is not the chosen answer for domain event architecture.

Kartoush may still model important internal business events in code, but that
does not mean those events should automatically be published through JobRunr.

The current JobRunr boundary is for durable task execution.

If Kartoush later needs explicit event streaming, cross-service delivery
guarantees, or broader integration messaging semantics, that should be settled
through a separate decision rather than by stretching JobRunr beyond its
intended role.

## Transaction and Failure Direction

When scheduled work depends on a committed database state, job registration must
occur only after the surrounding transaction commits successfully.

Kartoush therefore uses a transaction-aware scheduling wrapper in `app`.

That avoids publishing durable background work for changes that later roll
back.

Post-commit scheduling failures should not be silently ignored.

The request path should surface those failures so operational problems are not
hidden behind apparently successful responses.

## Migration Direction

The current architecture is intentionally shaped so Kartoush can evolve later
without rewriting application code around JobRunr types.

If workload or architectural needs change, Kartoush can:

- Keep the `platform-jobs` boundary and replace the adapter
- Introduce a richer job transport or broker-backed implementation
- Split specific high-value workloads away from the current JobRunr model

This does not make future migration free, but it does reduce coupling by
keeping the scheduling contract JobRunr-agnostic today.

## Consequences

### Positive

This decision:

- Gives Kartoush a durable background-job mechanism without premature broker
  complexity
- Preserves a clean application boundary around scheduling
- Supports retryable customer-facing side effects such as activation email
  delivery
- Provides a clear home for recurring cleanup work
- Leaves a migration path open if the workload outgrows the current approach

### Negative

This decision:

- Adds framework and persistence complexity compared to plain inline execution
- Does not provide the semantics of a full event-streaming or brokered system
- Requires care around transaction boundaries, retries, and handler idempotence
- Can still lead to future migration work if Kartoush later needs a stronger
  distributed messaging model

## Related Decisions

- ADR 0002: Architecture Style
- ADR 0011: Layering and Facade Contracts
- ADR 0013: Logging and Observability
- ADR 0018: Module Boundaries and Dependencies
- ADR 0023: Testing Strategy and Conventions
- ADR 0031: Transactional Email Provider and Sending Domain Strategy
