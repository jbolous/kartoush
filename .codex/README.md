# Codex Guidance for Kartoush

This directory is lightweight working context for Codex and other AI-assisted
sessions in the Kartoush repository.

Use it as:

- `memory/` for durable repo facts that should stay stable across tasks
- `workflows/` for short checklists on how to explore, plan, implement,
  review, and ship work in this repo
- `handoffs/latest.md` as a fill-in template when a session stops mid-task

Notable memory files:

- `memory/editorial-style.md` for writing and formatting rules that are easy to miss in code-focused sessions
- `AGENTS.md` for a short agent-facing entry point into the repo guidance

Keep this directory:

- Keep it concise
- Keep it repo-specific
- Keep it grounded in code, ADRs, and scripts that actually exist
- Keep it free of framework or agent-system complexity

When updating it:

- Prefer facts already visible in `README`, ADRs, Gradle files, module
  structure, tests, and GitHub scripts
- Do not turn it into long-form project documentation
- Update `memory/` when architecture or workflow rules become durable
- Use `handoffs/latest.md` for per-task status, not `memory/`
- Keep local assistant runtime config out of the repo unless the team decides to standardize it

Validation:

- Run `bash .github/scripts/validate-editorial-style.sh` after editing `.codex/` files
- The script checks sentence-case bullets and catches suspicious early prose wrapping in `.codex/` and issue-import markdown
