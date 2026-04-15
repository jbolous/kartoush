#!/usr/bin/env bash
set -euo pipefail

VERBOSE="false"
DRY_RUN="false"
declare -a ISSUE_NUMBERS=()
MAX_RETRY_COUNT=3

print_help() {
  cat <<'EOF'
Usage:
  .github/scripts/requeue-failed-imports.sh [options] [issue_number[,issue_number...]] ...

Options:
  -n, --dry-run  Show what would be requeued without moving files
  -v, --verbose  Print verbose diagnostic output
  -h, --help     Show this help text

Arguments:
  issue_number  Requeue only the specified failed issue number
                Comma-separated values are also supported

Examples:
  .github/scripts/requeue-failed-imports.sh
  .github/scripts/requeue-failed-imports.sh --dry-run
  .github/scripts/requeue-failed-imports.sh 159 160 161
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--dry-run)
      DRY_RUN="true"
      shift
      ;;
    -v|--verbose)
      VERBOSE="true"
      shift
      ;;
    -h|--help)
      print_help
      exit 0
      ;;
    --*)
      echo "Unknown argument: $1" >&2
      printf '\n' >&2
      print_help >&2
      exit 1
      ;;
    *)
      ISSUE_NUMBERS+=( "${1//,/}" )
      shift
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
preflight_requeue

print_verbose "REPO_ROOT=$REPO_ROOT"
print_verbose "FAILED_DIR=$FAILED_DIR"
print_verbose "PENDING_DIR=$PENDING_DIR"
print_verbose "IMPORTED_DIR=$IMPORTED_DIR"
print_verbose "DRY_RUN=$DRY_RUN"

moved=0
skipped=0
not_found=0

restore_failed_file() {
  local file="$1"
  local base_name original_name target_file

  base_name="$(basename "$file")"
  printf '%s\n' "$base_name"

  parse_trailing_metadata "$file"

  if [[ -n "$META_RETRY_COUNT" ]] && (( META_RETRY_COUNT >= MAX_RETRY_COUNT )); then
    print_warning "  Skipping because retry limit was reached (RetryCount=$META_RETRY_COUNT)"
    skipped=$((skipped + 1))
    printf '\n'
    return
  fi

  original_name="$(printf '%s\n' "$base_name" | sed -E 's/^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}-[0-9]{2}-[0-9]{2}-(issue-[0-9]+-)?//')"
  target_file="$PENDING_DIR/$original_name"

  print_verbose "Restoring file as $target_file"

  if [[ "$DRY_RUN" == "true" ]]; then
    if [[ "$META_ISSUE_CREATED" == "true" ]]; then
      print_info "  [DRY RUN] Requeue existing issue as $original_name"
    else
      print_info "  [DRY RUN] Requeue as $original_name"
    fi
    moved=$((moved + 1))
    printf '\n'
    return
  fi

  if [[ "$META_ISSUE_CREATED" == "true" ]]; then
    cp "$file" "$target_file"
  else
    sed '/^<!--$/,/^-->$/d' "$file" > "$target_file"
  fi
  rm -f "$file"

  if [[ "$META_ISSUE_CREATED" == "true" ]]; then
    print_success "  Requeued existing issue as $original_name"
  else
    print_success "  Requeued as $original_name"
  fi
  moved=$((moved + 1))
  printf '\n'
}

restore_issue_by_number() {
  local issue_number="$1"
  local matches file base_name original_name target_file

  mapfile -t matches < <(grep -l "^Issue: #$issue_number\$" "$FAILED_DIR"/*.md 2>/dev/null || true)

  if [[ ${#matches[@]} -eq 0 ]]; then
    print_warning "Issue #$issue_number not found in failed/"
    not_found=$((not_found + 1))
    return
  fi

  if [[ ${#matches[@]} -gt 1 ]]; then
    print_warning "Multiple failed files matched issue #$issue_number"
    skipped=$((skipped + 1))
    return
  fi

  file="${matches[0]}"
  base_name="$(basename "$file")"
  printf '%s\n' "$base_name"

  original_name="$(printf '%s\n' "$base_name" | sed -E 's/^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}-[0-9]{2}-[0-9]{2}-(issue-[0-9]+-)?//')"
  target_file="$PENDING_DIR/$original_name"

  print_verbose "Restoring failed issue #$issue_number as $target_file"

  if [[ "$DRY_RUN" == "true" ]]; then
    print_info "  [DRY RUN] Requeue existing issue as $original_name"
    moved=$((moved + 1))
    printf '\n'
    return
  fi

  cp "$file" "$target_file"
  rm -f "$file"

  print_success "  Requeued existing issue as $original_name"
  moved=$((moved + 1))
  printf '\n'
}

files=( "$FAILED_DIR"/*.md )

print_header "Requeueing failed imports"
printf '%s\n\n' "-------------------------"

if [[ ${#ISSUE_NUMBERS[@]} -gt 0 ]]; then
  for issue_number in "${ISSUE_NUMBERS[@]}"; do
    restore_issue_by_number "$issue_number"
  done
else
  [[ -e "${files[0]}" ]] || {
    print_warning "No failed files to requeue"
    exit 0
  }

  for file in "${files[@]}"; do
    restore_failed_file "$file"
  done
fi

print_header "Summary"
printf '%s\n' "-------"
printf 'Moved:   %s\n' "$moved"
printf 'Skipped: %s\n' "$skipped"
if [[ ${#ISSUE_NUMBERS[@]} -gt 0 ]]; then
  printf 'Missing: %s\n' "$not_found"
fi
