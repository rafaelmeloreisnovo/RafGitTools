#!/usr/bin/env python3
"""Validate the RAFAELIA workflow master index without remote inference."""
from __future__ import annotations
import argparse, hashlib, json, sys
from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any

REQUIRED_NODE_TYPES={"REPOSITORY","CLAIM","TEST","DATASET","ARTIFACT","METHOD","SOFTWARE","DEVICE","ABI","ENVIRONMENT","PARAMETER_SET"}
REQUIRED_STATE_AXES={"source_status","epistemic_status","operational_status","claim_gate"}
REQUIRED_RELATION_FIELDS={"producer","consumer","schema_version","compatibility","authentication","privacy_class","timeout_seconds","retry_policy","idempotency","failure_behavior","evidence_required"}
SOURCE_STATES={"VERIFIED","VERIFIED_LIMITED","DECLARED_BY_AUTHOR","TOKEN_VAZIO","CONTRADICTION"}
OPERATIONAL_STATES={"VERIFIED","TESTED","PARTIAL","BLOCKED","TOKEN_VAZIO","CONTRADICTION"}
CLAIM_GATES={"BLOCKED","ALLOWED_STRUCTURAL","ALLOWED"}
RUNTIME_STATES={"VERIFIED","TESTED","PARTIAL","BLOCKED","TOKEN_VAZIO","CONTRADICTION"}
EMPTY_STATES={"TOKEN_VAZIO","BLOCKED","PARTIAL"}

def load(path:Path)->dict[str,Any]:
    data=json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data,dict): raise ValueError("root must be an object")
    return data

def parse_time(value:Any,label:str,errors:list[str])->datetime|None:
    if value is None:return None
    if not isinstance(value,str) or not value.strip():
        errors.append(f"{label}: timestamp must be ISO-8601 string or null"); return None
    try:return datetime.fromisoformat(value.replace("Z","+00:00"))
    except ValueError: errors.append(f"{label}: invalid ISO-8601 timestamp {value}"); return None

def semantic_digest(data:dict[str,Any])->str:
    view=dict(data)
    view["nodes"]=sorted(data.get("nodes",[]),key=lambda x:x.get("id",""))
    view["relations"]=sorted(data.get("relations",[]),key=lambda x:x.get("id",""))
    payload=json.dumps(view,ensure_ascii=False,sort_keys=True,separators=(",",":")).encode()
    return hashlib.sha256(payload).hexdigest()

def merge_section(defaults:dict[str,Any], node:dict[str,Any], key:str)->dict[str,Any]:
    base=dict(defaults.get(key,{})); base.update(node.get(key,{})); return base

def expand_node(data:dict[str,Any], node:dict[str,Any])->dict[str,Any]:
    out=dict(node); defaults=data.get("defaults",{})
    for key in ("states","temporal","runtime_evidence","supply_chain","recovery"):
        out[key]=merge_section(defaults,node,key)
    return out

