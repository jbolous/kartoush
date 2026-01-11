# Non-Reservable Product Behavior

## Status

Proposed

## Context

Kartoush uses a derived inventory availability model where availability is
advisory until inventory is explicitly reserved. Reservable products provide
strong guarantees through cart-owned reservations that are consumed at order
creation.

Some products are non-reservable (first-come, first-served, or otherwise not
eligible for reservations). Without explicit rules, checkout behavior for these
items can become unpredictable and lead to inconsistent user experiences.

This decision defines how non-reservable products behave in carts and checkout,
and what guarantees are provided.

---

## Decision

Adopt a **best-effort, no-guarantee** model for non-reservable products:

- Non-reservable products do not create reservations.
- Availability for non-reservable products is advisory only.
- Cart to order conversion must perform a final availability check for
  non-reservable items at conversion time.
- If any non-reservable item is unavailable at conversion time, conversion
  fails by default with a clear error identifying impacted items.

---

## Cart Behavior

- Non-reservable items may be added to a cart like any other item.
- Adding or updating quantity does not reserve inventory.
- The cart should surface that the item is not reserved and may become
  unavailable.

The exact UI/UX is outside the scope of this decision, but the semantics must
be preserved.

---

## Checkout Behavior

### Payment authorization

Payment authorization is attempted before cart to order conversion.

If authorization succeeds but conversion fails due to non-reservable
availability, the system must not create an order and must not consume
reservations. Any payment authorization reuse or reversal behavior is handled
according to the payment lifecycle decision.

---

### Conversion-time availability check

At cart to order conversion time:

- The system validates non-reservable item availability.
- The check is performed as close as possible to conversion.
- The check provides best-effort correctness; inventory contention may still
  exist at high concurrency.

If the check fails, conversion fails.

---

## Failure Semantics

If non-reservable availability fails at conversion time:

- No order is created.
- No reservations are consumed.
- The cart remains usable.
- The response must clearly identify which items failed and why.

---

## Partial Order Policy

Default behavior is **all-or-nothing** conversion.

If a future requirement calls for partial orders (convert reservable items, drop
non-reservable unavailable items), that must be introduced as an explicit
follow-up decision.

---

## Consequences

- Users may experience checkout failures for non-reservable items due to
  contention.
- The system remains correct without inventing reservation guarantees where
  they do not exist.
- Product types remain explicit: reservable items provide guarantees, while
  non-reservable items do not.

These tradeoffs are intentional.

---

## Guardrails

- Do not create reservations for non-reservable products.
- Do not claim strong inventory guarantees for non-reservable products.
- Fail conversion when non-reservable items are unavailable unless an explicit
  partial-order decision is adopted.
- Any exception to these rules must be explicitly documented.

---

## Notes

This decision does not define:

- how non-reservable eligibility is configured
- how inventory contention is handled operationally
- UI messaging and presentation details
