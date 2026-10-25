#!/usr/bin/env python3
"""S16 seven-gate hosted validator; never production L0 or provider enforcement."""
import json
from pathlib import Path
import sys
EXPECTED=("G1","G2","G3","G4","G5","G6","G7")
def validate(d):
    if not isinstance(d,dict) or d.get("schema")!="rafaelia.s16.seven_gate_closure.v1":
        return "INVALID_SCHEMA"
    if d.get("claim_allowed") is not False or d.get("human_review")!="TOKEN_VAZIO_NOT_COMPLETED":
        return "FALSE_GLOBAL_PROMOTION"
    g=d.get("gates")
    if not isinstance(g,list) or tuple(x.get("id") for x in g)!=EXPECTED:
        return "INCOMPLETE_GATES"
    if any(not x.get("blockers") for x in g):
        return "SILENTLY_CLOSED_GAPS"
    if any(x.get("state","").startswith("PASS") and not x.get("evidence") for x in g):
        return "PASS_WITHOUT_EVIDENCE"
    if any(not x.get("state") or not isinstance(x.get("evidence"),list) for x in g):
        return "UNCLEAR_GATE_STATE"
    if g[3].get("state")!="PASS_SCOPED_PRIOR_RUN_ENHANCED_PENDING":
        return "FALSE_APK_GATE_COMPLETION"
    if "REAL_SOURCE_BUILT_BOOTSTRAP" not in g[3]["blockers"]:
        return "FALSE_PKG_CLAIM"
    if g[4]["state"]!="TOKEN_VAZIO_NOT_RUN" or g[5]["state"]!="TOKEN_VAZIO_NOT_RUN":
        return "UNVERIFIED_HARDWARE_CLAIM"
    if g[6]["state"]!="TOKEN_VAZIO_NOT_ENFORCED":
        return "FALSE_PROVIDER_ENFORCEMENT"
    if d.get("reversal",{}).get("runtime")!="RAFAELIA_S15_MIDDLEWARE=0":
        return "ROLLBACK_MISSING"
    if d.get("source",{}).get("producer_commit")!="68195995718b9f0653ca4ce4575d375ce60ac756":
        return "SOURCE_SHA_DRIFT"
    if not {"NO_DELETE","NO_AUTO_MERGE","NO_CREDENTIAL_UPLOAD","TOKEN_VAZIO_NOT_ZERO"}<=set(d.get("nonnegotiables",[])):
        return "SCOPE_OR_SECURITY_LOST"
    return "PASS_SCOPED_SEVEN_GATE_LEDGER_ONLY"
if __name__=="__main__":
    try: d=json.loads(Path(sys.argv[1]).read_text(encoding="utf8"))
    except (IndexError,OSError,ValueError,TypeError): d=None
    state=validate(d)
    print(state)
    sys.exit(0 if state=="PASS_SCOPED_SEVEN_GATE_LEDGER_ONLY" else 2)
