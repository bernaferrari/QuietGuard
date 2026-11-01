# QuietGuard · Play listing

Built with [Screenshot Studio](https://github.com/bernaferrari/screenshot-studio) (`npx screenshot-studio`); the app keeps no copy of it.

| Change | File |
| --- | --- |
| Cards, order, captures | `gallery.json` |
| Words per language | `<locale>/copy.json`, `<locale>/listing.json` |
| Look | `theme.css`, `theme.js` |
| Canvas and capture sizes | `profiles.json` |
| Scene tour | `capture-emulator.py` (build and runs: `studio.json` → `capture`) |

```sh
npx screenshot-studio recapture                 # rebuild, capture every size on emulator-5578, export every language
npx screenshot-studio tour --profile phone       # the scene tour alone
npx screenshot-studio studio                    # editor
npx screenshot-studio export --locale en-US
npx screenshot-studio check                     # fast check after each edit
```

The capture build (`app/src/storeCapture`) shows the real UI over fictional apps, rules and traffic. It has no
VPN service and no INTERNET permission. Play metadata in `fastlane/` is staged separately.
