# Explore Workflow

Before changing code:

1. Read the relevant issue, ADR, and any directly related docs
2. Inspect the owning module and its public facade first
3. Check whether the behavior already has tests
4. Find the boundary lines:
   - Controller vs app service
   - Facade vs internal service
   - Domain vs persistence
   - Module vs module
5. Check current CI or task conventions if the change affects tests or workflows
6. Look for overlapping open issues before creating new issue-import files
7. Check whether the inconsistency is a deliberate tradeoff before treating it as cleanup work

Questions to answer before planning:

- Which module should own this behavior
- Whether the rule is already documented in an ADR or behavior doc
- Whether the change needs a new contract model, exception, migration, or config property
- What the narrowest useful test slice is
- Whether an existing class, issue, or ADR should be updated instead of creating a parallel one
