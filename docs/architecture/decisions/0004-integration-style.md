# Integration Style

## Status

Proposed

## Context

Kartoush is adopting a modular monolith architecture with explicit module
boundaries and module-owned data. The integration style between modules affects:

- coupling and change impact
- transaction boundaries and consistency guarantees
- testability and complexity
- the ability to evolve modules independently over time

This decision defines the default approach for module-to-module integration and
when to use synchronous calls versus asynchronous events.

---

## Decision

Adopt a **sync-first, event-enabled** integration style:

- The default integration between modules is synchronous calls through explicit
  module interfaces.
- Modules may publish domain events for significant state changes.
- Events are used to enable loose coupling, derived read models, and
  cross-module reactions where strong consistency is not required.

This keeps the system simple early while leaving room for more decoupled
patterns as the domain evolves.

---

## Synchronous Integration

Use synchronous calls when:

- The caller needs an immediate result to proceed
- The workflow requires strong consistency
- The operation is part of a single user-facing request flow
- The caller is coordinating a multi-step workflow across modules

Rules:

- Calls must go through explicit interfaces, not shared implementation details.
- The caller should not reach into another module’s persistence model.

---

## Asynchronous Integration (Events)

Use events when:

- Other modules need to react to a state change without blocking the original flow
- Eventual consistency is acceptable
- Building or updating published read models
- Audit, notification, or indexing style concerns

Rules:

- Events represent facts that already happened (past tense).
- Events should be stable contracts and versioned if they must change.
- Consumers should be able to process events idempotently.

---

## Cross-Module Workflows

Default approach:

- A single module acts as the workflow coordinator.
- The coordinator calls other modules synchronously through their interfaces.
- Other modules enforce their invariants and apply changes to their own data.

Events may be emitted after state changes to allow other modules to update read
models or perform side effects without coupling the coordinator to those concerns.

---

## Consistency Expectations

- Synchronous calls are used for operations requiring strong consistency.
- Events provide eventual consistency between modules.
- Published read models may lag behind write models and should be treated as
  eventually consistent.

---

## Consequences

- The system stays simple to reason about early.
- Events add complexity (delivery, ordering, retries) and should be used
  intentionally.
- Some cross-module concerns can be handled without growing synchronous coupling.

---

## Guardrails

- Do not use events to implement request-response workflows.
- Do not require multiple modules to participate in a single database
  transaction.
- Event consumers must handle duplicates safely (idempotent processing).
- Event payloads should avoid leaking internal persistence models.

---

## Notes

This decision may be refined once concrete infrastructure is chosen for event
delivery, but the sync-first, event-enabled approach should remain stable.
