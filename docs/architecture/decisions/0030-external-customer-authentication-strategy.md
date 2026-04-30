# ADR 0030: External customer authentication strategy

## Status

Accepted

## Context

Kartoush now has a working customer password flow:

- Post-activation password setup
- Password-based sign-in
- Password reset
- Opaque bearer tokens backed by server-side auth sessions

That closes the first auth slice, but it leaves one important follow-up
question open: how Kartoush should support external customer authentication
providers such as Google, Facebook, or Apple without reworking the core auth
model later.

This matters now for a few reasons:

- ADR 0028 already established that opaque Kartoush-issued bearer tokens are
  the application session mechanism
- ADR 0029 established that credential ownership belongs in the auth boundary
  rather than the customer module
- The current password-based implementation should not become an accidental
  assumption that password is the only long-term customer auth method
- External authentication introduces account-linking and identity-mapping
  questions that are easy to get wrong if left implicit

Task `#271` needs a strategy that is clear enough to guide future
provider-specific work without forcing implementation details too early.

This ADR is meant to set the direction now. The actual provider work can still
be deferred until the rest of the auth and API security work is further along.

## Decision

Kartoush will treat external customer authentication as an additional
authentication method inside the auth boundary, not as a replacement for the
existing customer and session model.

The strategy is:

1. External provider authentication is a customer auth method, not a separate
   customer identity system
2. Kartoush customer identity remains the application identity after external
   authentication succeeds
3. Kartoush will continue issuing its own opaque bearer tokens after
   successful external authentication
4. External provider identity links will be stored inside the auth boundary
   and associated with an existing Kartoush customer identifier
5. One customer may support multiple authentication methods over time,
   including password and multiple external providers
6. External provider email claims are supporting signals, not the primary
   identity key
7. Existing customer accounts must not be silently linked to an external
   provider based only on matching email
8. Customer lifecycle rules still apply: only `ACTIVE` customers may receive
   authenticated Kartoush sessions
9. The first provider-specific implementation should focus on existing-customer
   sign-in and account linking before provider-led customer registration

## Identity Mapping Model

Kartoush remains the source of truth for customer identity.

External authentication providers can prove who the user is, but they do not
replace the internal customer model.

The identity mapping is:

- Kartoush customer
  - Internal customer identifier
  - Customer lifecycle state
  - Customer profile and business ownership
- External provider identity
  - Provider name
  - Provider subject or stable provider user identifier
  - Optional provider email and related metadata
  - Link to one Kartoush customer identifier

This means the auth boundary should treat the `(provider, provider-subject)`
pair as the stable external identity key, not provider email.

Provider email may still be useful for:

- Initial account discovery
- User-facing linking flows
- Operational support and audit context

Email alone is not enough to prove account ownership.

## Account-Linking Strategy

Kartoush will allow one customer to authenticate through more than one method
over time.

Examples:

- Password only
- Google only
- Password plus Google
- Password plus Google plus Apple

The linking rules are intentionally conservative:

- If a provider identity is already linked to a Kartoush customer, Kartoush
  may authenticate that customer through the provider flow
- If a provider identity is not linked yet, Kartoush must not automatically
  attach it to an existing customer account based only on matching email
- Linking an external provider to an existing customer should require an
  explicit linking flow with additional proof, such as:
  - the customer already being signed in to Kartoush
  - or a separate verified claim flow that proves account ownership

This avoids turning a loose email match into accidental account takeover.

## Coexistence With Password Authentication

Password authentication remains a first-class customer auth method.

External authentication should coexist with it rather than replace it by
default.

Implications:

- A customer may have a password and one or more linked external providers
- A customer may later add a password after first using an external provider
- A customer may later add an external provider after first using a password
- Auth-session issuance and downstream protected-route behavior should not need
  to care which upstream auth method succeeded

The auth boundary should therefore separate three concerns:

- Upstream authentication method verification
- Kartoush customer identity resolution
- Kartoush bearer-token issuance

## Session Strategy

External authentication does not change the Kartoush session model.

After a provider flow succeeds and the customer is resolved:

- Kartoush issues its own opaque bearer token
- Kartoush persists its own auth session state
- Downstream API protection continues to rely on Kartoush session validation,
  not direct provider tokens

