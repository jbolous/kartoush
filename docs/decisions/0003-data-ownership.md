# 0003 – Data Ownership

## Status
Proposed

## Context

Kartoush is adopting a modular monolith architecture. Strong module boundaries
require an explicit approach to data ownership. Without clear rules, modules
tend to couple through shared tables, shared persistence models, and direct
cross-module database access.

This decision defines what it means for a module to own data, how other modules
can read or update that data, and what patterns are allowed for cross-module
workflows.

---

## Decision

Adopt a **module-owned data** model:

- Each module is the system of record for the data it owns.
- Only the owning module may perform writes (create, update, delete) to its data.
- Other modules must not directly access the owning module’s persistence layer.
- Cross-module access happens through explicit module interfaces.

---

## What Ownership Means

A module owns data when it is responsible for:

- defining the canonical meaning of that data

- validating and enforcing invariants on writes

- exposing approved read views and write operations to other modules

Ownership implies accountability. If rules change, the owning module is where
those changes are made and enforced.

---

## Access Rules

### Writes

- Writes to a module’s data must go through the owning module’s public interface.
- No other module may update another module’s tables, entities, or persistence models.

### Reads

Prefer, in order:

1. Read through an owning module query interface (recommended)
2. Read from a published read model owned by the consuming module (when needed)

Direct database reads across module boundaries are discouraged. If introduced,
they must be treated as an explicit exception and documented.

---

## Cross-Module Workflows

When a workflow touches multiple modules:

- One module acts as the coordinator for the workflow, calling other modules
  through their interfaces.
- Each module remains responsible for validating and applying changes to its own data.
- The coordinator does not bypass module boundaries.

---

## Shared Concepts

Some concepts appear in multiple modules (identifiers, codes, reference values).

Use these rules:

- Share identifiers and immutable reference values freely.
- Do not share mutable persistence models.
- If two modules both need mutable state for the same concept, ownership must
  be clarified: one module owns it, or the concept must be split.

---

## Consequences

- Module boundaries are harder to violate accidentally.
- Refactoring becomes safer because ownership is explicit.
- Some use cases will require interface design rather than direct persistence access.

These tradeoffs are intentional to preserve long-term maintainability.

---

## Guardrails

- No shared mutable tables or entities across modules.
- No cross-module writes except through module interfaces.
- Any exception to these rules must be explicitly documented.

---

## Notes

This decision may be refined once persistence technology and module structure
are chosen, but the ownership principles should remain stable.
