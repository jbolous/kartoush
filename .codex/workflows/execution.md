# Execution Workflow

This file defines the default execution behavior for Codex while working in Kartoush.

The objective is to complete tasks accurately with minimal:
- Token usage
- Repository scanning
- Shell commands
- Retries
- Unnecessary testing

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
- Exact file paths
- Targeted grep/ripgrep usage
- Narrow module-level searches

Avoid:
- Full repository scans
- Repeated searches for the same symbol
- Loading large unrelated files

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
- Rename broadly used types without strong justification
- Reorganize packages unnecessarily
- Modify unrelated code during focused tasks
- Perform cleanup-only changes unless requested

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
- Explicitly requested
- Build configuration changed
- Broad cross-module impact exists

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
- Preserve formatting
- Avoid duplicate tasks
- Keep summaries concise

## Pull Request Maintenance

When additional commits materially change an existing PR:

- Update the PR title if the scope changed
- Update the PR description to reflect the current implementation
- Remove outdated implementation details
- Add newly introduced behavior, tests, or scope changes
- Keep the PR description aligned with the actual diff

Do not leave PR descriptions stale after significant updates.

Keep PR updates concise and focused on:
- What changed
- Why it changed
- Testing performed

---

# Response Style

Provide:
- Concise summaries
- Exact files changed
- Focused reasoning
- Clear next steps only when necessary

Avoid:
- Long essays
- Repeated explanations
- Generic best-practice commentary
- Unnecessary architectural analysis

---

# Goal

Complete the requested work with the smallest reasonable:
- Context footprint
- Diff size
- Validation scope
- Execution cost

while preserving correctness and Kartoush architectural boundaries.
