#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

declare -a TARGETS=()

if [[ $# -gt 0 ]]; then
  TARGETS=( "$@" )
else
  TARGETS=(
    "$ROOT_DIR/.codex"
    "$ROOT_DIR/.github/issue-import/pending"
    "$ROOT_DIR/.github/issue-import/templates"
  )
fi

failed=0

print_header() {
  printf '%s\n' "$1"
  printf '%s\n' "$(printf '%*s' "${#1}" '' | tr ' ' '-')"
}

print_header "Checking editorial style"

for target in "${TARGETS[@]}"; do
  [[ -e "$target" ]] || continue

  while IFS= read -r match; do
    [[ -n "$match" ]] || continue
    if [[ $failed -eq 0 ]]; then
      printf '%s\n' ""
      printf '%s\n' "Found lowercase bullet starts that violate sentence case:"
    fi
    printf '%s\n' "$match"
    failed=1
  done < <(grep -R -n -E '^- [a-z]' "$target" 2>/dev/null || true)
done

if [[ $failed -ne 0 ]]; then
  printf '\n'
  printf '%s\n' "Fix the bullets above so they start in sentence case."
  exit 1
fi

early_wrap_matches="$(
  python3 - "${TARGETS[@]}" <<'PY'
from pathlib import Path
import sys

def is_prose(line: str) -> bool:
    stripped = line.strip()
    if not stripped:
        return False
    blocked_prefixes = (
        "#",
        "- ",
        "* ",
        ">",
        "```",
        "|",
        "Title:",
        "Parent:",
        "Labels:",
        "Status:",
        "<!--",
        "-->",
    )
    if stripped.startswith(blocked_prefixes):
        return False
    if stripped[0].isdigit() and len(stripped) > 1 and stripped[1] == ".":
        return False
    return True

for raw_target in sys.argv[1:]:
    target = Path(raw_target)
    if not target.exists():
        continue

    files = [target] if target.is_file() else sorted(target.rglob("*.md"))

    for path in files:
        lines = path.read_text().splitlines()
        in_code_block = False
        in_html_comment = False

        for index in range(len(lines) - 1):
            line = lines[index]
            next_line = lines[index + 1]
            stripped = line.strip()
            next_stripped = next_line.strip()

            if stripped.startswith("```"):
                in_code_block = not in_code_block
                continue

            if "<!--" in stripped:
                in_html_comment = True
            if in_code_block or in_html_comment:
                if "-->" in stripped:
                    in_html_comment = False
                continue

            if is_prose(line) and is_prose(next_line) and len(line.rstrip()) < 72:
                print(f"{path}:{index + 1}:{line.rstrip()}")
PY
)"

if [[ -n "$early_wrap_matches" ]]; then
  printf '\n'
  printf '%s\n' "Found suspicious early prose wrapping:"
  printf '%s\n' "$early_wrap_matches"
  printf '\n'
  printf '%s\n' "Reflow the prose above so it reads naturally instead of wrapping early."
  exit 1
fi

printf '\n'
printf '%s\n' "Editorial style check passed."
