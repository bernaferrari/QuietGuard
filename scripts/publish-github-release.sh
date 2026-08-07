#!/usr/bin/env bash
# Create a GitHub Release for the current tag and attach the staged APK.
# Usage: ./scripts/publish-github-release.sh <apk-path>
set -euo pipefail

APK="${1:?usage: $0 <apk-path>}"
if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

TAG="${GITHUB_REF_NAME:?GITHUB_REF_NAME is required}"
if [[ "$TAG" != v* ]]; then
  echo "Refusing to publish: GITHUB_REF_NAME='$TAG' is not a v* tag" >&2
  exit 1
fi

: "${GH_TOKEN:?GH_TOKEN is required}"

NOTES="$(cat <<'EOF'
QuietGuard build from **this** repository (not NetGuard).

- ABI: **arm64-v8a** only
- Debug-signed unless repo signing secrets were configured
- Sideload at your own risk; enable unknown sources

Source: this tag. Workflow: `.github/workflows/release.yml`.
EOF
)"

gh release create "$TAG" "$APK" --title "$TAG" --notes "$NOTES"
