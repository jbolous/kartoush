# 0007 – Inventory Availability Mode

## Status
Proposed

## Context

Kartoush will manage inventory-related behavior as part of its core domain.
Before defining reservation ownership or checkout behavior, we need to be
explicit about what “inventory availability” means and what guarantees the
system provides.

Inventory availability influences:
- how and when items can be reserved
- what guarantees are made to callers
- how eventual consistency is handled
- how failures and contention are managed

This decision defines the availability model without committing to specific
persistence or infrastructure choices.

---

## Decision

Adopt a **derived availability model with authoritative stock levels**.

- **Stock levels are authoritative** and represent the system of record.
- **Availability is derived** from stock, reservations, and business rules.
- Availability is **eventually consistent** outside of reservation boundaries.
- Strong guarantees are provided **only when inventory is explicitly reserved**.

---

## What Availability Means

Inventory availability answers the question:

> “Can this item be reserved or purchased *right now*?”

Availability is not a stored value. It is computed based on:
- current stock on hand
- active reservations
- product-specific rules (for example, non-reservable items or backorders)

---

## Availability Guarantees

### Without a Reservation

- Availability is **best-effort and advisory**.
- Concurrent callers may observe slightly different availability values.
- Availability checks do not guarantee future success.

This mode supports browsing, search, and discovery use cases.

---

### With a Reservation

- Availability becomes **strongly consistent** for the reserved quantity.
- Once reserved, inventory is guaranteed to the owning workflow for the
  duration of the reservation.
- Other callers must observe reduced availability.

This mode supports checkout and transactional workflows.

---

## Consistency Model

- Availability calculations may be **eventually consistent** due to timing,
  concurrency, or asynchronous updates.
- Reservation creation is the boundary where **strong consistency is enforced**.
- The system must tolerate temporary over-reads but must prevent over-reservation.

---

## Special Cases

### Non-reservable Inventory

Some inventory may be marked as non-reservable:
- first-come, first-served items
- promotional or limited-time inventory

For these items:
- availability is advisory only
- reservations are not created
- checkout behavior must handle contention explicitly

---

### Backorderable Inventory

If backorders are supported:
- availability may be positive even when stock on hand is zero
- backorder limits and fulfillment timing are enforced separately
- reservations may represent future fulfillment rather than physical stock

---

## Consequences

- Availability checks alone are insufficient for transactional guarantees.
- Reservation logic becomes the central mechanism for enforcing inventory safety.
- Consumers of availability data must treat it as informational unless paired
  with a reservation.

These tradeoffs are intentional to balance performance, simplicity, and
correctness.

---

## Guardrails

- Do not store availability as a primary source of truth.
- Do not provide strong guarantees based on availability checks alone.
- Reservation creation is the only operation allowed to enforce strict inventory guarantees.
- Any deviation from this model must be explicitly documented.

---

## Notes

This decision intentionally defers:
- reservation ownership and lifecycle details
- persistence and locking strategies
- performance optimization techniques

These will be addressed in subsequent decisions.
