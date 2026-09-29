#!/usr/bin/env bash
set -euo pipefail

root="${1:-docs/site/rafaelia-signing}"
json="$root/latest.json"
txt="$root/latest.txt"
vars="${2:-configs/rafaelia-signing-variables.v1.json}"

[ -f "$json" ] || { echo "SIGNING_PAGE_JSON_MISSING"; exit 1; }
[ -f "$txt" ] || { echo "SIGNING_PAGE_TEXT_MISSING"; exit 1; }
[ -f "$vars" ] || { echo "SIGNING_VARIABLE_CONTRACT_MISSING"; exit 1; }

python3 - "$json" "$txt" "$vars" <<'PY'
import json
import sys
from pathlib import Path

json_path, text_path, variables_path = map(Path, sys.argv[1:])

def reject(code):
    print(code, file=sys.stderr)
    raise SystemExit(1)

expected = {
    "schema", "source_sha", "signed_apk_sha256", "certificate_sha256",
    "signer_id", "certificate_match", "state",
}
try:
    receipt = json.loads(json_path.read_text(encoding="utf-8"))
except (OSError, UnicodeError, json.JSONDecodeError):
    reject("SIGNING_PAGE_JSON_INVALID")

if not isinstance(receipt, dict) or set(receipt) != expected:
    reject("SIGNING_PAGE_FIELDS_INVALID")
if receipt.get("schema") != "rafaelia.signed-release/v1":
    reject("SIGNING_PAGE_SCHEMA_INVALID")
if any(not isinstance(receipt.get(key), str) for key in expected):
    reject("SIGNING_PAGE_VALUE_TYPE_INVALID")

# V1 intentionally publishes an awaiting receipt only. A future signed/failed
# receipt requires a successor contract and validator; this gate fails closed.
if receipt["state"] != "AWAITING_REAL_SIGNING_RECEIPT":
    reject("SIGNING_PAGE_STATE_UNSUPPORTED")
if any(receipt[key] != "TOKEN_VAZIO" for key in (
    "source_sha", "signed_apk_sha256", "certificate_sha256",
    "signer_id", "certificate_match",
)):
    reject("SIGNING_PAGE_UNBOUND_RECEIPT")

try:
    lines = text_path.read_text(encoding="utf-8").splitlines()
except (OSError, UnicodeError):
    reject("SIGNING_PAGE_TEXT_INVALID")

text_receipt = {}
for line in lines:
    if not line or "=" not in line:
        reject("SIGNING_PAGE_TEXT_LINE_INVALID")
    key, value = line.split("=", 1)
    if key in text_receipt:
        reject("SIGNING_PAGE_TEXT_DUPLICATE_KEY")
    text_receipt[key] = value
if set(text_receipt) != expected | {"private_key_material"}:
    reject("SIGNING_PAGE_TEXT_FIELDS_INVALID")
if text_receipt.pop("private_key_material") != "NEVER_PUBLISH":
    reject("PRIVATE_KEY_BOUNDARY_MISSING")
if text_receipt != receipt:
    reject("SIGNING_PAGE_TEXT_JSON_MISMATCH")

try:
    variables = json.loads(variables_path.read_text(encoding="utf-8"))
except (OSError, UnicodeError, json.JSONDecodeError):
    reject("SIGNING_VARIABLE_CONTRACT_INVALID")
if not isinstance(variables, dict) or variables.get("schema") != "rafaelia.signing-variables/v1":
    reject("SIGNING_VARIABLE_SCHEMA_INVALID")

public = variables.get("public_variables")
secrets = variables.get("secrets")
if not isinstance(public, dict) or not isinstance(secrets, dict):
    reject("SIGNING_VARIABLE_GROUPS_INVALID")
if set(public) != {
    "RAFAELIA_SIGNER_ID", "RAFAELIA_EXPECTED_CERT_SHA256",
    "RAFAELIA_ANDROID_KEY_ALIAS", "RAFAELIA_PAGES_REPO",
}:
    reject("SIGNING_PUBLIC_VARIABLES_INVALID")
for name in ("RAFAELIA_SIGNER_ID", "RAFAELIA_EXPECTED_CERT_SHA256", "RAFAELIA_ANDROID_KEY_ALIAS"):
    entry = public.get(name)
    if not isinstance(entry, dict) or entry.get("state") != "TOKEN_VAZIO":
        reject("SIGNING_PUBLIC_VALUE_MUST_REMAIN_UNBOUND")
pages_repo = public.get("RAFAELIA_PAGES_REPO")
if not isinstance(pages_repo, dict) or pages_repo.get("recommended_value") != "rafaelmeloreisnovo/RafGitTools":
    reject("SIGNING_PAGES_REPOSITORY_MISMATCH")

secret_names = {
    "RAFAELIA_ANDROID_KEYSTORE_B64", "RAFAELIA_ANDROID_STORE_PASSWORD",
    "RAFAELIA_ANDROID_KEY_PASSWORD", "RAFAELIA_PAGES_PAT",
}
if set(secrets) != secret_names or any(
    not isinstance(value, str) or not value.startswith("PRIVATE;")
    for value in secrets.values()
):
    reject("SIGNING_SECRET_VALUE_MUST_NOT_BE_PUBLISHED")

print("RAFAELIA_SIGNING_PAGE_CONTRACT=PASS")
PY

if grep -RInE 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|MIIE[A-Za-z0-9+/]{20,}' "$root"; then
  echo "PRIVATE_KEY_MATERIAL_DETECTED_IN_PAGES"
  exit 1
fi
