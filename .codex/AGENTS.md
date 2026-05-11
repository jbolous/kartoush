## Codex Refactoring and Code Generation Guidelines

### General Principles
- Prefer complete, boundary-aware changes over local edits
- Update all affected classes, constructors, imports, tests, and call sites
- Do not leave stale names, duplicate classes, or unused support classes
- Remove dead code introduced during refactors
- Keep module boundaries clean (no platform-* -> auth/customer/app dependencies)
- Prefer interfaces at module boundaries and inject abstractions, not implementations

### Naming and Structure
- Use use-case driven names for classes that represent workflows or behaviors
    - Example:
        - CustomerRegistrationValidator
        - CustomerUpdateValidator
        - CustomerPasswordSetupValidator
- Avoid DTO-driven or overly procedural names
    - Example: CreateCustomerInputValidator
- Keep names concise but intention-revealing

### Validation Patterns
- Use Support classes for shared validation logic across multiple flows
- Do not create *Helper classes
- If a Support class is introduced:
    - Update all duplicate call sites to use it
- If only one call site exists:
    - Do not introduce a Support class yet

### Shared Logic and Boundaries
- Shared contracts should live in platform modules
- Domain-specific implementations should live in their owning module
- Consumers should depend on interfaces, not concrete implementations
- Avoid leaking configuration or implementation details across module boundaries

### Testing Rules
- Tests should not depend on configuration or implementation from other modules
- Use test-specific implementations for shared interfaces when needed
- Keep tests aligned with module boundaries

### Cleanup Checklist
After changes, search for and remove or fix:
- Stale class names after renames
- Duplicate support classes
- Unused imports
- Dead code introduced during refactors
- Cross-module dependencies that violate boundaries

### Execution Strategy
- Prefer small, incremental changes over large refactors
- Do not reprocess or re-evaluate previously completed work
- Limit changes to a small set of files per task
- Avoid scanning unrelated parts of the codebase
- Do not attempt to solve multiple concerns in a single step

### Command Execution Rules
- Prefer simple, copy-pasteable shell commands
- Use one command per line unless chaining is necessary
- Do not wrap commands in prose or markdown when the user asks for commands
- When using multiline shell commands, ensure quoting, escaping, and line continuations are valid
- Do not assume permission to run `git`, `gh`, or custom repository scripts
- If a command fails due to permissions, stop and report the exact failure instead of retrying with unrelated changes
- When using `gh`, verify required labels, assignees, milestones, and project fields before creating or updating issues
- When using custom scripts, inspect the script usage first before running it
- Prefer dry-run modes before making bulk GitHub or issue-import changes

### GitHub and Issue Import Rules
- Preserve required GitHub issue labels when creating or updating issues
- For Kartoush issue imports, use the repository’s custom importer format and scripts
- Before running the issue importer, inspect the expected directory structure and script options
- Use dry-run first when available
- Do not manually create issues with `gh` if the task requires the custom importer
- If labels, project fields, or permissions are missing, report the blocker clearly instead of partially completing the import

### Safety Around Repository Commands
- Do not run destructive commands unless explicitly requested
- Do not run `git push`, `gh issue create`, `gh pr create`, or custom importer scripts without confirming the command is properly formatted
- Show the exact command before executing when the command changes GitHub state

### Command Permissions
- Codex may run non-destructive local commands without asking:
    - `./gradlew test`
    - `./gradlew verifyAll`
    - `./gradlew :customer:test`
    - `./gradlew :app:test`
    - `grep`
    - `find`
    - `git status`
    - `git diff`
- Codex must not run state-changing GitHub commands without explicit user approval:
    - `git push`
    - `gh issue create`
    - `gh issue edit`
    - `gh pr create`
    - Custom issue importer scripts
