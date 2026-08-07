#!/usr/bin/env bash
# Build and stage an arm64 QuietGuard APK (debug-signed, or release if signing env is set).
# Optional env: SIGNING_STORE_BASE64, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS,
# SIGNING_KEY_PASSWORD, RUNNER_TEMP, GITHUB_REF_NAME, GITHUB_SHA, GITHUB_OUTPUT.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ABI="${QUIETGUARD_ABI:-arm64-v8a}"
TMP="${RUNNER_TEMP:-${TMPDIR:-/tmp}}"
TMP="${TMP%/}"

MODE="debug"
if [[ -n "${SIGNING_STORE_BASE64:-}" && -n "${SIGNING_STORE_PASSWORD:-}" \
   && -n "${SIGNING_KEY_ALIAS:-}" && -n "${SIGNING_KEY_PASSWORD:-}" ]]; then
  STORE="$TMP/quietguard.jks"
  echo "$SIGNING_STORE_BASE64" | base64 --decode > "$STORE"
  {
    echo "storeFile=$STORE"
    echo "storePassword=$SIGNING_STORE_PASSWORD"
    echo "keyAlias=$SIGNING_KEY_ALIAS"
    echo "keyPassword=$SIGNING_KEY_PASSWORD"
  } > "$ROOT/keystore.properties"
  MODE="release"
  echo "==> Release signing keystore written"
else
  echo "==> No signing secrets — debug-signed APK"
fi

GRADLE=(./gradlew "--no-daemon" "--build-cache" "-Pquietguard.abis=${ABI}")
if [[ "$MODE" == "release" ]]; then
  "${GRADLE[@]}" :app:assembleRelease
else
  "${GRADLE[@]}" :app:assembleDebug
fi

TAG="${GITHUB_REF_NAME:-}"
if [[ -z "$TAG" || "$TAG" != v* ]]; then
  SHA="${GITHUB_SHA:-unknown}"
  TAG="dev-${SHA:0:8}"
fi

mkdir -p "$ROOT/dist"
if [[ "$MODE" == "release" ]]; then
  SRC="$(find "$ROOT/app/build/outputs/apk/release" -name '*.apk' ! -name '*unsigned*' | head -n1)"
  NAME="QuietGuard-${TAG}-arm64-release.apk"
else
  SRC="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
  NAME="QuietGuard-${TAG}-arm64-debug.apk"
fi

if [[ -z "${SRC:-}" || ! -f "$SRC" ]]; then
  echo "Missing assembled APK (mode=$MODE)" >&2
  exit 1
fi

DEST="$ROOT/dist/$NAME"
cp "$SRC" "$DEST"
echo "==> Staged $DEST"
ls -lh "$DEST"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "path=$DEST"
    echo "name=$NAME"
    echo "mode=$MODE"
  } >> "$GITHUB_OUTPUT"
fi
