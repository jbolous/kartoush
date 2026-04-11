#!/usr/bin/env bash
set -euo pipefail

VERBOSE="false"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    --verbose)
      VERBOSE="true"
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

SOURCE="${BASH_SOURCE[0]}"
while [[ -L "$SOURCE" ]]; do
  SCRIPT_PATH="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ "$SOURCE" != /* ]] && SOURCE="$SCRIPT_PATH/$SOURCE"
done

SCRIPT_PATH="$(cd -P "$(dirname "$SOURCE")" && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_PATH/issue-import-common.sh"

setup_repo_paths
preflight_import

print_verbose "REPO=$REPO"
print_verbose "REPO_ROOT=$REPO_ROOT"
print_verbose "IMPORT_ROOT=$IMPORT_ROOT"
print_verbose "PENDING_DIR=$PENDING_DIR"
print_verbose "IMPORTED_DIR=$IMPORTED_DIR"
print_verbose "FAILED_DIR=$FAILED_DIR"
print_verbose "DRY_RUN=$DRY_RUN"

declare -A FILE_PARENT
declare -A FILE_ISSUE_NUMBER
declare -A FILE_ISSUE_ID
declare -A FILE_ISSUE_URL
declare -A TITLE_TO_ISSUE_NUMBER

created_count=0
linked_count=0
failed_count=0

