# Issue Import Workflow

This directory supports a small GitHub issue import workflow for Kartoush.
It is designed for importing pre-written issue definitions from markdown files,
creating the issues in GitHub, linking sub-issues to parent issues, and keeping
the file lifecycle visible in the repository.

## Configuration

Importer settings are loaded in this precedence order:

- shell environment variables
- `.github/issue-import/config.local.env`
- `.github/issue-import/config.env`
- script fallback defaults for supported settings

Use this pattern:

- keep shared repo defaults in `config.env`
- copy `config.local.env.example` to `config.local.env` for machine-specific overrides
- do not commit `config.local.env`
- treat the default imported status as a fixed importer rule: `Backlog`

This avoids editing tracked project settings locally and accidentally overwriting them in Git.

## Directory Layout

- `pending/`
  Contains markdown files waiting to be imported.
- `imported/`
  Contains files that were successfully imported and annotated with GitHub issue
  metadata.
- `failed/`
  Contains files that could not be fully processed and were annotated with
  failure details.
- `templates/`
  Contains starter markdown templates for creating new epic and task files.

The `.gitignore` in this directory keeps these folders in the repository while
allowing imported artifacts to be ignored by default, except for `.gitkeep` and
this README.

## File Format

Each issue definition must be a markdown file in `pending/` with a small
metadata header followed by a blank line and then the issue body.

The easiest way to start a new import file is to copy one of the templates in
`templates/` and rename it with the numeric prefix you want to use in
`pending/`.

Use a numeric filename prefix to make import order explicit, for example:

- `00-epic-import-and-workflow-tooling.md`
- `01-task-validate-sub-issue-linking.md`

The import script creates all issues in pass 1 and links parent-child
relationships in pass 2, so linking does not happen immediately after each
issue is created. Even so, parent titles are resolved against issues that were
recorded earlier in the same run, so epics should still appear before their
children in `pending/`. In practice this also matches how the work is usually
numbered, with the epic using a lower prefix than its tasks.

Required metadata:

- `Title:`

Optional metadata:

- `Parent:`
- `Labels:`
- `Status:`

Rules:

- metadata must appear at the top of the file
- metadata must be one key per line
- metadata parsing stops at the first blank line
- any non-empty, non-metadata line before the first blank line is treated as an
  error
- `Title:` is required
- `Parent:` is optional
- `Labels:` is optional and accepts comma-separated label names
- `Status:` is optional and defaults to `Backlog`
- dependency references belong in a markdown `Dependencies` section in the body,
  not in the metadata header

Example epic:

```md
Title: Epic: Issue Import and Workflow Tooling
Labels: enhancement, documentation

### Goal

Validate that the import script correctly creates an epic and assigns child
issues as sub-issues.

### Why

- verify parent-child relationships are created automatically
- confirm metadata parsing works correctly
- ensure sub-issue API integration behaves as expected
```

Example child task:

```md
Title: Task: Validate sub-issue linking during import
Parent: Epic: Issue Import and Workflow Tooling
Labels: enhancement, application
Status: Backlog

### Summary

Ensure that a task is properly linked as a sub-issue to the epic created during
the same import run.

### Scope

- create a task via import script
- link it to the epic using metadata
- verify linkage in GitHub UI
```

Example task with dependencies:

```md
Title: [Task]: Configure compute infrastructure

## Summary

Provision the compute layer after the network is available.

## Dependencies

- 03-task-vpc-and-networking
- #123
```

## Templates

Starter templates are available in:

- `templates/epic-template.md`
- `templates/task-template.md`

The task template includes a `Parent:` example for epic-linked work. Remove
that line when creating a standalone task.

The task template also shows:

- how to use `Parent: #123` when the parent issue number is already known
- where to list `Dependencies` using source file names or GitHub issue numbers that the importer can resolve
- how to provide optional comma-separated `Labels:` metadata
- how to provide optional `Status:` metadata, with `Backlog` used when omitted

Recommended workflow:

