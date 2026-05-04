# Plan Workflow

When preparing a plan:

1. State the target module ownership clearly
2. Separate behavior changes from cleanup changes
3. Call out migrations, config, docs, and tests explicitly
4. Keep the plan incremental and reversible where possible
5. If the work crosses module boundaries, explain why the dependency direction is still valid

Good plan shape:

- Module and boundary changes
- Runtime behavior changes
- Test changes
- Documentation or ADR updates
- Risks or open questions

Avoid:

- Mixing opportunistic refactors into behavior work without calling it out
- Introducing new shared modules without explaining why an existing one is not appropriate
- Planning broad framework work when a narrow repo-specific change is enough