def validate(data:dict[str,Any])->list[str]:
    errors=[]
    if data.get("schema_version")!="2.0.0": errors.append("schema_version must be 2.0.0")
    if data.get("claim_allowed") is not False: errors.append("claim_allowed must remain false")
    ontology=data.get("ontology",{})
    if not REQUIRED_NODE_TYPES.issubset(set(ontology.get("node_types",[]))): errors.append("ontology missing required node types")
    if set(ontology.get("state_axes",[]))!=REQUIRED_STATE_AXES: errors.append("ontology state_axes mismatch")
    if set(ontology.get("relation_fields",[]))!=REQUIRED_RELATION_FIELDS: errors.append("ontology relation_fields mismatch")
    policies=data.get("policies",{})
    for key in ("token_vazio_is_valid","negative_results_are_assets","documentation_is_not_runtime_evidence","hash_is_not_scientific_validation","temporal_inference_is_forbidden","blocked_or_empty_requires_owner_and_exit_criteria","rollback_must_be_drilled","dependencies_must_be_pinned"):
        if policies.get(key) is not True: errors.append(f"policy {key} must remain true")
    if policies.get("automatic_merge") is not False: errors.append("automatic_merge must remain false")
    nodes=data.get("nodes")
    if not isinstance(nodes,list) or not nodes:return errors+["nodes must be a non-empty list"]
    ids=set(); repos=set()
    for pos,raw_node in enumerate(nodes):
        node=expand_node(data,raw_node) if isinstance(raw_node,dict) else raw_node
        label=node.get("id",f"nodes[{pos}]") if isinstance(node,dict) else f"nodes[{pos}]"
        if not isinstance(node,dict):errors.append(f"{label}: must be object");continue
        required={"id","repository","layer","canonical_source","owner","critical","dependencies","states","temporal","runtime_evidence","supply_chain","recovery","next_action","exit_criteria"}
        missing=required-set(node)
        if missing:errors.append(f"{label}: missing {sorted(missing)}");continue
        if node["id"] in ids:errors.append(f"duplicate node id {node['id']}")
        ids.add(node["id"])
        if node["repository"] in repos:errors.append(f"duplicate repository {node['repository']}")
        repos.add(node["repository"])
        for key in ("repository","layer","canonical_source","owner","next_action","exit_criteria"):
            if not isinstance(node[key],str) or not node[key].strip():errors.append(f"{label}: {key} must be non-empty")
        if node["canonical_source"]=="TOKEN_VAZIO":errors.append(f"{label}: canonical_source cannot be TOKEN_VAZIO")
        if not isinstance(node["critical"],bool):errors.append(f"{label}: critical must be boolean")
        if not isinstance(node["dependencies"],list):errors.append(f"{label}: dependencies must be list")
        states=node["states"]
        if set(states)!=REQUIRED_STATE_AXES:errors.append(f"{label}: state axes incomplete")
        else:
            if states["source_status"] not in SOURCE_STATES:errors.append(f"{label}: invalid source_status")
            if states["operational_status"] not in OPERATIONAL_STATES:errors.append(f"{label}: invalid operational_status")
            if states["claim_gate"] not in CLAIM_GATES:errors.append(f"{label}: invalid claim_gate")
        temporal=node["temporal"]
        for key in ("observed_at","valid_from","valid_until","superseded_at","event_sequence"):
            if key not in temporal:errors.append(f"{label}: temporal missing {key}")
        observed=parse_time(temporal.get("observed_at"),f"{label}.observed_at",errors)
        valid_from=parse_time(temporal.get("valid_from"),f"{label}.valid_from",errors)
        valid_until=parse_time(temporal.get("valid_until"),f"{label}.valid_until",errors)
        superseded=parse_time(temporal.get("superseded_at"),f"{label}.superseded_at",errors)
        if observed and valid_from and observed<valid_from:errors.append(f"{label}: observed_at precedes valid_from")
        if valid_until and valid_from and valid_until<valid_from:errors.append(f"{label}: valid_until precedes valid_from")
        if superseded and valid_from and superseded<valid_from:errors.append(f"{label}: superseded_at precedes valid_from")
        if not isinstance(temporal.get("event_sequence"),int) or temporal.get("event_sequence",0)<1:errors.append(f"{label}: event_sequence must be positive integer")
        runtime=node["runtime_evidence"]
        if runtime.get("status") not in RUNTIME_STATES:errors.append(f"{label}: invalid runtime status")
        for key in ("artifact","device","abi","environment"):
            if key not in runtime:errors.append(f"{label}: runtime_evidence missing {key}")
        if states.get("operational_status") in {"VERIFIED","TESTED"} and runtime.get("artifact")=="TOKEN_VAZIO":errors.append(f"{label}: tested/verified operation requires runtime artifact")
        if states.get("claim_gate")=="ALLOWED" and runtime.get("status") not in {"VERIFIED","TESTED"}:errors.append(f"{label}: ALLOWED claim requires tested runtime evidence")
        recovery=node["recovery"]
        for key in ("safe_state","rollback","drill_command","last_drill_status","last_drill_artifact"):
            if key not in recovery or not isinstance(recovery[key],str) or not recovery[key].strip():errors.append(f"{label}: recovery.{key} required")
        supply=node["supply_chain"]
        for key in ("source_ref","dependency_lock","sbom","attestation"):
            if key not in supply:errors.append(f"{label}: supply_chain missing {key}")
        if states.get("operational_status") in EMPTY_STATES or runtime.get("status") in EMPTY_STATES:
            if not node["owner"].strip() or not node["next_action"].strip() or not node["exit_criteria"].strip():errors.append(f"{label}: incomplete owner/action/exit contract")
    for raw_node in nodes:
        node=expand_node(data,raw_node) if isinstance(raw_node,dict) else raw_node
        if isinstance(node,dict):
            for dep in node.get("dependencies",[]):
                if dep not in ids:errors.append(f"{node.get('id')}: unknown dependency {dep}")
    if data.get("control_plane") not in ids:errors.append("control_plane must reference a node id")
    rels=data.get("relations")
    if not isinstance(rels,list):errors.append("relations must be list")
    else:
        rel_ids=set()
        for pos,rel in enumerate(rels):
            label=rel.get("id",f"relations[{pos}]") if isinstance(rel,dict) else f"relations[{pos}]"
            if not isinstance(rel,dict):errors.append(f"{label}: must be object");continue
            required={"id"}|REQUIRED_RELATION_FIELDS
            missing=required-set(rel)
            if missing:errors.append(f"{label}: missing {sorted(missing)}");continue
            if rel["id"] in rel_ids:errors.append(f"duplicate relation id {rel['id']}")
            rel_ids.add(rel["id"])
            if rel["producer"] not in ids or rel["consumer"] not in ids:errors.append(f"{label}: unknown producer or consumer")
            if not isinstance(rel["timeout_seconds"],int) or rel["timeout_seconds"]<=0:errors.append(f"{label}: timeout_seconds must be positive")
            if rel["idempotency"] not in {"required","not-required"}:errors.append(f"{label}: invalid idempotency")
            for key in REQUIRED_RELATION_FIELDS-{"timeout_seconds"}:
                if not isinstance(rel[key],str) or not rel[key].strip():errors.append(f"{label}: {key} must be non-empty")
    return errors

