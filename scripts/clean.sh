#!/usr/bin/env sh
# Removes build output and tool caches that are regenerated automatically.
# Usage: scripts/clean.sh [--dry-run] [--include-idea]
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DRY_RUN=0
TARGETS="build app/build .gradle .kotlin captures .cxx .externalNativeBuild"

for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    --include-idea) TARGETS="$TARGETS .idea" ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

# A running Gradle daemon keeps files in .gradle open.
[ "$DRY_RUN" -eq 0 ] && [ -x "$ROOT/gradlew" ] && "$ROOT/gradlew" --stop >/dev/null 2>&1 || true

for target in $TARGETS; do
  path="$ROOT/$target"
  if [ -e "$path" ]; then
    size="$(du -sh "$path" | cut -f1)"
    if [ "$DRY_RUN" -eq 1 ]; then
      echo "would remove  $target  ($size)"
    else
      rm -rf "$path"
      echo "removed       $target  ($size)"
    fi
  fi
done
