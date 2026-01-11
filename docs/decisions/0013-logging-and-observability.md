# Logging and Observability

## Status

Proposed

## Context

Kartoush needs consistent logging to support:

- Local development debugging
- Production diagnosis
- Performance and reliability investigations

This project aims to stay lightweight early, while keeping a clear path to deeper observability later (metrics and tracing).

## Decision

We will implement application logging using:

- SLF4J API with Logback as the default backend
- One logger per class using `LoggerFactory.getLogger(<Class>.class)`
- Logger fields will be named `LOG`

Logs will be structured and consistent:

- One log event per meaningful action or failure
- Include a correlation ID in all request-scoped logs
- Avoid logging sensitive data
- Prefer parameterized logs over string concatenation

We will define standard log levels:

- ERROR: unexpected failures, request cannot be completed
- WARN: unexpected situations that are recoverable
- INFO: lifecycle events and major state transitions
- DEBUG: developer troubleshooting, disabled by default in prod
- TRACE: extremely verbose, disabled by default

We will start with logging only.
Metrics and distributed tracing are deferred, but the design will not block adopting OpenTelemetry later.

## Consequences

### Positive

- Consistent logs across modules
- Easy local debugging without heavy tooling
- Correlation IDs enable tracing a request through the system

### Negative

- Logging alone does not provide full observability
- Some issues will still require adding metrics later

## Implementation Notes

- Provide a request filter / interceptor to generate and propagate a correlation ID
- Ensure correlation ID is added to MDC and included in log patterns
- Define a default Logback configuration suitable for local dev and production
- Add guidelines for avoiding PII and secrets in logs

## Deferred

- Metrics collection and dashboards
- Distributed tracing (OpenTelemetry)
- Centralized log storage (ELK, etc.)
