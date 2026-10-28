# QuietGuard · Play listing

Built with [Screenshot Studio](studio/). Read `~/Documents/screenshot-studio/PLAYBOOK.md` first.

| Change | File |
| --- | --- |
| Cards, order, captures | `gallery.json` |
| Words per language | `<locale>/copy.json`, `<locale>/listing.json` |
| Look | `theme.css`, `theme.js` |
| Canvas and capture sizes | `profiles.json` |

```sh
./gradlew :app:installStoreCapture -Pquietguard.abis=arm64-v8a --no-configuration-cache
uv run --with pillow python store-listing/capture-emulator.py --serial emulator-5578   # the shared Resizable_Experimental emulator
node store-listing/studio/server.cjs 8790        # editor
node store-listing/studio/export.cjs --locale en-US
```

The capture build (`app/src/storeCapture`) shows the real UI over fictional apps, rules and traffic. It has no
VPN service and no INTERNET permission. Play metadata in `fastlane/` is staged separately.
