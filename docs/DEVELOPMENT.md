# Developing QuietGuard

Related: [README](../README.md) · [Architecture](ARCHITECTURE.md) · [Threat model](THREAT-MODEL.md) · [Provenance](PROVENANCE.md)

## Install a prebuilt APK

When a `v*` tag is pushed, GitHub Actions publishes a **debug-signed** arm64 QuietGuard APK on
[Releases](https://github.com/bernaferrari/QuietGuard/releases/latest). That build is from **this** repo, not NetGuard.
Until a tag exists, the Releases page is empty; build from source instead.

Optional: add repo secrets (`SIGNING_STORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`)
to publish a **release-signed** APK instead. See [`.github/workflows/release.yml`](../.github/workflows/release.yml).

Do **not** install upstream NetGuard APKs and expect QuietGuard UI or engine behavior.

## Build for Android

Prerequisites: JDK 17+, Android SDK/NDK (`quietguard.ndkVersion` in `gradle.properties`), Rust/Cargo.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Limit native targets with `-Pquietguard.abis=arm64-v8a`. A different installed NDK can be used with
`-Pquietguard.ndkVersion=<version>`.

Local Android check (host tests + optional arm64 debug APK, **not** run on every GitHub push):

```bash
./scripts/verify-android-ci.sh
```

## Build for the web

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
./scripts/simulate-web-ci.sh          # production Wasm bundle + checks
```

Deploy: [quietguard.vercel.app](https://quietguard.vercel.app/) (UI demo only; it does not filter traffic).

## Continuous integration

Every push and pull request runs [`.github/workflows/ci.yml`](../.github/workflows/ci.yml), intentionally **not** a full
Android/NDK assemble (that is slow on GitHub and is why the web job exists).

| Job | What it runs |
|-----|----------------|
| **KMP web** | `./scripts/verify-web-deploy.sh --ci --web-only` |
| **Rust** | `cargo test --locked` in `app/src/main/rust` (host, no NDK) |

Android host tests + arm64 APK: `./scripts/verify-android-ci.sh` locally, or the **Release** workflow on a `v*` tag.
Not in CI: multi-ABI, emulator, Play packaging.

## Architecture in one picture

```text
Compose screens → ViewModels → repositories / platform bridges
        ↓                    ↓                     ↓
   Android VpnService    Rust engine           Wasm demo (UI only)
```

Modules: `:app` (host), `:shared` (KMP UI + domain), `:webApp`, `:sqliteWasmWorker`.
Rules and dependency direction: [ARCHITECTURE.md](ARCHITECTURE.md).

## Behavior notes

- Local-first: filtering does not proxy through a QuietGuard backend.
- Update checks are **opt-in** (`update_check` defaults off). The hosts download is user-triggered. SOCKS5 defaults off.
- OEM VPN and battery policies can stop the tunnel; traffic is unfiltered while the service is down.

## QuietGuard and NetGuard

| | **QuietGuard** | **NetGuard** (upstream) |
|--|----------------|-------------------------|
| **Lineage** | Fork of NetGuard; GPLv3 | Original project by Marcel Bokhorst |
| **UI** | Kotlin Multiplatform + Compose + Material 3 | Classic Android views |
| **Packet engine** | Rust via UniFFI (`app/src/main/rust`) | C/JNI |
| **Platforms** | Android + Wasm web demo | Android |
| **Install** | [This repo's Releases](https://github.com/bernaferrari/QuietGuard/releases) or build from source | [Upstream APKs](https://github.com/M66B/NetGuard/releases) |

QuietGuard does **not** ship NetGuard binaries under a new name. Full authorship split: [PROVENANCE.md](PROVENANCE.md).

## Store screenshots

The Play screenshots and README images are real app screens from the `storeCapture` build type, which seeds
fictional apps and traffic and has no VPN service or INTERNET permission. Capture and compose them with
[Screenshot Studio](../store-listing/README.md).