1. Copy the appropriate template into `pending/`
2. Rename it with a numeric prefix such as `00-` or `01-`
3. Replace the placeholder title and section content
4. Run `bash .github/scripts/validate-editorial-style.sh`
5. Run a dry run before importing for real

Example:

```bash
cp .github/issue-import/templates/task-template.md \
  .github/issue-import/pending/00-task-example.md
```

The editorial style script checks:

- sentence-case bullets
- suspicious early prose wrapping

It is intended for:

- `.github/issue-import/pending/`
- `.github/issue-import/templates/`
- `.codex/`

You can also print a template directly from the importer:

```bash
.github/scripts/import-issues.sh --print-template epic
.github/scripts/import-issues.sh --print-template task
```

Template files must remain outside `pending/` so they are not imported
accidentally.

## AI Prompt Snippets

If you want ChatGPT or Codex to generate import-ready markdown, give it one of
these prompts and tell it to output raw markdown only.

Prompt for a standalone task:

```text
Generate a GitHub issue import task file for the Kartoush importer.

Requirements:
- Output raw markdown only
- Use this exact metadata header order:
  Title:
  Labels:
  Status:
- Leave a single blank line after metadata
- Use these sections in this exact order:
  ## Summary
  ## Scope
  ## Acceptance Criteria
  ## Dependencies
  ## Notes
- Use flat bullet lists only
- Do not include YAML front matter
- Do not include commentary before or after the markdown
- Do not wrap prose early unless needed for a list item
- When status is unknown, use: Status: Backlog
- When there are no dependencies, use:
  ## Dependencies
  - None

Task details:
[PASTE TASK DETAILS HERE]
```

Prompt for an epic-linked task:

```text
Generate a GitHub issue import task file for the Kartoush importer.

Requirements:
- Output raw markdown only
- Use this exact metadata header order:
  Title:
  Parent:
  Labels:
  Status:
- Leave a single blank line after metadata
- Use these sections in this exact order:
  ## Summary
  ## Scope
  ## Acceptance Criteria
  ## Dependencies
  ## Notes
- Use flat bullet lists only
- Do not include YAML front matter
- Do not include commentary before or after the markdown
- Do not wrap prose early unless needed for a list item
- Use `Parent: #<issue-number>` when the epic number is known
- When status is unknown, use: Status: Backlog
- When there are no dependencies, use:
  ## Dependencies
  - None

Task details:
[PASTE TASK DETAILS HERE]
```

Prompt to review an existing task file for importer compatibility:

```text
Review this markdown file for compatibility with the Kartoush GitHub issue importer.

Check:
1. Metadata header contains only supported keys at the top of the file
2. Metadata header is followed by exactly one blank line
3. Required metadata is present
4. `Labels:` is comma-separated if present
5. `Status:` is a valid Kartoush project status if present
6. Sections are present and ordered correctly
7. `Dependencies` uses bullet list entries
8. No YAML front matter or extra prose appears before metadata ends

Output:
- Valid
- Problems
- Corrected raw markdown

Markdown to review:
[PASTE MARKDOWN HERE]
```

## Parent Resolution

`Parent:` can be defined in one of two ways:

- by title, for example:
  `Parent: Epic: Issue Import and Workflow Tooling`
- by explicit GitHub issue number, for example:
  `Parent: #153`

When a parent title is used, the import script resolves it against issues
created earlier in the same import run. This is what allows an epic and its
children to be imported together from `pending/`.

If the parent cannot be resolved, the child issue may already exist in GitHub,
but the file will be moved to `failed/` and marked for manual follow-up.

## Dependency Resolution

Entries in a `Dependencies` section can be defined in either of these ways:

- by source file name, for example: `03-task-vpc-and-networking`
- by explicit GitHub issue number, for example: `#123`

Source file names are useful when related work is being imported from local
markdown files. GitHub issue numbers are useful when a dependency already
exists in GitHub and is not represented by a local import file.

Dependencies entries must stay as bullet list items. Non-list prose or empty
dependency bullets inside a `Dependencies` section are treated as importer
errors and move the file to `failed/` with a specific message.

## Label Handling

