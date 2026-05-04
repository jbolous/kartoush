# GitHub Workflow Memory

## Branching and merge

- Branch naming ADR:
  - `<type>/<scope>-<short-description>`
- Allowed types:
  - `feat`
  - `fix`
  - `chore`
  - `docs`
  - `refactor`
  - `test`
- Use lowercase kebab-case
- Do not include issue numbers in branch names
- Merge strategy is squash-and-merge
- PR titles should read cleanly as the final squashed commit message

## Pull requests

- Keep PRs small and focused
- CI runs on pull requests and on pushes to `main`
- Current required CI shape:
  - Unit tests
  - Auth API integration tests
  - Customer API integration tests
  - Terms API integration tests
  - Customer module integration tests
  - Auth module integration tests

## Issue import workflow

- Importer files live in `.github/issue-import/pending/`
- Imported files move to `.github/issue-import/imported/`
- Failed imports move to `.github/issue-import/failed/`
- Use the templates in `.github/issue-import/templates/`
- Metadata header order matters
- Task files usually use:
  - `Title:`
  - `Parent:` when applicable
  - `Labels:`
  - `Status:`
- Dependencies go in a `## Dependencies` section, not metadata
- Importer default status is `Backlog`
- Run a dry-run before a real import:
  - `.github/scripts/import-issues.sh --dry-run --verbose`

## Existing issue labels

- `epic`
- `task`
- `architecture`
- `documentation`
- `application`
- `platform`
- `infrastructure`
- `integration`
- `testing`
- `ci`
- Plus domain-specific labels such as `aws` and `customer`

## Current issue and epic conventions

- Use one epic for a broader outcome and linked task issues for concrete work
- Prefer updating overlapping issues rather than creating duplicates
- Keep task scope narrow and implementation-ready
- Make dependencies explicit in the issue body when sequencing matters
