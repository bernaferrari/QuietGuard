#!/usr/bin/env bash
# Serve webApp production dist with COOP/COEP (required for Room sqlite-web / OPFS).
# Usage: ./scripts/serve-web-dist.sh [port]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/webApp/build/dist/wasmJs/productionExecutable"
PORT="${1:-4173}"

if [[ ! -f "$DIST/index.html" ]]; then
  echo "Dist missing; building first..."
  "$ROOT/scripts/verify-web-deploy.sh" --ci
fi

if command -v vercel >/dev/null 2>&1 && [[ -f "$DIST/vercel.json" ]]; then
  echo "Serving $DIST on http://127.0.0.1:$PORT (vercel dev, COOP/COEP via vercel.json)"
  cd "$DIST"
  exec vercel dev --listen "$PORT"
fi

if command -v python3 >/dev/null 2>&1; then
  echo "Serving $DIST on http://127.0.0.1:$PORT (python http.server + COOP/COEP)"
  cd "$DIST"
  exec python3 - "$PORT" <<'PY'
import http.server
import sys

PORT = int(sys.argv[1])
HEADERS = {
    "Cross-Origin-Opener-Policy": "same-origin",
    "Cross-Origin-Embedder-Policy": "require-corp",
}

class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {
        **getattr(http.server.SimpleHTTPRequestHandler, "extensions_map", {}),
        ".wasm": "application/wasm",
        ".js": "application/javascript",
        ".mjs": "application/javascript",
    }

    def end_headers(self):
        for k, v in HEADERS.items():
            self.send_header(k, v)
        super().end_headers()

http.server.ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
PY
fi

echo "No suitable server found (need vercel or python3)" >&2
exit 1
