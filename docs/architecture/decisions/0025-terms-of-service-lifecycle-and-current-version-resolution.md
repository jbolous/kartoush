# ADR 0025: Terms of Service lifecycle and current version resolution

## Status

Proposed

## Context

Kartoush currently enforces Terms of Service acceptance during customer
registration and persists an audit record of the accepted Terms version value
and acceptance timestamp.

That is sufficient for the current registration flow, but it is not enough to
support the next stage of the design:

- a canonical Terms of Service model with long-form content
- current and historical Terms retrieval through the API
- future-dated Terms activation
- consistent comparisons between accepted Terms and current Terms

Without explicit lifecycle rules, a future `terms_of_service` model would leave
several important questions ambiguous:

- what makes a Terms version current
- how a future Terms version is prepared without replacing the current one too
  early
- when a previous Terms version becomes historical
- whether Terms content can still change after it is made available to
  customers
- how registration should determine which Terms version it enforces

This ADR defines the intended lifecycle and resolution rules that future Terms
modeling and API work must follow.

## Decision

Kartoush will model Terms of Service as a canonical lifecycle-managed record
with the following states:

- `DRAFT`
- `SCHEDULED`
- `ACTIVE`
- `SUPERSEDED`

The current Terms version is the single Terms record in `ACTIVE` status.

Future Terms changes may be prepared in `DRAFT`, scheduled for future
activation using `SCHEDULED`, and promoted to `ACTIVE` only when their
effective time arrives. The previously active Terms record then becomes
`SUPERSEDED`.

Terms content is mutable only while a Terms record remains in `DRAFT`.
After a Terms record leaves `DRAFT`, its content is treated as immutable.

## Lifecycle States

### `DRAFT`

`DRAFT` is used for Terms content that is still being prepared and is not yet
eligible to be enforced during registration.

Characteristics:

- not used for registration validation
- not considered part of the public Terms history
- content may still be edited
- may later move to `SCHEDULED` or directly to `ACTIVE`

### `SCHEDULED`

`SCHEDULED` is used for Terms content that is approved for future activation
but must not replace the current Terms version until a specific effective time.

Characteristics:

- not yet enforced during registration
- has a required future `effectiveAt`
- content is immutable
- becomes the next active Terms version once its effective time arrives

### `ACTIVE`

`ACTIVE` is the single Terms version currently enforced during registration.

Characteristics:

- used as the source of truth for current Terms validation
- has a required `effectiveAt`
- content is immutable
- remains active until superseded by a newer Terms version

### `SUPERSEDED`

`SUPERSEDED` is a historical Terms version that was previously active but is no
longer the current version.

Characteristics:

- no longer enforced during registration
- remains available for historical lookup and acceptance comparisons
- has both `effectiveAt` and `supersededAt`
- content is immutable

## Current Terms Resolution

The current Terms version is resolved using lifecycle status, not by selecting
the latest version string or the most recent effective date.

Rule:

- current Terms = the single record in `ACTIVE` status

This rule is chosen because it keeps the meaning of "current" explicit and
avoids hidden date-based behavior.

Once a canonical Terms model exists, registration validation must use this
canonical lookup and must no longer rely on a hardcoded application value.

## Future Activation Behavior

Kartoush must support a future Terms version becoming active on a date in the
future without prematurely replacing the current active Terms version.

The intended flow is:

1. create or edit a Terms version in `DRAFT`
2. move it to `SCHEDULED` with a future `effectiveAt`
3. keep the current Terms version in `ACTIVE`
4. once the scheduled `effectiveAt` arrives:
   - the scheduled version becomes `ACTIVE`
   - the previously active version becomes `SUPERSEDED`
   - the previous version receives `supersededAt`

This transition must occur transactionally so that Kartoush never exposes two
simultaneously active Terms versions.

## Content Immutability

Terms content is editable only in `DRAFT`.

Once a Terms version moves to `SCHEDULED`, `ACTIVE`, or `SUPERSEDED`, the
following are treated as immutable:

- version identifier
- content
- content type

This protects the meaning of historical acceptance records and preserves the
ability to reason about what customers agreed to at a given point in time.

## Registration Behavior

Registration must validate acceptance against the current Terms version only.

Implications:

- `DRAFT` Terms versions are never valid for registration
- `SCHEDULED` Terms versions are not valid until they become `ACTIVE`
- `ACTIVE` is the only valid current Terms target during registration
- `SUPERSEDED` Terms versions remain relevant for historical acceptance review,
  not for new registration

## Acceptance History Relationship

Customer Terms acceptance records and canonical Terms records serve different
purposes.

Acceptance records answer:

- which Terms version a customer accepted
- when the customer accepted it

Canonical Terms records answer:

- what the Terms content was
- which Terms version was current at a given time
- how Terms versions progressed over time

Acceptance persistence may initially store the accepted Terms version value.
A later refinement may allow acceptance records to reference the canonical
Terms record directly if stronger referential integrity is needed.

## Consequences

### Positive

This decision:

- defines an explicit lifecycle for canonical Terms records
- supports future-dated activation without ambiguous "current version" logic
- protects historical Terms content from silent mutation
- gives registration and future APIs a clear source of truth

### Negative

This decision:

- introduces more lifecycle complexity than a single hardcoded version value
- requires explicit promotion logic when scheduled Terms become effective
- may require a follow-up migration if acceptance records later reference
  canonical Terms records directly

## Alternatives Considered

### Only track a version string and no canonical Terms record

This was rejected because it is too weak for historical retrieval, future API
support, and precise reasoning about the content customers accepted.

### Define current Terms by latest `effectiveAt`

This was rejected because it hides lifecycle intent in date ordering and makes
future scheduling harder to reason about operationally.

### Use only `ACTIVE` and `SUPERSEDED`

This was rejected because it does not provide a clear state for future-dated
Terms versions that are approved but not yet current.

## Follow-Ups

- implement the canonical `terms_of_service` persistence model
- replace the hardcoded current Terms source with canonical Terms lookup
- expose current and historical Terms metadata through the API

## Related Decisions

- ADR 0003: Data Ownership
- ADR 0011: Layering and Facade Contracts
- ADR 0018: Module Boundaries and Dependencies
- ADR 0019: Internal vs External APIs
- ADR 0024: Customer activation token ownership and lifecycle