`Labels:` can be defined as a comma-separated metadata value, for example:

```md
Labels: customer, application
```

During import, the script validates requested labels against the repository's
existing GitHub labels.

- valid labels are applied during issue creation
- invalid labels are ignored so they do not block issue creation
- ignored labels are reported as warnings in the importer output
- when the title starts with `[Task]`, `Task:`, `[Epic]`, or `Epic:`, the importer automatically adds the corresponding `task` or `epic` label if it is not already present

## Status Handling

`Status:` can be defined as metadata, for example:

```md
Status: In Progress
```

During import, the script applies the effective status to the GitHub project item.

- when `Status:` is omitted, the importer defaults to `Backlog`
- when `Status:` is valid, that value is applied
- when `Status:` is invalid, the importer warns and falls back to `Backlog`
- dry-run output shows the effective status that would be applied

## Import Script

The main import entrypoint is:

```bash
.github/scripts/import-issues.sh
```

Optional flag:

- `--dry-run`
- `--verbose`
- `--print-template epic|task`

Environment variables supported by the script:

- `REPO`
  Defaults to the resolved value from `config.local.env`, then `config.env`, then `jbolous/kartoush`
- `PROJECT_ID`
  Optional explicit GitHub ProjectV2 node id. When set, the importer uses this project directly.
- `PROJECT_TITLE`
  Defaults to the resolved value from `config.local.env`, then `config.env`, then `Kartoush - MVP`. When `PROJECT_ID` is not set, the importer resolves the target project by exact title match against the repository owner.
- `IMPORT_ROOT`
  Defaults to `.github/issue-import`
- `PENDING_DIR`
  Defaults to `$IMPORT_ROOT/pending`
- `IMPORTED_DIR`
  Defaults to `$IMPORT_ROOT/imported`
- `FAILED_DIR`
  Defaults to `$IMPORT_ROOT/failed`

### What the script does

The import runs in two passes.

Before either pass begins, the script runs import preflight checks.

Pass 1: create issues

- reads each `pending/*.md` file
- parses `Title:` and optional `Parent:`
- parses optional `Labels:`
- parses optional `Status:` and resolves the effective project status
- resolves the target project using `PROJECT_ID` when provided, otherwise by exact `PROJECT_TITLE` match
- appends this footer to the GitHub issue body:

  ```html
  ---

  <sub>Created via Kartoush issue import workflow</sub>
  ```

- creates the issue with `gh issue create`
- applies any valid requested labels during issue creation
- optionally assigns the issue to the authenticated GitHub user when
  `--assign-to-self` is used
- records the created issue number, URL, and internal GitHub issue id for use in
  pass 2

Pass 2: link sub-issues and move files

- applies the effective project status to the GitHub project item
- resolves each `Parent:` value
- resolves dependency source-file references from any `Dependencies` section
- updates issue bodies to replace resolved dependency references with GitHub
  issue references such as `#123`
- links child issues using the GitHub sub-issues API
- logs warnings for unresolved dependencies
- moves successfully processed files to `imported/`
- moves failed files to `failed/`

### Example usage

Dry run:

```bash
.github/scripts/import-issues.sh --dry-run
```

Dry-run performs read-only GitHub validation for importer prerequisites such as authentication and label lookup, but it does not create, edit, or link issues.

Dry run with verbose logging:

```bash
.github/scripts/import-issues.sh --dry-run --verbose
```

Print the epic template:

```bash
.github/scripts/import-issues.sh --print-template epic
```

Print the task template:

```bash
.github/scripts/import-issues.sh --print-template task
```

Import into the default repository:

```bash
.github/scripts/import-issues.sh
```

Import with verbose logging:

```bash
.github/scripts/import-issues.sh --verbose
```

Import and assign newly created issues to yourself:

```bash
.github/scripts/import-issues.sh --assign-to-self
```

Import into a different repository:

```bash
REPO=your-org/your-repo .github/scripts/import-issues.sh
```

Use a local override file for machine-specific settings:

```bash
cp .github/issue-import/config.local.env.example .github/issue-import/config.local.env
```

