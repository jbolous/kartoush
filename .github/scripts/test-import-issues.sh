#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -P "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"

export IMPORT_ISSUES_SOURCE_ONLY="true"
# shellcheck source=/dev/null
source "$REPO_ROOT/.github/scripts/import-issues.sh"

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

assert_eq() {
  local expected="$1"
  local actual="$2"
  local message="$3"

  if [[ "$expected" != "$actual" ]]; then
    printf 'Expected: %s\n' "$expected" >&2
    printf 'Actual:   %s\n' "$actual" >&2
    fail "$message"
  fi
}

assert_file_contains() {
  local file="$1"
  local expected="$2"
  local message="$3"

  grep -Fq "$expected" "$file" || fail "$message"
}

assert_file_not_contains() {
  local file="$1"
  local expected="$2"
  local message="$3"

  if grep -Fq "$expected" "$file"; then
    fail "$message"
  fi
}

test_parse_metadata_reports_specific_invalid_line() {
  local temp_file

  temp_file="$(mktemp)"
  cat > "$temp_file" <<'EOF'
Title: [Task]: Example
Oops: wrong

Body
EOF

  if parse_metadata "$temp_file"; then
    fail "parse_metadata should fail for invalid metadata"
  fi

  assert_eq "Invalid metadata line in $(basename "$temp_file"): Oops: wrong" "$PARSE_METADATA_ERROR" "parse_metadata should preserve the invalid line details"
  rm -f "$temp_file"
}

test_parse_metadata_reports_missing_title() {
  local temp_file

  temp_file="$(mktemp)"
  cat > "$temp_file" <<'EOF'
Parent: Epic: Example

Body
EOF

  if parse_metadata "$temp_file"; then
    fail "parse_metadata should fail when Title is missing"
  fi

  assert_eq "Missing required Title metadata in $(basename "$temp_file")" "$PARSE_METADATA_ERROR" "parse_metadata should preserve the missing title details"
  rm -f "$temp_file"
}

test_lookup_issue_number_by_title_in_dir_matches_literal_title() {
  local temp_dir
  local temp_file
  local title

  temp_dir="$(mktemp -d)"
  temp_file="$temp_dir/2026-05-12T00-00-00-issue-999-01-task-example.md"
  title='[Task]: Support OAuth [GraphQL] + GitHub?'

  cat > "$temp_file" <<EOF
Title: $title

Body

<!--
Imported: 2026-05-12T00:00:00-0500
Issue: #999
URL: https://github.com/jbolous/kartoush/issues/999
-->
EOF

  if ! lookup_issue_number_by_title_in_dir "$temp_dir" "$title"; then
    fail "lookup_issue_number_by_title_in_dir should match literal-safe titles"
  fi

  assert_eq "999" "$LOOKED_UP_PARENT_ISSUE_NUMBER" "lookup_issue_number_by_title_in_dir should resolve the issue number from importer metadata"
  rm -rf "$temp_dir"
}

test_strip_trailing_metadata_comment_preserves_normal_html_comment() {
  local temp_file

  temp_file="$(mktemp)"
  cat > "$temp_file" <<'EOF'
Visible content

<!-- normal trailing comment -->
EOF

  strip_trailing_metadata_comment "$temp_file"

  assert_file_contains "$temp_file" "<!-- normal trailing comment -->" "strip_trailing_metadata_comment should keep normal trailing comments"
  rm -f "$temp_file"
}

test_strip_trailing_metadata_comment_removes_importer_footer_only() {
  local temp_file

  temp_file="$(mktemp)"
  cat > "$temp_file" <<'EOF'
Visible content

<!--
Imported: 2026-05-12T00:00:00-0500
Issue: #999
URL: https://github.com/jbolous/kartoush/issues/999
-->
EOF

  strip_trailing_metadata_comment "$temp_file"

  assert_file_not_contains "$temp_file" "Imported: 2026-05-12T00:00:00-0500" "strip_trailing_metadata_comment should remove importer metadata footers"
  assert_file_contains "$temp_file" "Visible content" "strip_trailing_metadata_comment should preserve the main body"
  rm -f "$temp_file"
}

test_parse_trailing_metadata_ignores_non_importer_comment_lines() {
  local temp_file

  temp_file="$(mktemp)"
  cat > "$temp_file" <<'EOF'
Body

<!--
Note: This is not importer metadata
Issue: #321
-->
EOF

  parse_trailing_metadata "$temp_file"

  assert_eq "" "${META_ISSUE_NUMBER:-}" "parse_trailing_metadata should ignore non-importer trailing comments"
  rm -f "$temp_file"
}

test_parse_trailing_metadata_reads_importer_footer() {
  local temp_file

  temp_file="$(mktemp)"
  cat > "$temp_file" <<'EOF'
Body

<!--
Imported: Failed
Timestamp: 2026-05-12T00:00:00-0500
IssueCreated: true
Issue: #321
URL: https://github.com/jbolous/kartoush/issues/321
RetryCount: 2
Error: Example failure
-->
EOF

  parse_trailing_metadata "$temp_file"

  assert_eq "321" "$META_ISSUE_NUMBER" "parse_trailing_metadata should read importer issue numbers"
  assert_eq "2" "$META_RETRY_COUNT" "parse_trailing_metadata should read importer retry counts"
  assert_eq "Example failure" "$META_ERROR" "parse_trailing_metadata should read importer error summaries"
  rm -f "$temp_file"
}

test_rewrite_dependencies_rejects_non_list_content() {
  local temp_file

  temp_file="$(mktemp)"
  cat > "$temp_file" <<'EOF'
## Dependencies
This should have been a bullet
EOF

  if rewrite_dependencies_in_body_file "$temp_file"; then
    fail "rewrite_dependencies_in_body_file should fail for prose inside Dependencies"
  fi

  assert_eq "Invalid Dependencies entry at body line 2: This should have been a bullet" "$DEPENDENCY_PARSE_ERROR" "rewrite_dependencies_in_body_file should explain malformed dependency prose"
  rm -f "$temp_file"
}

test_rewrite_dependencies_rejects_empty_bullets() {
  local temp_file

  temp_file="$(mktemp)"
  cat > "$temp_file" <<'EOF'
## Dependencies
-    
EOF

  if rewrite_dependencies_in_body_file "$temp_file"; then
    fail "rewrite_dependencies_in_body_file should fail for empty dependency bullets"
  fi

  assert_eq "Empty Dependencies entry at body line 2" "$DEPENDENCY_PARSE_ERROR" "rewrite_dependencies_in_body_file should explain empty dependency bullets"
  rm -f "$temp_file"
}

test_parse_metadata_reports_specific_invalid_line
test_parse_metadata_reports_missing_title
test_lookup_issue_number_by_title_in_dir_matches_literal_title
test_strip_trailing_metadata_comment_preserves_normal_html_comment
test_strip_trailing_metadata_comment_removes_importer_footer_only
test_parse_trailing_metadata_ignores_non_importer_comment_lines
test_parse_trailing_metadata_reads_importer_footer
test_rewrite_dependencies_rejects_non_list_content
test_rewrite_dependencies_rejects_empty_bullets

printf 'Importer regression checks passed.\n'