This keeps provider-based sign-in consistent with the existing password-based
auth model established in ADR 0028.

## Lifecycle Rules

External authentication does not bypass customer lifecycle rules.

The same lifecycle constraints apply:

- `PENDING` customers cannot receive authenticated sessions
- `ACTIVE` customers may authenticate through supported methods
- Non-active customers cannot receive authenticated sessions

This keeps lifecycle ownership in the customer boundary while letting auth
enforce the same eligibility rules across methods.

## Initial Scope Constraint

The first provider-specific implementation should not try to solve every
external-auth scenario at once.

Kartoush should start with:

- One provider at a time
- Existing-customer sign-in through a linked provider identity
- Explicit account linking for existing customers

Kartoush should defer:

- Automatic customer creation from external provider sign-in
- Multiple-provider rollout in the same task
- Rich profile synchronization from provider claims

That keeps the first implementation aligned with the current customer
registration and activation model instead of quietly replacing it.

## Out of Scope

This ADR does not settle:

- Which provider should be implemented first
- OAuth or OpenID Connect library choices
- Controller, callback, or redirect mechanics for a specific client platform
- Whether later external-auth onboarding should create new customers directly
- How much provider profile data should be stored locally
- Fine-grained authorization rules after sign-in

Those concerns belong in later provider-specific design and implementation
tasks, not in this ADR.

## Rationale

### Keep Kartoush as the identity owner

External providers can authenticate a user, but Kartoush still needs its own
customer identity, lifecycle rules, and session model.

Treating the provider as the application identity source would blur module
ownership and make future provider changes harder.

### Do not auto-link by email alone

Matching email is convenient, but it is not strong enough proof of account
ownership.

Email addresses can be:

- Reused
- Changed at the provider
- Shared across systems in unexpected ways

Conservative linking rules reduce the risk of accidental or unsafe account
linking.

### Preserve one downstream auth model

Kartoush already chose opaque bearer tokens backed by server-side state.
Keeping that model after external authentication means protected-route logic
and session handling stay consistent across auth methods.

### Allow multiple auth methods per customer

Customers should not be forced into a single permanent auth method.

Supporting multiple methods keeps the system flexible and avoids avoidable
migration pain if a customer starts with password auth and later wants Google
sign-in, or the other way around.

## Consequences

### Positive

This decision:

- Keeps external auth aligned with the existing auth boundary
- Avoids coupling downstream API protection to provider-specific tokens
- Preserves conservative account-linking rules
- Allows password and provider-based auth to coexist cleanly
- Gives later provider-specific work a clear architectural target

### Negative

This decision:

- Requires explicit account-link records and related persistence work
- Defers the convenience of silent email-based account linking
- Leaves customer-creation-through-provider for later work
- Adds another auth-method concept that later code must model carefully

## Alternatives Considered

### Replace Kartoush session issuance with provider tokens

This was rejected because it would cut across ADR 0028 and make downstream API
protection dependent on provider-specific token semantics rather than Kartoush
session state.

### Use provider email as the primary identity key

This was rejected because email is not a strong enough durable identifier for
safe account linking on its own.

### Force customers to choose either password or external auth, but not both

This was rejected because it creates unnecessary migration friction and treats
auth method as a permanent account characteristic rather than a customer
capability.

## Follow-Up

- Task [`#274`](https://github.com/jbolous/kartoush/issues/274) should keep transactional email delivery behind provider-agnostic
  boundaries that future linking or verification flows can also use.
- Task [`#275`](https://github.com/jbolous/kartoush/issues/275) may influence future provider rollout if domain, email, or trust
  considerations affect account-linking or verification flows.
- Future provider-specific implementation tasks should decide which provider is
  first, how linking is initiated, and whether external-auth onboarding may
  create new customers directly

## Related Decisions

- [ADR 0015: Headless and API Strategy](./0015-headless-and-api-strategy.md)
- [ADR 0018: Module Boundaries and Dependencies](./0018-module-boundaries-and-dependencies.md)
- [ADR 0019: Internal vs External APIs](./0019-internal-vs-external-apis.md)
- [ADR 0028: Authentication and authorization strategy](./0028-authentication-and-authorization-strategy.md)
- [ADR 0029: Customer credential ownership and registration handoff](./0029-customer-credential-ownership-and-registration-handoff.md)
