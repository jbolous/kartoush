# Architecture Decision Records

This directory contains **Architecture Decision Records (ADRs)** for Kartoush.

ADRs document significant architectural and technical decisions along with the context in which they were made and the tradeoffs involved.

Their purpose is simple: to make architectural reasoning **explicit, reviewable, and durable** as the project evolves.

Kartoush intentionally records decisions early in the design process so that architectural thinking remains visible even as the system evolves.

---

## What Is an ADR?

An Architecture Decision Record captures:

- The context of a problem or design choice
- The decision being considered or made
- Alternatives that were evaluated
- The tradeoffs involved
- The consequences or limitations of the decision

ADRs exist to answer the question:

> Why does the system work this way?

They are preferred over undocumented assumptions or relying on memory.

---

## When to Write an ADR

An ADR should be written when a decision:

- Affects system structure or module boundaries
- Introduces constraints that will be difficult to reverse
- Impacts multiple domains or components
- Represents a meaningful tradeoff rather than an obvious choice

Not every technical choice needs an ADR.  
If a decision is easy to change or isolated in scope, documentation in code or a task description is usually sufficient.

---

## Decision Lifecycle

Decision records may move through several states:

- **Proposed**  
  An open question or architectural exploration.

- **Accepted**  
  A decision has been made and agreed upon.

- **Superseded**  
  A previous decision has been replaced by a newer one.

The status of the decision should be clearly stated at the top of each record.

---

## How ADRs Evolve

Architectural decisions are not permanent.

When a decision changes:

- A **new ADR** is created
- The original ADR remains unchanged
- The newer ADR references the earlier one

This preserves historical context and makes architectural evolution visible over time.

ADRs are not rewritten to reflect current thinking.  
They represent what was believed to be correct at the time.

---

## Relationship to the Code

ADRs explain **why** a decision was made.  
The code shows **how** that decision is implemented.

If the code no longer reflects an ADR:

- Either the code has drifted
- Or a new ADR is needed

In both cases the mismatch should be resolved explicitly.

---

## File Naming

ADRs are stored as Markdown files and named using a simple numeric prefix:

- 0001-project-goals.md
- 0002-architecture-style.md
- 0003-data-ownership.md

Numbers indicate chronological order and do not imply priority.

---

## ADR Index

- [0001 - Project Goals and Non-Goals](./0001-project-goals.md)
- [0002 - Architecture Style](./0002-architecture-style.md)
- [0003 - Data Ownership](./0003-data-ownership.md)
- [0004 - Integration Style](./0004-integration-style.md)
- [0005 - Build Tool](./0005-build-tool.md)
- [0006 - Deployment Assumptions](./0006-deployment-assumptions.md)
- [0007 - Inventory Availability](./0007-inventory-availability.md)
- [0008 - Reservation Ownership and Lifecycle](./0008-reservation-ownership-and-lifecycle.md)
- [0009 - Cart to Order Conversion](./0009-cart-to-order-conversion.md)
- [0010 - Payment Lifecycle and Guarantees](./0010-payment-lifecycle-and-guarantees.md)
- [0011 - Layering and Facade Contracts](./0011-layering-and-facade-contracts.md)
- [0012 - Non-Reservable Product Behavior](./0012-non-reservable-product-behavior.md)
- [0013 - Logging and Observability](./0013-logging-and-observability.md)
- [0014 - B2B vs B2C Channel Strategy](./0014-b2b-vs-b2c-channel-strategy.md)
- [0015 - Headless and API Strategy](./0015-headless-and-api-strategy.md)
- [0016 - Order Throughput and Spike Handling](./0016-order-throughput-and-spike-handling.md)
- [0017 - Performance Testing Strategy](./0017-performance-testing-strategy.md)
- [0018 - Module Boundaries and Dependencies](./0018-module-boundaries-and-dependencies.md)
- [0019 - Internal vs External APIs](./0019-internal-vs-external-apis.md)
- [0020 - Use Dedicated Contract Models for Public Module Facades](./0020-use-dedicated-contract-models-for-public-module-facades.md)
- [0021 - Branch Naming Convention](./0021-branch-naming-convention.md)
- [0022 - Merge Strategy](./0022-merge-strategy.md)
- [0023 - Testing Strategy and Conventions](./0023-testing-strategy-and-conventions.md)
- [0024 - Customer Activation Token Ownership and Lifecycle](./0024-customer-activation-token-ownership-and-lifecycle.md)
- [0025 - Terms of Service Lifecycle and Current Version Resolution](./0025-terms-of-service-lifecycle-and-current-version-resolution.md)
- [0026 - Registration and Terms Acceptance Approach](./0026-registration-and-terms-acceptance-approach.md)

---

## What ADRs Are Not

ADRs are intentionally not:

- Design documents for every class or method
- Tutorials on architecture patterns
- Justifications of best practices

They exist to document **decisions**, not implementations.

---

## Independence Disclaimer

These Architecture Decision Records are part of an independent project created on personal time and equipment. They are not affiliated with, derived from, or representative of any employer or proprietary system.
