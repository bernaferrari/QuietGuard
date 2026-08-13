#!/usr/bin/env bash
# Deploy the verified wasm dist to Vercel.
# Requires VERCEL_TOKEN. Optionally VERCEL_ORG_ID and VERCEL_PROJECT_ID.
#
# Usage:
#   ./scripts/verify-web-deploy.sh --ci
#   VERCEL_TOKEN=... VERCEL_ORG_ID=... VERCEL_PROJECT_ID=... ./scripts/deploy-web-vercel.sh
#   ./scripts/deploy-web-vercel.sh --preview   # non-production deploy
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/webApp/build/dist/wasmJs/productionExecutable"
PREVIEW=false

for arg in "$@"; do
  case "$arg" in
    --preview) PREVIEW=true ;;
    -h|--help)
      echo "Usage: $0 [--preview]"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

if [[ ! -f "$DIST/index.html" || ! -f "$DIST/netguard.js" ]]; then
  echo "Dist not ready. Run ./scripts/verify-web-deploy.sh first." >&2
  exit 1
fi

# Never upload or promote a distribution that has changed since its last build.
# In particular, this catches netguard.js referencing a new content-hashed Wasm
# file that is missing from a stale deployment directory.
"$ROOT/scripts/verify-web-deploy.sh" --ci --skip-build

PROJECT_JSON="$ROOT/webApp/.vercel/project.json"
REPO_JSON="$ROOT/.vercel/repo.json"
if [[ -z "${VERCEL_ORG_ID:-}" && -f "$PROJECT_JSON" ]]; then
  VERCEL_ORG_ID="$(jq -er '.orgId' "$PROJECT_JSON" 2>/dev/null || true)"
fi
if [[ -z "${VERCEL_PROJECT_ID:-}" && -f "$PROJECT_JSON" ]]; then
  VERCEL_PROJECT_ID="$(jq -er '.projectId' "$PROJECT_JSON" 2>/dev/null || true)"
fi

# Newer Vercel CLI versions may store monorepo links in .vercel/repo.json rather
# than a project.json inside the deployment directory.
if [[ -z "${VERCEL_ORG_ID:-}" && -f "$REPO_JSON" ]]; then
  VERCEL_ORG_ID="$(jq -er '.projects[] | select(.directory == ".") | .orgId' "$REPO_JSON" 2>/dev/null | head -n 1 || true)"
fi
if [[ -z "${VERCEL_PROJECT_ID:-}" && -f "$REPO_JSON" ]]; then
  VERCEL_PROJECT_ID="$(jq -er '.projects[] | select(.directory == ".") | .id' "$REPO_JSON" 2>/dev/null | head -n 1 || true)"
fi
export VERCEL_ORG_ID VERCEL_PROJECT_ID

if [[ -z "${VERCEL_TOKEN:-}" && -f "$HOME/Library/Application Support/com.vercel.cli/auth.json" ]]; then
  VERCEL_TOKEN="$(python3 -c "import json; print(json.load(open('$HOME/Library/Application Support/com.vercel.cli/auth.json'))['token'])")"
  export VERCEL_TOKEN
fi

: "${VERCEL_TOKEN:?Set VERCEL_TOKEN (vercel login, or https://vercel.com/account/tokens)}"

export VERCEL_ORG_ID="${VERCEL_ORG_ID:?Set VERCEL_ORG_ID}"
export VERCEL_PROJECT_ID="${VERCEL_PROJECT_ID:?Set VERCEL_PROJECT_ID}"

cd "$DIST"

# Bundle the output before uploading. Kotlin/Wasm distributions contain enough
# generated files to exhaust Vercel's per-file upload quota without an archive.
args=(deploy --yes --archive=tgz --token "$VERCEL_TOKEN")
if [[ "$PREVIEW" == false ]]; then
  # Build against the production target but do not move any public domain until
  # the immutable deployment URL passes the Wasm integrity checks below.
  args+=(--prod --skip-domain)
fi

echo "==> Deploying $(basename "$DIST") to Vercel"
deployment_output="$(vercel "${args[@]}")"
printf '%s\n' "$deployment_output"

# Vercel prints a bare URL in CI and structured JSON in agent-aware terminals.
# Normalize both forms before assigning the stable production alias.
if deployment_url="$(printf '%s\n' "$deployment_output" | jq -er '.deployment.url // .url' 2>/dev/null)"; then
  :
else
  deployment_url="$(
    printf '%s\n' "$deployment_output" |
      grep -Eo 'https://[A-Za-z0-9.-]+\.vercel\.app' |
      head -n 1
  )"
fi

if [[ -z "$deployment_url" ]]; then
  echo "Could not determine the Vercel deployment URL." >&2
  exit 1
fi

echo "    deployment: $deployment_url"

if [[ "$PREVIEW" == false ]]; then
  echo "==> Verifying deployed JavaScript -> Wasm asset references"
  remote_check_dir="$(mktemp -d)"
  cleanup_remote_check() {
    rm -rf "$remote_check_dir"
  }
  trap cleanup_remote_check EXIT

  curl --fail --silent --show-error \
    "$deployment_url/netguard.js" \
    --output "$remote_check_dir/netguard.js"
  curl --fail --silent --show-error \
    "$deployment_url/sqlite-wasm-worker/worker.js" \
    --output "$remote_check_dir/sqlite-worker.js"

  remote_wasm_references="$({
    grep -ahoE '[0-9a-f]{20}\.wasm' \
      "$remote_check_dir/netguard.js" \
      "$remote_check_dir/sqlite-worker.js" \
      | sort -u
  } || true)"
  if [[ -z "$remote_wasm_references" ]]; then
    echo "Deployed JavaScript contains no content-hashed Wasm references" >&2
    exit 1
  fi

  while IFS= read -r wasm_asset; do
    curl --fail --silent --show-error \
      "$deployment_url/$wasm_asset" \
      --output "$remote_check_dir/$wasm_asset"
    if [[ "$(od -An -tx1 -N4 "$remote_check_dir/$wasm_asset" | tr -d '[:space:]')" != "0061736d" ]]; then
      echo "Deployed asset is not a valid Wasm binary: $wasm_asset" >&2
      exit 1
    fi
    echo "    ok: $wasm_asset"
  done <<< "$remote_wasm_references"

  # Promote the stable domain only after the candidate deployment is proven to
  # contain every Wasm binary referenced by its JavaScript loaders.
  echo "==> Promoting $deployment_url to quietguard.vercel.app"
  vercel alias set "$deployment_url" quietguard.vercel.app --token "$VERCEL_TOKEN"
fi
