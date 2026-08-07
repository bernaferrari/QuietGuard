# QuietGuard architecture

Related: [README](../README.md) · [Provenance](PROVENANCE.md) · [Threat model](THREAT-MODEL.md)

QuietGuard is a **local Android `VpnService` sinkhole**. Shared Kotlin Multiplatform
owns UI, rules, and logs. Android owns the VPN process. Rust owns the packet hot path.
Wasm reuses the UI with **no** device firewall.

## Gradle modules

| Module | Role |
|--------|------|
| `:shared` | Compose Multiplatform UI, ViewModels, repositories, Room/DataStore contracts, `expect`/`actual` + `NetGuardPlatform` bridges |
| `:app` | Android host: `VpnService` (`ServiceSinkhole`), UniFFI glue, notifications, tiles, workers |
| `:webApp` | Wasm shell that hosts the same `AppContent` |
| `:sqliteWasmWorker` | Web Worker SQLite driver for the Wasm Room path |

Source sets in `:shared`: `commonMain` (UI + domain), `storageMain` (Room), `androidMain`, `wasmJsMain`, `androidHostTest`.

## Runtime shapes

```text
Android
  ActivityMain → AppContent (shared Compose)
       ↓
  Feature ViewModels → repositories / DataStore / Room
       ↓
  NetGuardPlatform bridges (installed from :app)
       ↓
  ServiceSinkhole (VpnService) → NativeVpnEngineController
       ↓
  Rust crate app/src/main/rust (UniFFI NativeEngine, libnetguard.so)

Wasm demo
  webApp Main → AppContent (same Compose)
       ↓
  Wasm Room + demo seed data
       ↓
  NetGuardPlatform no-ops (firewall / hosts / watchdog do nothing)
```

Packet filtering exists **only** on Android while the VPN is up. The web demo is not a security boundary.

## Shared Kotlin (canonical app logic)

Dependency direction is downward only:

```text
Compose screen
    ↓ immutable state / user events
Feature ViewModel
    ↓
Repository or platform bridge
    ↓
Room, DataStore, Android VPN, or Wasm implementation
```

- Screens do not load data, register listeners, or coordinate persistence.
- Repositories do not know about Compose.
- Platform differences stop at `platform/` adapters, Koin `expect`/`actual` modules, and `NetGuardPlatform` (lateinit bridges installed by the host).

Packages worth knowing:

- `ui/` screens, Navigation 3 (`AppNavigation`), theme, `UiAsyncState`
- `data/` DataStore, `PreferenceKeys`, traffic + preferences repositories
- `domain/` firewall rules repository
- `platform/` `NetGuardPlatform` + `PlatformContext`

## Android host

`:app` is a NetGuard-descended VPN host, now Kotlin, wired to the Rust engine:

- `ServiceSinkhole` — `VpnService`, tunnel lifecycle, housekeeping
- `service/NativeVpnEngineController` — UniFFI `NativeEngine`
- `platform/NetGuardPlatformBindings` — installs Android implementations of the shared bridges
- Workers, tiles, widgets, IAB leftovers — host-only; not the shared UI architecture

Native build: Cargo cross-compiles `libnetguard.so` per ABI; UniFFI generates Kotlin into
`app/build/generated/source/uniffi`. Limit ABIs with `-Pquietguard.abis=arm64-v8a` (local/release).
NDK version is **only** `quietguard.ndkVersion` in `gradle.properties`. The Release workflow
installs that value via `scripts/quietguard-ndk-version.sh`; everyday CI does not touch the NDK.

## Rust engine

`app/src/main/rust` — packet parse, sessions, DNS/TLS helpers, SOCKS5 optional path, UniFFI API.
`cargo test` covers this crate (CI **Rust** job). The old C/JNI tree is not maintained.

## Wasm demo

`wasmJsMain` reuses screens and storage contracts. Firewall/hosts/watchdog bridges are empty.
SQLite runs in `:sqliteWasmWorker`. Treat this as a UI preview, not a product security surface.

## UI conventions (still in force)

These rules apply to shared Compose code; they are not the whole architecture.

### State

- A screen renders one immutable state stream from its feature ViewModel.
- Durable data lives in repositories; transient UI choices live in the ViewModel or
  `rememberSaveable` when strictly presentational.
- Root-tab ViewModels live above Navigation 3 entries when state must survive tab replacement.
  Detail ViewModels stay entry-scoped.
- Composables collect ViewModel state with lifecycle-aware collection.

### Async: `UiAsyncState<T>`

Canonical contract in `ui/util/UiAsyncState.kt`:

- `hasReceived == false` — no authoritative result yet
- `isInitialLoading` / `isRefreshing` / `isReady` / `hasFailed`

An empty collection is empty **only** when the resource is ready. Do not infer readiness from
`data.isEmpty()`. Loading must not replace usable content.

### Events

- User actions call named ViewModel methods.
- ViewModels coordinate repository writes and platform side effects.
- Visible refresh/retry must restart real work.
- Cancellation is rethrown; other failures become explicit resource state.

### Presentation

- Initial loading, confirmed empty, filtered empty, content, and failure are distinct.
- Navigation destinations always render a meaningful state.
- Motion honors reduced-motion; no infinite decorative motion when requested.
- Decorative icons inside labeled placeholders stay unlabeled; interactive icons have
  content descriptions and Material-sized targets.

### Maintenance

- Orchestration in ViewModels; rendering in screen/component files.
- Extract cohesive components before a screen reaches roughly 700–900 lines.
- Prefer explicit feature models over another boolean on a screen.
- Host tests for state transitions (`:shared:testAndroidHostTest`); Android + Wasm compile
  checks for shared architectural changes.

## CI mapping

Push/PR CI stays fast on purpose (KMP web was built so GitHub never has to NDK-cross every commit):

| When | Proves |
|------|--------|
| Push/PR **KMP web** | Wasm distribution (`scripts/verify-web-deploy.sh`) |
| Push/PR **Rust** | Host `cargo test` in `app/src/main/rust` (no NDK) |
| Local `scripts/verify-android-ci.sh` | Shared host tests + optional arm64 debug APK |
| Tag `v*` **Release** | NDK + arm64 APK via `.github/actions/setup-android-ndk-rust` |
