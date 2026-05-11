# Codex Agent Guidance

Use this file as the short entry point for Codex work in Kartoush.

Read these first:

- `.codex/README.md`
- `.codex/memory/architecture.md`
- `.codex/memory/conventions.md`
- `.codex/memory/editorial-style.md`
- `.codex/memory/github-workflow.md`
- `.codex/workflows/explore.md`
- `.codex/workflows/implement.md`
- `.codex/workflows/review.md`

Kartoush-specific reminders:

- Keep changes narrow and boundary-aware
- Remove stale names, duplicate classes, and dead code during refactors
- Remove unnecessary imports introduced or left behind by the change
- Prefer use-case-driven names over DTO-driven names
- Use `Support` for shared validation logic when more than one call site needs it
- Do not introduce `Helper` classes for domain validation shortcuts
- Keep tests aligned with module boundaries
- Prefer dry-run modes before running importer or bulk GitHub tooling
- Do not run state-changing GitHub commands or custom importer scripts without explicit user approval

Efficiency rules:

- Read the smallest useful surface first
- Use targeted `rg` searches instead of broad repo scans
- Reuse `.codex/memory/` and ADR context before re-deriving project rules
- Run the narrowest test slice that proves the change
- Do not rerun broad suites without a concrete reason
- Split unrelated local changes into separate branches or commits
- Keep repo-owned guidance separate from local assistant config
- Avoid repeated review passes unless new information changed the decision
- Keep branch contents aligned with the current task scope
- Batch GitHub body edits into one clean update when possible

Local assistant config:

- Keep editor- or machine-specific Codex config out of the repo unless the repo explicitly chooses to standardize it
