#!/usr/bin/env bash
# Generate a provenance-preserving runtime manifest from the canonical source lock.
#
# Usage:
#   generate_runtime_manifest.sh [LOCK_FILE [BUILD_DIR [OUT_FILE [--require-artifacts]]]]
#
# The default mode permits a SOURCE_ONLY manifest so failed or pre-build runs can
# still preserve source provenance. Promotion callers may pass
# --require-artifacts (or REQUIRE_ARTIFACTS=1) to reject an empty artifact set.
set -euo pipefail

LOCK_FILE="${1:-runtime-lock.json}"
BUILD_DIR="${2:-.}"
OUT_FILE="${3:-RUNTIME_MANIFEST.json}"
MODE="${4:-}"
REQUIRE_ARTIFACTS="${REQUIRE_ARTIFACTS:-0}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCK_TOOL="${SCRIPT_DIR}/runtime_lock_contract.py"

case "${MODE}" in
  "") ;;
  --require-artifacts) REQUIRE_ARTIFACTS=1 ;;
  *)
    echo "[FALHA] quarto argumento desconhecido: ${MODE}" >&2
    exit 2
    ;;
esac

case "${REQUIRE_ARTIFACTS}" in
  0|1) ;;
  *)
    echo "[FALHA] REQUIRE_ARTIFACTS deve ser 0 ou 1." >&2
    exit 2
    ;;
esac

if [ ! -f "${LOCK_FILE}" ]; then
  echo "[FALHA] runtime lock ausente: ${LOCK_FILE}" >&2
  exit 2
fi
if [ ! -d "${BUILD_DIR}" ]; then
  echo "[FALHA] diretório de build ausente: ${BUILD_DIR}" >&2
  exit 2
fi

mkdir -p "$(dirname "${OUT_FILE}")"

TIMESTAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
RUN_ID="${GITHUB_RUN_ID:-local}"
LOCK_SHA256="$(sha256sum "${LOCK_FILE}" | awk '{print $1}')"

python3 "${LOCK_TOOL}" validate "${LOCK_FILE}"

if [ -n "${GITHUB_SHA:-}" ]; then
  RAFGITTOOLS_COMMIT="${GITHUB_SHA}"
elif git rev-parse --verify HEAD >/dev/null 2>&1; then
  RAFGITTOOLS_COMMIT="$(git rev-parse HEAD)"
else
  echo "[FALHA] não foi possível determinar o commit real do RafGitTools." >&2
  exit 2
fi
TERMUX_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/termux-app-rafacodephi commit)"
CONVERSATIONS_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/CONVERSATIONS_CHUNKS_PRIVATE commit)"
LLAMA_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/llamaRafaelia commit)"
RAFPOLIMATA_COMMIT="$(python3 "${LOCK_TOOL}" get "${LOCK_FILE}" rafaelmeloreisnovo/RafPolimata commit)"

# Each artifact path is stored relative to BUILD_DIR. This prevents device- or
# runner-specific absolute paths from contaminating otherwise identical receipts.
ARTIFACTS_FILE="$(mktemp)"
trap 'rm -f "${ARTIFACTS_FILE}"' EXIT

while IFS= read -r -d '' file; do
  sha="$(sha256sum "${file}" | awk '{print $1}')"
  size="$(stat -c%s "${file}" 2>/dev/null || stat -f%z "${file}")"
  python3 - "${BUILD_DIR}" "${file}" "${sha}" "${size}" >> "${ARTIFACTS_FILE}" <<'PY'
import json
import sys
from pathlib import Path

root = Path(sys.argv[1]).resolve()
artifact = Path(sys.argv[2]).resolve()
try:
    relative = artifact.relative_to(root).as_posix()
except ValueError as exc:
    raise SystemExit(f"artifact escaped build root: {artifact}") from exc

print(
    json.dumps(
        {
            "path": relative,
            "sha256": sys.argv[3],
            "size": int(sys.argv[4]),
        },
        ensure_ascii=False,
        sort_keys=True,
    )
)
PY
done < <(find "${BUILD_DIR}" -type f -name '*.so' -print0 2>/dev/null | sort -z)

ARTIFACT_COUNT="$(awk 'NF {count += 1} END {print count + 0}' "${ARTIFACTS_FILE}")"
if [ "${REQUIRE_ARTIFACTS}" = "1" ] && [ "${ARTIFACT_COUNT}" -eq 0 ]; then
  echo "[FALHA] promoção exige artefatos .so, mas nenhum foi encontrado em ${BUILD_DIR}." >&2
  exit 3
fi

python3 - \
  "${OUT_FILE}" \
  "${TIMESTAMP}" \
  "${RUN_ID}" \
  "${RAFGITTOOLS_COMMIT}" \
  "${TERMUX_COMMIT}" \
  "${CONVERSATIONS_COMMIT}" \
  "${LLAMA_COMMIT}" \
  "${RAFPOLIMATA_COMMIT}" \
  "${LOCK_FILE}" \
  "${LOCK_SHA256}" \
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
    lock_file,
    lock_sha256,
    artifacts_file,
) = sys.argv[1:]

artifacts = []
for line in Path(artifacts_file).read_text(encoding="utf-8").splitlines():
    if line:
        artifacts.append(json.loads(line))

lock_data = json.loads(Path(lock_file).read_text(encoding="utf-8"))
artifact_state = "ARTIFACTS_PRESENT" if artifacts else "SOURCE_ONLY"
verification_state = "HASHED_NOT_PROMOTED" if artifacts else "TOKEN_VAZIO"

manifest = {
    "schema": "rafaelia.runtime-manifest.v1",
    "timestamp": timestamp,
    "github_run_id": run_id,
    "source_lock": {
        "path": Path(lock_file).name,
        "sha256": lock_sha256,
        "state": lock_data.get("release_state", "TOKEN_VAZIO"),
    },
    "components": {
        "rafgittools_commit": rafgittools_commit,
        "termux_rafcodephi_commit": termux_commit,
        "conversations_chunks_commit": conversations_commit,
        "llama_rafaelia_commit": llama_commit,
        "rafpolimata_commit": rafpolimata_commit,
    },
    "artifact_state": artifact_state,
    "artifact_verification_state": verification_state,
    "artifact_count": len(artifacts),
    "artifacts": artifacts,
    "runtime_state": "TOKEN_VAZIO_DEVICE_EXECUTION",
    "claim_allowed": False,
}

Path(out_file).write_text(
    json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
    encoding="utf-8",
)
PY

MANIFEST_SHA256="$(sha256sum "${OUT_FILE}" | awk '{print $1}')"
printf '%s  %s\n' "${MANIFEST_SHA256}" "$(basename "${OUT_FILE}")" > "${OUT_FILE}.sha256"

echo "[RAF] Runtime manifest written to ${OUT_FILE}"
echo "[RAF] artifact_count=${ARTIFACT_COUNT}"
echo "[RAF] manifest_sha256=${MANIFEST_SHA256}"
cat "${OUT_FILE}"
