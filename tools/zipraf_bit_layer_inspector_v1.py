#!/usr/bin/env python3
"""Read-only inspector for canonical ZIPRAF Bit Layer Phase-A vectors.

This tool is a consumer/verifier. It is not the ZIPRAF format authority and
does not define canonical block-mask M or geometry G(M).
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any, Dict, List, Optional


class InspectionError(ValueError):
    pass


def read_json(path: Path) -> Dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise InspectionError("JSON_ROOT_NOT_OBJECT")
    return data


def bitplanes(value: int) -> List[int]:
    if not isinstance(value, int) or isinstance(value, bool) or value < 0 or value > 255:
        raise InspectionError("BYTE_OUT_OF_RANGE")
    return [(value >> k) & 1 for k in range(8)]


def qmask(q: int) -> int:
    if not isinstance(q, int) or isinstance(q, bool) or q < 1 or q > 8:
        raise InspectionError("Q_OUT_OF_RANGE")
    return (0xFF << (8 - q)) & 0xFF


def _is_governed_gap(value: Any) -> bool:
    return isinstance(value, str) and value.upper().startswith("TOKEN_VAZIO")


def inspect(
    config: Dict[str, Any],
    vectors: Dict[str, Any],
    *,
    source_sha256: str = "",
    sample_id: Optional[str] = None,
) -> Dict[str, Any]:
    failures: List[str] = []

    authority = config.get("authority")
    if not isinstance(authority, dict):
        authority = {}
        failures.append("AUTHORITY_MISSING")

    if config.get("mode") != "READ_ONLY":
        failures.append("MODE_NOT_READ_ONLY")
    if config.get("mutations_allowed") is not False:
        failures.append("MUTATION_BOUNDARY_BROKEN")
    if config.get("claim_allowed") is not False:
        failures.append("INSPECTOR_CLAIM_BOUNDARY_BROKEN")

    if vectors.get("contract") != authority.get("contract"):
        failures.append("CONTRACT_MISMATCH")
    if vectors.get("parent_contract") != authority.get("parent_contract"):
        failures.append("PARENT_CONTRACT_MISMATCH")
    if vectors.get("phase") != authority.get("phase"):
        failures.append("PHASE_MISMATCH")
    if vectors.get("claim_allowed") is not False:
        failures.append("UPSTREAM_CLAIM_BOUNDARY_BROKEN")

    if vectors.get("widths") != config.get("accepted_widths"):
        failures.append("WIDTH_SET_MISMATCH")
    if vectors.get("q_values") != config.get("accepted_q_values"):
        failures.append("Q_SET_MISMATCH")

    geometry = vectors.get("geometry")
    if not isinstance(geometry, dict):
        geometry = {}
        failures.append("GEOMETRY_GAP_MAP_MISSING")
    for key in config.get("required_governed_gaps", []):
        if not _is_governed_gap(geometry.get(key)):
            failures.append(f"GAP_PROMOTED_OR_MISSING:{key}")

    samples = vectors.get("samples")
    if not isinstance(samples, list) or not samples:
        samples = []
        failures.append("SAMPLES_MISSING")

    selected: Optional[Dict[str, Any]] = None
    seen_ids = set()
    valid_samples = 0

    for raw in samples:
        if not isinstance(raw, dict):
            failures.append("SAMPLE_NOT_OBJECT")
            continue
        sid = raw.get("id")
        if not isinstance(sid, str) or not sid:
            failures.append("SAMPLE_ID_INVALID")
            continue
        if sid in seen_ids:
            failures.append(f"DUPLICATE_SAMPLE_ID:{sid}")
            continue
        seen_ids.add(sid)

        try:
            expected_planes = bitplanes(raw.get("byte"))
        except InspectionError:
            failures.append(f"BYTE_INVALID:{sid}")
            continue

        if raw.get("planes_lsb_to_msb") != expected_planes:
            failures.append(f"PLANES_MISMATCH:{sid}")
            continue

        q_values = raw.get("q_reconstruction")
        if not isinstance(q_values, dict):
            failures.append(f"Q_RECONSTRUCTION_MISSING:{sid}")
            continue

        value = raw["byte"]
        sample_ok = True
        for q in config.get("accepted_q_values", []):
            expected = value & qmask(q)
            if q_values.get(str(q)) != expected:
                failures.append(f"Q_RECONSTRUCTION_MISMATCH:{sid}:q{q}")
                sample_ok = False
        if sample_ok:
            valid_samples += 1

        if sid == sample_id:
            selected = {
                "id": sid,
                "byte": value,
                "planes_lsb_to_msb": expected_planes,
                "q_reconstruction": {str(q): value & qmask(q) for q in config.get("accepted_q_values", [])},
            }

    if sample_id is not None and selected is None:
        failures.append("SAMPLE_NOT_FOUND")

    return {
        "schema": "rafgittools.zipraf_bit_layer_inspection_receipt.v1",
        "status": "PASS_INSPECTION" if not failures else "HOLD_FAIL_CLOSED",
        "claim_allowed": False,
        "mode": "READ_ONLY",
        "mutations_performed": False,
        "authority": {
            "repository": authority.get("repository", "TOKEN_VAZIO"),
            "merge_commit": authority.get("merge_commit", "TOKEN_VAZIO"),
            "path": authority.get("path", "TOKEN_VAZIO"),
            "git_blob_sha": authority.get("git_blob_sha", "TOKEN_VAZIO"),
        },
        "source_sha256": source_sha256 or "TOKEN_VAZIO_NOT_PROVIDED",
        "contract": vectors.get("contract", "TOKEN_VAZIO"),
        "phase": vectors.get("phase", "TOKEN_VAZIO"),
        "validated_samples": valid_samples,
        "geometry": {
            "M": geometry.get("M", "TOKEN_VAZIO"),
            "G(M)": geometry.get("G(M)", "TOKEN_VAZIO"),
            "T-BL-010": geometry.get("T-BL-010", "TOKEN_VAZIO"),
            "policy": config.get("geometry_policy", "PRESERVE_UPSTREAM_TOKEN_VAZIO"),
        },
        "selected_sample": selected,
        "failures": failures,
        "boundary": "INSPECTION_ONLY_NO_FORMAT_AUTHORITY_NO_GEOMETRY_PROMOTION",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default="configs/zipraf-bit-layer-inspector-v1.json")
    parser.add_argument("--vectors", required=True)
    parser.add_argument("--sample-id")
    args = parser.parse_args()

    config_path = Path(args.config)
    vectors_path = Path(args.vectors)
    raw = vectors_path.read_bytes()
    result = inspect(
        read_json(config_path),
        json.loads(raw.decode("utf-8")),
        source_sha256=hashlib.sha256(raw).hexdigest(),
        sample_id=args.sample_id,
    )
    print(json.dumps(result, indent=2, ensure_ascii=False, sort_keys=True))
    return 0 if result["status"] == "PASS_INSPECTION" else 2


if __name__ == "__main__":
    raise SystemExit(main())
