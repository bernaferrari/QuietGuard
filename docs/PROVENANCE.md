# QuietGuard provenance

How to tell **QuietGuard-owned work** from **inherited NetGuard history**.

This repository is a GitHub fork of [NetGuard](https://github.com/M66B/NetGuard)
(`isFork=true`, parent `M66B/NetGuard`). Remotes on a typical clone:

- `origin` → `bernaferrari/QuietGuard`
- `upstream` → `M66B/NetGuard`

QuietGuard keeps the local VPN sinkhole idea and GPLv3 obligations. It does **not**
claim feature parity with every NetGuard option, and it does **not** ship NetGuard’s
Play/F-Droid binaries under a new name.

## How to re-verify (do not trust stale numbers)

```bash
gh repo view bernaferrari/QuietGuard --json isFork,parent
git remote -v
git shortlog -sn --all | head
git rev-list --count HEAD
gh release list -R bernaferrari/QuietGuard
```

A snapshot on 2026-08-06 was on the order of **~3.5k** commits under **M66B**,
**~200** under **Marcel Bokhorst**, **~110** under **Bernardo Ferrari**, and about
**~4k** commits total. QuietGuard-specific work is a small minority of the graph.

If you are evaluating originality: judge QuietGuard by the trees below, not by
total commit count on the fork.

## QuietGuard-owned surfaces

| Area | Paths / notes |
|------|----------------|
| Compose Multiplatform UI & domain | `shared/` — `commonMain` screens, navigation, ViewModels, repositories; `androidMain` / `wasmJsMain` adapters; Room DB sources under `storageMain` (exported schema JSON under `shared/schemas/`) |
| Android host wiring | `app/` Kotlin host (`ServiceSinkhole`, UniFFI integration, platform bridges) — NetGuard-descended VPN host concepts, modernized around the new stack |
| Rust VPN / packet engine | `app/src/main/rust` (`engine`, packet/session/DNS/TLS, `uniffi_api`) → Kotlin `NativeEngine` + `libnetguard.so` per ABI |
| Web demo | `webApp/` + `sqliteWasmWorker/` |
| Tooling | `scripts/` (web verify/deploy, local Android verify, release staging) |
| Docs | `docs/ARCHITECTURE.md`, this file, `docs/THREAT-MODEL.md` |
| Packaging drafts | `playstore/`, `fastlane/metadata` — not proof of a published store binary |

## Inherited / lineage (NetGuard)

- Project origin and GPLv3 from Marcel Bokhorst / NetGuard
- Product concept: no-root firewall via local `VpnService`, per-app Wi-Fi / mobile rules, on-device logs
- Historical commit graph and upstream tags/APKs — **not** QuietGuard builds
- `:app` still reflects a NetGuard-descended VPN host (Kotlin today; no Java under `app/src`)
- No maintained `jni/netguard` C sources in this repo; active native path is Rust UniFFI.
  An empty local `jni/` tree is not required (empty dirs are not tracked in git).

## QuietGuard vs NetGuard (summary)

| | QuietGuard | NetGuard (upstream) |
|--|------------|---------------------|
| UI | KMP + Compose + Material 3 (`shared/`) | Classic Android views |
| Packet engine | Rust UniFFI (`app/src/main/rust`) | C/JNI (`jni/netguard`) |
| Platforms | Android + Wasm web demo | Android |
| Install | This repo’s GitHub Releases (when tagged) or build from source | Upstream APKs |
| CI | Web verify + host Rust tests; Android APK only on `v*` tags / local script | Upstream’s own pipeline |

## Related docs

- [Architecture](ARCHITECTURE.md)
- [Threat model](THREAT-MODEL.md)
