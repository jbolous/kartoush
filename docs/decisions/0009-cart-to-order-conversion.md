# 0009 – Cart to Order Conversion Rules

## Status

Proposed

## Context

Kartoush uses a cart-owned reservation model to provide strong inventory
guarantees during checkout. Inventory availability is advisory until inventory
is explicitly reserved, and reservations are consumed at checkout.

To complete the checkout flow, the system needs explicit rules for how a cart
is validated and converted into an order, including:

- eligibility for conversion
- reservation consumption
- failure handling
- guarantees once an order is created

This decision defines the business rules for cart to order conversion without
committing to specific implementation or persistence strategies.

---

## Decision

Adopt a **payment-confirmed, reservation-backed cart to order conversion model**:

- A cart may be converted into an order only after successful payment confirmation
  (for example, payment authorization).
- All active reservations owned by the cart must be valid at conversion time.
- Order creation and reservation consumption are treated as a single business
  operation.
- Conversion is all-or-nothing. Partial orders are not created by default.

---

## Eligibility for Conversion

A cart is eligible for conversion when:

- The cart exists and is active.
- All cart line items are valid (product exists, quantity > 0).
- All reservable cart line items have active, unexpired reservations.
- Payment has been successfully confirmed according to checkout rules.
- Required checkout inputs (for example, customer identity and shipping details)
  are present and valid.

If any eligibility check fails, conversion must fail with a clear error.

---

## Conversion Process

### 1. Validation

Before creating an order, the system must:

- validate the cart state
- verify all required reservations are still valid
- verify payment confirmation is present and valid

Validation must not mutate state.

---

### 2. Reservation Consumption and Order Creation

On successful validation:

- all reservations owned by the cart are consumed
- consumed reservations are converted into order allocations
- a single order is created from the cart contents

Reservation consumption and order creation must be treated as a single business
operation and must be safe to retry.

---

### 3. Order Creation

- A single order is created from the cart contents.
- The order becomes the new system of record for fulfillment.
- Cart data may be copied or referenced, but the cart itself is no longer used
  for inventory guarantees.

Order identifiers must be stable and unique.

---

## Failure Handling

### Validation Failure

If validation fails:

- no reservations are consumed
- no order is created
- the cart remains usable (unless explicitly invalidated)

---

### Failure During Conversion

If a failure occurs after validation but before completion:

- the system must not create an order without consuming reservations
- the system must not consume reservations without creating an order
- the operation must be safe to retry

The exact atomicity mechanism is deferred.

---

### Payment Failure

If payment confirmation fails:

- no reservations are consumed
- no order is created
- the cart remains usable while reservations are still valid

If a failure occurs after payment confirmation but before order creation:

- the system must not create an order without consuming reservations
- the system must not consume reservations without creating an order
- the operation must be safe to retry or explicitly compensated


---

### Expired Reservations at Checkout

If one or more reservations expire during checkout:

- conversion fails
- the user must be informed and allowed to retry
- retrying may trigger re-reservation attempts

---

## Non-Reservable Items

- Non-reservable items do not participate in reservation consumption.
- Conversion may fail if non-reservable items become unavailable during checkout.
- This behavior must be communicated clearly to the caller.

---

## Guarantees After Conversion

Once an order is successfully created:

- Inventory for reservable items is guaranteed to the order.
- Payment has already been confirmed.
- The cart no longer owns reservations.
- Subsequent failures (for example, fulfillment or downstream processing issues)
  do not automatically release inventory or invalidate the order.

---

## Consequences

- Checkout becomes a clearly bounded operation.
- Strong inventory guarantees apply only after successful conversion.
- Failure handling must be explicit and retry-safe.

These tradeoffs are intentional to preserve correctness and predictability.

---

## Guardrails

- Do not create partial orders by default.
- Do not consume reservations outside of cart to order conversion.
- Do not rely on availability checks alone for checkout guarantees.
- Any deviation from all-or-nothing conversion must be explicitly documented.

---

## Notes

This decision intentionally defers:

- payment authorization and capture sequencing
- fulfillment initiation and downstream workflows
- compensation and rollback strategies for post-order failures
