# Kartoush Project Terms Dictionary

This dictionary explains terms that appear repeatedly in Kartoush architecture decisions, implementation tasks, and code reviews. Each entry gives a plain-language definition followed by how Kartoush uses or plans to use the concept.

## Architecture and Structure

### Modular monolith

A single deployable application divided into modules with clear ownership boundaries.

**In Kartoush:** The backend runs as one application, while modules such as `customer`, `auth`, and `notification` keep their responsibilities and data separate.

### Module

A part of an application that owns a specific set of behavior and data.

**In Kartoush:** A module normally exposes a small public boundary and keeps its implementation and database access internal.

### Module boundary

The line that controls what one module may use from another.

**In Kartoush:** Modules communicate through published facades and shared contracts instead of reaching into another module's repositories or entities.

### Data ownership

The rule that one module is responsible for storing and changing a particular kind of data.

**In Kartoush:** The `customer` module owns customer lifecycle data, while `auth` owns passwords and authentication sessions.

### Facade

A module's public entry point. It lets other modules request behavior without depending on internal code.

**In Kartoush:** The `app` module calls domain-module facades rather than their internal services or repositories.

### DTO

A data transfer object: a simple object used to move data across an API or module boundary.

**In Kartoush:** DTOs carry HTTP and facade data without exposing persistence entities or internal domain objects.

### Domain event

A message describing something important that happened in the business domain, such as a customer becoming active.

**In Kartoush:** Domain events may describe important business occurrences, but Kartoush does not use its background-job system as a general domain-event transport.

### Lifecycle

The allowed states of something and the transitions between them.

**In Kartoush:** The customer lifecycle defines when a customer may move between `PENDING`, `ACTIVE`, `INACTIVE`, and `DELETED`.

### Idempotent

Safe to retry without performing the successful operation twice.

**In Kartoush:** Idempotency matters for requests and background jobs that may be retried after timeouts or temporary failures.

### Entity

An object that represents data stored in a database, commonly as a row in a table.

**In Kartoush:** JPA entities stay inside the module that owns their data and are not exposed through HTTP or facade boundaries.

### Repository

Code responsible for retrieving and storing a module's entities.

**In Kartoush:** Each module owns its repositories, and other modules must not call them directly.

### Value type

A small type that represents a meaningful domain value instead of using a general-purpose primitive such as a string.

**In Kartoush:** Shared types such as `CustomerId`, `Email`, and `PhoneNumber` make validation and intent explicit across module boundaries.

## API and Authentication

### Internal API

An endpoint intended for trusted operational or administrative use rather than customer-facing clients.

**In Kartoush:** Internal routes belong under `/internal/**` and are not treated as stable public integration points.

### External API

An endpoint intended for first-party frontends or other approved consumers outside the backend.

**In Kartoush:** External APIs form part of the platform's public contract and must be changed deliberately.

### API-first

Designing backend capabilities as explicit API contracts instead of tying them to a particular user interface.

**In Kartoush:** Web and future clients consume the same backend capabilities through documented APIs.

### Headless

A backend that exposes APIs but does not include or depend on a specific user interface.

**In Kartoush:** The backend supports a separate web experience without owning the frontend implementation.

### Authentication

Proving who a caller is.

**In Kartoush:** Customer authentication validates an opaque bearer token and loads the associated active customer session.

### Authorization

Deciding what an authenticated caller is allowed to do.

**In Kartoush:** Authorization will protect customer and internal operations according to the caller's identity and permitted access.

### JWT

A signed token that carries information a server can validate without looking up a session record for every request.

**In Kartoush:** Customer access tokens are not JWTs. Kartoush uses opaque tokens backed by server-side sessions.

### Bearer token

A secret presented with a request to gain access. Anyone who possesses a valid bearer token can use it.

**In Kartoush:** A signed-in customer sends an opaque bearer token in the HTTP `Authorization` header.

### Opaque token

A token whose value has no useful meaning to the client.

**In Kartoush:** The backend looks up each opaque bearer token to find and validate its authentication session.

### Auth session

A server-side record representing an authenticated customer session.

**In Kartoush:** An auth session connects an opaque token to a customer and allows access to expire or be revoked centrally.

### Activation token

A one-time secret used to prove that a customer may activate a pending account.

**In Kartoush:** Activation tokens expire, belong to one customer, and are consumed after successful activation.

### Password reset token

A one-time secret used to authorize setting a replacement password.

**In Kartoush:** Reset tokens are issued only for eligible active customers and are consumed after a successful reset.

### Hash

A one-way transformation used to compare a secret without storing its original value.

**In Kartoush:** Passwords and one-time tokens are stored as hashes so their original values are not recoverable from the database.

## Persistence and Background Work

### Migration

A versioned database change applied in a controlled order.

**In Kartoush:** Flyway applies migrations to create and evolve module-owned PostgreSQL tables.

### Transaction

A group of database changes that either all succeed or all roll back.

**In Kartoush:** Transactions keep related lifecycle and token changes consistent, and dependent background work is scheduled only after a successful commit.

### Background job

Work saved for execution outside the original request.

**In Kartoush:** JobRunr persists retryable work, including customer email delivery and recurring cleanup, so it can survive application restarts.

## Email and Delivery

### SPF

Sender Policy Framework: a DNS record that identifies which mail systems may send email for a domain.

**In Kartoush:** SPF will authorize the selected transactional email provider to send messages for the Kartoush notification domain.

### DKIM

DomainKeys Identified Mail: a cryptographic signature used to confirm that an email was authorized by its sending domain and was not changed in transit.

**In Kartoush:** DKIM will let receiving mail systems verify messages sent through the transactional email provider.

### DMARC

Domain-based Message Authentication, Reporting, and Conformance: an email-domain policy that tells receiving systems how to handle messages that fail SPF or DKIM checks.

**In Kartoush:** DMARC will provide delivery guidance and reports for messages sent from the Kartoush notification domain.

### Provider adapter

Code that translates a shared application contract into a provider-specific API call.

**In Kartoush:** Email provider adapters let `notification` support services such as Mailtrap and Brevo without coupling customer or authentication flows to their APIs.
