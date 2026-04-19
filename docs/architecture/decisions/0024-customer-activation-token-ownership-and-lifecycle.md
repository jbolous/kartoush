# ADR 0024: Customer activation token ownership and lifecycle

Date: 2026-04-18

## Status

Accepted

## Context

Kartoush now supports customer activation and activation token resend flows.
That introduces several architectural questions:

- which module should own activation tokens and activation rules
- how resend should affect previously issued tokens
- whether activation belongs in customer lifecycle management or in a future
  authentication module
- how much token behavior should be exposed through the public API

These decisions affect lifecycle consistency, module boundaries, and how the
system evolves once authentication capabilities become more sophisticated.

## Decision

Activation tokens remain owned by the customer module and are treated as part
of the customer lifecycle rather than as part of authentication.

The following rules apply:

1. customer activation is modeled as the transition from `PENDING` to `ACTIVE`
2. activation tokens are owned by the customer module because they exist only
   to govern that lifecycle transition
3. activation token validation requires the token to belong to the target
   customer, be unexpired, and be unconsumed
4. successful activation consumes the token and activates the customer in the
   same service flow
5. resend is allowed only while the customer is still `PENDING`
6. resend invalidates prior outstanding activation tokens by consuming them
   before issuing a new token
7. the public API does not expose token hashes or token management operations

## Why this remains in the customer module

Activation is not currently an authentication concern in Kartoush.

The purpose of the activation token is to determine whether a newly created
customer has completed the transition into an active lifecycle state. That is
closer to customer onboarding and customer status management than to login,
session management, credential verification, or authorization.

Moving this behavior into a future authentication module too early would blur
the current module boundary and force the authentication module to own
customer lifecycle transitions that it does not yet need to control.

## Resend and invalidation strategy

Kartoush uses a supersession strategy for resend flows.

When resend is requested for a pending customer:

- any currently active activation token for that customer is consumed
- a new activation token is issued
- only the newest token remains valid for activation

This strategy was chosen because it is simpler and safer than keeping multiple
concurrently valid activation tokens.

## Consequences

### Positive

This decision:

- keeps activation behavior aligned with customer lifecycle ownership
- avoids premature coupling to a future authentication module
- ensures resend behavior is deterministic and easy to reason about
- reduces the risk of multiple valid activation tokens existing at once

### Negative

This decision:

- keeps token delivery concerns temporarily adjacent to customer lifecycle work
- may require a future ADR if authentication responsibilities expand enough to
  justify moving parts of the flow
- does not yet provide a production-grade delivery mechanism beyond the
  configured email service abstraction

## Alternatives considered

### Move activation immediately into a dedicated authentication module

This was rejected because the current activation flow is about onboarding a
customer into the `ACTIVE` lifecycle state, not about authenticating an
existing customer.

### Keep multiple resend tokens valid until one is used

This was rejected because it increases state complexity and makes it harder to
reason about which token should be considered authoritative.

### Expose raw activation token behavior directly through the public API

This was rejected because tokens are externally usable secrets and should not
become part of the public customer contract except where explicitly required.

## Related Decisions

- ADR 0003: Data Ownership
- ADR 0011: Layering and Facade Contracts
- ADR 0018: Module Boundaries and Dependencies
- ADR 0019: Internal vs External APIs
