#!/usr/bin/env bash
set -euo pipefail

root="${1:-docs/site/rafaelia-signing}"
json="$root/latest.json"
txt="$root/latest.txt"
vars="configs/rafaelia-signing-variables.v1.json"

[ -f "$json" ] || { echo "SIGNING_PAGE_JSON_MISSING"; exit 1; }
[ -f "$txt" ] || { echo "SIGNING_PAGE_TXT_MISSING"; exit 1; }
[ -f "$vars" ] || { echo "SIGNING_VARIABLE_CONTRACT_MISSING"; exit 1; }

for key in schema source_sha signed_apk_sha256 certificate_sha256 signer_id certificate_match; do
  grep -q ""$key"" "$json" || {
    echo "SIGNING_PAGE_KEY_MISSING=$key"
    exit 1
  }
done

grep -q 'private_key_material=NEVER_PUBLISH' "$txt" || {
  echo "PRIVATE_KEY_BOUNDARY_MISSING"
  exit 1
}

if grep -RInE 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|MIIE[A-Za-z0-9+/]{20,}' "$root"; then
  echo "PRIVATE_KEY_MATERIAL_DETECTED_IN_PAGES"
  exit 1
fi

echo "RAFAELIA_SIGNING_PAGE_CONTRACT=PASS"
