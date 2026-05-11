# Codex Agent Guidance

You are working in the Kartoush repository.

Your primary objective is to complete tasks accurately with minimal:
- token usage
- repository scanning
- shell commands
- retries
- unnecessary testing

## Start Here

Read only:
- `.codex/memory/architecture.md`
- `.codex/memory/conventions.md`
- `.codex/workflows/execution.md`

Read additional memory files only if directly relevant to the current task.

Do NOT preload all documentation or workflow files.

---

# Core Rules

## Repository Exploration

- Read the minimum number of files required
- Prefer targeted searches over broad scans
- Avoid repeatedly opening the same files
- Avoid recursive repository exploration unless necessary
- Do not inspect unrelated modules

## Implementation Style

- Keep changes narrow and focused
- Preserve existing module boundaries
- Prefer explicit and simple implementations
- Avoid speculative refactors
- Avoid introducing new architectural patterns unnecessarily
- Match existing project conventions

## Testing

Testing priority:
1. Single focused test
2. Small related test class
3. Module-level validation only if necessary

Never run:
- `./gradlew build`
- `./gradlew test`
- `./gradlew verifyAll`

unless explicitly requested or clearly required.

Prefer targeted commands such as:

```bash
./gradlew :customer:test --tests CustomerServiceTest
```

or

```bash
./gradlew :customer:integrationTest --tests ActivationTokenEntityRepositoryTest
```

Do not rerun large test suites after small fixes unless the failure indicates broader impact.

## Shell Efficiency

Before running commands:
- Think through quoting and paths carefully
- Prefer exact file paths
- Avoid retry loops
- Avoid expensive recursive commands

If a command fails:
1. Briefly identify the root cause
2. Correct the issue
3. Retry once with the corrected command

Do not enter repeated retry cycles.

## GitHub Workflow

When updating GitHub issues or PRs:
- Preserve existing formatting and structure
- Prefer updating existing tasks over creating duplicates
- Keep summaries concise and actionable

Avoid excessive branch introspection or git status loops.

## Response Style

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
