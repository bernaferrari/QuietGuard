#!/usr/bin/env bash
# Print quietguard.ndkVersion from gradle.properties (single source of truth).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROPS="$ROOT/gradle.properties"
version="$(awk -F= '/^[[:space:]]*quietguard\.ndkVersion=/ {gsub(/[[:space:]]/, "", $2); print $2; found=1}
    END { if (!found) exit 1 }' "$PROPS")"
if [[ -z "$version" ]]; then
  echo "quietguard.ndkVersion missing from $PROPS" >&2
  exit 1
fi
printf '%s\n' "$version"
