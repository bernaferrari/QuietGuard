"""Capture QuietGuard's store screens on a dedicated emulator (needs adb and Pillow).

Builds nothing: install the capture build first, which seeds fictional apps and traffic and never
starts the VPN or touches the network:

    ./gradlew :app:installStoreCapture -Pquietguard.abis=arm64-v8a --no-configuration-cache
    python3 store-listing/capture-emulator.py --serial emulator-5572

Use your own AVD and port. Scenes are tapped by their English labels and recorded in
en-US/raw/tap-map.json, so other locales replay the same taps (--locale de-DE).

Writes <locale>/raw/<profile>/<scene>.png plus a <scene>.nodes.json layout sidecar for each capture.
"""
import argparse
import json
import re
import subprocess
import sys
import time
from io import BytesIO
from pathlib import Path

from PIL import Image

parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
parser.add_argument('--serial', required=True, help='Dedicated emulator serial; never a physical phone')
parser.add_argument('--profile', choices=['phone', 'tablet7', 'tablet10', 'all'], default='all')
parser.add_argument('--locale', help='Play locale to capture in, e.g. de-DE (default: the studio default locale)')
parser.add_argument('--only', help='Comma-separated scene names to capture (default: all)')
args = parser.parse_args()
assert args.serial.startswith('emulator-'), 'Use a dedicated emulator, not a personal device.'

listing = Path(__file__).resolve().parent
sys.path.insert(0, str(listing / 'studio'))
from uitree import Navigator, set_app_locale, write_sidecar  # noqa: E402

studio = json.loads((listing / 'studio.json').read_text())
reference = studio.get('defaultLocale', 'en-US')
locale = args.locale or reference
only = set(args.only.split(',')) if args.only else None
# The reference locale records which node each tap hits; other locales replay those tree paths.
navigator = Navigator(listing / reference / 'raw' / 'tap-map.json', recording=locale == reference)
raw = listing / locale / 'raw'
package = 'com.bernaferrari.quietguard.storecapture'
activity = package + '/com.bernaferrari.quietguard.ActivityMain'


def adb(*parts):
    return subprocess.check_output(['adb', '-s', args.serial, *parts], stderr=subprocess.STDOUT)


def broadcast(*extra):
    adb('shell', 'am', 'broadcast', '-a', 'com.android.systemui.demo', '--es', 'command', *extra)


last_xml = None


def dump():
    global last_xml
    for attempt in range(5):
        try:
            adb('shell', 'uiautomator', 'dump', '--compressed', '/sdcard/quietguard-capture.xml')
            last_xml = adb('shell', 'cat', '/sdcard/quietguard-capture.xml').decode()
            if f'package="{package}"' in last_xml:
                return last_xml
        except subprocess.CalledProcessError:
            pass
        time.sleep(1.5)
    raise AssertionError('QuietGuard capture build is not in the foreground')


def find(label, text_only=False):
    pattern = re.compile(label)
    for attempt in range(4):
        dump()
        try:
            return navigator.resolve(f'{profile}|{theme}|{pattern.pattern}', last_xml,
                                     lambda n: bool(pattern.fullmatch(n.get('text', '')) or
                                                    (not text_only and pattern.fullmatch(n.get('desc', '')))))
        except AssertionError:
            if attempt == 3:
                raise
            time.sleep(1)


def tap(label, wait=1.6):
    x1, y1, x2, y2 = find(label)['b']
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(wait)


def go(tab, wait=1.6):
    """Switch tabs by the navigation label (the Home hero's toggle is also described as "Firewall")."""
    x1, y1, x2, y2 = find(tab, text_only=True)['b']
    adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(wait)


def swipe_up(fraction=.45):
    x = width // 2
    adb('shell', 'input', 'swipe', str(x), str(int(height * .75)), str(x), str(int(height * (.75 - fraction))), '600')
    time.sleep(1.2)


def scroll_to_end():
    for _ in range(4):
        swipe_up(.55)


