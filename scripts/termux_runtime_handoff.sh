#!/usr/bin/env bash
# One-command, append-only RafGitTools -> Termux RAFCODE-Φ runtime handoff.
#
# Usage:
#   termux_runtime_handoff.sh [LOCK_FILE [BUILD_DIR [OUT_DIR [--require-artifacts]]]]
#
# This script does not install packages, access the network, execute arbitrary
# shared libraries or promote scientific/runtime claims. It validates source
# provenance, creates a typed runtime manifest, verifies SHA-256 sidecars and
# records the device context that actually performed the handoff.
set -euo pipefail

LOCK_FILE="${1:-runtime-lock.json}"
BUILD_DIR="${2:-.}"
OUT_DIR="${3:-runtime-receipts}"
MODE="${4:-}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GENERATOR="${SCRIPT_DIR}/generate_runtime_manifest.sh"
LOCK_TOOL="${SCRIPT_DIR}/runtime_lock_contract.py"

case "${MODE}" in
  ""|--require-artifacts) ;;
  *)
    echo "[FALHA] quarto argumento desconhecido: ${MODE}" >&2
    exit 2
    ;;
esac

for command in bash python3 sha256sum find sort awk stat uname; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "[FALHA] comando obrigatório ausente: ${command}" >&2
    exit 2
  fi
done

if [ ! -x "${GENERATOR}" ]; then
  echo "[FALHA] gerador não executável: ${GENERATOR}" >&2
  exit 2
fi
if [ ! -f "${LOCK_TOOL}" ]; then
  echo "[FALHA] validador ausente: ${LOCK_TOOL}" >&2
  exit 2
fi
if [ ! -f "${LOCK_FILE}" ]; then
  echo "[FALHA] runtime lock ausente: ${LOCK_FILE}" >&2
  exit 2
fi
if [ ! -d "${BUILD_DIR}" ]; then
  echo "[FALHA] diretório de build ausente: ${BUILD_DIR}" >&2
  exit 2
fi

mkdir -p "${OUT_DIR}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
EVENT_ID="TERMUX-HANDOFF-${STAMP}-$$"
MANIFEST="${OUT_DIR}/RUNTIME_MANIFEST_${STAMP}_$$.json"
RECEIPT="${OUT_DIR}/TERMUX_RUNTIME_HANDOFF_${STAMP}_$$.json"

python3 "${LOCK_TOOL}" validate "${LOCK_FILE}"

GENERATOR_COMMAND=(
  bash
  "${GENERATOR}"
  "${LOCK_FILE}"
  "${BUILD_DIR}"
  "${MANIFEST}"
)
if [ "${MODE}" = "--require-artifacts" ]; then
  GENERATOR_COMMAND+=(--require-artifacts)
fi
"${GENERATOR_COMMAND[@]}"

if [ ! -f "${MANIFEST}" ] || [ ! -f "${MANIFEST}.sha256" ]; then
  echo "[FALHA] manifesto ou sidecar não foi produzido." >&2
  exit 3
fi
(
  cd "$(dirname "${MANIFEST}")"
  sha256sum -c "$(basename "${MANIFEST}.sha256")"
)

DEVICE_CLASS="NON_ANDROID_LOCAL"
if command -v getprop >/dev/null 2>&1; then
  ANDROID_SDK="$(getprop ro.build.version.sdk 2>/dev/null || true)"
  CPU_ABI="$(getprop ro.product.cpu.abi 2>/dev/null || true)"
  if [ -n "${ANDROID_SDK}" ] || [ -n "${CPU_ABI}" ]; then
    DEVICE_CLASS="ANDROID_RUNTIME"
  fi
else
  ANDROID_SDK=""
  CPU_ABI=""
fi

case "${PREFIX:-}" in
  *com.termux.rafacodephi*|*com.termux*)
    DEVICE_CLASS="TERMUX_ANDROID"
    ;;
esac

if [ -z "${CPU_ABI}" ]; then
  CPU_ABI="$(uname -m)"
fi

