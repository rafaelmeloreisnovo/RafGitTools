#!/usr/bin/env bash
# Generate RUNTIME_MANIFEST.json with commits, .so SHA-256s, and build metadata.
# Usage: generate_runtime_manifest.sh [<LOCK_FILE> [<BUILD_DIR> [<OUT_FILE>]]]
set -euo pipefail

LOCK_FILE="${1:-runtime-lock.json}"
BUILD_DIR="${2:-.}"
OUT_FILE="${3:-RUNTIME_MANIFEST.json}"

TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
RUN_ID="${GITHUB_RUN_ID:-local}"

LLAMA_COMMIT=$(python3 -c "
import json, sys
d = json.load(open('${LOCK_FILE}'))
print(d['components']['llama_rafaelia']['commit'])
")
RAF_COMMIT=$(python3 -c "
import json, sys
d = json.load(open('${LOCK_FILE}'))
print(d['components']['rafpolimata']['commit'])
")

# Build JSON array of .so artifacts
SO_JSON=""
while IFS= read -r f; do
  SHA=$(sha256sum "$f" | awk '{print $1}')
  SIZE=$(stat -c%s "$f" 2>/dev/null || stat -f%z "$f")
  [ -n "${SO_JSON}" ] && SO_JSON="${SO_JSON},"
  SO_JSON="${SO_JSON}
    {\"path\":\"${f}\",\"sha256\":\"${SHA}\",\"size\":${SIZE}}"
done < <(find "${BUILD_DIR}" -name '*.so' 2>/dev/null | sort)

cat > "${OUT_FILE}" <<EOF
{
  "schema": "rafaelia.runtime-manifest.v1",
  "timestamp": "${TIMESTAMP}",
  "github_run_id": "${RUN_ID}",
  "components": {
    "llama_rafaelia_commit": "${LLAMA_COMMIT}",
    "rafpolimata_commit": "${RAF_COMMIT}"
  },
  "artifacts": [${SO_JSON}
  ]
}
EOF

echo "[RAF] Runtime manifest written to ${OUT_FILE}"
cat "${OUT_FILE}"
