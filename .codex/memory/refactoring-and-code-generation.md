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
