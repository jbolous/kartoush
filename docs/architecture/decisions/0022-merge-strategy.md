# 0022: Merge Strategy

## Status

Accepted

## Context

The repository defines a branch naming convention, but the merge strategy for pull requests must also be explicit.

Without a defined merge strategy, contributors may choose different merge methods, leading to inconsistent history and reduced clarity when reviewing changes over time.

Pull requests are the primary unit of reviewed work. The history on `main` should reflect completed, intentional changes rather than intermediate development steps.

## Decision

Pull requests must be merged using **Squash and merge**.

The pull request title must be written so it can serve as a clean final commit message.

Repository settings must allow **Squash and merge** and disable other merge methods.

## Consequences

### Positive

- produces a clean and readable `main` branch history
- ensures each commit on `main` represents a completed, reviewed change
- removes intermediate and fixup commits from permanent history
- reinforces the importance of well-written pull request titles

### Negative

- removes granular commit history from `main`
- reduces precision when using tools like `git bisect`
- makes partial cherry-picking of changes more difficult
- requires discipline to keep pull requests small and focused

## Notes

Pull requests should remain small and focused so that each squashed commit represents a single logical change.

Exceptions to this strategy should be rare and explicitly justified.
