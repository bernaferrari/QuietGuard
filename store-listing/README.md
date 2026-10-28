# QuietGuard · Play listing

Built with [Screenshot Studio](studio/). Read `~/Documents/screenshot-studio/PLAYBOOK.md` first.

| Change | File |
| --- | --- |
| Cards, order, captures | `gallery.json` |
| Words per language | `<locale>/copy.json`, `<locale>/listing.json` |
| Look | `theme.css`, `theme.js` |
| Canvas and capture sizes | `profiles.json` |

```sh
store-listing/recapture.sh                       # rebuild, capture every size on emulator-5578, export every language
node store-listing/studio/server.cjs 8790        # editor
```

The capture build (`app/src/storeCapture`) shows the real UI over fictional apps, rules and traffic. It has no
VPN service and no INTERNET permission. Play metadata in `fastlane/` is staged separately.
