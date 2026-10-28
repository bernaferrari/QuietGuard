#!/bin/sh
# Rebuild the capture app, capture every device size on the shared emulator, and export every language.
#   store-listing/recapture.sh            # all sizes, all languages
#   store-listing/recapture.sh phone      # one size, then export
# Uses the one Resizable_Experimental emulator (emulator-5578); never a physical phone.
set -eu
cd "$(dirname "$0")/.."
serial=${SERIAL:-emulator-5578}
profiles=${1:-phone tablet7 tablet10}
playwright=${PLAYWRIGHT_MODULE:-$HOME/.cache/weakspot-shots/node_modules/playwright}

ANDROID_SERIAL=$serial ./gradlew :app:installStoreCapture -Pquietguard.abis=arm64-v8a --no-configuration-cache -q
for profile in $profiles; do
  uv run -q --with pillow python store-listing/capture-emulator.py --serial "$serial" --profile "$profile"
done
(cd store-listing && PLAYWRIGHT_MODULE=$playwright node studio/export.cjs)
