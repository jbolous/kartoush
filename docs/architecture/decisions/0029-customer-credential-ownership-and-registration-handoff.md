# ADR 0029: Customer credential ownership and registration handoff

## Status

Accepted

## Context

ADR 0028 established that authentication belongs in a dedicated auth boundary
and that customer activation remains in the customer lifecycle. It left one
important follow-up unanswered: how customer registration should hand off to
credential ownership once real authentication is introduced.

That question now matters because the current implementation still carries
temporary credential handling inside the customer module:

- `DefaultCustomerFacade` creates new customers with a hard-coded
  `TEMPORARY_PASSWORD_HASH`
- The customer domain model and persistence model both still require a
  `passwordHash`
- Registration currently creates a `PENDING` customer, persists Terms
  acceptance, and issues an activation token without a dedicated auth boundary

Without a clear decision, Kartoush would drift on a few related questions:

- Whether registration should create usable customer credentials immediately
- Whether auth should provision a customer-linked account during registration
  or only after activation
- Whether the customer module should remain responsible for any password or
  credential persistence
- Whether pending customers should be able to authenticate before activation
- How customer credentials stay separate from administrative identities

Task `#252` requires this handoff to be explicit before task `#253` introduces
the authentication module in code.

## Decision

Kartoush will use the following customer credential ownership and registration
handoff model:

1. Customer registration remains a customer-lifecycle operation, not a
   credential-creation operation
2. Registration creates the `PENDING` customer, records Terms acceptance, and
   issues the activation token, but it does not create a usable customer
   credential
3. Registration does not provision a customer auth account before activation
4. Customer credentials are created and stored only inside the dedicated auth
   boundary
5. The first usable customer credential is established through a dedicated
   post-activation credential setup flow rather than during registration
6. Customer authentication is allowed only for customers in the `ACTIVE`
   lifecycle state
7. Administrative identities remain outside this flow and are not modeled
   through customer registration
8. The current `passwordHash` field and temporary placeholder behavior in the
   customer module are transitional debt that should be removed as part of the
   auth foundation work

## Registration-To-Auth Handoff

The registration flow stays responsible for customer onboarding:

- Create the customer in `PENDING`
- Persist the Terms acceptance audit record
- Issue and deliver the activation token

The registration flow does not:

- Accept a password
- Provision a customer auth account
- Persist a credential hash
- Create an authenticated session
- Make the customer immediately able to sign in

After activation succeeds, the auth boundary becomes responsible for the next
step in onboarding:

- Establish the first customer credential through a dedicated setup flow
- Persist customer auth account and credential data
- Enable later sign-in using the configured auth mechanism

This keeps activation separate from credential ownership while still letting
the two flows work together cleanly.

## Ownership Model

The customer module remains the source of truth for:

- The customer record
- Customer lifecycle state
- Customer profile data
- Registration and activation behavior

The auth boundary becomes the source of truth for:

- Customer credential hashes or equivalent secret material
- Auth account state needed for sign-in
- Token issuance and token validation
- Credential setup and credential verification flows

The auth boundary can depend on customer identifiers and lifecycle state, but
the customer module should not own customer secrets.

## Lifecycle Implications

This decision implies the following access rules:

- `PENDING` customers cannot sign in
- `ACTIVE` customers may complete credential setup and later sign in
- Customers in non-active states cannot authenticate

This keeps credential ownership aligned with lifecycle state and avoids
accidentally allowing pre-activation sign-in.

## Boundary Contract Implications

The long-term contract between customer and auth should be minimal:

- Auth needs a stable customer identifier to associate credentials with a
  customer
- Auth needs a way to check customer lifecycle state before allowing sign-in
- Customer registration and activation should not require the customer module
  to know how credentials are stored

This means the current customer-domain dependency on `passwordHash` is a
temporary implementation artifact, not part of the intended module contract.

## Rationale

### Do not create usable credentials during registration

Registration currently creates a `PENDING` customer who has not yet completed
activation. Creating a usable credential at the same time would blur the line
between onboarding and authentication and make it easier for pre-activation
access rules to become inconsistent.

### Keep credentials entirely out of the customer boundary

Once a dedicated auth boundary exists, customer secrets should not remain on
the customer aggregate or customer table. Keeping password hashes in both
places would create split ownership and make future auth changes harder.

### Require a post-activation setup flow

If activation remains a customer-lifecycle concern, first-time credential
setup needs to be its own auth concern. That gives Kartoush a cleaner
onboarding sequence:

1. Register the customer
2. Activate the customer
3. Establish the first credential
4. Sign in

That sequence is clearer than overloading registration or activation with
credential ownership.

### Keep administrative identities separate

Administrative identities do not emerge from customer onboarding and should not
share this registration or credential lifecycle.

## Consequences

### Positive

This decision:

- Removes the need for customer-owned password placeholders in the target
  design
- Keeps registration and activation aligned with the customer lifecycle
- Gives the auth module a clean ownership boundary for customer credentials
- Prevents pending customers from gaining accidental sign-in capability
- Preserves the separate treatment of customer and administrative identities

### Negative

This decision:

- Requires an additional first-time credential setup flow before sign-in is
  fully usable
- Means the current customer model and schema need refactoring in task `#253`
- Introduces a dependency from customer sign-in behavior to customer lifecycle
  status checks
- Likely requires at least one follow-up task beyond basic sign-in to cover
  first-time credential setup explicitly

## Alternatives Considered

### Create customer credentials during registration

This was rejected because it couples registration to credential ownership too
early and makes pre-activation sign-in rules harder to control.

### Keep password hashes in the customer module and mirror them into auth later

This was rejected because it creates split ownership of customer secrets and
turns the auth boundary into a partial duplicate rather than the real source of
truth.

### Treat customer activation as the point where auth silently creates the first credential

This was rejected because activation is already defined as a customer-lifecycle
transition, not a credential-management step. Combining the two would muddy the
boundary just established in ADR 0028.

## Follow-Up

- Task [`#253`](https://github.com/jbolous/kartoush/issues/253) should remove the temporary customer-side credential placeholder
  path while introducing the auth module and its persistence model
- Task [`#254`](https://github.com/jbolous/kartoush/issues/254) should build on this handoff model rather than assuming
  registration already created a usable customer credential

## Related Decisions

- [ADR 0018: Module Boundaries and Dependencies](./0018-module-boundaries-and-dependencies.md)
- [ADR 0019: Internal vs External APIs](./0019-internal-vs-external-apis.md)
- [ADR 0024: Customer Activation Token Ownership and Lifecycle](./0024-customer-activation-token-ownership-and-lifecycle.md)
- [ADR 0026: Registration and Terms Acceptance Approach](./0026-registration-and-terms-acceptance-approach.md)
- [ADR 0028: Authentication and Authorization Strategy](./0028-authentication-and-authorization-strategy.md)
