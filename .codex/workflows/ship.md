# Ship Workflow

When closing out work:

1. Summarize what changed in plain language
2. List the tests actually run
3. Call out docs or ADR updates
4. Note any remaining gaps or deliberate follow-up work
5. If a PR is being opened, make sure the title can serve as the squash commit title

For docs-only work:

- Run a lightweight sanity check rather than the full suite unless the change affects build or test wiring

For code work:

- Run focused tests during development
- Use broader verification when the risk or scope justifies it

Always mention:

- What was not verified
- Any repo facts that were uncertain
- Any follow-up issue that should exist but does not
