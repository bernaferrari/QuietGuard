<p align="center">
  <img src="store-listing/brand/icon.png" width="88" alt="QuietGuard icon">
</p>

<h1 align="center">QuietGuard</h1>

<p align="center"><strong>Decide which apps reach the internet.</strong></p>

<p align="center">A private, no-root firewall for Android. Block Wi-Fi or mobile data app by app, see who tried to connect, and keep all of it on your phone.</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.bernaferrari.quietguard"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80" alt="Get it on Google Play"></a>
</p>

<p align="center">
  <a href="https://quietguard.vercel.app/">Web demo</a> ·
  <a href="docs/ARCHITECTURE.md">Architecture</a> ·
  <a href="docs/THREAT-MODEL.md">Threat model</a> ·
  <a href="docs/PROVENANCE.md">What I wrote vs inherited</a>
</p>

<p align="center">
  <img src="store-listing/en-US/phone/phone-01-home.jpg" height="540" alt="Your apps, on quiet: Home with the firewall enabled">
  &nbsp;&nbsp;
  <img src="store-listing/en-US/phone/phone-02-firewall.jpg" height="540" alt="Wi-Fi or mobile, app by app: the firewall list with per-app switches">
</p>

## Quiet by default

<table>
  <tr>
    <td width="25%"><img src="store-listing/en-US/phone/phone-03-log.jpg" alt="See who calls home: the traffic log with blocked tracker domains"></td>
    <td width="25%"><img src="store-listing/en-US/phone/phone-04-detail.jpg" alt="A flashlight stays offline: an app blocked on Wi-Fi and mobile data"></td>
    <td width="25%"><img src="store-listing/en-US/phone/phone-05-rules.jpg" alt="Set it once, stay quiet: default access and firewall rules"></td>
    <td width="25%"><img src="store-listing/en-US/phone/phone-06-filter.jpg" alt="Filter to what matters: blocked connections grouped by app"></td>
  </tr>
</table>

- **Two switches per app.** Allow or block Wi-Fi and mobile data separately, so streaming can wait for Wi-Fi while a flashlight never connects at all.
- **A log you can read.** Every connection shows the app, the domain, and whether it got through. Filter to blocked ones and group them by app.
- **Rules for the edge cases.** Allow an app only while the screen is on, block roaming, lock everything down, and set what new apps get by default.

## On a bigger screen

The app list and an app's rules sit side by side on tablets and foldables.

<p align="center">
  <img src="store-listing/en-US/tablet10/tablet10-01-firewall.jpg" width="80%" alt="QuietGuard on a landscape tablet with the app list beside an app's rules">
</p>

## Private by design

QuietGuard filters on the device through Android's `VpnService`: traffic is not routed through a QuietGuard server, and there is no account, tracking, or analytics. Update checks are off unless you turn them on. It is a local firewall, not an anonymity tool; the [threat model](docs/THREAT-MODEL.md) spells out what it does and does not protect.

QuietGuard is a fork of [NetGuard](https://github.com/M66B/NetGuard), Marcel Bokhorst's no-root firewall, and stays under the GPLv3. The interface and app logic are rewritten in Kotlin Multiplatform and Compose, with a packet engine in Rust.

<sub>Building from source, releases, or contributing? See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).</sub>
