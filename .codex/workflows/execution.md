# Execution Workflow

This file defines the default execution behavior for Codex while working in Kartoush.

The objective is to complete tasks accurately with minimal:
- token usage
- repository scanning
- shell commands
- retries
- unnecessary testing

---

# Repository Exploration

## Read Minimally

- Read the minimum number of files required
- Prefer targeted searches over broad scans
- Avoid repeatedly opening the same files
- Avoid recursive repository exploration unless necessary
- Do not inspect unrelated modules

## Search Strategy

Prefer:
- exact file paths
- targeted grep/ripgrep usage
- narrow module-level searches

Avoid:
- full repository scans
- repeated searches for the same symbol
- loading large unrelated files

---

# Implementation Style

## Keep Changes Focused

- Keep diffs narrow and task-oriented
- Preserve existing module boundaries
- Match existing project conventions
- Prefer explicit implementations over abstraction
- Avoid speculative refactors
- Avoid introducing new frameworks or patterns unnecessarily

## Refactoring Constraints

Do not:
- rename broadly used types without strong justification
- reorganize packages unnecessarily
- modify unrelated code during focused tasks
- perform cleanup-only changes unless requested

---

# Testing Workflow

## Testing Priority

Run the smallest useful validation first:

1. Single focused test
2. Small related test class
3. Module-level validation only if necessary

## Avoid Expensive Validation

Never run:
- `./gradlew build`
- `./gradlew test`
- `./gradlew verifyAll`

unless:
- explicitly requested
- build configuration changed
- broad cross-module impact exists

Prefer targeted commands such as:

```bash
./gradlew :customer:test --tests CustomerServiceTest
```

or

```bash
./gradlew :customer:integrationTest --tests ActivationTokenEntityRepositoryTest
```

Do not rerun large test suites after small fixes unless failures indicate broader impact.

Formatting-only, comment-only, documentation-only, and non-functional refactors do not require rerunning integration tests unless runtime behavior changed.

---

# Shell Efficiency

## Before Running Commands

- Think through quoting and paths carefully
- Prefer exact file paths
- Avoid retry loops
- Avoid expensive recursive commands

## Failure Handling

If a command fails:

1. Briefly identify the root cause
2. Correct the issue
3. Retry once with the corrected command

Do not enter repeated retry cycles.

---

# Git Workflow

- Keep git operations minimal
- Avoid repeated `git status` checks
- Avoid unnecessary branch introspection
- Avoid rebasing unless explicitly required

When updating GitHub issues or PRs:
- preserve formatting
- avoid duplicate tasks
- keep summaries concise

## Pull Request Maintenance

When additional commits materially change an existing PR:

- Update the PR title if the scope changed
- Update the PR description to reflect the current implementation
- Remove outdated implementation details
- Add newly introduced behavior, tests, or scope changes
- Keep the PR description aligned with the actual diff

Do not leave PR descriptions stale after significant updates.

Keep PR updates concise and focused on:
- what changed
- why it changed
- testing performed

---

# Response Style

Provide:
- concise summaries
- exact files changed
- focused reasoning
- clear next steps only when necessary

Avoid:
- long essays
- repeated explanations
- generic best-practice commentary
- unnecessary architectural analysis

---

# Goal

Complete the requested work with the smallest reasonable:
- context footprint
- diff size
- validation scope
- execution cost

while preserving correctness and Kartoush architectural boundaries.
