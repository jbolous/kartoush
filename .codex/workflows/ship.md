# Ship Workflow

When closing out work:

1. Check `git status` and remove unrelated local files from the task branch
2. Confirm the staged files all belong to the current task scope
3. Summarize what changed in plain language
4. List the tests actually run
5. Call out docs or ADR updates
6. Note any remaining gaps or deliberate follow-up work
7. If a PR is being opened, make sure the title can serve as the squash commit title
8. If a PR is being opened, make sure the testing text uses shared commands and established wording

For docs-only work:

- Run a lightweight sanity check rather than the full suite unless the change affects build or test wiring

For code work:

- Run focused tests during development
- Use broader verification when the risk or scope justifies it

Always mention:

- What was not verified
- Any repo facts that were uncertain
- Any follow-up issue that should exist but does not
