# Layering and Facade Contracts

## Status
Proposed

## Context

Kartoush is adopting a modular monolith architecture with module-owned data and
a sync-first, event-enabled integration style. To preserve module boundaries and
prevent business logic from drifting into transport or persistence layers, we
need explicit rules for layering and module-to-module communication.

This decision defines:
- where business logic lives
- what a module facade is responsible for
- how modules are allowed to communicate

---

## Decision

Adopt a layered module structure with a facade as the public contract for each
module:

- Each module exposes one or more facades in an `api` package.
- Controllers and other modules interact with a module only through its facade.
- Business logic lives in application and domain layers, not in controllers,
  facades, or persistence components.

---

## Layering Model

Each module should follow these layers:

1. Transport layer
   - REST controllers, event handlers, schedulers
   - Responsibilities: input parsing, request validation at the edge, translating
     domain outcomes to transport responses
   - No business rules

2. Facade layer
   - Module contract and boundary
   - Responsibilities: enforce module API shape, delegate to application services,
     map inputs and outputs to module DTOs
   - Minimal logic only (validation of required fields and simple mapping)

3. Application layer
   - Use-case orchestration
   - Responsibilities: coordinate domain services, manage workflow steps, call
     other modules through their facades

4. Domain layer
   - Business rules and invariants
   - Responsibilities: enforce policies such as reservation rules, availability
     semantics, and conversion rules

5. Persistence and adapters
   - Repositories, database mapping, external clients
   - Responsibilities: I/O and mapping only
   - No business rules beyond persistence concerns

---

## Where Business Logic Lives

- Domain rules live in domain services and domain models.
- Workflow logic lives in application services.
- Controllers and facades remain thin and contain no business rules.

---

## Module-to-Module Communication Rules

### Allowed

1. Synchronous calls through facades (default)
   - A module may call another module only through the target module’s facade
     interfaces and DTOs.

2. Asynchronous events (when justified)
   - Modules may publish domain events for significant state changes.
   - Events are used for side effects and derived read models where strong
     consistency is not required.

3. Published read models (pragmatic reads)
   - Cross-module reads may be served from published read models that are
     explicitly intended for consumption.
   - Consumers must treat published read models as read-only.

### Not Allowed

- Direct access to another module’s persistence layer, repositories, or tables.
- Shared mutable domain objects across modules.
- Calling another module’s internal services directly.
- Using events for request-response workflows.

---

## Dependency Rules

- `apps/api` depends on module facades, not module internals.
- A module may depend on another module’s `api` package only.
- Module internals must not be referenced outside the module.

---

## Consequences

- Module boundaries are enforceable through code review and build structure.
- Refactoring module internals becomes safer because external callers depend
  only on facade contracts.
- Additional mapping code may exist, but it is intentional to preserve boundaries.

---

## Guardrails

- Facades must remain thin and must not accumulate business logic.
- All cross-module calls must go through facades.
- Any exception to these rules must be explicitly documented.
