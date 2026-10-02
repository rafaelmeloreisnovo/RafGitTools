#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path

STATES = {
    "PASS", "FAIL", "NOT_RUN", "PENDING", "AUDIT", "TOKEN_VAZIO",
    "IMPLEMENTED_UNTESTED", "OBSERVED_UNPROMOTED", "ROUTE_STATE_BLOCKED",
}
HEX40 = re.compile(r"^[0-9a-f]{40}$")
HEX64 = re.compile(r"^[0-9a-f]{64}$")

TOP = {"schema","receipt_id","observed_at","producer","source","execution","evidence","result","gaps","lineage","receipt_sha256","claim_allowed"}
PRODUCER = {"repository","ref","commit","artifact_id"}
SOURCE = {"source_id","source_ref","sha256"}
EXECUTION = {"execution_id","environment","architecture","device_id","state"}
EVIDENCE = {"kind","ref","sha256","state","scope"}
RESULT = {"state","summary"}
GAP = {"gap_id","state","blocking","detail"}
LINEAGE = {"parent_receipt","supersedes"}

def _obj(x, name, required, allowed, errors):
    if not isinstance(x, dict):
        errors.append(f"{name}: expected object")
        return {}
    missing = sorted(required - set(x))
    extra = sorted(set(x) - allowed)
    if missing:
        errors.append(f"{name}: missing {missing}")
    if extra:
        errors.append(f"{name}: additionalProperties {extra}")
    return x

def _str(v, path, errors):
    if not isinstance(v, str) or not v:
        errors.append(f"{path}: expected non-empty string")

def _state(v, path, errors):
    if v not in STATES:
        errors.append(f"{path}: invalid state {v!r}")

def _hash(v, path, n, errors):
    if v == "TOKEN_VAZIO":
        return
    ok = HEX40.fullmatch(v or "") if n == 40 else HEX64.fullmatch(v or "")
    if not ok:
        errors.append(f"{path}: expected TOKEN_VAZIO or {n}-hex lowercase digest")

def validate_doc(doc):
    errors = []
    root = _obj(doc, "$", TOP, TOP, errors)
    if root.get("schema") != "rafaelia.receipt-envelope/v1":
        errors.append("$.schema: expected rafaelia.receipt-envelope/v1")
    for k in ("receipt_id","observed_at"):
        _str(root.get(k), f"$.{k}", errors)
    if not isinstance(root.get("claim_allowed"), bool):
        errors.append("$.claim_allowed: expected boolean")
    _hash(root.get("receipt_sha256"), "$.receipt_sha256", 64, errors)

    p = _obj(root.get("producer"), "$.producer", PRODUCER, PRODUCER, errors)
    for k in ("repository","ref","artifact_id"):
        _str(p.get(k), f"$.producer.{k}", errors)
    _hash(p.get("commit"), "$.producer.commit", 40, errors)

    s = _obj(root.get("source"), "$.source", SOURCE, SOURCE, errors)
    for k in ("source_id","source_ref"):
        _str(s.get(k), f"$.source.{k}", errors)
    _hash(s.get("sha256"), "$.source.sha256", 64, errors)

    e = _obj(root.get("execution"), "$.execution", EXECUTION, EXECUTION, errors)
    for k in ("execution_id","environment","architecture","device_id"):
        _str(e.get(k), f"$.execution.{k}", errors)
    _state(e.get("state"), "$.execution.state", errors)

    evs = root.get("evidence")
    if not isinstance(evs, list):
        errors.append("$.evidence: expected array")
        evs = []
    for i, item in enumerate(evs):
        x = _obj(item, f"$.evidence[{i}]", EVIDENCE, EVIDENCE, errors)
        for k in ("kind","ref","scope"):
            _str(x.get(k), f"$.evidence[{i}].{k}", errors)
        _hash(x.get("sha256"), f"$.evidence[{i}].sha256", 64, errors)
        _state(x.get("state"), f"$.evidence[{i}].state", errors)

    res = _obj(root.get("result"), "$.result", RESULT, RESULT, errors)
    _state(res.get("state"), "$.result.state", errors)
    if not isinstance(res.get("summary"), str):
        errors.append("$.result.summary: expected string")

    gaps = root.get("gaps")
    if not isinstance(gaps, list):
        errors.append("$.gaps: expected array")
        gaps = []
    for i, item in enumerate(gaps):
        g = _obj(item, f"$.gaps[{i}]", GAP, GAP, errors)
        _str(g.get("gap_id"), f"$.gaps[{i}].gap_id", errors)
        _state(g.get("state"), f"$.gaps[{i}].state", errors)
        if not isinstance(g.get("blocking"), bool):
            errors.append(f"$.gaps[{i}].blocking: expected boolean")
        if not isinstance(g.get("detail"), str):
            errors.append(f"$.gaps[{i}].detail: expected string")

    lin = _obj(root.get("lineage"), "$.lineage", LINEAGE, LINEAGE, errors)
    for k in ("parent_receipt","supersedes"):
        _str(lin.get(k), f"$.lineage.{k}", errors)

    if root.get("claim_allowed") is True:
        if not HEX40.fullmatch(p.get("commit") or ""):
            errors.append("claim gate: producer.commit must be concrete 40-hex")
        if s.get("source_ref") == "TOKEN_VAZIO" or not HEX64.fullmatch(s.get("sha256") or ""):
            errors.append("claim gate: concrete source_ref and source.sha256 required")
        if e.get("state") != "PASS":
            errors.append("claim gate: execution.state must be PASS")
        for k in ("execution_id","environment","architecture"):
            if e.get(k) == "TOKEN_VAZIO":
                errors.append(f"claim gate: execution.{k} must be concrete")
        if not evs:
            errors.append("claim gate: at least one evidence item required")
        if any(x.get("state") != "PASS" or not HEX64.fullmatch(x.get("sha256") or "") for x in evs if isinstance(x, dict)):
            errors.append("claim gate: every evidence item must be PASS with concrete sha256")
        if res.get("state") != "PASS":
            errors.append("claim gate: result.state must be PASS")
        if any(isinstance(g, dict) and g.get("blocking") is True for g in gaps):
            errors.append("claim gate: blocking gaps must be zero")
        if not HEX64.fullmatch(root.get("receipt_sha256") or ""):
            errors.append("claim gate: receipt_sha256 must be concrete")

    return errors

def main(argv):
    if len(argv) != 2:
        print("usage: validate_receipt_envelope_v1.py RECEIPT.json", file=sys.stderr)
        return 2
    path = Path(argv[1])
    try:
        doc = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"FAIL parse: {exc}", file=sys.stderr)
        return 1
    errors = validate_doc(doc)
    if errors:
        print("FAIL")
        for err in errors:
            print(f"- {err}")
        return 1
    print("PASS structural+semantic receipt-envelope/v1")
    print("NOTE: PASS does not prove runtime/device/scientific truth.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
