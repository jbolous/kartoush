# Headless and API Strategy

## Status

Proposed

## Context

Kartoush provides commerce capabilities that may be consumed by multiple types of clients, including:

- First-party web or mobile user interfaces
- Internal administrative tools
- Potential third-party integrations or partners

Following the decision to support both B2B and B2C channels within a shared platform (ADR 0014), a clear strategy is required for how functionality is exposed and consumed.

Specifically, Kartoush must decide whether it is:

- A headless-first platform
- A traditional storefront-driven application
- A hybrid system that supports both approaches

This decision impacts controller design, API contracts, authentication flows, and long-term extensibility.

## Decision

Kartoush will adopt an **API-first, headless-ready strategy**, while remaining **agnostic to specific UI implementations**.

Key aspects of this decision:

- Core business capabilities are exposed via well-defined APIs
- APIs are treated as first-class interfaces, not internal conveniences
- The platform does not assume a specific frontend technology or deployment model
- Kartoush does not mandate a standalone storefront as part of the core platform

This approach allows Kartoush to support multiple channels and clients without coupling core domain logic to presentation concerns.

## Alternatives Considered

### Headless-Only Platform

Kartoush would expose APIs exclusively, with all UIs implemented externally.

- Pros:
  - Maximum flexibility for clients
  - Clear separation of concerns
- Cons:
  - Increased integration complexity early
  - Less guidance for first-party UI implementations

### Traditional Server-Rendered Storefront

Kartoush would include a primary, server-rendered UI.

- Pros:
  - Simpler initial user experience
  - Fewer moving parts early
- Cons:
  - Tighter coupling between UI and domain logic
  - Harder to support multiple channels and clients

### Hybrid with First-Class Storefront

Kartoush would include a default storefront while also exposing APIs.

- Pros:
  - Easier onboarding
  - Concrete reference implementation
- Cons:
  - Risk of API becoming secondary
  - Increased maintenance burden

## Consequences

### Positive

- Clear separation between domain logic and presentation
- Enables multiple UI implementations (B2B, B2C, admin)
- Supports future integrations without refactoring core logic
- Aligns with modular monolith and facade-based layering decisions

### Negative

- Requires disciplined API design
- Increases upfront emphasis on contracts and documentation
- Some UI concerns must be deferred or handled externally

## Follow-Up Decisions

- Internal vs external API boundaries (Architecture ADR A002)
- Authentication and authorization strategy
- API versioning and compatibility guarantees

## Notes

This decision intentionally avoids committing to a specific frontend implementation. It establishes APIs as the primary integration surface while allowing UI strategies to evolve independently.
