"""Capture QuietGuard's store screens on the capture emulator (needs adb and Pillow).

`node store-listing/studio/recapture.cjs` builds the capture build, which seeds fictional apps and
traffic and never starts the VPN or touches the network, installs it, runs this tour for every device
size and exports. Run alone: python3 store-listing/capture-emulator.py [--profile phone] [--locale de-DE].

Scenes are tapped by their English labels and recorded in en-US/raw/tap-map.json, so other locales
replay the same taps. Writes <locale>/raw/<profile>/<scene>.png plus a <scene>.nodes.json layout sidecar.
The emulator plumbing is the studio's capture.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / 'studio'))
from capture import Device, parser  # noqa: E402

args = parser(__doc__).parse_args()
d = Device(args, 'com.bernaferrari.quietguard.storecapture', 'com.bernaferrari.quietguard.ActivityMain')


def tap(label):
    d.tap(label, wait=1.6, attempts=4, pause=1)


def go(tab):
    """Switch tabs by the navigation label (the Home hero's toggle is also described as "Firewall")."""
    d.tap(tab, wait=1.6, text_only=True, attempts=4, pause=1)


def launch():
    d.launch(clear=True)  # fresh navigation; the app reseeds its fixtures on start


with d:
    for profile in d.profiles():
        d.night(False)
        launch()
        d.shot('01-home')

        go('Firewall')
        if profile == 'tablet10':
            tap('Camera')  # list and detail side by side
        d.shot('02-firewall')
        if profile != 'tablet10':
            tap('Camera')
            d.shot('04-detail')
            d.back()

        go('Log')
        d.shot('03-log')
        tap('Blocked')
        tap('By app')
        d.shot('08-blocked')

        go('Settings')
        d.shot('05-settings')
        if profile == 'phone':
            tap('Firewall rules')
            d.shot('06-rules')
            d.back()
            for _ in range(4):
                d.scroll(.55, start=.75, duration=600)
            d.shot('07-appearance')

        d.night(True)
        launch()
        d.shot('09-home-dark')
        go('Firewall')
        if profile == 'tablet10':
            tap('Camera')
        d.shot('10-firewall-dark')
