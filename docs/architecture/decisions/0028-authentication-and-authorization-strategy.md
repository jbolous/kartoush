# ADR 0028: Authentication and authorization strategy

## Status

Accepted

## Context

Kartoush has reached the point where authentication and authorization can no
longer remain implied future work.

Several parts of the current system already point at the need for an explicit
auth strategy:

- ADR 0015 identifies authentication and authorization as a required
  follow-up to the API-first, headless-ready platform direction.
- ADR 0019 distinguishes internal and external APIs, but it does not yet
  define how those surfaces are protected.
- ADR 0024 explicitly states that customer activation is not an
  authentication concern and should remain in the customer lifecycle.
- The customer module still carries a temporary `passwordHash` placeholder,
  which is a boundary smell rather than an intended steady-state design.
- Internal management endpoints now exist and need a clear security model
  instead of relying on internal-only exposure indefinitely.

Without a concrete decision, Kartoush would risk drifting across several
important questions:

- Whether first-party clients should use browser-style sessions, JWTs, or a
  different token approach
- Whether customer identities should also be reused for administrative access
- Where credential ownership belongs relative to the customer module
- How public, authenticated, and administrative API routes are expected to
  differ

Task `#251` requires an initial architecture decision that is concrete enough
to guide implementation tasks without overcommitting the system to a full
identity platform too early.

## Decision

Kartoush will adopt an initial authentication and authorization strategy with
the following characteristics:

1. Customer authentication and administrative authentication are modeled as
   separate identity categories.
2. Customer identities are not reused for administrative API access.
3. Credentials and authentication behavior belong in a dedicated
   authentication boundary, not in the customer module long term.
4. The initial API authentication mechanism will use opaque bearer tokens
   backed by server-side authentication state rather than self-contained JWTs.
5. The authentication strategy must support distinct public, customer-authenticated,
   and administrative access modes, while route classification and enforcement are
   defined separately.
6. Customer activation remains outside the authentication boundary and
   continues to belong to the customer lifecycle per ADR 0024.

## Identity Model

Kartoush will recognize at least two authenticated identity categories in the initial
design:

- **Customer identities**
  - Used for customer-facing authentication flows
  - Represent authenticated customer access to customer-facing protected API
    operations
- **Administrative identities**
  - Used for internal or backoffice management access
  - Represent authenticated access to administrative HTTP surfaces

This split is required even if Kartoush does not yet introduce a full employee
or backoffice domain model.

In other words, Kartoush needs a distinct administrative identity concept
immediately, but it does not need a rich employee domain until business
requirements justify one.

## Token Strategy

The first auth implementation will use **opaque bearer tokens** backed by
server-side authentication state.

Implications:

- Clients authenticate using credentials and receive a bearer token
- The bearer token is presented on subsequent authenticated API requests
- The server remains authoritative for token validity, revocation, and
  identity resolution
- Token contents are not treated as a distributed source of truth for
  identity or authorization state

Kartoush intentionally chooses this instead of self-contained JWTs for the
first implementation.

## Access Modes

The auth strategy must support three distinct access modes:

- **Public access**
  - Requests that intentionally proceed without an authenticated identity
- **Authenticated customer access**
  - Requests that require a customer identity
- **Authenticated administrative access**
  - Requests that require an administrative identity

This ADR does not define which endpoints belong in each mode or how those
classifications are enforced. That policy belongs to the API security work.

## Boundary Ownership

The authentication boundary will own:

- Credential verification
- Token issuance and validation
- Identity resolution
- Auth-session or token persistence

The customer module will continue to own:

- Customer lifecycle state
- Activation-token lifecycle
- Customer profile data

This means customer lifecycle behavior may interact with future auth
capabilities, but the customer module should not remain the long-term owner of
credential storage or authentication behavior. The registration-to-auth handoff
is left for follow-up design work.

## Out of Scope

This ADR does not settle:

