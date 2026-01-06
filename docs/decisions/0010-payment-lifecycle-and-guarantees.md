# 0010 – Payment Lifecycle and Guarantees

## Status

Proposed

## Context

Kartoush converts a cart into an order only after payment confirmation.
Inventory guarantees are enforced through reservations and are consumed at
order creation.

To avoid ambiguous checkout behavior, the system needs explicit rules for:

- what “payment confirmed” means
- when inventory is committed
- how failures, retries, and reversals are handled
- what guarantees exist once an order is created

This decision defines payment semantics independently of any specific payment
provider or orchestration platform.

---

## Decision

Adopt an **authorize-then-capture payment lifecycle** with explicit guarantees:

- Payment confirmation is defined as **successful payment authorization**.
- A cart may be converted into an order only after payment authorization succeeds.
- Payment capture occurs **after** order creation.
- Authorization, order creation, and reservation consumption are coordinated as
  a single business flow, but may involve multiple technical steps.

---

## Payment States

The system recognizes the following conceptual payment states:

1. **Not Attempted**
   - No payment interaction has occurred.

2. **Authorization Failed**
   - Payment authorization was attempted and failed.
   - No inventory is consumed.
   - The cart remains usable while reservations are valid.

3. **Authorized**
   - Payment authorization succeeded.
   - Inventory may be committed through order creation.
   - Authorization may expire if not captured within a provider-defined window.

4. **Captured**
   - Funds have been captured successfully.
   - The order is financially complete.

5. **Reversed / Refunded**
   - Authorized or captured funds have been reversed or refunded.
   - Inventory is not automatically released unless explicitly handled by a
     downstream process.

---

## Checkout Flow

### 1. Payment Authorization

- Payment authorization is attempted for the cart total.
- Authorization must be idempotent for repeated attempts.
- If authorization fails:
  - no reservations are consumed
  - no order is created
  - the user may retry while reservations remain valid

---

### 2. Order Creation

After successful authorization:

- The cart is validated.
- All required reservations are verified as active.
- Reservations are consumed.
- A single order is created.

Order creation and reservation consumption must be treated as a single business
operation and must be safe to retry.

---

### 3. Payment Capture

- Payment capture occurs after order creation.
- Capture may be synchronous or asynchronous.
- Capture failures must be detectable and recoverable.

The exact capture timing and retry strategy is intentionally deferred.

---

## Failure Handling

### Authorization Failure

- No inventory is committed.
- The cart remains usable.
- The user may retry payment.

---

### Failure After Authorization, Before Order Creation

- No order is created.
- Reservations are not consumed.
- Authorization may be reused or reversed depending on provider behavior.

---

### Failure After Order Creation, Before Capture

- The order exists with an authorized payment.
- Inventory is committed to the order.
- Capture must be retried or explicitly reversed.
- Automatic inventory release does not occur without an explicit compensating action.

---

## Guarantees

Once an order is successfully created:

- Inventory for reservable items is guaranteed to the order.
- Payment authorization has been confirmed.
- The cart no longer owns reservations.
- Subsequent payment or fulfillment failures do not implicitly undo the order.

Any rollback behavior must be handled explicitly by downstream processes.

---

## Consequences

- Inventory commitment is aligned with financial commitment.
- Some orders may exist in an “authorized but not captured” state.
- Compensation logic becomes explicit rather than implicit.

These tradeoffs are intentional to preserve correctness and auditability.

---

## Guardrails

- Do not treat availability checks as payment guarantees.
- Do not create orders without confirmed payment authorization.
- Do not automatically release inventory on payment capture failure.
- Payment and order operations must be idempotent and retry-safe.

---

## Notes

This decision intentionally defers:

- provider-specific authorization and capture mechanics
- retry timing and backoff strategies
- refund and chargeback handling
- orchestration or routing concerns (for example, multi-provider strategies)
