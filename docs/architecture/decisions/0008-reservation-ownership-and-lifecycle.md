# Reservation Ownership and Lifecycle

## Status
Proposed

## Context

Kartoush uses a derived inventory availability model with authoritative stock
levels. Availability checks are advisory. Strong guarantees begin only when
inventory is explicitly reserved.

To avoid over-reservation and unclear cleanup behavior, we need an explicit
model for:
- who owns a reservation
- when it is created
- how long it lasts
- how it is released or converted into a purchase
- what happens on failure paths

This decision defines reservation ownership and lifecycle without committing to
specific persistence or locking mechanisms.

---

## Decision

Adopt a **cart-owned reservation** model with an explicit lifecycle:

- Reservations are owned by a specific cart (and optionally a cart line item).
- Reservations are created only through a reservation service owned by the
  inventory domain.
- Reservations have a time-to-live (TTL) and expire automatically if not used.
- On successful checkout, reservations are converted into an order allocation.
- Reservations are released on cart abandonment, explicit removal, or expiration.

---

## Ownership Model

### Owner

- The reservation owner is the cart.
- Each reservation is scoped to a cart item (product and quantity), and tied to
  the cart line item identity.

This makes it possible to:
- release reservations when a cart line item is removed
- adjust quantities without affecting unrelated items
- avoid a single global reservation per cart that becomes ambiguous

### What is reserved

A reservation represents a claim against availability for:
- SKU (or product identifier)
- quantity
- location or stock pool (if applicable)
- owner (cart and cart line item)

---

## Lifecycle

### 1. Created
A reservation is created when:
- a cart line item is created for a reservable product, or
- a cart line item quantity is increased for a reservable product

Creation must:
- validate the item is reservable
- enforce "no over-reservation" against authoritative stock and existing reservations
- return success or failure to the caller

### 2. Updated
A reservation is updated when:
- the cart line item quantity changes
- the product selection changes
- the stock pool changes (if supported)

Rules:
- increasing quantity requires availability checks at reservation time
- decreasing quantity should release immediately
- updates must be idempotent when repeated with the same intent

### 3. Released
A reservation is released when:
- the cart line item is removed
- the cart is explicitly cleared
- checkout fails after a reservation was held and the cart is not continuing
- a user abandons the cart and the reservation expires

Release must be safe to call multiple times.

### 4. Expired
Reservations have a TTL.

Rules:
- expiration is enforced by the inventory domain, not by clients
- clients must tolerate expired reservations and re-reserve if needed
- expiration should release capacity back into availability calculations

TTL duration is a configuration detail and is intentionally not set in this decision.

### 5. Consumed at checkout
On successful checkout:
- the reservation is consumed and converted into an order allocation
- consumption must be atomic from a business perspective, meaning the system
  must not create an order without consuming the reservation, and must not
  consume a reservation without creating an order

The details of atomicity depend on persistence choices and are deferred.

---

## Reservable vs Non-Reservable Products

- Reservable products participate in this lifecycle.
- Non-reservable products do not create reservations.
- Checkout flows must handle contention for non-reservable products explicitly.

This aligns with the availability model defined in 0007.

---

## Failure and Edge Cases

### Expired reservation during checkout
- If a reservation expires before checkout completion, checkout must fail with
  a clear error and allow the user to retry, which will attempt to re-reserve.

### Partial reservation updates
- If a cart has multiple items, reservation failures should affect only the
  impacted line item, not the entire cart, unless checkout requires all-or-nothing.

### Idempotency
- Create, update, release, and consume operations must be idempotent based on:
  - cart identifier
  - cart line item identifier
  - product identifier
  - intended quantity

---

## Consequences

- Reservations become the central mechanism that provides inventory guarantees.
- Cart operations may fail due to insufficient availability when creating or
  increasing reserved quantity.
- TTL-based expiration requires background cleanup or time-based enforcement.

These tradeoffs are intentional to preserve correctness and predictable behavior.

---

## Guardrails

- Only the inventory domain may create, update, release, or consume reservations.
- No other module may directly mutate reservation state.
- Reservation guarantees apply only to reservable products.
- Every reservation must have an owner and a TTL.
- Any exception to these rules must be explicitly documented.

---

## Notes

This decision intentionally defers:
- persistence model, locking approach, and concurrency control mechanisms
- the specific TTL duration and cleanup strategy implementation details
- how stock pools and multi-location inventory are represented, if applicable
