#!/usr/bin/env bash
# Generate RUNTIME_MANIFEST.json with locked source commits, .so SHA-256s,
# and build metadata.
# Usage: generate_runtime_manifest.sh [<LOCK_FILE> [<BUILD_DIR> [<OUT_FILE>]]]
set -euo pipefail

LOCK_FILE="${1:-runtime-lock.json}"
BUILD_DIR="${2:-.}"
OUT_FILE="${3:-RUNTIME_MANIFEST.json}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCK_TOOL="${SCRIPT_DIR}/runtime_lock_contract.py"

TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
RUN_ID="${GITHUB_RUN_ID:-local}"

python3 "${LOCK_TOOL}" validate "${LOCK_FILE}"

RAFGITTOOLS_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/RafGitTools commit)"
TERMUX_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/termux-app-rafacodephi commit)"
CONVERSATIONS_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/CONVERSATIONS_CHUNKS_PRIVATE commit)"
LLAMA_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/llamaRafaelia commit)"
RAFPOLIMATA_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/RafPolimata commit)"

# Build JSON array of .so artifacts. Paths are encoded through Python so that
# quotes, backslashes, and UTF-8 names cannot corrupt the manifest.
ARTIFACTS_FILE="$(mktemp)"
trap 'rm -f "${ARTIFACTS_FILE}"' EXIT

while IFS= read -r -d '' file; do
  sha="$(sha256sum "${file}" | awk '{print $1}')"
  size="$(stat -c%s "${file}" 2>/dev/null || stat -f%z "${file}")"
  python3 - "${file}" "${sha}" "${size}" >> "${ARTIFACTS_FILE}" <<'PY'
import json
import sys

print(json.dumps({"path": sys.argv[1], "sha256": sys.argv[2], "size": int(sys.argv[3])}, ensure_ascii=False))
PY
done < <(find "${BUILD_DIR}" -type f -name '*.so' -print0 2>/dev/null | sort -z)

python3 - \
  "${OUT_FILE}" \
  "${TIMESTAMP}" \
  "${RUN_ID}" \
  "${RAFGITTOOLS_COMMIT}" \
  "${TERMUX_COMMIT}" \
  "${CONVERSATIONS_COMMIT}" \
  "${LLAMA_COMMIT}" \
  "${RAFPOLIMATA_COMMIT}" \
  "${ARTIFACTS_FILE}" <<'PY'
import json
import sys
from pathlib import Path

(
    out_file,
    timestamp,
    run_id,
    rafgittools_commit,
    termux_commit,
    conversations_commit,
    llama_commit,
    rafpolimata_commit,
    artifacts_file,
) = sys.argv[1:]

artifacts = []
for line in Path(artifacts_file).read_text(encoding="utf-8").splitlines():
    if line:
        artifacts.append(json.loads(line))

manifest = {
    "schema": "rafaelia.runtime-manifest.v1",
    "timestamp": timestamp,
    "github_run_id": run_id,
    "components": {
        "rafgittools_commit": rafgittools_commit,
        "termux_rafcodephi_commit": termux_commit,
        "conversations_chunks_commit": conversations_commit,
        "llama_rafaelia_commit": llama_commit,
        "rafpolimata_commit": rafpolimata_commit,
    },
    "artifacts": artifacts,
}

Path(out_file).write_text(
    json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
PY

echo "[RAF] Runtime manifest written to ${OUT_FILE}"
cat "${OUT_FILE}"
