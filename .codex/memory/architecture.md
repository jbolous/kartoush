# Architecture Memory

## System shape

- Kartoush is a modular monolith backend
- Java 25, Spring Boot 4, Gradle, PostgreSQL, Flyway
- Main modules in `settings.gradle`:
  - `app`
  - `auth`
  - `customer`
  - `notification`
  - `platform:platform-types`
  - `platform:platform-validation`
  - `test-support`

## Module roles

- `app`
  - HTTP entry point
  - Controllers, request and response models, OpenAPI docs, error mapping, and app services
- `customer`
  - Customer lifecycle, activation, registration, Terms of Service, and public customer facades
- `auth`
  - Customer password ownership, sign-in, auth sessions, password setup, and password reset
- `notification`
  - Shared email delivery boundary and provider adapters
  - Currently email only
- `platform-types`
  - Shared value types and ULID support
- `platform-validation`
  - Shared request validation helpers and validation error model
- `test-support`
  - Shared integration-test annotations and Postgres/Testcontainers support

## Boundary expectations

- `app` depends on module facades, not persistence internals
- Facade contracts are explicit and should not expose entities
- Customer lifecycle stays in `customer`
- Credential and sign-in behavior stays in `auth`
- Email sending stays in `notification`
- Shared primitive-like types such as `CustomerId`, `Email`, `PhoneNumber`, and `CustomerStatus` live in `platform-types`

## Current layering patterns

- Package naming commonly follows:
  - `domain`
  - `facade`
  - `internal`
  - `persistence`
  - `service`
  - `exception`
- Implementations commonly use `Default...` naming
- Persistence repositories and entities stay inside the owning module

## API and facade boundary nuance

- The repo is moving toward app-owned HTTP request models and separate facade contracts
- That split is already visible in auth and terms endpoints
- Customer create and update still bind directly to facade input models in `CustomerController`
- Do not assume the split is complete everywhere

## Auth and session direction

- Kartoush uses opaque bearer tokens backed by server-side auth sessions
- Auth currently issues tokens but protected-route enforcement is still separate follow-up work

## Notification direction

- Provider-specific email code lives under `notification.email.provider.*`
- Shared delivery types live under `notification.email.delivery`
- Shared email composition for customer flows lives under `notification.email.customer`
