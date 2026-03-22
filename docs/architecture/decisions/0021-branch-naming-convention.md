# 0021 - Branch Naming Convention

## Status

Accepted

## Context

Consistent branch naming improves collaboration, readability, and traceability across the codebase.

The project currently has inconsistent naming patterns such as:

- feat/...
- feature/...

Additionally, some branches use nested path structures (e.g., feat/customer/...) while others use flat naming.

A standard convention is needed to:

- align with commit message conventions
- improve clarity in Git tools (GitHub, GitKraken)
- reduce cognitive overhead when scanning branches
- establish a consistent workflow for contributors

## Decision

Branch names will follow this format:

`<type>/<scope>-<short-description>`

### Rules

- Use lowercase kebab-case for all parts
- Use a single "/" separator between type and the rest of the branch name
- Do not use nested path structures (no additional "/")
- Keep names concise but descriptive
- Do not include issue numbers in branch names

### Allowed Types

- feat — new functionality
- fix — bug fixes
- chore — maintenance, build, or tooling changes
- docs — documentation updates
- refactor — code changes without behavior changes
- test — test-related changes

### Examples

- feat/customer-update-semantics
- feat/customer-email-update-endpoint
- fix/customer-soft-delete-bug
- chore/gradle-version-catalog-cleanup
- docs/customer-lifecycle-api
- refactor/customer-service-cleanup
- test/customer-controller-webmvc

## Rationale

- Aligns with Conventional Commits (feat(...), fix(...), etc.)
- Avoids unnecessary hierarchy and complexity in branch names
- Improves readability in tooling and pull request workflows
- Keeps naming predictable and easy to follow

## Consequences

### Positive

- Consistent and predictable branch naming
- Easier navigation and filtering in Git tools
- Reduced ambiguity for contributors

### Negative

- Existing branches may not follow this convention
- Minor overhead to enforce consistency going forward

## Notes

This convention applies to all new branches. Existing branches do not need to be renamed retroactively.
