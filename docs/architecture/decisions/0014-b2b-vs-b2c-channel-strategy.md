# B2B vs B2C Channel Strategy

## Status

Proposed

## Context

Kartoush is intended to support both business-to-consumer (B2C) and business-to-business (B2B) commerce use cases.

While these channels share a common set of core capabilities (catalog, inventory, carts, orders, payments), they differ meaningfully in areas such as:

- Customer and account modeling
- Pricing and discounting
- Catalog visibility
- Checkout workflows
- Payment terms and authorization rules

A clear strategy is required to determine how B2B and B2C coexist within the platform, and how much logic is shared versus specialized.

This decision must balance:

- Reuse of core domain logic
- Flexibility for channel-specific behavior
- Long-term maintainability and extensibility

## Decision

Kartoush will be implemented as a **single platform with a shared commerce core**, extended by **channel-specific behavior for B2B and B2C** where required.

Key aspects of this decision:

- Core domains such as catalog, inventory, cart, order, and payment are shared
- Channel-specific rules (pricing, checkout constraints, authorization) are implemented as extensions or strategies, not forks
- B2B and B2C are treated as first-class channels, but not separate platforms
- The platform explicitly avoids duplicating domain models across channels

This approach favors reuse and consistency while still allowing meaningful differentiation where business requirements demand it.

## Alternatives Considered

### Separate B2B and B2C Applications

Two distinct applications or services for B2B and B2C.

- Pros:
  - Clear isolation
  - Simplified mental model per channel
- Cons:
  - Duplication of core logic
  - Higher maintenance cost
  - Increased risk of behavioral drift

### Single Platform with Feature Flags Only

One platform with all behavior controlled via flags.

- Pros:
  - Maximum reuse
  - Minimal structural complexity
- Cons:
  - Harder to reason about behavior
  - Feature flags can become implicit architecture
  - Risk of fragile condition-based logic

### Fully Modular Channel Services

Channel-specific services layered on top of a shared infrastructure.

- Pros:
  - Clear separation of concerns
- Cons:
  - Increased complexity early
  - Premature service boundaries

## Consequences

### Positive

- High reuse of core commerce logic
- Clear extension points for channel-specific behavior
- Easier to evolve platform incrementally
- Alignes well with a modular monolith architecture

### Negative

- Requires discipline to prevent channel logic leakage
- Extension mechanisms must be carefully designed
- Some complexity is pushed into configuration and strategy selection

## Follow-Up Decisions

- Headless and API strategy (ADR 0015)
- Authorization and identity modeling
- Pricing and promotion extensibility

## Notes

This decision establishes the *structural relationship* between B2B and B2C, but intentionally defers specific implementation details to future ADRs and tasks.
