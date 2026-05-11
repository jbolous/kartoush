#!/usr/bin/env bash

set -euo pipefail

DRY_RUN=false

if [[ "${1:-}" == "--dry-run" ]]; then
  DRY_RUN=true
  shift
fi

if [ "$#" -eq 0 ]; then
  echo "Usage: $0 [--dry-run] <issue|range> [more issues/ranges...]"
  echo "Examples:"
  echo "  $0 --dry-run 293-299"
  echo "  $0 293"
  echo "  $0 290 293-299 305"
  exit 1
fi

expand_args() {
  for arg in "$@"; do
    if [[ "$arg" =~ ^([0-9]+)-([0-9]+)$ ]]; then
      start=${BASH_REMATCH[1]}
      end=${BASH_REMATCH[2]}

      if (( start > end )); then
        echo "Invalid range: $arg" >&2
        exit 1
      fi

      for ((i=start; i<=end; i++)); do
        echo "$i"
      done
    elif [[ "$arg" =~ ^[0-9]+$ ]]; then
      echo "$arg"
    else
      echo "Invalid issue or range: $arg" >&2
      exit 1
    fi
  done
}

transform_title() {
  perl -pe '
    my %ACRONYMS = map { uc($_) => $_ } qw(
      API APIs HTTP HTTPS URL URLs URI URIs UUID UUIDs ID IDs JSON XML SQL
      UI UX JWT OAuth REST GraphQL DNS TCP UDP IP HTML CSS JS CLI SDK DB
      JPA DTO DTOs ADR ADRs RFC RFCs ULID ULIDs JUnit Flyway Gradle GitHub
    );

    my %SMALL = map { $_ => 1 } qw(
      a an the and but or for nor on at to from by of in with as
    );

    chomp;

    my @words = split /\s+/;

    for my $i (0..$#words) {
      my $word = $words[$i];

      my $leading = "";
      my $trailing = "";

      $word =~ s/^([^A-Za-z0-9]+)// and $leading = $1;
      $word =~ s/([^A-Za-z0-9]+)$// and $trailing = $1;

      my $clean = $word;
      my $upper = uc($clean);

      if (exists $ACRONYMS{$upper}) {
        $word = $ACRONYMS{$upper};
      }
      elsif ($i != 0 && $i != $#words && $SMALL{lc($clean)}) {
        $word = lc($word);
      }
      else {
        $word = lc($word);
        $word =~ s/^(\w)/uc($1)/e;
      }

      $words[$i] = "$leading$word$trailing";
    }

    $_ = join(" ", @words) . "\n";
  '
}

transform_body() {
  perl -0777 -pe '
    my %ACRONYMS = map { uc($_) => $_ } qw(
      API APIs HTTP HTTPS URL URLs URI URIs UUID UUIDs ID IDs JSON XML SQL
      UI UX JWT OAuth REST GraphQL DNS TCP UDP IP HTML CSS JS CLI SDK DB
      JPA DTO DTOs ADR ADRs RFC RFCs ULID ULIDs JUnit Flyway Gradle GitHub
    );

    my $in_code_block = 0;

    $_ = join "\n", map {
      my $line = $_;

      if ($line =~ /^```/) {
        $in_code_block = !$in_code_block;
        $line;
      }
      elsif ($in_code_block) {
        $line;
      }
      elsif ($line =~ /^(\s*(?:[-*]|\d+\.)\s+)(.+)$/) {
        my $prefix = $1;
        my $text = $2;

        my @segments = split /(`[^`]*`|(?i:https?):\/\/[^\s<>()]+)/, $text;

        for my $segment (@segments) {
          next if $segment =~ /^`[^`]*`$/;
          next if $segment =~ /^(?i:https?):\/\/[^\s<>()]+$/;

          $segment = lc($segment);
          $segment =~ s/\b([a-z0-9]+)\b/
            my $w = uc($1);
            exists $ACRONYMS{$w} ? $ACRONYMS{$w} : $1
          /ge;
        }

        $text = join "", @segments;

        if ($text !~ /^\s*(?:\[[ xX]\]\s+)*(?i:https?):\/\//) {
          $text =~ s/^(\s*(?:\[[ xX]\]\s+)*)(\S)/$1 . uc($2)/e;
        }

        "$prefix$text";
      }
      else {
        $line;
      }
    } split /\n/;
  '
}

UPDATED_COUNT=0
UNCHANGED_COUNT=0
FAILED_COUNT=0

ISSUES=$(expand_args "$@" | sort -n | uniq)

for ISSUE in $ISSUES; do
  echo "Processing issue #$ISSUE..."

  if ! DATA=$(gh issue view "$ISSUE" --json title,body); then
    echo "Failed to read #$ISSUE"
    ((FAILED_COUNT+=1))
    continue
  fi

  TITLE=$(printf "%s" "$DATA" | jq -r .title)
  BODY=$(printf "%s" "$DATA" | jq -r '.body // ""')

  UPDATED_TITLE=$(printf "%s" "$TITLE" | transform_title)
  UPDATED_TITLE=${UPDATED_TITLE%$'\n'}

  UPDATED_BODY=$(printf "%s" "$BODY" | transform_body)

  if [ "$TITLE" != "$UPDATED_TITLE" ] || [ "$BODY" != "$UPDATED_BODY" ]; then
    if [ "$DRY_RUN" = true ]; then
      echo "[DRY RUN] Would update #$ISSUE"

      if [ "$TITLE" != "$UPDATED_TITLE" ]; then
        echo "  Title:"
        echo "    Before: $TITLE"
        echo "    After:  $UPDATED_TITLE"
      fi
    else
      if gh issue edit "$ISSUE" --title "$UPDATED_TITLE" --body "$UPDATED_BODY"; then
        echo "Updated #$ISSUE"
      else
        echo "Failed to update #$ISSUE"
        ((FAILED_COUNT+=1))
        continue
      fi
    fi

    ((UPDATED_COUNT+=1))
  else
    echo "No changes for #$ISSUE"
    ((UNCHANGED_COUNT+=1))
  fi
done

echo ""
echo "Summary:"
if [ "$DRY_RUN" = true ]; then
  echo "Would update: $UPDATED_COUNT"
else
  echo "Updated: $UPDATED_COUNT"
fi
echo "Unchanged: $UNCHANGED_COUNT"
echo "Failed: $FAILED_COUNT"
