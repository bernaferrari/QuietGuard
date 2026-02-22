# NetGuard Next

<p>
  <img alt="License: GPLv3" src="https://img.shields.io/badge/License-GPLv3-blue.svg" />
  <img alt="Platform: Android" src="https://img.shields.io/badge/Platform-Android-3DDC84.svg" />
  <img alt="Min SDK: 26" src="https://img.shields.io/badge/minSdk-26-orange.svg" />
</p>

No-root Android firewall with a Material 3 Expressive experience, adaptive navigation, and a redesigned traffic log.

This project is a modernized fork of the original NetGuard firewall for Android.
It keeps the no-root, VPN-based firewall model from upstream, but refreshes the app architecture and UX for current Android and Material 3 patterns.

## Screenshots

<p>
  <img src="screenshots/01-main.png" width="220" alt="Home screen" />
  <img src="screenshots/02-main-details.png" width="220" alt="Firewall details screen" />
  <img src="screenshots/03-main-access.png" width="220" alt="App access screen" />
  <img src="screenshots/08-notifications.png" width="220" alt="Notifications screen" />
</p>

More screenshots are available in `screenshots/`.

## Upstream Reference

This fork is based on the original NetGuard project by Marcel Bokhorst (M66B):
https://github.com/M66B/NetGuard

## What Is Different In This Fork

This fork focuses on a modern UI and navigation model while keeping the core firewall behavior:

- Kotlin + Jetpack Compose UI rewrite
- Material 3 expressive styling
- Adaptive navigation (bottom bar on compact layouts, rail/suite behavior on larger layouts)
- Tablet-friendly master/detail flow for firewall rules
- Cleaner back-stack behavior across tabs
- Redesigned traffic log screen with timeline and "By app" views
- App icon support in log rows and in the app picker
- Protocol/status filters and searchable app picker
- In-app guidance to enable filtering when allowed traffic logs are needed
- Improved touch/ripple clipping consistency in settings and controls
- Modernized settings UX with clearer grouped options

### Modern UI Preview

<p>
  <img src="assets/home.png" width="220" alt="Redesigned home screen" />
  <img src="assets/firewall.png" width="220" alt="Redesigned firewall screen" />
  <img src="assets/details.png" width="220" alt="Redesigned app details screen" />
  <img src="assets/settings.png" width="220" alt="Redesigned settings screen" />
</p>

These screens highlight this fork's updated look-and-feel.

## Core Capabilities

- No-root firewall using Android `VpnService`
- Per-app allow/block rules for Wi-Fi and mobile data
- Optional filtering mode for per-address visibility and controls
- DNS and forwarding screens
- Traffic log with protocol/status filtering
- Log retention configuration
- Open-source codebase under GPLv3

## Important Behavior Notes

- This app uses a local VPN tunnel, not a remote VPN server.
- If filtering is disabled, traffic logging may mainly show blocked attempts. Enable filtering to capture full allowed traffic context.
- Some OEM ROMs have VPN stack bugs that can affect startup or reliability.

## Project Layout

- App module: `app/`
- UI code: `app/src/main/kotlin/eu/faircode/netguard/ui/`
- Screens: `app/src/main/kotlin/eu/faircode/netguard/ui/screens/`
- Main tabs: `app/src/main/kotlin/eu/faircode/netguard/ui/main/`
- Native engine: `app/src/main/jni/netguard/`
- Resources: `app/src/main/res/`

## License

Licensed under GNU GPLv3. See `LICENSE`.
