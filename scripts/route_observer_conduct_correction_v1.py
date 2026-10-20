#!/usr/bin/env python3
from __future__ import annotations
import argparse,json,re
from pathlib import Path
SHA40=re.compile(r"^[0-9a-f]{40}$")
ROOT=Path(__file__).resolve().parents[1]
CONTRACT=ROOT/"contracts/observer_conduct_correction_router.v1.json"
def load(p): return json.loads(Path(p).read_text(encoding="utf-8"))

def decide(intake, request, c):
    errors=[]; gaps=[]
    if intake.get("schema")!=c["accepted_intake_schema"]: errors.append("unsupported_intake_schema")
    if intake.get("claim_allowed") is not False: errors.append("intake_self_promotion")
    conducts=set(intake.get("conduct",[]))
    state=intake.get("epistemic_state")

    if conducts & set(c["refusal_conduct"]):
        return {"state":"OBSERVATION_REFUSED_PRESERVED","errors":[],"open_requirements":[],"allowed_actions":["PRESERVE_RECEIPT","REVIEW"],"forbidden_actions":c["forbidden_actions"],"claim_allowed":False,"merge_allowed":False}

    if state in set(c["epistemic_hold_states"]):
        return {"state":"REVIEW_ONLY_HOLD","errors":[],"open_requirements":["epistemic_resolution_or_program_reproducer"],"allowed_actions":["CLASSIFY","REVIEW","DRY_RUN"],"forbidden_actions":c["forbidden_actions"],"claim_allowed":False,"merge_allowed":False}

    correction_candidate=bool(conducts & set(c["correction_candidate_conduct"]))
    if not correction_candidate:
        return {"state":"OBSERVATION_ONLY_NO_MUTATION","errors":[],"open_requirements":[],"allowed_actions":["CLASSIFY","REVIEW","EMIT_RECEIPT"],"forbidden_actions":c["forbidden_actions"],"claim_allowed":False,"merge_allowed":False}

    for f in c["correction_envelope_required"]:
        if request.get(f) in (None,"",[],{}): gaps.append(f)
    if gaps:
        return {"state":"HOTFIX_HOLD_TARGET_REQUIRED","errors":[],"open_requirements":gaps,"allowed_actions":["CLASSIFY","DRY_RUN","EMIT_RECEIPT"],"forbidden_actions":c["forbidden_actions"],"claim_allowed":False,"merge_allowed":False}

    rev=str(request.get("exact_source_revision",""))
    if not SHA40.fullmatch(rev): errors.append("exact_source_revision")
    target=request.get("explicit_target")
    if not isinstance(target,dict) or not target.get("repository") or not target.get("path"): errors.append("explicit_target")
    patch=request.get("bounded_patch")
    if not isinstance(patch,dict): errors.append("bounded_patch")
    else:
        for key,msg in [
          ("direct_main_write","DIRECT_MAIN_WRITE"),
          ("force_push","FORCE_PUSH"),
          ("remote_delete","REMOTE_DELETE"),
          ("auto_merge","AUTO_MERGE"),
          ("auto_claim_promotion","AUTO_CLAIM_PROMOTION")
        ]:
            if patch.get(key): errors.append(msg)
    if errors:
        st="BLOCKED"; actions=["CLASSIFY","DRY_RUN","EMIT_RECEIPT"]
    else:
        st="HOTFIX_ELIGIBLE_BOUNDED"; actions=["DRY_RUN","WORK_BRANCH_PATCH","RUN_FALSIFIER","RUN_REGRESSION_GATES","EMIT_APPEND_ONLY_RECEIPT","ROLLBACK_IF_REQUIRED"]
    return {"state":st,"errors":errors,"open_requirements":[],"allowed_actions":actions,"forbidden_actions":c["forbidden_actions"],"claim_allowed":False,"merge_allowed":False}

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--intake",type=Path,required=True)
    ap.add_argument("--request",type=Path)
    ap.add_argument("--contract",type=Path,default=CONTRACT)
    a=ap.parse_args()
    out=decide(load(a.intake),load(a.request) if a.request else {},load(a.contract))
    print(json.dumps(out,indent=2,sort_keys=True))
    return 0 if out["state"]!="BLOCKED" else 1
if __name__=="__main__": raise SystemExit(main())
