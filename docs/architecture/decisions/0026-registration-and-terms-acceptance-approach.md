# ADR 0026: Registration and Terms acceptance approach

## Status

Accepted

## Context

Kartoush registration creates customers in a `PENDING` lifecycle state and
requires explicit Terms of Service acceptance as part of onboarding.

With canonical Terms lifecycle management now in place, the registration flow
must make three things explicit:

- why Terms acceptance is enforced during registration instead of after account
  creation
- how the current Terms version is determined at registration time
- why Terms acceptance is persisted separately from the customer record

Without this decision, the registration flow would remain underspecified in a
few important ways:

- whether a customer can exist without agreeing to the current Terms
- whether registration should rely on a hardcoded Terms version or on the
  canonical Terms model
- whether Terms acceptance belongs on the customer row or in a separate audit
  record

This ADR defines the intended registration and Terms acceptance approach for
the current stage of the design.

## Decision

Kartoush will enforce Terms of Service acceptance during registration.

A registration request is valid only when:

- Terms acceptance is explicitly `true`
- the submitted Terms version matches the single canonical Terms record
  currently in `ACTIVE` status

Successful registration will:

- create the customer in `PENDING`
- persist a separate Terms acceptance audit record
- issue an activation token for subsequent account activation

If registration validation fails, Kartoush will not create:

- a customer record
- a Terms acceptance record
- an activation token

After customer and Terms acceptance persistence succeeds, downstream activation
token issuance or delivery may still fail independently. In that case the API
request may fail after the customer and Terms acceptance records have already
been committed.

## Current Terms Version Strategy

Registration does not use a hardcoded Terms version value.

Instead, the current Terms version is resolved from the canonical
`terms_of_service` model using the current `ACTIVE` Terms record.

Implications:

- `DRAFT` Terms are never valid for registration
- `SCHEDULED` Terms are not valid until they become `ACTIVE`
- `SUPERSEDED` Terms remain historically relevant but are not valid for new
  registrations

This keeps registration aligned with the canonical Terms lifecycle rather than
relying on application constants.

## Terms Acceptance Persistence Strategy

Kartoush persists Terms acceptance separately from the customer record.

The acceptance audit record stores:

- the customer association
- the accepted Terms version value
- the acceptance timestamp

This data is not stored as mutable state on the customer itself.

## Rationale

### Enforce Terms at registration

Kartoush requires Terms acceptance before a customer can complete onboarding.

This ensures:

- registration has a clear contractual precondition
- the system does not create partially registered customers who have not
  agreed to the current Terms
- future onboarding behavior remains explicit and testable

### Use canonical current Terms lookup

Resolving the current Terms version from the canonical Terms model avoids
duplicating source-of-truth logic in the registration flow.

This ensures:

- registration tracks the same current Terms used elsewhere in the system
- future Terms scheduling and activation behavior naturally affects
  registration
- registration behavior remains aligned with the documented Terms lifecycle

### Persist acceptance separately

A separate Terms acceptance record is preferred over adding acceptance fields
to the customer row because acceptance is historical audit data, not current
customer profile state.

This separation:

- keeps customer lifecycle state distinct from legal acceptance history
- allows multiple acceptance records if the model later evolves to support
  re-acceptance scenarios
- makes acceptance retrieval explicit instead of hiding it in customer state

At the current stage, the acceptance record stores the accepted Terms version
value rather than a foreign key to the canonical Terms row. This keeps the
write path simple while still preserving a durable audit value, assuming Terms
version identifiers remain immutable once a Terms record leaves `DRAFT`.

## Consequences

### Positive

This decision:

- makes registration requirements explicit
- ties registration to the canonical current Terms source of truth
- preserves Terms acceptance as auditable history
- keeps customer state and acceptance history separate

### Negative

This decision:

- requires an additional persistence write during registration
- relies on Terms version immutability for acceptance audit strength
- may require a future refinement if acceptance records need a direct foreign
  key to canonical Terms records

## Alternatives Considered

### Store Terms acceptance fields on the customer row

This was rejected because it mixes historical acceptance data with current
customer state and makes future acceptance history harder to model cleanly.

### Keep registration tied to a hardcoded Terms version

This was rejected because canonical Terms lifecycle management already exists,
and registration should not maintain a separate source of truth.

### Allow customer creation before Terms acceptance

This was rejected because it weakens the onboarding contract and introduces
partially registered customer states without clear business value.

## Related Decisions

- ADR 0011: Layering and Facade Contracts
- ADR 0018: Module Boundaries and Dependencies
- ADR 0019: Internal vs External APIs
- ADR 0025: Terms of Service Lifecycle and Current Version Resolution
