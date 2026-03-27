# Kartoush

> A modular monolith e-commerce backend built to demonstrate production-grade architecture, domain modeling, and system design at scale.

Kartoush is designed to showcase how a real-world backend system can be intentionally architected, evolved, and documented over time.

It reflects the kind of system design, tradeoffs, and engineering discipline expected from a senior engineer working on production platforms.

This is not a tutorial project.  
This is a design-first system built the way real-world systems evolve.

## Overview

Kartoush is a backend system inspired by real-world e-commerce platforms, designed to model complex domains such as:

- Customer lifecycle management  
- Inventory reservation and backordering  
- Order and checkout workflows (in progress)  

The system is built using a **modular monolith architecture**, emphasizing:

- clear domain boundaries  
- explicit contracts between modules  
- long-term maintainability and evolvability  

This project reflects how a **senior engineer designs, builds, and evolves a system over time**, focusing on real-world tradeoffs rather than simplified examples.

---

## Why This Stands Out

Kartoush focuses on engineering depth over surface-level features.

It demonstrates:

- **Architecture Ownership**  
  A modular monolith with strict boundaries and documented decisions (ADRs)

- **Real Domain Complexity**  
  Inventory modeling that accounts for reservations, expiration, and concurrency

- **Production-Oriented Design**  
  Lifecycle rules, validation, idempotency, and observability designed from the start

- **Decision Transparency**  
  Tradeoffs are documented and revisited as the system evolves

- **Evolution Over Time**  
  The system is intentionally built to reflect how real platforms grow, adapt, and change

---

## Why Kartoush Exists

Most public GitHub projects fail to demonstrate:

- architectural ownership  
- system-level tradeoffs  
- non-functional requirements  
- long-term maintainability  

Kartoush was created to close that gap.

It showcases how I approach:

- system decomposition  
- domain boundaries  
- scalability and performance  
- observability and operability  
- evolution over time  

Everything in this repository exists to make engineering decisions explicit.

---

## Tech Stack

- Java
- Spring Boot
- Gradle
- PostgreSQL
- Flyway
- SLF4J

---

## Where to Start (for Reviewers)

If you are reviewing this project:

1. Read the Architecture Decision Records in `docs/architecture/decisions`  
2. Review the customer domain and lifecycle rules  
3. Examine inventory modeling and reservation handling  
4. Explore module boundaries and facade contracts  
5. Inspect commit history to understand how decisions evolved  

---

### A Deliberate Learning Project

Kartoush is also a deliberate learning investment.

While it reflects patterns and practices I use professionally, it is intentionally designed to push into areas that are difficult to explore safely inside a large, existing production system.

This includes:

- Stress-testing architectural decisions at scale  
- Revisiting tradeoffs with the benefit of hindsight  
- Experimenting with alternative designs and documenting why they do or do not work  

Where learning drives a decision, that context is documented explicitly. The goal is not to present a perfect system, but an honest one.

Some decisions intentionally revisit problems I have encountered in production systems, particularly areas where earlier tradeoffs revealed shortcomings as systems scaled and evolved.

---

## Project Name

The name **Kartoush** is a play on the word *cartouche*.

A cartouche represents an enclosing boundary that gives structure and meaning to what is inside. In e-commerce systems, the cart plays a similar role, acting as the boundary where pricing, inventory, promotions, and fulfillment rules intersect.

Kartoush was chosen to reflect that idea:

- Clear boundaries matter  
- Structure enables complexity  
- Systems fail when responsibilities leak across layers  

The slightly altered spelling is intentional. This project is inspired by real-world patterns, but it is not a clone of any single system. It exists as a space to explore, refine, and sometimes challenge architectural decisions.

---

## Design Philosophy

Kartoush is guided by a few core principles.

### Design for Change

Real systems are never done.

Kartoush favors:

- Clear boundaries  
- Stable interfaces and contracts  
- Evolvable internals  

### Make Tradeoffs Explicit

Every meaningful architectural decision is documented, including:

- Alternatives considered  
- Reasons for rejection  
- Known limitations  

### Favor Boring, Understandable Architecture

The system avoids unnecessary complexity in favor of:

- Predictable behavior  
- Debuggability  
- Operational clarity  

---

## Architecture Overview

Kartoush uses a **modular monolith** architecture, meaning the system is deployed as a single application but structured into clearly defined, independent modules.

This allows:

- Strong domain boundaries  
- Independent evolution of modules  
- Simplified local development  
- A future path toward splitting modules into separate services if warranted  

## Documentation

Additional architecture documentation lives in the `docs` directory.

- `docs/architecture/README.md` — architecture overview and design principles
- `docs/architecture/decisions/` — Architecture Decision Records (ADRs)
- `docs/persistence/` — data modeling and persistence strategy

### Module Structure

Kartoush is organized around domain-oriented modules rather than technical layers.

Each module may contain its own:

- domain model
- facade or application contract
- service logic
- persistence implementation

This keeps business capabilities cohesive while still enforcing clear boundaries between modules.

| Concern | Role |
| ------ | ------ |
| Domain Model | Encapsulates business concepts and rules |
| Facade / Contract | Exposes stable entry points to callers |
| Services | Coordinates use cases and business workflows |
| Persistence | Handles storage and retrieval |

---

## Key Architectural Decisions

Important decisions are captured as **Architecture Decision Records (ADRs)**.

Examples include:

- Modular monolith vs microservices  
- Facade layering and communication rules  
- Inventory reservation and backorder behavior  
- Performance handling under high concurrency  
- Logging and observability strategy  

These live in the `docs/architecture/decisions` directory.

> The documentation is as important as the code.

---

## Inventory Is a Core Concern

Inventory is intentionally chosen as a core domain because it is deceptively difficult to model correctly over time.

Kartoush accounts for real-world constraints such as:

- Reservable vs non-reservable products  
- Cart-based inventory reservations  
- Reservation expiration  
- Backorder eligibility tied to future supply  
- Concurrent checkout behavior  

Naive inventory models tend to fail under load or as systems scale.  
Kartoush explicitly designs for those failure modes.

---

## Non-Functional Requirements

Kartoush intentionally designs for performance, reliability, and observability from the start, rather than treating them as afterthoughts.

### Performance

- Designed to handle high-volume order spikes  
- Avoids global locks and contention hotspots  
- Documents scaling bottlenecks and mitigation strategies  

### Observability

- Structured logging through SLF4J  
- Clear ownership of log responsibility  
- Designed for future integration with centralized observability tools  

### Reliability

- Idempotent operations where required to ensure safe retries  
- Explicit failure handling paths  
- Defensive modeling of partial system failure  

---

## What This Project Is Not

Kartoush is intentionally not:

- A polished SaaS product  
- A UI-heavy demo  
- A framework showcase  
- A tutorial codebase  

This is a thinking artifact that also happens to compile.

---

## Project Status

Kartoush is an evolving system.

Some components are:

- Fully implemented  
- Partially implemented  
- Documented but intentionally deferred  

This reflects how real systems are built and prioritized.

---

## How to Explore the Project

If you are reviewing this as a hiring manager or engineer:

1. Start with the Architecture Decision Records in `docs/architecture/decisions`
2. Review the facade boundaries that isolate callers from internal implementation details  
3. Examine inventory modeling and reservation flow  
4. Review logging and performance decisions  
5. Inspect commit history to see how decisions evolved  

---

## Independence Disclaimer

This project is an independent work created on personal time and equipment. It is not affiliated with, derived from, or representative of any employer or proprietary system.

---

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
