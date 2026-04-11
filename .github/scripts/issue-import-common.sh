#!/usr/bin/env bash

if [[ -t 1 ]]; then
  COLOR_RED=$'\033[0;31m'
  COLOR_GREEN=$'\033[0;32m'
  COLOR_YELLOW=$'\033[1;33m'
  COLOR_BLUE=$'\033[1;36m'
  COLOR_BOLD=$'\033[1m'
  COLOR_RESET=$'\033[0m'
else
  COLOR_RED=""
  COLOR_GREEN=""
  COLOR_YELLOW=""
  COLOR_BLUE=""
  COLOR_BOLD=""
  COLOR_RESET=""
fi

print_info() {
  printf '%s%s%s\n' "$COLOR_BLUE" "$1" "$COLOR_RESET"
}

print_success() {
  printf '%s%s%s\n' "$COLOR_GREEN" "$1" "$COLOR_RESET"
}

print_warning() {
  printf '%s%s%s\n' "$COLOR_YELLOW" "$1" "$COLOR_RESET"
}

print_error() {
  printf '%s%s%s\n' "$COLOR_RED" "$1" "$COLOR_RESET" >&2
}

print_header() {
  printf '\n%s%s%s\n' "$COLOR_BOLD" "$1" "$COLOR_RESET"
}

print_verbose() {
  if [[ "${VERBOSE:-false}" == "true" ]]; then
    printf '%s[VERBOSE]%s %s\n' "$COLOR_YELLOW" "$COLOR_RESET" "$1"
  fi
}

resolve_script_source() {
  local source="$1"

  while [[ -L "$source" ]]; do
    local script_dir
    script_dir="$(cd -P "$(dirname "$source")" && pwd)"
    source="$(readlink "$source")"
    [[ "$source" != /* ]] && source="$script_dir/$source"
  done

  printf '%s\n' "$source"
}

setup_repo_paths() {
  local source
  source="$(resolve_script_source "${BASH_SOURCE[1]}")"

  SCRIPT_DIR="$(cd -P "$(dirname "$source")" && pwd)"
  REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || pwd)"

  REPO="${REPO:-jbolous/kartoush}"
  IMPORT_ROOT="${IMPORT_ROOT:-$REPO_ROOT/.github/issue-import}"
  PENDING_DIR="${PENDING_DIR:-$IMPORT_ROOT/pending}"
  IMPORTED_DIR="${IMPORTED_DIR:-$IMPORT_ROOT/imported}"
  FAILED_DIR="${FAILED_DIR:-$IMPORT_ROOT/failed}"

  mkdir -p "$PENDING_DIR" "$IMPORTED_DIR" "$FAILED_DIR"
}

preflight_common() {
  if [[ -z "${BASH_VERSION:-}" ]]; then
    print_error "✗ This script must be run with bash"
    exit 1
  fi

  print_success "✓ Bash detected ($BASH_VERSION)"

  if (( BASH_VERSINFO[0] < 4 )); then
    print_error "✗ Bash 4+ is required (associative arrays are used)"
    exit 1
  fi

  if ! git rev-parse --show-toplevel >/dev/null 2>&1; then
    print_error "✗ Not inside a git repository"
    exit 1
  fi

  print_success "✓ Git repository detected"
}

preflight_import() {
  print_header "Running preflight checks..."
  printf '%s\n' "---------------------------"

  preflight_common

  if ! command -v gh >/dev/null 2>&1; then
    print_error "✗ GitHub CLI (gh) is not installed"
    print_info "  Install with: brew install gh"
    exit 1
  fi

  print_success "✓ GitHub CLI found"

  if [[ "${DRY_RUN:-false}" == "true" ]]; then
    print_info "• Skipping GitHub authentication check in dry-run mode"
  elif ! gh auth status >/dev/null 2>&1; then
    print_error "✗ GitHub CLI is not authenticated"
    print_info "  Run: gh auth login"
    exit 1
  else
    print_success "✓ GitHub CLI authenticated"
  fi

  printf '\n'
}

preflight_requeue() {
  print_header "Running preflight checks..."
  printf '%s\n' "---------------------------"

  preflight_common
}

timestamp_for_filename() {
  date +"%Y-%m-%dT%H-%M-%S"
}

timestamp_for_metadata() {
  date +"%Y-%m-%dT%H:%M:%S%z"
}

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

strip_trailing_metadata_comment() {
  local file="$1"
  local temp_file

  temp_file="$(mktemp)"
  perl -0pe 's/(?:\n\s*)*<!--\n(?:.*?\n)*?-->[\r\n\s]*$//s' "$file" > "$temp_file"
  mv "$temp_file" "$file"
}

parse_trailing_metadata() {
  local file="$1"
  local line

  META_IMPORTED=""
  META_TIMESTAMP=""
  META_ISSUE_CREATED=""
  META_ISSUE_NUMBER=""
  META_URL=""
  META_ERROR=""
  META_RETRY_COUNT=""

  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" =~ ^Imported:\ (.+)$ ]]; then
      META_IMPORTED="${BASH_REMATCH[1]}"
    elif [[ "$line" =~ ^Timestamp:\ (.+)$ ]]; then
      META_TIMESTAMP="${BASH_REMATCH[1]}"
    elif [[ "$line" =~ ^IssueCreated:\ (.+)$ ]]; then
      META_ISSUE_CREATED="${BASH_REMATCH[1]}"
    elif [[ "$line" =~ ^Issue:\ #([0-9]+)$ ]]; then
      META_ISSUE_NUMBER="${BASH_REMATCH[1]}"
    elif [[ "$line" =~ ^URL:\ (.+)$ ]]; then
      META_URL="${BASH_REMATCH[1]}"
    elif [[ "$line" =~ ^Error:\ (.+)$ ]]; then
      META_ERROR="${BASH_REMATCH[1]}"
    elif [[ "$line" =~ ^RetryCount:\ ([0-9]+)$ ]]; then
      META_RETRY_COUNT="${BASH_REMATCH[1]}"
    fi
  done < "$file"
}
