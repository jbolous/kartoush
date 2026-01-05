# 0002 – Architecture Style

## Status
Proposed

## Context

Kartoush is in a pre-implementation phase. The project needs an architecture
style that supports deliberate, incremental development while keeping
complexity low as the domain is still forming.

The chosen style should:
- Enable clear ownership and boundaries
- Make change impact easy to reason about
- Avoid premature operational complexity
- Allow evolution if future requirements justify it

This decision establishes the high-level architectural approach that will guide
future decisions around module boundaries, data ownership, integration patterns,
and deployment assumptions.

---

## Decision

Adopt a **modular monolith** architecture as the initial style.

The system will start as a single deployable application, with strong internal
module boundaries that reflect domain responsibilities and ownership.

---

## What We Mean by Modular Monolith

In the context of Kartoush, a modular monolith means:

- A single deployable application in the initial phase
- Internally divided into well-defined modules with clear responsibilities
- Modules communicate through explicit interfaces, not shared implementation details
- Each module owns its data and controls how it is accessed
- Dependencies between modules are directional and intentional

This is not a loosely structured monolith. Module boundaries are treated as
architectural constraints, not informal conventions.

The intent is to preserve the simplicity of a single deployable system while
retaining many of the design benefits typically associated with
service-oriented architectures.

---

## Alternatives Considered

### Microservices from the start

**Pros**
- Independent deployment and scaling
- Clear service boundaries if the domain is already stable

**Cons**
- High operational complexity early (deployment, networking, observability)
- Distributed systems challenges before requirements are well understood
- Slower iteration for an early-stage project

---

### Fully monolithic, no modular boundaries

**Pros**
- Simplest possible starting point
- Minimal upfront structure

**Cons**
- Boundaries erode quickly
- Harder to refactor into clean modules later
- Increased risk of tightly coupled, hard-to-change code

---

### Serverless or event-first architecture

**Pros**
- Strong scalability primitives
- Encourages asynchronous and loosely coupled designs

**Cons**
- Pushes complexity into integration, testing, and debugging early
- Makes transactional boundaries and data ownership harder to reason about
- Not aligned with the current exploratory phase of the project

---

## Consequences

- Early work will emphasize defining and preserving module boundaries.
- Deployment, scaling, and service extraction decisions can be deferred until
  there is evidence they are needed.
- If a module becomes a clear candidate for separation, it should already have
  an internal contract that makes extraction feasible.

These tradeoffs are intentional and aligned with the project’s current goals.

---

## Guardrails

- Modules must avoid circular dependencies.
- Data ownership must be explicit. A module owns its data and defines access rules.
- Cross-module calls should be intentional and limited to well-defined interfaces.
- Modules should be structured so they could be extracted into services in the
  future with minimal refactoring, if justified.

---

## Notes

This decision may be revisited if concrete evidence shows that independent
deployment, scaling, or organizational constraints require a different
architecture style.
