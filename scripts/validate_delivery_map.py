#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAP = ROOT / "configs" / "rafgittools-delivery-map.v1.json"

errors = []
try:
    data = json.loads(MAP.read_text(encoding="utf-8"))
except Exception as exc:
    raise SystemExit(f"delivery-map: invalid JSON: {exc}")

if data.get("schema") != "rafgittools.development-delivery-map/v1":
    errors.append("schema mismatch")
if data.get("claim_allowed") is not False:
    errors.append("claim_allowed must remain false")
if data.get("release_allowed") is not False:
    errors.append("release_allowed must remain false")
if data.get("authority", {}).get("implementation") != "github:rafaelmeloreisnovo/RafGitTools":
    errors.append("implementation authority mismatch")

gates = data.get("gates")
if not isinstance(gates, list) or not gates:
    errors.append("gates must be a non-empty list")
else:
    ids = [item.get("id") for item in gates]
    if any(not value for value in ids):
        errors.append("every gate requires an id")
    if len(ids) != len(set(ids)):
        errors.append("gate ids must be unique")
    for item in gates:
        if not item.get("state"):
            errors.append(f"{item.get('id')}: state required")
        if "TOKEN_VAZIO" in str(item.get("state")) and not item.get("next"):
            errors.append(f"{item.get('id')}: TOKEN_VAZIO requires next")

required_paths = [
    "docs/RAFGITTOOLS_DEVELOPMENT_DELIVERY_MAP_V1.md",
    "docs/RELEASE_NOTES_NEXT.md",
    "docs/RESPONSIVE_LAYOUT_GATE_V1.md",
    "docs/architecture/RAFGITTOOLS_DRIVE_GITHUB_DELIVERY_ARCHITECTURE_V1.md",
    "contracts/drive-github-staging-receipt-v1.schema.json",
    "app/src/main/kotlin/com/rafgittools/ui/components/ResponsiveUtils.kt",
    "app/src/main/kotlin/com/rafgittools/bridge/DriveStagingGate.kt",
]
for rel in required_paths:
    if not (ROOT / rel).is_file():
        errors.append(f"missing required path: {rel}")

responsive = (ROOT / "app/src/main/kotlin/com/rafgittools/ui/components/ResponsiveUtils.kt").read_text(encoding="utf-8")
home = (ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt").read_text(encoding="utf-8")
gate = (ROOT / "app/src/main/kotlin/com/rafgittools/bridge/DriveStagingGate.kt").read_text(encoding="utf-8")

for symbol in ("windowSizeForWidth", "ResponsiveContentFrame"):
    if symbol not in responsive:
        errors.append(f"responsive symbol missing: {symbol}")
if "ResponsiveContentFrame(" not in home:
    errors.append("HomeScreen does not consume ResponsiveContentFrame")
if "DriveStagingGate.verifyAndWriteReceipt" not in home:
    errors.append("HomeScreen does not execute the Drive staging gate")
if "TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED" not in gate:
    errors.append("Drive staging gate must preserve unresolved GitHub recipient")

if errors:
    for error in errors:
        print(f"FAIL: {error}")
    raise SystemExit(1)

print("RafGitTools development/delivery gate: PASS")
print(f"gates={len(gates)} claim_allowed=false release_allowed=false")