- Endpoint-by-endpoint public or protected route classification
- Spring Security configuration or request-filter design
- Token expiration, rotation, logout, or per-device session semantics
- Detailed registration and credential-creation workflow behavior
- Whether administrative identities eventually require a richer employee or
  backoffice domain model

## Rationale

### Use separate customer and administrative identities

Customer identities and administrative access represent different concerns.

Keeping them separate:

- Avoids conflating public commerce identities with internal operational access
- Prevents future authorization rules from being shaped around an awkward
  “customer who is also an admin” shortcut
- Leaves room for a richer administrative model later without forcing that
  complexity into the first auth slice

### Prefer opaque bearer tokens over JWTs initially

Kartoush is API-first and headless-ready, so a token-based approach fits the
platform direction better than browser-centric server sessions as the primary
model.

At the same time, Kartoush does not yet need the operational and semantic
complexity that comes with JWTs, such as:

- Token claim design as a long-lived contract
- Revocation and invalidation tradeoffs
- Accidental duplication of authorization state into token contents

Opaque bearer tokens keep the client contract simple while preserving
server-side authority over current authentication state.

### Keep auth ownership out of the customer module

The temporary password placeholder in the customer module is a useful signal:
credential ownership is not settled correctly today.

Leaving credential behavior in the customer module long term would blur module
responsibilities and undermine the dedicated authentication boundary that the
system now needs.

### Keep activation outside auth

Customer activation is still part of onboarding and lifecycle transition, not
credential verification or session management.

Preserving ADR 0024 avoids turning the new authentication boundary into a
catch-all for any token-shaped behavior.

## Consequences

### Positive

This decision:

- Gives implementation tasks a concrete first auth direction
- Keeps the client-facing auth model compatible with an API-first platform
- Avoids premature JWT and full identity-platform complexity
- Preserves a clean distinction between customer and administrative access
- Keeps customer activation aligned with the customer lifecycle

### Negative

This decision:

- Requires server-side token persistence and lookup
- Introduces another module boundary that must be designed carefully
- Does not yet provide a full permission model beyond identity categories
- May require a later ADR if Kartoush adopts richer administrative identity or
  fine-grained authorization needs

## Alternatives Considered

### Use self-contained JWTs from the start

This was rejected for the first implementation because it adds token-contract
and revocation complexity before Kartoush has established stable auth
boundaries and authorization needs.

### Use server-managed browser sessions as the primary auth model

This was rejected as the primary strategy because Kartoush is intentionally
API-first and headless-ready. Browser-session mechanics may still be layered on
later for specific clients, but they should not define the core auth model.

### Move activation into the auth module now

This was rejected because ADR 0024 already establishes that activation is a
customer lifecycle concern, not an authentication concern.

## Follow-Up

- Task [`#252`](https://github.com/jbolous/kartoush/issues/252) should define how customer credential ownership and registration
  interact with this auth boundary.
- Task [`#253`](https://github.com/jbolous/kartoush/issues/253) should introduce the dedicated authentication module and initial
  persistence foundation.
- Task [`#254`](https://github.com/jbolous/kartoush/issues/254) should implement the first customer sign-in flow using opaque
  bearer tokens.
- Task [`#257`](https://github.com/jbolous/kartoush/issues/257) and its child tasks should define endpoint classification,
  protection rules, and enforcement behavior using this auth strategy as input.

## Related Decisions

- [ADR 0015: Headless and API Strategy](./0015-headless-and-api-strategy.md)
- [ADR 0018: Module Boundaries and Dependencies](./0018-module-boundaries-and-dependencies.md)
- [ADR 0019: Internal vs External APIs](./0019-internal-vs-external-apis.md)
- [ADR 0024: Customer Activation Token Ownership and Lifecycle](./0024-customer-activation-token-ownership-and-lifecycle.md)
- [ADR 0026: Registration and Terms Acceptance Approach](./0026-registration-and-terms-acceptance-approach.md)
