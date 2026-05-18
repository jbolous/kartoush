# JobRunr Background Job Configuration

## Purpose

This document explains how Kartoush wires JobRunr for durable background job
execution and how that setup behaves by environment.

This document covers runtime configuration only.

It does not replace the architectural decision work tracked separately for the
broader background job direction.

See:

- ADR 0032: Durable Background Job Execution Strategy

## Current Role

JobRunr is the infrastructure mechanism for durable background job execution in
Kartoush.

At this stage, Kartoush provides:

- A framework-free platform scheduling boundary through `platform-jobs`
- A JobRunr-backed adapter in `app`
- Postgres-backed persistent job storage
- Local and development dashboard support
- Durable activation email job scheduling after customer registration and resend

At this stage, Kartoush does not yet provide:

- A broader recurring cleanup job implementation

Those are follow-up tasks in the same epic.

## Module Boundary

The JobRunr runtime wiring lives in `app`.

The shared scheduling contract lives in:

- `platform:platform-jobs`

Application code should depend on:

- `BackgroundJobScheduler`
- `JobRequest`
- `JobHandler`

Application and domain code should not depend directly on JobRunr types.

## Persistence Model

JobRunr stores job metadata in PostgreSQL.

The current configuration uses:

- `jobrunr.database.table-prefix=jobrunr.`

That places JobRunr tables in a dedicated `jobrunr` schema while still using
the same PostgreSQL database as the rest of the application.

## Spring Profile Direction

The current profile behavior is:

- `dev`
  - Background job server enabled by default
  - Dashboard enabled by default
- `test`
  - Background job server disabled
  - Dashboard disabled
- `prod`
  - Background job server enabled by default
  - Dashboard disabled by default

## After-Commit Scheduling

Kartoush schedules background jobs through a transaction-aware app-side
wrapper.

That wrapper:

- Registers enqueue and schedule operations only after a successful transaction
  commit
- Executes the deferred scheduling work in its own transaction boundary
- Rejects scheduling attempts outside an active transaction
- Prevents later job scheduling flows from publishing background work before
  the underlying database changes are durable
- Surfaces post-commit scheduling failures back through the request path instead
  of silently swallowing them after the original database transaction commits

## Configuration Surface

### Shared Configuration

Shared settings currently live in `application.yml`:

- `jobrunr.database.table-prefix`

Kartoush also creates the `jobrunr` schema explicitly through Flyway before
JobRunr starts creating its own tables there.

### Development Configuration

Development settings currently live in `application-dev.yml`:

- `jobrunr.background-job-server.enabled`
- `jobrunr.dashboard.enabled`
- `jobrunr.dashboard.port`

Environment overrides:

```bash
export KARTOUSH_JOBRUNR_BACKGROUND_JOB_SERVER_ENABLED=true
export KARTOUSH_JOBRUNR_DASHBOARD_ENABLED=true
export KARTOUSH_JOBRUNR_DASHBOARD_PORT=8000
```

### Production Configuration

Production defaults currently live in `application-prod.yml`:

- `jobrunr.background-job-server.enabled`
- `jobrunr.dashboard.enabled`

Environment overrides:

```bash
export KARTOUSH_JOBRUNR_BACKGROUND_JOB_SERVER_ENABLED=true
export KARTOUSH_JOBRUNR_DASHBOARD_ENABLED=false
```

## Local Quick Start

Use this flow when you want to verify that JobRunr starts locally and persists
jobs to PostgreSQL:

1. Start PostgreSQL locally and make sure Kartoush can connect to it
2. Run the application with the `dev` profile

```bash
export SPRING_PROFILES_ACTIVE=dev
./gradlew :app:bootRun
```

3. Open the JobRunr dashboard locally if enabled

```text
http://localhost:8000
```

4. Trigger a code path that enqueues a background job, such as customer
   registration or activation-token resend

At the current stage, activation email scheduling is the first customer-facing
flow that persists durable background jobs through JobRunr.

The persisted job payload carries the customer identifier and an encrypted
activation token. Plaintext activation secrets are not stored in JobRunr
tables, and retries reuse the same issued token instead of generating a new
one during background execution.

## Test Behavior

The `test` profile disables the background job server and dashboard.

This keeps most automated tests deterministic while focused integration tests
still verify that JobRunr can persist scheduled jobs.

Activation and sign-in flow tests currently stub the scheduler so they can
continue asserting email-driven behavior without relying on asynchronous job
execution.

## Operational Notes

- JobRunr storage is durable because job records are written to PostgreSQL
- The local dashboard is intended for development use only at this stage
- Production-sensitive dashboard exposure should stay disabled unless there is
  a deliberate operational reason to enable it
- JobRunr is being introduced as durable task infrastructure, not as a domain
  event system
