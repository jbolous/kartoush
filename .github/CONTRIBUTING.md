# Contributing to Kartoush

Thank you for contributing to Kartoush.

Kartoush is an active modular monolith backend project. The repository already contains production code, integration tests, ADRs, and supporting workflow tooling, so contributions should align with the current implementation rather than treat the repo as design-only.

## Current Expectations

- Keep changes small and focused
- Respect module boundaries
- Prefer updating existing guidance and issues over creating duplicate direction
- Document meaningful architectural tradeoffs in ADRs or issue notes when the reasoning is not obvious from the code

## Where to Start

- Read the root `README.md` for setup, testing, and current project context
- Review ADRs under `docs/architecture/decisions/`
- Check `.codex/README.md` for lightweight repo memory and workflow guidance
- Review the existing issue before starting work so new changes stay aligned with the intended scope

## Contributions That Fit Well

- Feature work tied to an existing task or epic
- Refactors that improve boundaries, clarity, or testability
- Documentation updates that remove stale guidance or explain intentional exceptions
- Test improvements that strengthen existing behavior or close coverage gaps

## Pull Requests

- Keep pull requests narrow enough to review quickly
- Explain what changed, why it changed, and how it was verified
- Use PR titles that work as clean squash-merge commit messages
- Run the relevant Gradle tests for the area you changed
- Do not include local-only workarounds in shared docs or PR testing notes unless other developers need them too

## Current Transition Notes

Kartoush has a few areas that are intentionally mixed while the codebase is being cleaned up.

- The repo is moving toward app-owned HTTP request models and separate facade contracts, but that split is not complete everywhere yet
- Notification delivery now lives in the `notification` module, but durable background job processing is still follow-up work
- Some documentation and workflow rules are newer than older merged code, so align new work with the current direction instead of copying older inconsistencies forward

When you touch one of these areas, either follow the established local pattern or make the cleanup explicit in the task or PR.

## Testing and Validation

- Use `./gradlew verifyAll` for full verification when the change is broad
- Use targeted module and integration tests when the change is narrow and the full suite is unnecessary
- If you edit `.codex/` or issue-import files, run `bash .github/scripts/validate-editorial-style.sh`

## Issues and Planning

- Keep issues focused on one outcome
- Prefer updating overlapping issues instead of creating duplicates
- Make dependencies explicit when one task must land before another
- Use the issue import workflow under `.github/issue-import/` when you are creating new planned work

## Code of Conduct

Be respectful, constructive, and collaborative.

Assume good intent, focus on the work, and prefer clarity over cleverness.
