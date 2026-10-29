# Developing QuietGuard

A Kotlin Multiplatform app: one Compose UI for Android and a Wasm web demo, with a Rust packet engine behind
Android's `VpnService`. `:app` is the Android app; the UI and domain live in `:shared`.

[README](../README.md) · [Architecture](ARCHITECTURE.md) · [Threat model](THREAT-MODEL.md) · [Provenance](PROVENANCE.md)

## Quick start

```bash
git clone https://github.com/bernaferrari/QuietGuard.git
cd QuietGuard
rustup target add aarch64-linux-android
./gradlew :app:installDebug -Pquietguard.abis=arm64-v8a
```

## What you need

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 17+ | Gradle toolchain is 17; CI uses Temurin 21 |
| Android SDK | compileSdk 37.2, targetSdk 36, minSdk 26 | Set `ANDROID_HOME` or `sdk.dir` in `local.properties` |
| Android NDK | 30.0.16248370 | Pinned as `quietguard.ndkVersion` in `gradle.properties`; override with `-Pquietguard.ndkVersion=<version>` |
| Rust | stable, with Cargo | One rustup target per ABI you build: `aarch64-linux-android`, `armv7-linux-androideabi`, `i686-linux-android`, `x86_64-linux-android` |

Gradle 9.8.1 comes from the wrapper. The web demo needs no Android SDK, NDK or Rust.

## Run it

| Target | Command |
| --- | --- |
| Android, debug | `./gradlew :app:installDebug -Pquietguard.abis=arm64-v8a` |
| Android, every ABI | `./gradlew :app:assembleDebug` (armeabi-v7a, arm64-v8a, x86, x86_64) |
| Web, dev server | `./gradlew :webApp:wasmJsBrowserDevelopmentRun` |
| Web, production bundle + checks | `./scripts/simulate-web-ci.sh` |

The web build is a UI demo over seed data and filters nothing. It is deployed at
[quietguard.vercel.app](https://quietguard.vercel.app/).

## Where things live

| Path | What's there |
| --- | --- |
| `shared/` | Compose Multiplatform UI, ViewModels, repositories, Room and DataStore |
| `app/` | Android host: `VpnService` (`ServiceSinkhole`), UniFFI glue, tiles, workers |
| `app/src/main/rust/` | Rust packet engine (parsing, sessions, DNS/TLS, SOCKS5), exposed through UniFFI |
| `app/src/storeCapture/` | Capture build: fictional data for store screenshots |
| `webApp/`, `sqliteWasmWorker/` | Wasm shell and its Web Worker SQLite driver |
| `scripts/` | Local CI mirrors, release staging, web deploy |
| `store-listing/` | Play screenshots, listing copy and icon layers (Screenshot Studio) |
| `fastlane/` | Play metadata |

How the layers depend on each other: [ARCHITECTURE.md](ARCHITECTURE.md).

## Before you push

```bash
./scripts/verify-web-deploy.sh --ci --web-only
(cd app/src/main/rust && cargo test --locked)
./scripts/verify-android-ci.sh
```

[CI](../.github/workflows/ci.yml) runs the first two on every push and pull request (jobs **KMP web** and
**Rust engine tests**). It never builds the Android app: `verify-android-ci.sh` (shared host tests plus an arm64
debug APK) is local only, and the [Release](../.github/workflows/release.yml) workflow builds the APK on `v*` tags.
Multi-ABI builds, emulator tests and Play packaging are not covered anywhere.

## Store screenshots and icon

Screenshot Studio lives in `store-listing/studio/`. It is synced from `~/Documents/screenshot-studio`, so never
edit it here.

- The `storeCapture` build type seeds fictional apps, rules and traffic. It has no VPN service and no INTERNET
  permission, and installs beside the real app as `com.bernaferrari.quietguard.storecapture`. Build it alone with
  `./gradlew :app:assembleStoreCapture -Pquietguard.abis=arm64-v8a --no-configuration-cache`.
- `store-listing/capture-emulator.py` is the scene tour; cards and order are in `store-listing/gallery.json`.

```bash
node store-listing/studio/recapture.cjs                  # build, install, capture, export every language
node store-listing/studio/recapture.cjs en-US --profile phone
node store-listing/studio/recapture.cjs --no-build       # reuse the last APK
node store-listing/studio/recapture.cjs --no-build --no-capture   # export only, from existing captures
PLAYWRIGHT_MODULE=… node store-listing/studio/export.cjs --locale en-US
node store-listing/studio/server.cjs                     # live editor
```

Captures run on the dedicated emulator `CAPTURE_SERIAL` (default `emulator-5578`), never a personal phone.

The icon is drawn as layers in `store-listing/brand/*.svg`; `store-listing/brand/icon.json` lists every output.
Regenerate them all with `PLAYWRIGHT_MODULE=… node store-listing/studio/icon.cjs`. More in
[store-listing/README.md](../store-listing/README.md).

## Good to know

- Release signing: `release` and `play` sign only when a `keystore.properties` exists (gitignored). Never commit it
  or a keystore. Tag builds are debug-signed unless the repo secrets `SIGNING_STORE_BASE64`,
  `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD` are set (see `scripts/stage-release-apk.sh`).
- `play` differs from `release` by blanking the hosts download URL and the GitHub update check
  (`PLAY_STORE_RELEASE = true`).
- `-Pquietguard.abis=arm64-v8a` (comma-separated) builds only those ABIs and their Cargo targets; unset, all four
  are built and need all four rustup targets.
- Test the VPN on a device or emulator you don't depend on: while the service is down, traffic is unfiltered.
- QuietGuard ships no NetGuard binaries; its packet engine is Rust, not NetGuard's C. What came from NetGuard and what is new:
  [PROVENANCE.md](PROVENANCE.md).