KERNEL_NAME="$(uname -s)"
KERNEL_RELEASE="$(uname -r)"
PREFIX_VALUE="${PREFIX:-TOKEN_VAZIO}"
SHELL_VALUE="${SHELL:-TOKEN_VAZIO}"
MANIFEST_SHA256="$(awk '{print $1}' "${MANIFEST}.sha256")"
LOCK_SHA256="$(sha256sum "${LOCK_FILE}" | awk '{print $1}')"

python3 - \
  "${RECEIPT}" \
  "${EVENT_ID}" \
  "${STAMP}" \
  "${LOCK_FILE}" \
  "${LOCK_SHA256}" \
  "${MANIFEST}" \
  "${MANIFEST_SHA256}" \
  "${DEVICE_CLASS}" \
  "${CPU_ABI}" \
  "${ANDROID_SDK}" \
  "${KERNEL_NAME}" \
  "${KERNEL_RELEASE}" \
  "${PREFIX_VALUE}" \
  "${SHELL_VALUE}" <<'PY'
import json
import sys
from pathlib import Path

(
    receipt_path,
    event_id,
    stamp,
    lock_file,
    lock_sha256,
    manifest_file,
    manifest_sha256,
    device_class,
    cpu_abi,
    android_sdk,
    kernel_name,
    kernel_release,
    prefix,
    shell,
) = sys.argv[1:]

manifest = json.loads(Path(manifest_file).read_text(encoding="utf-8"))
receipt = {
    "schema": "rafaelia.termux-runtime-handoff-receipt.v1",
    "event_id": event_id,
    "recorded_at_compact_utc": stamp,
    "source_lock": {
        "path": Path(lock_file).name,
        "sha256": lock_sha256,
        "validation": "PASS",
    },
    "runtime_manifest": {
        "path": Path(manifest_file).name,
        "sha256": manifest_sha256,
        "artifact_state": manifest.get("artifact_state", "TOKEN_VAZIO"),
        "artifact_count": manifest.get("artifact_count", 0),
        "artifact_verification_state": manifest.get(
            "artifact_verification_state", "TOKEN_VAZIO"
        ),
    },
    "execution_context": {
        "device_class": device_class,
        "cpu_abi": cpu_abi or "TOKEN_VAZIO",
        "android_sdk": android_sdk or "TOKEN_VAZIO",
        "kernel_name": kernel_name,
        "kernel_release": kernel_release,
        "prefix": prefix,
        "shell": shell,
    },
    "checks": {
        "lock_contract": "PASS",
        "manifest_generated": "PASS",
        "manifest_sha256_sidecar": "PASS",
        "shared_library_execution": "TOKEN_VAZIO_NOT_EXECUTED",
        "apk_installation": "TOKEN_VAZIO_NOT_EXECUTED",
        "device_runtime_smoke": "TOKEN_VAZIO_NOT_EXECUTED",
    },
    "claim_allowed": False,
    "F_ok": "source lock and generated manifest were validated and hash-anchored in the observed execution context",
    "F_gap": "shared libraries, APK installation and runtime behavior were not executed by this handoff",
    "F_next": "run the smallest architecture-matched selftest and append its command, exit code and output hash",
}

Path(receipt_path).write_text(
    json.dumps(receipt, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
    encoding="utf-8",
)
PY

RECEIPT_SHA256="$(sha256sum "${RECEIPT}" | awk '{print $1}')"
printf '%s  %s\n' "${RECEIPT_SHA256}" "$(basename "${RECEIPT}")" > "${RECEIPT}.sha256"
(
  cd "$(dirname "${RECEIPT}")"
  sha256sum -c "$(basename "${RECEIPT}.sha256")"
)

echo "[RAF] handoff_event=${EVENT_ID}"
echo "[RAF] device_class=${DEVICE_CLASS}"
echo "[RAF] manifest=${MANIFEST}"
echo "[RAF] receipt=${RECEIPT}"
echo "[RAF] receipt_sha256=${RECEIPT_SHA256}"