parse_metadata() {
  local file="$1"
  local title=""
  local parent=""
  local body_file
  local in_metadata="true"

  body_file="$(mktemp)"

  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$in_metadata" == "true" ]]; then
      if [[ -z "$line" ]]; then
        in_metadata="false"
        continue
      elif [[ "$line" == Title:* ]]; then
        title="$(trim "${line#Title:}")"
      elif [[ "$line" == Parent:* ]]; then
        parent="$(trim "${line#Parent:}")"
      else
        print_verbose "Invalid metadata line in $(basename "$file"): $line"
        rm -f "$body_file"
        return 1
      fi
    else
      printf '%s\n' "$line" >> "$body_file"
    fi
  done < "$file"

  if [[ -z "$title" ]]; then
    print_verbose "Missing required Title metadata in $(basename "$file")"
    rm -f "$body_file"
    return 1
  fi

  PARSED_TITLE="$title"
  PARSED_PARENT="$parent"
  PARSED_BODY_FILE="$body_file"
}

parse_existing_issue_metadata() {
  local file="$1"
  parse_trailing_metadata "$file"
  EXISTING_ISSUE_NUMBER="$META_ISSUE_NUMBER"
  EXISTING_ISSUE_URL="$META_URL"
}

append_footer() {
  local body_file="$1"
  printf '\n\n---\n\n<sub>Created via Kartoush issue import workflow</sub>\n' >> "$body_file"
}

lookup_issue_rest_id() {
  local issue_number="$1"
  local issue_id
  local gh_exit_code

  set +e
  issue_id="$(
    gh api \
      "/repos/$REPO/issues/$issue_number" \
      --jq '.id' 2>&1
  )"
  gh_exit_code=$?
  set -e

  if [[ $gh_exit_code -ne 0 ]]; then
    print_verbose "gh api issue lookup failed: $issue_id"
    return 1
  fi

  LOOKED_UP_ISSUE_ID="$issue_id"
}

lookup_issue_number_by_title_in_dir() {
  local dir="$1"
  local title="$2"
  local matches file

  mapfile -t matches < <(grep -l "^Title: $title\$" "$dir"/*.md 2>/dev/null || true)

  if [[ ${#matches[@]} -eq 0 ]]; then
    return 1
  fi

  if [[ ${#matches[@]} -gt 1 ]]; then
    print_verbose "Multiple files matched title '$title' in $dir"
    return 1
  fi

  file="${matches[0]}"
  parse_trailing_metadata "$file"

  if [[ -z "$META_ISSUE_NUMBER" ]]; then
    print_verbose "Matched title '$title' in $file but no issue number was present"
    return 1
  fi

  LOOKED_UP_PARENT_ISSUE_NUMBER="$META_ISSUE_NUMBER"
}

lookup_existing_parent_issue_number() {
  local parent_title="$1"

  if lookup_issue_number_by_title_in_dir "$IMPORTED_DIR" "$parent_title"; then
    print_verbose "Resolved parent by imported metadata: '$parent_title' -> #$LOOKED_UP_PARENT_ISSUE_NUMBER"
    return 0
  fi

  if lookup_issue_number_by_title_in_dir "$FAILED_DIR" "$parent_title"; then
    print_verbose "Resolved parent by failed metadata: '$parent_title' -> #$LOOKED_UP_PARENT_ISSUE_NUMBER"
    return 0
  fi

  return 1
}

move_to_failed() {
  local file="$1"
  local error_message="$2"
  local issue_created="${3:-false}"
  local issue_number="${4:-}"
  local issue_url="${5:-}"
  local base_name
  local failed_at
  local filename_ts
  local failed_file
  local retry_count=1

  base_name="$(basename "$file")"
  failed_count=$((failed_count + 1))

  if [[ "$DRY_RUN" == "true" ]]; then
    print_warning "  [DRY RUN] Move to failed"
    printf '    Error: %s\n' "$error_message"
    return
  fi

  failed_at="$(timestamp_for_metadata)"
  filename_ts="$(timestamp_for_filename)"
  failed_file="$FAILED_DIR/${filename_ts}-${base_name}"

  parse_trailing_metadata "$file"
  if [[ -n "$META_RETRY_COUNT" ]]; then
    retry_count=$((META_RETRY_COUNT + 1))
  fi

  strip_trailing_metadata_comment "$file"

  {
    cat "$file"
    printf '\n\n<!--\n'
    printf 'Imported: FAILED\n'
    printf 'Timestamp: %s\n' "$failed_at"
    printf 'IssueCreated: %s\n' "$issue_created"
    if [[ -n "$issue_number" ]]; then
      printf 'Issue: #%s\n' "$issue_number"
    fi
    if [[ -n "$issue_url" ]]; then
      printf 'URL: %s\n' "$issue_url"
    fi
    printf 'RetryCount: %s\n' "$retry_count"
    printf 'Error: %s\n' "$error_message"
    printf '%s\n' '-->'
  } > "$failed_file"

  rm -f "$file"
  print_warning "  Failed"
}

move_to_imported() {
  local file="$1"
  local issue_number="$2"
  local issue_url="$3"
  local base_name

  base_name="$(basename "$file")"

  if [[ "$DRY_RUN" == "true" ]]; then
    print_info "  [DRY RUN] Move to imported"
    return
  fi

  local imported_at filename_ts imported_file
  imported_at="$(timestamp_for_metadata)"
  filename_ts="$(timestamp_for_filename)"
  imported_file="$IMPORTED_DIR/${filename_ts}-issue-${issue_number}-${base_name}"

  strip_trailing_metadata_comment "$file"

  {
    cat "$file"
    printf '\n\n<!--\n'
    printf 'Imported: %s\n' "$imported_at"
    printf 'Issue: #%s\n' "$issue_number"
    printf 'URL: %s\n' "$issue_url"
    printf '%s\n' '-->'
  } > "$imported_file"

  rm -f "$file"
  print_success "  Imported (#$issue_number)"
}

create_issue() {
  local file="$1"
  local title="$2"
  local body_file="$3"

  append_footer "$body_file"

  if [[ "$DRY_RUN" == "true" ]]; then
    local key
    local dry_run_issue_number
    key="$(basename "$file" .md)"
    dry_run_issue_number=$((1000 + created_count))
    print_info "  [DRY RUN] Create issue"
    printf '    Title: %s\n' "$title"
    CREATED_ISSUE_URL="dry-run://$key"
    CREATED_ISSUE_NUMBER="$dry_run_issue_number"
    CREATED_ISSUE_ID="DRY-ID-$dry_run_issue_number"
    created_count=$((created_count + 1))
    return 0
  fi

  local gh_output issue_url issue_number

  print_verbose "Executing: gh issue create --repo $REPO --title \"$title\" --body-file $body_file"
  set +e
  gh_output="$(gh issue create --repo "$REPO" --title "$title" --body-file "$body_file" 2>&1)"
  local gh_exit_code=$?
  set -e

  if [[ $gh_exit_code -ne 0 ]]; then
    print_verbose "gh issue create failed: $gh_output"
    return 1
  fi

  issue_url="$(printf '%s\n' "$gh_output" | tail -n 1 | tr -d '\r')"
  issue_number="$(echo "$issue_url" | sed -E 's#.*/issues/([0-9]+)$#\1#')"

  if ! lookup_issue_rest_id "$issue_number"; then
    return 1
  fi

  CREATED_ISSUE_URL="$issue_url"
  CREATED_ISSUE_NUMBER="$issue_number"
  CREATED_ISSUE_ID="$LOOKED_UP_ISSUE_ID"

  created_count=$((created_count + 1))
  print_success "  Created (#$issue_number)"
  printf '    URL: %s\n' "$issue_url"
  print_verbose "Issue ID: $LOOKED_UP_ISSUE_ID"
}

resolve_parent_issue_number() {
  local parent_raw="$1"

  if [[ "$parent_raw" =~ ^#([0-9]+)$ ]]; then
    RESOLVED_PARENT_ISSUE_NUMBER="${BASH_REMATCH[1]}"
    print_verbose "Resolved parent directly from issue number: #$RESOLVED_PARENT_ISSUE_NUMBER"
    return 0
  fi

  if [[ -n "${TITLE_TO_ISSUE_NUMBER[$parent_raw]:-}" ]]; then
    RESOLVED_PARENT_ISSUE_NUMBER="${TITLE_TO_ISSUE_NUMBER[$parent_raw]}"
    print_verbose "Resolved parent by title match: '$parent_raw' -> #$RESOLVED_PARENT_ISSUE_NUMBER"
    return 0
  fi

  if lookup_existing_parent_issue_number "$parent_raw"; then
    RESOLVED_PARENT_ISSUE_NUMBER="$LOOKED_UP_PARENT_ISSUE_NUMBER"
    return 0
  fi

  print_verbose "Could not resolve parent: $parent_raw"
  return 1
}

link_sub_issue() {
  local parent="$1"
  local child_id="$2"
  local gh_output
  local gh_exit_code

  printf '  Linking sub-issue\n'

  if [[ "$DRY_RUN" == "true" ]]; then
    printf '    [DRY RUN] Parent: #%s\n' "$parent"
    printf '    [DRY RUN] Child ID: %s\n' "$child_id"
    linked_count=$((linked_count + 1))
    return
  fi

  print_verbose "Executing: gh api --method POST /repos/$REPO/issues/$parent/sub_issues -F sub_issue_id=$child_id"

  set +e
  gh_output="$(
    gh api \
      --method POST \
      -H "Accept: application/vnd.github+json" \
      "/repos/$REPO/issues/$parent/sub_issues" \
      -F sub_issue_id="$child_id" 2>&1
  )"
  gh_exit_code=$?
  set -e

  if [[ $gh_exit_code -ne 0 ]]; then
    print_verbose "gh api sub-issue link failed: $gh_output"
    return 1
  fi

  linked_count=$((linked_count + 1))
  print_success "    Linked"
}

files=( "$PENDING_DIR"/*.md )
[[ -e "${files[0]}" ]] || {
  print_warning "No pending markdown files found in $PENDING_DIR"
  exit 0
}

print_header "Pass 1: Creating issues"
printf '%s\n\n' "-----------------------"

for file in "${files[@]}"; do
  base="$(basename "$file")"
  printf '%s\n' "$base"

  if ! parse_metadata "$file"; then
    move_to_failed "$file" "Metadata error"
    printf '\n'
    continue
  fi

  print_verbose "Parsed title: $PARSED_TITLE"
  if [[ -n "$PARSED_PARENT" ]]; then
    print_verbose "Parsed parent: $PARSED_PARENT"
  else
    print_verbose "Parsed parent: <none>"
  fi

  FILE_PARENT["$file"]="$PARSED_PARENT"

  parse_existing_issue_metadata "$file"

  if [[ -n "$EXISTING_ISSUE_NUMBER" ]]; then
    print_info "  Reusing existing issue #$EXISTING_ISSUE_NUMBER"

    if ! lookup_issue_rest_id "$EXISTING_ISSUE_NUMBER"; then
      rm -f "$PARSED_BODY_FILE"
      move_to_failed "$file" "Existing issue lookup failed" "true" "$EXISTING_ISSUE_NUMBER" "$EXISTING_ISSUE_URL"
      printf '\n'
      continue
    fi

    FILE_ISSUE_NUMBER["$file"]="$EXISTING_ISSUE_NUMBER"
    FILE_ISSUE_ID["$file"]="$LOOKED_UP_ISSUE_ID"
    FILE_ISSUE_URL["$file"]="$EXISTING_ISSUE_URL"
    TITLE_TO_ISSUE_NUMBER["$PARSED_TITLE"]="$EXISTING_ISSUE_NUMBER"

    rm -f "$PARSED_BODY_FILE"
    printf '\n'
    continue
  fi

  if ! create_issue "$file" "$PARSED_TITLE" "$PARSED_BODY_FILE"; then
    rm -f "$PARSED_BODY_FILE"
    move_to_failed "$file" "Create failed"
    printf '\n'
    continue
  fi

  FILE_ISSUE_NUMBER["$file"]="$CREATED_ISSUE_NUMBER"
  FILE_ISSUE_ID["$file"]="$CREATED_ISSUE_ID"
  FILE_ISSUE_URL["$file"]="$CREATED_ISSUE_URL"
  TITLE_TO_ISSUE_NUMBER["$PARSED_TITLE"]="$CREATED_ISSUE_NUMBER"

  rm -f "$PARSED_BODY_FILE"
  printf '\n'
done

printf '%s\n' "Pass 2: Linking sub-issues and moving files"
printf '%s\n\n' "-------------------------------------------"

for file in "${files[@]}"; do
  [[ -n "${FILE_ISSUE_NUMBER[$file]:-}" ]] || continue

  base="$(basename "$file")"
  printf '%s\n' "$base"

  parent="${FILE_PARENT[$file]}"
  num="${FILE_ISSUE_NUMBER[$file]}"
  id="${FILE_ISSUE_ID[$file]}"
  url="${FILE_ISSUE_URL[$file]}"

  if [[ -n "$parent" ]]; then
    if resolve_parent_issue_number "$parent"; then
      if ! link_sub_issue "$RESOLVED_PARENT_ISSUE_NUMBER" "$id"; then
        move_to_failed "$file" "Sub-issue linking failed" "true" "$num" "$url"
        print_warning "  Issue #$num was created but sub-issue linkage failed"
        printf '\n'
        continue
      fi
    else
      move_to_failed "$file" "Parent resolution failed" "true" "$num" "$url"
      print_warning "  Issue #$num was created but parent resolution failed"
      printf '\n'
      continue
    fi
  fi

  move_to_imported "$file" "$num" "$url"
  printf '\n'
done

printf '%sSummary%s\n' "$COLOR_BOLD" "$COLOR_RESET"
printf '%s\n' "-------"
printf '%sCreated:%s %s\n' "$COLOR_BOLD" "$COLOR_RESET" "$created_count"
printf '%sLinked:%s  %s\n' "$COLOR_BOLD" "$COLOR_RESET" "$linked_count"
if (( failed_count > 0 )); then
  print_error "${COLOR_BOLD}Failed:${COLOR_RESET}  $failed_count"
else
  print_info "${COLOR_BOLD}Failed:${COLOR_RESET}  $failed_count"
fi