Import into a different project title owned by the repo owner:

```bash
PROJECT_TITLE="Kartoush - Roadmap" .github/scripts/import-issues.sh
```

Import into an explicit project id:

```bash
PROJECT_ID=PVT_xxxxxxxxxxxxxxxxxx .github/scripts/import-issues.sh
```

### Quick Start

1. Create an epic file in `pending/`, for example:

   ```md
   Title: Epic: Example Import Epic

   ### Goal

   Create an example epic for issue import testing.
   ```

2. Create a child task file in `pending/`, for example:

   ```md
   Title: Task: Example Child Task
   Parent: Epic: Example Import Epic

   ### Summary

   Create a child task linked to the example epic.
   ```

3. Run a dry run:

   ```bash
   .github/scripts/import-issues.sh --dry-run
   ```

4. Review the dry-run output to confirm the files, titles, and parent
   relationships look correct.

5. Run the real import:

   ```bash
   .github/scripts/import-issues.sh
   ```

6. Verify the created issues and sub-issue relationship in GitHub

### Optional: create symbolic links for easier execution

If you run these scripts often, you can create symbolic links with shorter
names.

From the repository root:

```bash
ln -s .github/scripts/import-issues.sh import-issues
ln -s .github/scripts/requeue-failed-imports.sh requeue-failed-imports
```

You can then run:

```bash
./import-issues --dry-run
./requeue-failed-imports
```

If you prefer to run them from anywhere, create links in a directory that is on
your `PATH`, for example:

```bash
ln -s /absolute/path/to/kartoush/.github/scripts/import-issues.sh /usr/local/bin/import-issues
ln -s /absolute/path/to/kartoush/.github/scripts/requeue-failed-imports.sh /usr/local/bin/requeue-failed-imports
```

After that, you can run:

```bash
import-issues --dry-run
requeue-failed-imports
```

## Failure Handling

If something goes wrong, the script moves the source file into `failed/` and
adds a trailing HTML comment block with metadata about the failure.

That metadata can include:

- import status
- timestamp
- whether a GitHub issue was already created
- issue number
- issue URL
- retry count
- error summary

Typical failure cases:

- missing `Title:` metadata
- invalid metadata before the blank-line separator
- malformed `Dependencies` sections
- GitHub issue creation failure
- unresolved `Parent:`
- sub-issue linking failure

There are two important failure modes:

1. No issue was created

- the file can usually be fixed and safely requeued

2. The issue was created, but linking or later processing failed

- the file is still moved to `failed/`
- the failure metadata records `IssueCreated: true`
- the failure metadata records and increments `RetryCount`
- manual cleanup or manual parent linking may be required

## Requeue Script

The requeue entrypoint is:

```bash
.github/scripts/requeue-failed-imports.sh
```

This script moves safe-to-retry files from `failed/` back into `pending/`.

Optional flag:

- `--verbose`

Optional positional arguments:

- one or more GitHub issue numbers to restore from `failed/`

Before processing files, the script runs a lighter preflight check for the
local shell and repository environment.

### Requeue behavior

- failed files are restored from `failed/` back to `pending/`
- files with `IssueCreated: true` keep their metadata comment so the existing
  issue can be reused on the next import run
- files without a created issue have the trailing failed metadata block stripped
- the timestamp prefix added in `failed/` is removed

When issue numbers are passed to the requeue script:

- matching failed files are restored from `failed/` back to `pending/`
- the failed metadata comment is preserved when `IssueCreated: true`
- the original issue is intended to be reused rather than recreated

For normal failed-file requeue:

- files with `RetryCount` 3 or greater are skipped
- this prevents the same file from being requeued indefinitely after repeated failures

When issue numbers are passed explicitly:

- the matching failed file can still be requeued even if it has reached the
  normal retry limit
- this provides an operator override path without requiring manual file edits

Example:

```bash
.github/scripts/requeue-failed-imports.sh
```

Verbose example:

```bash
.github/scripts/requeue-failed-imports.sh --verbose
```

