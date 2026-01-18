# Order Throughput and Spike Handling

## Status

Proposed

## Context

Order placement is a critical workflow and a primary source of load on the system.

Kartoush must define expected behavior when:

- Many orders are placed concurrently
- Traffic arrives in bursts (promotions, events, external integrations)
- Downstream dependencies are slow or unavailable (payments, inventory, persistence)

This decision is intentionally implementation-agnostic. It documents the expected platform behavior and the architectural direction for handling spikes.

This ADR builds on the following prior decisions:
◊
- [ADR 0008: Reservation ownership and lifecycle](./0008-reservation-ownership-and-lifecycle.md)
- [ADR 0009: Cart to order conversion](./0009-cart-to-order-conversion.md)
- [ADR 0010: Payment lifecycle and guarantees](./0010-payment-lifecycle-and-guarantees.md)
- [ADR 0015: Headless and API Strategy](./0015-headless-and-api-strategy.md)


## Decision

Kartoush will handle order spikes using a **hybrid approach**:

1. The checkout API will perform **synchronous validation** and return a clear outcome.
2. The system will use **explicit idempotency** for order placement requests to prevent duplicate orders.
3. When the platform detects overload or dependency degradation, it will apply **backpressure** rather than attempting unbounded work.
4. The platform will define a clear separation between:
   - **Order acceptance** (request received, validated, and persisted as accepted)
   - **Order processing** (downstream actions such as payment capture, confirmation, fulfillment integration)

This decision does not require that all processing be asynchronous from day one, but it establishes the system boundaries so that async processing can be introduced without changing core contracts.

### Practical outcomes expected from this decision

- The API remains predictable under load.
- Duplicate submissions do not create duplicate orders.
- Degraded dependencies result in controlled failure modes rather than cascading failures.
- Work can be shifted to asynchronous processing when needed.

## Alternatives Considered

### Fully synchronous order placement

All operations (validation, reservation, payment, fulfillment integration) happen in the request thread.

- Pros:
  - Simple flow
  - Immediate results
- Cons:
  - Poor resilience under spikes
  - Higher risk of timeouts and cascading failures
  - Harder to scale

### Fully asynchronous order placement

API only accepts a request and immediately returns, with all processing done in background.

- Pros:
  - Great spike resilience
  - Natural backpressure via queues
- Cons:
  - More complex client experience (polling, callbacks)
  - Harder to provide immediate confirmation semantics
  - Requires more infrastructure and operational maturity

### Feature-flag based behavior

Keep both sync and async behavior and toggle via flags.

- Pros:
  - Flexibility
- Cons:
  - Risk of hidden complexity
  - Harder to reason about guarantees

## Consequences

### Positive

- Clear contract for order placement under normal and spike conditions
- Enables future async processing without breaking client contracts
- Supports resilient behavior when dependencies degrade
- Reduces risk of duplicate orders via idempotency

### Negative

- Requires intentional design of idempotency keys and request semantics
- Introduces conceptual separation between acceptance and processing
- Requires clear error modeling (retryable vs non-retryable outcomes)

## Follow-Up Decisions

- Performance testing strategy and scenarios (ADR 0017)
- Authentication and authorization details for order placement
- Error model and API response conventions for retry and failure
- Architecture ADR for internal vs external API boundaries (A002)

## Notes
◊
This ADR defines desired system behavior and boundaries. Specific implementation choices (queues, retries, processing pipelines) are expected to evolve as the system grows, but should remain consistent with the guarantees captured here.