def capture(name):
    if only and name not in only:
        return
    time.sleep(1.2)
    xml = dump()
    png = adb('exec-out', 'screencap', '-p')
    png = png[png.index(b'\x89PNG\r\n\x1a\n'):]  # some emulators print a display warning first
    image = Image.open(BytesIO(png)).convert('RGB')
    assert image.size == (width, height), image.size
    path = raw / profile / f'{name}.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)
    write_sidecar(path, xml, width, height)
    print(path.relative_to(listing), flush=True)


def launch():
    adb('shell', 'am', 'force-stop', package)
    adb('shell', 'pm', 'clear', package)  # fresh navigation; the app reseeds its fixtures on start
    set_app_locale(adb, package, None if locale == 'en-US' and reference == 'en-US' else locale)
    adb('shell', 'am', 'start', '-W', '-n', activity)
    time.sleep(4)


def status_bar():
    broadcast('exit')
    for extra in [('enter',), ('clock', '--es', 'hhmm', '0941'),
                  ('battery', '--es', 'level', '100', '--es', 'plugged', 'false'),
                  ('notifications', '--es', 'visible', 'false'),
                  ('network', '--es', 'wifi', 'show', '--es', 'level', '4', '--es', 'fully', 'true',
                   '--es', 'mobile', 'hide', '--es', 'sims', '0', '--es', 'airplane', 'hide')]:
        broadcast(*extra)


def night(on):
    adb('shell', 'cmd', 'uimode', 'night', 'yes' if on else 'no')
    time.sleep(2)


profiles = {name: (p['captureWidth'], p['captureHeight'], p['captureDensity'])
            for name, p in json.loads((listing / 'profiles.json').read_text()).items() if 'captureWidth' in p}
original = adb('shell', 'wm', 'size').decode() + adb('shell', 'wm', 'density').decode()
assert 'Override' not in original, 'Reset wm size/density first (an interrupted run leaves overrides).'
heads_up = adb('shell', 'settings', 'get', 'global', 'heads_up_notifications_enabled').decode().strip()
profile = theme = None
try:
    adb('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', 'com.android.internal.systemui.navbar.gestural')
    adb('shell', 'settings', 'put', 'global', 'sysui_demo_allowed', '1')
    adb('shell', 'settings', 'put', 'global', 'heads_up_notifications_enabled', '0')
    adb('shell', 'svc', 'wifi', 'disable')
    adb('shell', 'svc', 'data', 'disable')
    for profile, (width, height, density) in profiles.items():
        if args.profile not in ('all', profile):
            continue
        adb('shell', 'wm', 'size', f'{width}x{height}')
        adb('shell', 'wm', 'density', str(density))
        time.sleep(2)
        status_bar()
        theme = 'light'
        night(False)
        launch()
        capture('01-home')

        go('Firewall')
        if profile == 'tablet10':
            tap('Camera')  # list and detail side by side
        capture('02-firewall')
        if profile != 'tablet10':
            tap('Camera')
            capture('04-detail')
            adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
            time.sleep(1.2)

        go('Log')
        capture('03-log')
        tap('Blocked')
        tap('By app')
        capture('08-blocked')

        go('Settings')
        capture('05-settings')
        if profile == 'phone':
            tap('Firewall rules')
            capture('06-rules')
            adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
            time.sleep(1.2)
            scroll_to_end()
            capture('07-appearance')

        theme = 'dark'
        night(True)
        launch()
        capture('09-home-dark')
        go('Firewall')
        if profile == 'tablet10':
            tap('Camera')
        capture('10-firewall-dark')
        night(False)
finally:
    navigator.save()
    adb('shell', 'am', 'force-stop', package)
    adb('shell', 'cmd', 'uimode', 'night', 'no')
    adb('shell', 'wm', 'size', 'reset')
    adb('shell', 'wm', 'density', 'reset')
    broadcast('exit')
    adb('shell', 'settings', 'put', 'global', 'heads_up_notifications_enabled', heads_up if heads_up in ('0', '1') else '1')
