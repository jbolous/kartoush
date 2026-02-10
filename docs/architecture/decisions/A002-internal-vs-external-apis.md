# Architecture ADR A002: Internal vs External APIs

## Status

Proposed

## Context

Kartoush exposes APIs that serve multiple purposes, including:

- Internal module-to-module communication
- First-party client applications
- Potential third-party integrations

Without clear distinctions, APIs intended for internal use can accidentally become relied upon by external consumers, creating:

- Unintended stability guarantees
- Risky breaking changes
- Tight coupling between modules and clients

Following:

- ADR 0015 (Headless and API Strategy)
- Architecture ADR A001 (Module Boundaries and Dependencies)

Kartoush must explicitly define what constitutes an **internal API** versus an **external API**, and how each is governed.

## Decision

Kartoush will distinguish between **internal APIs** and **external APIs**, with different stability, visibility, and compatibility expectations.

### Internal APIs

Internal APIs:

- Are intended for use by Kartoush modules only
- Support internal workflows and orchestration
- May evolve as the internal architecture changes

Characteristics:

- No backward compatibility guarantees
- Not documented for third-party use
- May change or be removed as modules evolve
- Accessed only through module boundaries defined in A001

Internal APIs must not be treated as stable integration points.

### External APIs

External APIs:

- Are intended for consumption by first-party clients and third parties
- Represent Kartoush’s public integration surface
- Require explicit stability and compatibility guarantees

Characteristics:

- Backward compatibility is expected
- Changes must be additive or versioned
- Clearly documented and intentionally exposed
- Governed as long-lived contracts

External APIs are considered part of the platform’s public interface and must be evolved deliberately.

### Visibility and enforcement

- Internal APIs must not be exposed unintentionally through public routing or documentation.
- External APIs must be explicitly identified as such.
- Module boundaries and facade rules defined in A001 apply to both internal and external APIs.

This decision defines **intent and governance**, not specific routing or tooling mechanisms.

## Alternatives Considered

### Treat all APIs as public

Expose all APIs equally and rely on documentation discipline.

- Pros:
  - Simple model
- Cons:
  - High risk of accidental coupling
  - Difficult to evolve APIs safely

### Version everything from day one

Require explicit versioning for all APIs, internal and external.

- Pros:
  - Strong compatibility guarantees
- Cons:
  - High overhead
  - Noise for internal-only APIs

### No explicit distinction

Rely on conventions and tribal knowledge.

- Pros:
  - Minimal upfront effort
- Cons:
  - Fails at scale
  - Increases risk of breaking consumers unintentionally

## Consequences

### Positive

- Clear expectations around API stability
- Reduced risk of accidental public contracts
- Cleaner separation between internal architecture and external integrations
- Aligns with modular monolith and headless strategy

### Negative

- Requires discipline to maintain distinctions
- Requires clear documentation and review practices
- Some refactoring effort as APIs evolve

## Follow-Ups

- Define API documentation and exposure conventions
- Decide how API versioning is applied to external APIs
- Align routing and security configuration with API classification

## Notes

This ADR intentionally avoids prescribing specific tooling or framework mechanisms. Internal vs external API classification is an architectural constraint and should be enforced through review, documentation, and evolving tooling as needed.
