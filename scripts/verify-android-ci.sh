#!/usr/bin/env bash
# Local Android check (not everyday GitHub CI): shared host tests + single-ABI debug APK.
# Tag releases use scripts/stage-release-apk.sh instead.
# Usage:
#   ./scripts/verify-android-ci.sh
#   ./scripts/verify-android-ci.sh --abi arm64-v8a
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ABI="arm64-v8a"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --abi)
      ABI="${2:?--abi requires a value}"
      shift 2
      ;;
    -h|--help)
      echo "Usage: $0 [--abi arm64-v8a]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

cd "$ROOT"

echo "==> Shared Android host tests"
# AGP KMP androidLibrary + withHostTest → testAndroidHostTest
./gradlew :shared:testAndroidHostTest --no-daemon --build-cache

echo "==> Debug APK (ABI=${ABI})"
./gradlew :app:assembleDebug \
  -Pquietguard.abis="${ABI}" \
  --no-daemon \
  --build-cache

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "Missing debug APK: $APK" >&2
  exit 1
fi

echo "==> OK: $APK"
ls -lh "$APK"