def report(data:dict[str,Any],errors:list[str])->dict[str,Any]:
    expanded=[expand_node(data,n) for n in data.get("nodes",[]) if isinstance(n,dict)]
    op=Counter(n["states"]["operational_status"] for n in expanded)
    runtime=Counter(n["runtime_evidence"]["status"] for n in expanded)
    unresolved=[n["id"] for n in expanded if n["states"]["operational_status"] in EMPTY_STATES or n["runtime_evidence"]["status"] in EMPTY_STATES]
    return {"schema":"rafaelia.workflow-master-index.report.v2","status":"PASS" if not errors else "FAIL","claim_allowed":False,"semantic_digest":semantic_digest(data) if not errors else None,"nodes":len(data.get("nodes",[])),"relations":len(data.get("relations",[])),"operational_states":dict(op),"runtime_states":dict(runtime),"unresolved_nodes":unresolved,"validation_errors":errors,"boundary":"Structural validation does not prove remote runtime or scientific truth."}

def main()->int:
    p=argparse.ArgumentParser();p.add_argument("--index",type=Path,required=True);p.add_argument("--report",type=Path);a=p.parse_args()
    try:data=load(a.index)
    except Exception as exc:print(f"BLOCKED: {exc}",file=sys.stderr);return 2
    errors=validate(data);out=report(data,errors);encoded=json.dumps(out,indent=2,ensure_ascii=False)+"\n";print(encoded,end="")
    if a.report:a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(encoded,encoding="utf-8")
    return 1 if errors else 0
if __name__=="__main__":raise SystemExit(main())
