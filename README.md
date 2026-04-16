<p align="center">
  <img src="docs/assets/kartoush-logo.png" alt="Kartoush Logo" width="350"/>
</p>

<h1 align="center">Kartoush</h1>

<p align="center">
  Modular monolith backend platform for inventory, reservations, and order lifecycle management
</p>

> A modular monolith e-commerce backend built to demonstrate production-grade architecture, domain modeling, and system design at scale.

Kartoush is designed to showcase how a real-world backend system can be intentionally architected, evolved, and documented over time.

It reflects the system design, tradeoffs, and engineering discipline expected from a senior engineer building production systems.

This is not a tutorial project.  
This is a design-first system built the way real systems evolve.  
It is intentionally structured as a portfolio project to demonstrate real-world backend engineering practices, not just feature delivery.

---

## Overview

Kartoush is a backend system inspired by real-world e-commerce platforms, designed to model complex domains such as:

- Customer lifecycle management  
- Inventory reservation and backordering  
- Order and checkout workflows (in progress)  

The system is built using a **modular monolith architecture**, emphasizing:

- clear domain boundaries  
- explicit contracts between modules  
- long-term maintainability and evolvability  

This project reflects how production backend systems are intentionally designed, built, and evolved over time.

---

## Why This Stands Out

Kartoush prioritizes engineering depth over surface-level features.

It demonstrates:

- **Architecture Ownership**  
  A modular monolith with strict boundaries and documented decisions (ADRs)

- **Real Domain Complexity**  
  Inventory modeling that accounts for reservations, expiration, and concurrency

- **Production-Oriented Design**  
  Lifecycle rules, validation, idempotency, and observability designed from the start (e.g., explicit customer lifecycle state transitions and failure handling)

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

- Java 25
- Spring Boot
- Gradle
- PostgreSQL
- Flyway
- SLF4J
- Integration testing with Testcontainers (PostgreSQL)
- OpenAPI (springdoc) for API documentation

---

## Requirements

To build and run Kartoush locally, the following are required:

- **Java 25**
- **Container runtime (Docker, Rancher Desktop, or equivalent)** required for Testcontainers-based integration tests
- **Gradle Wrapper** (included in the repository)

---

## Environment Setup

### Java

Kartoush targets **Java 25**.

Verify your version:

```bash
java -version
```

If needed, install Java 25 and ensure it is set as your active JDK.

---

### Container Runtime (Docker, Rancher Desktop, etc.)

A container runtime is required to run integration tests using Testcontainers.  
This can be Docker, Rancher Desktop, or another compatible runtime.  
Ensure your container runtime is compatible with Docker APIs so Testcontainers can communicate with it.

Verify your container runtime is running:

```bash
docker ps
```

If your container runtime is not running, start it (e.g., Docker Desktop, Rancher Desktop) before executing tests.

---

### Database

No manual database setup is required.

Integration tests use Testcontainers to provision a PostgreSQL instance at runtime. Database schema is managed using Flyway migrations, which are applied automatically during startup.

This ensures tests run against a real database with a consistent, versioned schema without requiring local installation or manual setup.

---

## Quick Start

Build the project and run the full test suite locally:

```bash
./gradlew clean build
./gradlew verifyAll
```

---

## Build

To build the project:

```bash
./gradlew clean build
```

---

## Run Tests

### Run All Tests (Recommended)

```bash
./gradlew verifyAll
```

Runs the full verification suite, including:

- unit tests
- integration tests

---

### Unit Tests Only

```bash
./gradlew test
```

Runs only unit tests.

---

### Integration Tests Only

```bash
./gradlew integrationTest
```

Integration tests use Testcontainers and will:

- start a PostgreSQL container automatically
- configure the datasource at runtime
- run against a real database instance

Docker Desktop typically works with Testcontainers without any additional
configuration.

#### Rancher Desktop on macOS

If you use Rancher Desktop on macOS, Testcontainers may need explicit Docker
environment configuration.

Recommended local environment variables:

```bash
export DOCKER_HOST="unix://$HOME/.rd/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
export TESTCONTAINERS_HOST_OVERRIDE="$(rdctl shell ip a show vznat | awk '/inet / {sub("/.*", ""); print $2}')"
```

If `vznat` is not available in your Rancher Desktop VM, inspect the available
interfaces with:

```bash
rdctl shell ip a
```

Then replace `vznat` in the command above with the correct interface name.

If IntelliJ still skips or fails integration tests, add the same values to the
environment variables for your IntelliJ JUnit and Gradle run configuration
templates. IntelliJ may not inherit your shell environment reliably on macOS.

A common failure mode is Ryuk failing to mount `$HOME/.rd/docker.sock`. If that
happens, verify that `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
is set.

---

## Testing Strategy

Kartoush follows a layered testing approach designed to balance fast feedback with real-world validation.

- **Unit tests** provide fast, isolated validation of business logic  
- **Integration tests** validate behavior against a real PostgreSQL database using Testcontainers  
- **verifyAll** runs the full test suite and is the recommended pre-commit verification step  

Integration tests are treated as first-class and are designed to catch issues that only appear in real environments, such as persistence behavior, transaction boundaries, and database constraints.

For detailed testing conventions and design decisions, see:

- `docs/architecture/decisions/0023-testing-strategy-and-conventions.md`

---

## API Documentation

Swagger UI is available when the application is running:

<http://localhost:8080/swagger-ui.html>

This provides an interactive way to explore and test available endpoints.

---

## API Usage Example

### Create Customer

```bash
curl -X POST http://localhost:8080/customers \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### Get Customer

```bash
curl http://localhost:8080/customers/{customerId}
```

### Notes

- Only **active customers** are returned by default  
- Lifecycle rules (e.g., pending, inactive, deleted) are enforced at the domain level  
- Validation errors return structured responses  

---

## Troubleshooting

- Ensure your container runtime is running before executing integration tests  
- If tests fail with container-related errors, verify your container runtime is accessible  
- On macOS, ensure your container runtime (e.g., Docker Desktop, Rancher Desktop) is fully started before running tests  

---

## Where to Start (for Reviewers)

If you are reviewing this project:

1. Start with the Architecture Decision Records in `docs/architecture/decisions`  
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

## Independence Disclaimer

This project is an independent work created on personal time and equipment. It is not affiliated with, derived from, or representative of any employer or proprietary system.

---

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
