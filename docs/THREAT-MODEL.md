# QuietGuard threat model

QuietGuard is a **local** Android `VpnService` sinkhole (Rust UniFFI engine + Kotlin policy).
It is **not** a remote VPN, anonymity network, or QuietGuard-operated tunnel.

The [README](../README.md) summarizes this. Details and control-flow notes live here
so the landing page stays short.

## Assets

- Per-app allow/block rules and related preferences on device
- Connection / activity logs (app identity, IP/port, domain/data where logged, allow vs block)
- Optional PCAP captures and optional SOCKS5 credentials if the user enables those features
- The user’s intent to constrain which apps may use the network

`allowBackup` is disabled (`AndroidManifest`), but rules and logs are not presented as
encrypted against an unlocked-device attacker with backup or root access.

## Trust boundaries

| Boundary | Trust assumption |
|----------|------------------|
| QuietGuard process ↔ Android VPN stack | OS mediates traffic only while the VPN is up and authorized by the user |
| On-device store (rules, logs, prefs) | Readable by QuietGuard; protected like other app-private data |
| QuietGuard ↔ internet (default filtering) | No QuietGuard backend for filtering; the core firewall loop does not require an account or QuietGuard-operated proxy |
| App-initiated residual egress | See below. Local-first filtering coexists with limited app HTTP(S). |
| Web demo ↔ browser | Demo UI only; **zero** authority over phone traffic |

## In scope

- Reducing accidental or unwanted app network use (Wi-Fi / mobile) without root
- Local visibility into allowed vs blocked connection attempts
- Keeping the default firewall path free of third-party traffic relay, accounts, and analytics backends

## Out of scope / non-goals

- Stopping a compromised or rooted device / malware with VPN-bypass capability
- Anonymity, anti-tracking against network observers, Tor, or “hide my IP”
- Hiding *allowed* traffic from network observers
- Enterprise MDM / perfect always-on VPN across all OEM skins
- Treating the **web demo** as security software

## Residual risks

- OEM battery optimization / “network restrictions” killing or pausing the VPN
- User-disable or always-on VPN edge cases; traffic is unfiltered when the service is down
- DNS and routing behavior depend on settings and platform (including Private DNS complexity)
- Dependency and hosts-file supply chain when the user configures and triggers a hosts download
- Fork history can confuse supply-chain review: verify you build **this** repo’s sources, not an upstream NetGuard APK
- Billing-related permission and IAB helpers may appear in the tree for historical/store packaging paths; they are not part of the core firewall loop

## Residual app-initiated egress (control flow)

These paths are **not** the packet filter. They are QuietGuard’s own HTTP(S) or optional proxying.

### GitHub Releases update check

- Preference `PreferenceKeys.UPDATE_CHECK` defaults to `PreferenceKeys.UPDATE_CHECK_DEFAULT` (**false**, opt-in). Manual “check now” still works.
- When enabled, housekeeping calls `BuildConfig.GITHUB_LATEST_API`
  (`https://api.github.com/repos/bernaferrari/QuietGuard/releases/latest` on debug/release).
- The `play` **build type** blanks `GITHUB_LATEST_API` to `""`.
- Until this repo has a GitHub Release with an APK, a successful check has nothing installable to fetch.

### Hosts download

- User-triggered only. Preference `hosts_url` defaults to empty — **no automatic fetch** on a clean install.
- If `hosts_url` is the legacy NetGuard URL `https://www.netguard.me/hosts`, it is remapped to
  `BuildConfig.HOSTS_FILE_URI` (StevenBlack on debug/release; empty on the `play` build type).
  See `ServiceExternal.kt`.
- Custom `hosts_url` values still egress on any build type; Play blanking only affects the
  BuildConfig remap, not an arbitrary URL.

### Optional SOCKS5

- `socks5_enabled` defaults to **false**.
- When enabled, **allowed** TCP may be proxied to the user-configured SOCKS5 endpoint.

### Dead / inherited helpers

- `Util.getOrganization` (ipinfo.io) is defined but unreferenced. It is not a live product path.

## Related docs

- [Architecture](ARCHITECTURE.md)
- [Provenance (QuietGuard vs NetGuard)](PROVENANCE.md)
