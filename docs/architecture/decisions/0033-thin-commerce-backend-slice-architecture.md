# ADR 0033: Thin Commerce Backend Slice Architecture

## Status

Accepted

## Context

Kartoush now has a planned thin frontend track and a corresponding thin
commerce backend epic.

The current repository already documents important commerce rules, including:

- Inventory availability and reservation behavior
- Cart ownership and cart-to-order conversion
- Payment lifecycle and order creation guarantees
- B2B and B2C channel strategy
- Order throughput and duplicate-submission direction

Those decisions establish the broader commerce domain direction, but they do not
yet define the initial backend slice needed to support the thin frontend work.

Kartoush therefore still needs a concrete answer to these questions:

- What the first thin commerce backend slice includes
- Which modules should own product, cart, checkout, and order behavior
- Which APIs are external and intended for thin frontend consumption
- Which work is intentionally deferred so the first slice stays narrow

Without that decision, the follow-on implementation tasks risk:

- Collapsing product, cart, and order behavior into `app`
- Recreating broad "commerce" code without clear ownership boundaries
- Blurring product reads, cart mutation, and checkout submission into one
  oversized API surface
- Pulling in payment, pricing, or fulfillment complexity before the thin
  frontend actually needs it

## Decision

Kartoush will implement the initial thin commerce backend as a narrow,
API-first slice composed of three new domain modules:

- `product`
- `cart`
- `order`

Kartoush will apply the following rules:

1. `product` owns product catalog read behavior for the initial thin frontend
   use cases
2. `cart` owns cart creation, retrieval, and cart-item mutation behavior
3. `order` owns checkout submission and order creation from carts
4. `app` owns only transport concerns such as controllers, security wiring, and
   request-to-facade mapping
5. Cross-module calls occur only through published facades and facade DTOs
6. Internal-only administrative routes remain under `/internal/**` and are not
   part of this initial thin commerce slice

## Initial Slice Boundaries

The initial thin commerce backend includes only the capabilities needed for a
demoable first-party commerce flow:

- Product listing
- Product detail
- Cart creation and retrieval
- Add, update, and remove cart items
- Checkout submission
- Order creation from a cart

The initial thin commerce backend does not include:

- Storefront search
- Pricing or promotion engines
- Payment provider integration
- Fulfillment workflows
- Inventory-management UIs
- Broader administrative product or order operations

## Module Placement and Ownership

### `product`

The `product` module owns:

- Product persistence
- Product repository access
- Product read behavior
- Product facade contracts and published product read models

The initial `product` module is read-only.

It exists to support thin frontend catalog reads, not full product management.

### `cart`

The `cart` module owns:

- Cart persistence
- Cart-item persistence
- Cart mutation rules
- Cart facade contracts and published cart models

The `cart` module may depend on published `product` contracts when product
existence or baseline product read data is required.

The `cart` module must not read `product` persistence directly.

### `order`

The `order` module owns:

- Order persistence
- Checkout submission behavior
- Cart-to-order orchestration
- Order facade contracts and published order models

The `order` module may depend on published `cart` contracts to retrieve and
submit carts for conversion.

The `order` module must not read `cart` persistence directly.

Any future payment or inventory integration must also occur through explicit
published contracts rather than repository access across modules.

## External API Boundaries

The initial external API surface is:

- Product catalog APIs under `/api/products`
- Customer cart APIs under `/api/carts`
- Checkout submission API under `/api/checkout`

These are external APIs intended for first-party frontend consumption.

They should remain intentionally narrow and should use dedicated request and
response models rather than leaking persistence entities or internal module
types.

The expected boundary is:

- `GET /api/products`
- `GET /api/products/{productId}`
- Authenticated cart operations under `/api/carts`
- Authenticated checkout submission under `/api/checkout`

Administrative customer, product, order, or operational APIs belong under
`/internal/**` and are not part of this initial thin commerce slice.

## Persistence Direction

Each module owns its own schema and tables.

That means:

- `product` owns product tables
- `cart` owns cart and cart-item tables
- `order` owns order tables

Modules must not read or mutate another module’s tables directly.

The order-submission path should follow the existing cart-to-order and payment
ADRs:

- checkout submission is an explicit command
- cart validation occurs before order creation
- order creation becomes the new system of record for the submitted cart

This decision does not redefine the broader payment or reservation semantics.
It defines only the module and API shape for the first thin implementation
slice.

## Integration and Security Direction

The thin frontend should consume these APIs through the existing external
customer authentication model.

That means:

- product reads may remain public if the route classification continues to treat
  them as public storefront-style reads
- cart and checkout operations are customer-authenticated external APIs
- internal administrative access remains separate from customer-facing commerce
  APIs

This slice should align with the existing bearer-token request-auth path and
the internal-vs-external API distinction already documented elsewhere.

## Deferred Work

The following concerns are intentionally deferred:

- Rich pricing and promotion behavior
- Real payment provider integration
- Advanced order-management operations
- Inventory administration and reservation-management APIs
- Search, filtering, recommendation, and merchandising concerns
- Broader backoffice or operational product/order tooling

If those concerns become necessary, they should be added through follow-on tasks
or architecture decisions rather than by stretching this initial thin commerce
slice beyond its purpose.

## Consequences

### Positive

This decision:

- Gives follow-on commerce tasks a concrete module split
- Keeps product, cart, and order ownership explicit
- Preserves the modular-monolith boundary rules already used elsewhere in
  Kartoush
- Keeps the external API surface focused on the thin frontend needs
- Reduces the risk of `app` becoming the accidental home for commerce logic

### Negative

This decision:

- Adds three new domain modules instead of a quicker app-only implementation
- Requires more facade and DTO design up front
- Leaves some broader commerce concerns explicitly unresolved for now
- May need refinement later if pricing, inventory, or payment modules become
  first-class implementation concerns

