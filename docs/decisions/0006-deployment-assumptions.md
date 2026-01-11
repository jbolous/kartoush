# Deployment Assumptions

## Status
Proposed

## Context

Kartoush is adopting a modular monolith architecture with explicit module
boundaries, module-owned data, and a sync-first, event-enabled integration
style. Deployment assumptions influence future choices around configuration,
packaging, observability, and how environments are represented.

This decision defines the baseline deployment shape and environment assumptions
for the project while intentionally deferring infrastructure-specific choices.

---

## Decision

Adopt the following baseline deployment assumptions:

- **Single deployable** initially (one application artifact, one runtime)
- **Multiple environments** exist conceptually: local development, test, and production
- **Configuration is externalized** per environment (no environment-specific code branches)
- **Stateless application process** where possible (state lives in owned data stores)
- **Packaging supports containerization**, but container orchestrator choice is deferred

---

## Environment Assumptions

### Local development
- Developers can run the application locally with minimal dependencies.
- External services should have reasonable local equivalents or substitutes.

### Test
- The system can be deployed in a test environment representative of production.
- Automated tests and verification should be able to run consistently in this environment.

### Production
- Production deployments are repeatable and versioned.
- Operational concerns (monitoring, logging, alerts) are expected, but the specific
  tooling is not decided here.

---

## Configuration Assumptions

- Configuration is supplied at runtime (environment variables, config files, or
  equivalent mechanisms).
- Secrets are not stored in the repository.
- Defaults should be safe for local development, with explicit overrides for
  test and production.

---

## What Is Intentionally Deferred

This decision does not choose:
- cloud provider
- container orchestrator (Kubernetes, etc.)
- CI/CD platform details
- specific observability stack (logs, metrics, tracing)
- specific database or messaging technologies

These will be decided as implementation needs become clearer.

---

## Consequences

- Implementation should avoid assuming multiple deployables or service-to-service
  networking early.
- Configuration management becomes a first-class concern from the start.
- Container-friendly packaging is supported without committing to a specific
  runtime platform.

---

## Guardrails

- Do not hardcode environment-specific behavior in application logic.
- Do not store secrets in the repository.
- Treat local, test, and production environments as first-class, even if they
  are initially minimal.

---

## Notes

These assumptions may evolve if the project scope changes, but the default
position is to keep deployment simple until concrete needs justify additional
complexity.
