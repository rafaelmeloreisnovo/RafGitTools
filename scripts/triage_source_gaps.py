#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

import audit_source_gaps as source_gap

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / ".rafgittools" / "receipts" / "source-gap-triage-v1.json"
SCHEMA = "RAFGITTOOLS_SOURCE_GAP_TRIAGE_V1"

BLOCKER_PRIORITY = {
    "NOT_IMPLEMENTED_ERROR": "P0",
    "UNSUPPORTED_NOT_IMPLEMENTED": "P0",
    "ERROR_NOT_IMPLEMENTED": "P0",
    "KOTLIN_TODO_CALL": "P1",
}

ATTENTION_PRIORITY = {
    "TOKEN_VAZIO_SOURCE": "EVIDENCE",
    "TODO_COMMENT": "P2",
    "FIXME_COMMENT": "P2",
    "STUB_TEXT": "P2",
    "PLACEHOLDER_TEXT": "P2",
}


def owner_for(path: str) -> str:
    parts = Path(path).parts
    # Ownership is deliberately structural, not a claim about a human owner.
    if "java" in parts:
        i = parts.index("java")
        tail = parts[i + 1 :]
        if tail:
            return "JAVA/" + "/".join(tail[: min(4, len(tail) - 1 or 1)])
    if "kotlin" in parts:
        i = parts.index("kotlin")
        tail = parts[i + 1 :]
        if tail:
            return "KOTLIN/" + "/".join(tail[: min(4, len(tail) - 1 or 1)])
    if "cpp" in parts or "jni" in parts:
        return "NATIVE"
    if path.endswith("AndroidManifest.xml"):
        return "ANDROID_MANIFEST"
    return "APP_SOURCE"


def _groups(findings: list[source_gap.Finding], priority_map: dict[str, str], default_priority: str) -> list[dict]:
    grouped: dict[tuple[str, str, str], list[source_gap.Finding]] = defaultdict(list)
    for finding in findings:
        grouped[(priority_map.get(finding.marker, default_priority), owner_for(finding.path), finding.marker)].append(finding)

    entries = []
    for (priority, owner, marker), items in sorted(grouped.items()):
        paths = sorted({x.path for x in items})
        material = "\n".join(
            f"{x.path}:{x.line}:{x.line_sha256}"
            for x in sorted(items, key=lambda x: (x.path, x.line, x.marker))
        )
        entries.append({
            "priority": priority,
            "structural_owner": owner,
            "marker": marker,
            "count": len(items),
            "paths": paths,
            "finding_set_sha256": hashlib.sha256(material.encode()).hexdigest(),
            "claim_allowed": False,
            "falsifier": "rerun scanner after bounded correction and compare this exact finding set without promoting unsupported runtime claims",
            "closure_gate": "bounded path-level correction or domain evidence receipt + scanner/ledger before-after evidence",
        })
    return entries


def main() -> int:
    findings, file_hashes = source_gap.scan(source_gap.DEFAULT_SOURCE_ROOT, source_gap.DEFAULT_ALLOWLIST)
    unallowed = [f for f in findings if not f.allowlisted]
    blockers = [f for f in unallowed if f.severity == "BLOCKER"]
    warnings = [f for f in unallowed if f.severity == "WARNING"]

    blocker_groups = _groups(blockers, BLOCKER_PRIORITY, "P1")
    attention_groups = _groups(warnings, ATTENTION_PRIORITY, "P2")

    receipt = {
        "schema": SCHEMA,
        "observed_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source_tree_sha256": source_gap.source_tree_digest(file_hashes),
        "source_files": len(file_hashes),
        "blockers": len(blockers),
        "warnings": len(warnings),
        "source_gate_clear": len(blockers) == 0,
        "marker_counts": dict(sorted(Counter(x.marker for x in blockers).items())),
        "priority_counts": dict(sorted(Counter(BLOCKER_PRIORITY.get(x.marker, "P1") for x in blockers).items())),
        "attention_marker_counts": dict(sorted(Counter(x.marker for x in warnings).items())),
        "attention_priority_counts": dict(sorted(Counter(ATTENTION_PRIORITY.get(x.marker, "P2") for x in warnings).items())),
        "groups": blocker_groups,
        "attention_groups": attention_groups,
        "invariants": [
            "aggregate_count != root_cause",
            "triage_priority != claim_authority",
            "structural_owner != human_owner",
            "TOKEN_VAZIO != 0",
            "TOKEN_VAZIO state support != missing implementation",
            "zero executable blockers != zero uncertainty",
            "source gate clear != runtime/device/evidence claim",
        ],
        "claim_allowed": False,
    }

    OUT.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(receipt, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    OUT.write_text(payload, encoding="utf-8")
    digest = hashlib.sha256(payload.encode()).hexdigest()
    OUT.with_suffix(OUT.suffix + ".sha256").write_text(f"{digest}  {OUT.name}\n", encoding="utf-8")

    print(
        f"TRIAGE files={len(file_hashes)} blockers={len(blockers)} warnings={len(warnings)} "
        f"blocker_groups={len(blocker_groups)} attention_groups={len(attention_groups)} "
        f"source_gate_clear={receipt['source_gate_clear']} tree_sha256={receipt['source_tree_sha256']}"
    )
    print(f"triage_receipt={OUT}")
    print(f"triage_receipt_sha256={digest}")

    # Triage succeeds when it faithfully classifies the current state, including
    # the valid converged state of zero executable blockers. Claim authority
    # remains separate and fail-closed.
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