Restore specific failed issues back to `pending/`:

```bash
.github/scripts/requeue-failed-imports.sh 159 160 161
```

Comma-separated input is also accepted:

```bash
.github/scripts/requeue-failed-imports.sh 159, 160, 161
```

If a failed file is skipped because an issue was already created, inspect the
failure metadata and retry count to determine whether manual cleanup is needed.

If a failed file is requeued by issue number and it retains issue metadata, the
import script can detect the existing GitHub issue and skip issue creation on
the next run.

## Prerequisites

Before running the import:

- `gh` must be installed
- `bash` 4 or newer must be available
- the authenticated user must have permission to create issues in the target
  repository
- for sub-issue linking, the repository and token permissions must support the
  GitHub sub-issues API

For real imports, `gh` must be authenticated against GitHub.

If you use `--assign-to-self`, the authenticated user must also be assignable in
the target repository.

For `--dry-run`, `gh` must still be installed and authenticated so the importer
can validate safe read-only GitHub dependencies without mutating issues.

### Install GitHub CLI

macOS with Homebrew:

```bash
brew install gh
```

Windows with `winget`:

```powershell
winget install --id GitHub.cli
```

After installation, authenticate with:

```bash
gh auth login
```

You can verify authentication with:

```bash
gh auth status
```

### Install Bash 4+ on macOS

The import scripts use Bash features that are not supported by the older Bash
version shipped with macOS by default. On macOS, install a newer Bash with
Homebrew:

```bash
brew install bash
```

You can verify your Bash version with:

```bash
bash --version
```

If needed, invoke the scripts with the Homebrew-installed Bash explicitly.

### Script permissions

If your environment does not preserve executable bits, make the scripts
executable before running them:

```bash
chmod +x .github/scripts/import-issues.sh
chmod +x .github/scripts/requeue-failed-imports.sh
```

The shared helper file `.github/scripts/issue-import-common.sh` is sourced by
the scripts and does not need to be executable.

### Preflight checks

Before processing files, the scripts run preflight checks.

The import script checks:

- Bash is available
- Bash 4 or newer is being used
- GitHub CLI (`gh`) is installed
- GitHub CLI is authenticated for real imports
- the current working directory is inside a git repository

In `--dry-run` mode, the GitHub authentication check is skipped.

The requeue script checks only the local environment it needs:

- Bash is available
- Bash 4 or newer is being used
- the current working directory is inside a git repository

### Windows Support

This workflow requires a Bash-compatible environment.

Recommended options:

- Git Bash, which is included with Git for Windows
- Windows Subsystem for Linux (WSL)
- another shell with Bash 4 or newer

The scripts are not supported in native Windows Command Prompt or PowerShell
without a Bash environment.

If you are working on Windows:

- run the scripts from Git Bash, WSL, or another Bash-compatible shell
- use `gh auth login` and `gh auth status` from that same shell environment
- if symbolic link creation is restricted, run the scripts directly instead of
  creating symlink shortcuts
- depending on your Windows configuration, symlink creation may require
  Developer Mode or elevated permissions

## Recommended Workflow

1. Create one or more issue definition files in `pending/`
2. Run the import script with `--dry-run` to validate intent
3. Run the import script without `--dry-run`
4. Confirm created issues and parent-child relationships in GitHub
5. If anything fails, inspect the file in `failed/`
6. Fix and requeue only when no GitHub issue was already created

## Notes

- this workflow creates issues through the GitHub CLI, so the normal GitHub web
  issue template selection flow is bypassed
- imported issue files should still follow the same structure and conventions as
  the repository's issue templates
- shared script helpers live in `.github/scripts/issue-import-common.sh`
- files in `imported/` and `failed/` act as a lightweight audit trail
- pending files that retain imported metadata will reuse the existing GitHub
  issue on the next import run instead of creating a duplicate
- parent titles are resolved only within the current import run, not by querying
  GitHub for title matches
- parent-title linking depends on import order, while `Parent: #123` does not
- if you need to link to an already-existing GitHub issue, prefer `Parent: #123`
