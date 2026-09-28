#!/usr/bin/env python3
from __future__ import annotations
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SCHEMA=ROOT/"contracts/knowledge-campus-logistics-v1.schema.json"
CAMPUS=ROOT/"configs/knowledge-campus/campus.v1.json"
KINDS={"CAMPUS","GYMNASIUM","BUILDING","STREET","SHELF","BOOK","SESSION","CHAPTER","CHUNK","COMPONENT","PROPERTY","FUNCTION","FORMULA","PARABLE","TOKEN","GAP","EVIDENCE"}
STATES={"MATERIALIZED","REFERENCED","IMPLEMENTED_UNTESTED","NOT_RUN","PENDING","AUDIT","TOKEN_VAZIO"}
RELATIONS={"CONTAINS","PART_OF","DEPENDS_ON","IMPLEMENTS","DERIVES_FROM","CORRELATES_WITH","ANALOGOUS_TO","CONTRADICTS","SUPERSEDES","EVIDENCED_BY","BLOCKED_BY","NEXT","PREVIOUS"}

def validate(value:dict)->list[str]:
    errors=[]
    required={"schema","object_id","kind","source_ref","materialization_state","characteristics","relations","evidence_refs","gaps","routes"}
    missing=sorted(required-set(value))
    if missing: errors.append("missing:"+",".join(missing))
    if value.get("schema")!="rafgittools.knowledge-campus-logistics.v1": errors.append("schema")
    if value.get("kind") not in KINDS: errors.append("kind")
    if value.get("materialization_state") not in STATES: errors.append("materialization_state")
    for rel in value.get("relations",[]):
        if rel.get("type") not in RELATIONS: errors.append("relation_type")
        if not rel.get("target"): errors.append("relation_target")
    if value.get("kind")=="PARABLE" and any(r.get("type")=="EVIDENCED_BY" for r in value.get("relations",[])):
        errors.append("parable_cannot_self_promote_as_evidence")
    return errors

def main()->int:
    json.loads(SCHEMA.read_text(encoding="utf-8"))
    value=json.loads(CAMPUS.read_text(encoding="utf-8"))
    errors=validate(value)
    if errors:
        print("FAIL knowledge-campus-v1 "+",".join(errors)); return 1
    print("PASS knowledge-campus-v1"); return 0

if __name__=="__main__":
    raise SystemExit(main())
