# ADR 0027: App-Layer vs Facade Request Model Boundary

## Status

Accepted

## Context

Kartoush currently uses `CreateCustomerRequest` and `UpdateCustomerRequest`
as shared request contracts between the `app` module and the customer facade.

That current design is workable, but it leaves several boundary questions
underspecified:

- whether public controllers should bind HTTP payloads directly to facade
  request models
- where request-to-facade mapping should occur if app-layer request DTOs are
  introduced
- whether HTTP-shape validation belongs in controllers, app-layer DTOs, facade
  validators, or some combination of those layers

Task `#233` clarified the current state by removing misleading controller
signals and documenting that the customer create and update request models are
shared API and facade contracts today.

Before task `#238` changes the implementation boundary, Kartoush needs an
explicit target decision for the longer-term split between:

- app-layer HTTP request models
- module facade request contracts

Without that decision, boundary refactors are likely to drift between
controller convenience, validation concerns, and module-boundary purity
without a stable target.

## Decision

Kartoush will treat app-layer HTTP request models and module facade request
contracts as separate concerns when a request boundary split is introduced.

The intended boundary is:

1. Public controllers bind HTTP requests into app-layer request DTOs owned by
   the `app` module.
2. Controllers are responsible for mapping app-layer request DTOs into module
   facade request contracts.
3. Module facades continue to define the public contract exposed by the owning
   module to other modules and to the `app` module.
4. Facade-layer validators remain responsible for business-oriented request
   validation and module-specific rules.
5. App-layer request DTOs may use Jakarta Bean Validation only for HTTP-shape
   concerns such as required fields, formatting, or simple structural
   constraints that are specific to request binding.

This means the controller boundary should eventually look like:

- HTTP JSON -> app DTO
- controller mapping -> facade request contract
- facade validator -> business and module rules
- facade -> domain/application behavior

## Validation Ownership

Validation responsibility is intentionally split by concern.

### App-layer DTO validation

App-layer request DTO validation is for HTTP-facing constraints such as:

- required JSON fields
- simple format or length constraints that are part of the external request
  shape
- request binding semantics that should fail before facade orchestration

This validation should use Jakarta Bean Validation only when those constraints
are actually expressed on the app-layer DTO.

### Facade-layer validation

Facade-layer validation remains responsible for:

- module-specific business rules
- cross-field checks that depend on module semantics
- validations that depend on current application state or supporting services
- validations that should apply regardless of the HTTP transport mechanism

Examples include:

- validating accepted Terms against the current canonical Terms version
- module-specific text and phone validation rules
- checks that depend on customer lifecycle or current module policy

## Rationale

### Keep HTTP concerns in the app layer

Controllers own HTTP binding, routing, and response composition. Binding
public JSON directly into facade-owned request contracts weakens the
distinction between transport concerns and module contracts.

Using app-layer DTOs keeps HTTP shape decisions in the `app` module, where
they belong.

### Preserve explicit facade contracts

Module facades should continue to expose intentional contract models rather
than leaking transport-specific DTOs or domain types. A separate app DTO does
not replace the facade request contract; it makes the conversion explicit.

### Keep validation at the correct abstraction level

Jakarta Bean Validation is useful for HTTP-shape validation, but it is not a
replacement for facade-level validation. The facade remains the right place for
business rules and module-specific checks that should hold regardless of the
calling transport.

### Make controller mapping responsibility explicit

If app DTOs and facade request contracts differ, the controller must own the
translation step. Hiding that boundary through shared request classes makes
the system look simpler than it really is and obscures where boundary changes
should happen.

## Consequences

### Positive

This decision:

- gives a clear target for request-boundary refactors
- keeps HTTP transport concerns in the `app` module
- preserves explicit module facade contracts
- makes validation ownership more coherent and reviewable

### Negative

This decision:

- introduces extra DTO and mapping code at the controller boundary
- may duplicate some field names across app-layer and facade-layer request
  types
- requires discipline to avoid duplicating business validation in the app
  layer

## Alternatives Considered

### Keep shared request models between app and facade

This was rejected as the target boundary because it keeps the current coupling
between HTTP binding and facade contracts and makes controller mapping
responsibility implicit rather than explicit.

### Move all validation into the controller/app layer

This was rejected because module-specific business rules should not be owned
by the HTTP transport layer.

### Move all validation into Jakarta Bean Validation annotations

This was rejected because several customer validations depend on module
semantics and current application state, which are not well represented as
annotation-only rules.

## Follow-Up

- Task `#238` should implement the customer create and update request split in
  line with this decision.

## Related Decisions

- ADR 0018: Module Boundaries and Dependencies
- ADR 0019: Internal vs External APIs
- ADR 0020: Use Dedicated Contract Models for Public Module Facades
