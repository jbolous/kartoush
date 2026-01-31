# Architecture ADR A001: Module Boundaries and Dependencies

## Status

Proposed

## Context

Kartoush is implemented as a modular monolith, with explicit module boundaries and enforced dependency constraints. Preserving modularity requires clear rules for:

- What modules exist and what they own
- Which modules may depend on which others
- How cross-module communication occurs

Without formal boundaries, modular systems tend to drift into a shared "ball of mud" where:

- Domain concepts are duplicated across modules
- Dependencies become bidirectional
- Refactoring becomes expensive
- Future extraction into services becomes difficult

Kartoush already defines layering and facade contracts as a general rule (ADR 0011). This architecture decision focuses specifically on **module-level boundaries and dependency constraints**.

Related decisions:

- [ADR 0002: Architecture style](../../decisions/0002-architecture-style.md)
- [ADR 0003: Data ownership](../../decisions/0003-data-ownership.md)
- [ADR 0011: Layering and facade contracts](../../decisions/0011-layering-and-facade-contracts.md)
- [ADR 0015: Headless and API Strategy](../../decisions/0015-headless-and-api-strategy.md)

## Decision

Kartoush will define explicit modules with clear ownership and enforce a **one-directional dependency graph**.

### Core rules

1. Each module owns its domain model, persistence, and internal services.
2. Cross-module calls must occur through **published interfaces** (for example, facades or ports explicitly owned by the providing module).
3. Modules may not directly access another module’s persistence or internal implementation types.
4. Dependencies between modules must remain **acyclic**. Cycles are treated as architectural violations and must be addressed, not worked around.

### Dependency rules

A module may depend on:

- Shared platform modules (for example, logging or configuration utilities)
- **Platform types**, only if explicitly defined and intentionally scoped
- Published interfaces (facades or ports) of another module

A module may not depend on:

- Another module’s internal packages
- Another module’s persistence layer
- Another module’s implementation classes that are not part of a published contract

### Platform types

Platform types are a **small, intentionally defined set of low-level types** that are safe to share across modules.

Examples may include:

- Strongly typed identifiers
- Small, immutable value types (for example, money or quantity)
- Basic enums or constants with cross-module meaning

Platform types:

- Must not contain business rules
- Must not depend on any domain module
- Must not depend on frameworks (for example, Spring or JPA)
- Should change infrequently and require coordination when modified

There is **no implicit platform-types module**. If one exists, it must be explicitly created and documented.

#### Persistence and serialization guidance

Platform types may be used in JPA entities; however, mapping must be handled in the owning module (for example, using JPA `@Converter` implementations located in that module). Platform types themselves must remain framework-free and should not include persistence annotations.

### Cross-module communication patterns

Kartoush supports two primary patterns for cross-module interaction:

1. **Synchronous calls via facades or ports**
   - Used for request-response workflows where immediate results are required
2. **Event-driven communication**
   - Used where eventual consistency is acceptable
   - Events are owned and published by the producing module and treated as stable contracts
   - Event-driven communication occurs **within the modular monolith**, and does not imply distributed messaging infrastructure

The default approach is synchronous communication via facades or ports. Event-driven communication is used selectively to reduce coupling where appropriate.

## Alternatives Considered

### No formal module boundaries

Allow modules to directly call each other freely.

- Pros:
  - Fast early development
- Cons:
  - Architectural drift
  - Tight coupling and dependency cycles
  - Poor long-term maintainability

### Strict layered-only architecture

Enforce layers but allow dependencies across modules freely.

- Pros:
  - Simpler conceptual model
- Cons:
  - Does not prevent module coupling
  - Ownership remains unclear

### Event-driven only communication

Require all cross-module interactions to occur via events.

- Pros:
  - Strong decoupling
- Cons:
  - Higher complexity
  - Harder debugging and testing
  - Not suitable for all workflows

## Consequences

### Positive

- Clear ownership and responsibility boundaries
- Reduced risk of dependency cycles
- Improved testability and maintainability
- Keeps future service extraction feasible

### Negative

- Requires discipline and enforcement
- Some workflows require additional interface design
- Platform types require careful governance

## Follow-Ups

- Define the initial module list and responsibilities
- Decide how dependency rules are enforced (tooling vs code review)
- Architecture ADR A002: Internal vs External APIs

## Notes

This ADR defines architectural constraints, not implementation mechanics. Violations of module boundaries or dependency rules should be treated as architectural defects and resolved deliberately, not deferred.
