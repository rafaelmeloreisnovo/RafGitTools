#!/usr/bin/env python3
import json,sys
from pathlib import Path

def fail(m): raise SystemExit(m)

def validate(path):
    o=json.loads(Path(path).read_text(encoding="utf-8"))
    req={"schema","version","authority","federated_authority","upstream","local_scope","preflight_required","local_routes","invariants","claim_allowed"}
    miss=req-set(o)
    if miss: fail(f"missing {sorted(miss)}")
    if o["authority"]!="rafaelmeloreisnovo/RafGitTools": fail("local authority mismatch")
    if o["federated_authority"]!="rafaelmeloreisnovo/Mapa": fail("federated authority mismatch")
    if set(o["preflight_required"])!={"SOURCE","AUTHORITY","EXECUTION_TARGET","EVIDENCE_RULE"}: fail("preflight mismatch")
    if o["claim_allowed"] is not False: fail("claim_allowed must remain false")
    if o["local_scope"].get("private_body_policy")!="REFERENCE_ONLY": fail("private body boundary weakened")
    if o["local_scope"].get("secret_policy")!="NO_RAW_SECRET_MATERIAL": fail("secret boundary weakened")
    up=o["upstream"]
    if not up.get("expected_head") or len(up["expected_head"])!=40: fail("upstream head must be exact 40-char commit")
    owned=set(o["local_scope"].get("owned_workstreams",[]))
    if owned!={"WS01","WS03","WS10"}: fail("owned workstreams drift")
    return {"status":"PASS","owned_workstreams":sorted(owned),"upstream_head":up["expected_head"],"claim_allowed":False}

if __name__=="__main__":
    if len(sys.argv)!=2: fail("usage: validate_session_full_dispatch_adapter.py CONFIG")
    print(json.dumps(validate(sys.argv[1]),sort_keys=True))
