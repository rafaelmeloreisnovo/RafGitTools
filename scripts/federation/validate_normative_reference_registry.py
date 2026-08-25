#!/usr/bin/env python3
"""Validate the RAFAELIA normative reference registry without claiming conformity."""
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

SCHEMA = "rafaelia.normative-reference-registry/v1"
REQUIRED_IDS = {
    "ISO-9000-2026","ISO-9001-2015-AMD1-2024",
    "ISO-IEC-27001-2022-AMD1-2024","ISO-IEC-27002-2022",
    "IEEE-1012-2024","IEEE-730-2026","ISO-IEC-IEEE-29148-2018",
    "W3C-PROV-O-2013",
}
OFFICIAL_HOSTS = {"www.iso.org","standards.ieee.org","www.w3.org"}

def load(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError("registry root must be an object")
    return value

def validate(value: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if value.get("schema_version") != SCHEMA:
        errors.append("schema_version mismatch")
    if value.get("adoption_state") != "REFERENCE_ONLY":
        errors.append("adoption_state must remain REFERENCE_ONLY")
    if value.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")
    standards = value.get("standards")
    if not isinstance(standards, list):
        return errors + ["standards must be an array"]
    ids: list[str] = []
    by_id: dict[str, dict[str, Any]] = {}
    for position, item in enumerate(standards):
        label = f"standards[{position}]"
        if not isinstance(item, dict):
            errors.append(f"{label} must be an object")
            continue
        identifier = item.get("id")
        if not isinstance(identifier, str):
            errors.append(f"{label}.id must be a string")
            continue
        ids.append(identifier)
        by_id[identifier] = item
        if item.get("conformance_claim") is not False:
            errors.append(f"{identifier}: conformance_claim must be false")
        if item.get("certification_claim") is not False:
            errors.append(f"{identifier}: certification_claim must be false")
        if item.get("implementation_receipt") != "TOKEN_VAZIO":
            errors.append(f"{identifier}: implementation_receipt must remain TOKEN_VAZIO in this reference registry")
        host = urlparse(str(item.get("official_url", ""))).hostname
        if host not in OFFICIAL_HOSTS:
            errors.append(f"{identifier}: official_url must use an official authority host")
        for field in ("title","edition_state","mapping_purpose"):
            if not isinstance(item.get(field), str) or not item[field].strip():
                errors.append(f"{identifier}: {field} must be non-empty")
    if len(ids) != len(set(ids)):
        errors.append("standard ids must be unique")
    if set(ids) != REQUIRED_IDS:
        errors.append("registry must contain the exact governed reference set")
    if by_id.get("IEEE-1012-2024", {}).get("supersedes") != "IEEE-1012-2016":
        errors.append("IEEE 1012-2024 must record 1012-2016 as superseded")
    if by_id.get("IEEE-730-2026", {}).get("supersedes") != "IEEE-730-2014":
        errors.append("IEEE 730-2026 must record 730-2014 as superseded")
    state_9001 = by_id.get("ISO-9001-2015-AMD1-2024", {}).get("edition_state", "")
    if "PENDING_EXPECTED_2026_REPLACEMENT" not in state_9001:
        errors.append("ISO 9001 must distinguish current requirements from the expected 2026 replacement")
    prov = by_id.get("W3C-PROV-O-2013", {})
    if "STRUCTURAL_MAPPING_ONLY" not in str(prov.get("edition_state", "")):
        errors.append("PROV-O must remain structural mapping only")
    boundaries = value.get("boundaries")
    if not isinstance(boundaries, list) or "control_mapping_is_not_conformance" not in boundaries:
        errors.append("boundary control_mapping_is_not_conformance is required")
    return errors

def build_report(value: dict[str, Any]) -> dict[str, Any]:
    errors = validate(value)
    digest = hashlib.sha256(
        json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    ).hexdigest() if not errors else None
    return {
        "schema_version": "rafaelia.normative-reference-registry-report/v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "reference_count": len(value.get("standards", [])),
        "semantic_digest": digest,
        "defects": errors,
        "boundary": "Reference catalog validation is not conformity, certification, audit, or implementation evidence.",
    }

def main() -> int:
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", type=Path, default=root / "configs/normative-reference-registry.v1.json")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    try:
        report = build_report(load(args.registry))
    except Exception as exc:
        report = {"schema_version":"rafaelia.normative-reference-registry-report/v1","status":"FAIL","claim_allowed":False,"defects":[f"load failure: {exc}"]}
    rendered = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    print(rendered, end="")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered, encoding="utf-8")
    return 0 if report["status"] == "PASS" else 1

if __name__ == "__main__":
    raise SystemExit(main())
