#!/usr/bin/env python3
"""Route a consolidated ChatGPT evidence bundle into a bounded hotfix decision."""
from __future__ import annotations
import argparse, json, re
from pathlib import Path
SHA40=re.compile(r"^[0-9a-f]{40}$")

def load(p): return json.loads(Path(p).read_text(encoding="utf-8"))

def decide(bundle, request, contract):
    errors=[]; gaps=[]
    if bundle.get("schema")!=contract["accepted_bundle_schema"]: errors.append("unsupported bundle schema")
    if bundle.get("state")!=contract["required_bundle_state"]: errors.append("unexpected bundle state")
    if bundle.get("training_use") is not False: errors.append("training_use must be false")
    if bundle.get("dataset_use") is not False: errors.append("dataset_use must be false")
    if bundle.get("operational_evidence_use") is not True: errors.append("operational_evidence_use must be true")
    if bundle.get("claim_allowed") is not False or bundle.get("promotion_allowed") is not False:
        errors.append("bundle must remain non-promoting")
    urg={u.get("id"):u for u in bundle.get("urgency",[])}
    for uid in contract["required_p0_urgencies"]:
        if uid not in urg or urg[uid].get("level")!="P0" or not urg[uid].get("provenance"):
            errors.append(f"missing P0 urgency/provenance: {uid}")
    bg=set(bundle.get("gates",[]))
    for g in contract["required_bundle_gates"]:
        if g not in bg: errors.append(f"missing bundle gate: {g}")
    for tv in bundle.get("token_vazios",[]):
        if not tv.get("type") or not tv.get("closure"):
            errors.append("untyped TOKEN_VAZIO")
    if not request:
        gaps=contract["correction_envelope_required"][:]
    else:
        for f in contract["correction_envelope_required"]:
            if request.get(f) in (None,"",[],{}): gaps.append(f)
        rev=str(request.get("exact_source_revision",""))
        if rev and not SHA40.fullmatch(rev): errors.append("exact_source_revision must be 40-hex")
        target=request.get("explicit_target")
        if target and (not isinstance(target,dict) or not target.get("repository") or not target.get("path")):
            errors.append("explicit_target must bind repository and path")
        patch=request.get("bounded_patch")
        if isinstance(patch,dict):
            if patch.get("direct_main_write"): errors.append("DIRECT_MAIN_WRITE forbidden")
            if patch.get("force_push"): errors.append("FORCE_PUSH forbidden")
            if patch.get("remote_delete"): errors.append("REMOTE_DELETE forbidden")
            if patch.get("auto_merge"): errors.append("AUTO_MERGE forbidden")
            if patch.get("auto_claim_promotion"): errors.append("AUTO_CLAIM_PROMOTION forbidden")
        elif patch not in (None,""): errors.append("bounded_patch must be object")
    if errors:
        state="BLOCKED"
        actions=contract["action_lattice"]["evidence_only"]
    elif gaps:
        state="HOTFIX_HOLD_TARGET_REQUIRED"
        actions=contract["action_lattice"]["evidence_only"]
    else:
        state="HOTFIX_ELIGIBLE_BOUNDED"
        actions=contract["action_lattice"]["eligible"]
    return {
      "schema":"rafgittools.chatgpt-interaction-bundle-correction-decision/v2",
      "state":state,
      "errors":errors,
      "open_requirements":gaps,
      "p0_urgencies":contract["required_p0_urgencies"],
      "allowed_actions":actions,
      "forbidden_actions":contract["action_lattice"]["forbidden"],
      "claim_allowed":False,
      "merge_allowed":False
    }

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--bundle",type=Path,required=True)
    ap.add_argument("--request",type=Path)
    ap.add_argument("--contract",type=Path,default=Path(__file__).resolve().parents[1]/"contracts/chatgpt_interaction_bundle_correction_gate.v2.json")
    a=ap.parse_args()
    out=decide(load(a.bundle),load(a.request) if a.request else {},load(a.contract))
    print(json.dumps(out,indent=2,sort_keys=True))
    return 0 if out["state"]!="BLOCKED" else 1
if __name__=="__main__": raise SystemExit(main())
