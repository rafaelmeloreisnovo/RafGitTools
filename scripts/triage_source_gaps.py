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

PRIORITY = {
    "NOT_IMPLEMENTED_ERROR": "P0",
    "UNSUPPORTED_NOT_IMPLEMENTED": "P0",
    "ERROR_NOT_IMPLEMENTED": "P0",
    "KOTLIN_TODO_CALL": "P1",
    "TOKEN_VAZIO_SOURCE": "P1",
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


def main() -> int:
    findings, file_hashes = source_gap.scan(source_gap.DEFAULT_SOURCE_ROOT, source_gap.DEFAULT_ALLOWLIST)
    unallowed = [f for f in findings if not f.allowlisted]
    blockers = [f for f in unallowed if f.severity == "BLOCKER"]
    warnings = [f for f in unallowed if f.severity == "WARNING"]

    groups: dict[tuple[str, str, str], list[source_gap.Finding]] = defaultdict(list)
    for f in blockers:
        groups[(PRIORITY.get(f.marker, "P1"), owner_for(f.path), f.marker)].append(f)

    entries = []
    for (priority, owner, marker), items in sorted(groups.items()):
        paths = sorted({x.path for x in items})
        material = "\n".join(f"{x.path}:{x.line}:{x.line_sha256}" for x in sorted(items, key=lambda x:(x.path,x.line,x.marker)))
        entries.append({
            "priority": priority,
            "structural_owner": owner,
            "marker": marker,
            "count": len(items),
            "paths": paths,
            "finding_set_sha256": hashlib.sha256(material.encode()).hexdigest(),
            "claim_allowed": False,
            "falsifier": "rerun scanner after bounded correction and observe this exact finding set shrink without new P0 findings",
            "closure_gate": "bounded path-level correction + source-gap scanner before/after receipt",
        })

    receipt = {
        "schema": SCHEMA,
        "observed_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source_tree_sha256": source_gap.source_tree_digest(file_hashes),
        "source_files": len(file_hashes),
        "blockers": len(blockers),
        "warnings": len(warnings),
        "marker_counts": dict(sorted(Counter(x.marker for x in blockers).items())),
        "priority_counts": dict(sorted(Counter(PRIORITY.get(x.marker, "P1") for x in blockers).items())),
        "groups": entries,
        "invariants": [
            "aggregate_count != root_cause",
            "triage_priority != claim_authority",
            "structural_owner != human_owner",
            "TOKEN_VAZIO != 0",
        ],
        "claim_allowed": False,
    }

    OUT.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(receipt, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    OUT.write_text(payload, encoding="utf-8")
    digest = hashlib.sha256(payload.encode()).hexdigest()
    OUT.with_suffix(OUT.suffix + ".sha256").write_text(f"{digest}  {OUT.name}\n", encoding="utf-8")

    print(f"TRIAGE files={len(file_hashes)} blockers={len(blockers)} warnings={len(warnings)} groups={len(entries)} tree_sha256={receipt['source_tree_sha256']}")
    print(f"triage_receipt={OUT}")
    print(f"triage_receipt_sha256={digest}")
    # Triage is successful when it faithfully classifies the existing debt.
    # It does not close the original blocker gate.
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
