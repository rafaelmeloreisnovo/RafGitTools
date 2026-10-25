#!/usr/bin/env python3
"""S16 v2 append-only evidence validator. HOSTED governance tool, never L0."""
import json
from pathlib import Path
import re
import sys
EXPECTED_STATES=(
 "PASS_SCOPED_SOURCE_IDENTICAL","PASS_SCOPED_RELOCATABLE_ABI",
 "PASS_SCOPED_ANDROID_JNI_BUILD","PASS_SCOPED_DEBUG_APK_STATIC",
 "TOKEN_VAZIO_DEVICE_NOT_RUN","TOKEN_VAZIO_RESILIENCE_NOT_RUN",
 "TOKEN_VAZIO_PROVIDER_HUMAN_NOT_ENFORCED")
EXPECTED_ABI=("arm64-v8a","armeabi-v7a","universal")
HEX64=re.compile(r"^[0-9a-f]{64}$")
def valid(v):
 if not isinstance(v,dict) or v.get("schema")!="rafaelia.s16.seven_gate_closure.v2":return "WRONG_SCHEMA"
 if v.get("claim_allowed") is not False or v.get("classification",{}).get("claim_allowed") is not False:
  return "FALSE_PROMOTION"
 g=v.get("gates")
 if not isinstance(g,list) or len(g)!=7 or [(x.get("id"),x.get("status")) for x in g]!=[(f"G{i+1}",s) for i,s in enumerate(EXPECTED_STATES)]:
  return "GATE_STATE_DRIFT"
 if any(not x.get("still_open") for x in g):return "UNTRACKED_BLOCKER"
 c=v.get("classification",{})
 for k in ("real_apt_pkg_proot","apk_runtime","core_executed","device","physical_rollback","license_certified"):
  if c.get(k) is not False:return "UNVERIFIED_EXECUTION_OR_RIGHTS"
 if c.get("bootstrap")!="BRIDGE_ONLY_DIAGNOSTIC_NOT_REAL_PKG" or c.get("apk_signed")!="DEBUG_SIGNATURE_ONLY":
  return "MISLABELED_DIAGNOSTIC_ARTIFACT"
 p=v.get("provenance",{})
 if p.get("android_commit")!="9607b3fe03077584c47e43f473faa42688c58aec" or p.get("android_workflow_run")!=40146110344:
  return "INVALID_EXACT_HEAD_BINDING"
 if p.get("git_artifact_id")!=12581206607 or p.get("drive_readback_sha256_equal") is not True:
  return "CUSTODY_UNATTESTED"
 for k in ("apk_receipt_sha256","bootstrap_receipt_sha256","bundle_sha256"):
  if not isinstance(p.get(k),str) or not HEX64.fullmatch(p[k]):return "MISSING_CRYPTODIGEST"
 a=v.get("apk_artifacts",[])
 if not isinstance(a,list) or tuple(x.get("abi") for x in a)!=EXPECTED_ABI:return "APK_ABI_MISSING"
 if any(not HEX64.fullmatch(x.get("sha256","")) or not isinstance(x.get("size_bytes"),int) or x["size_bytes"]<1000 for x in a):
  return "BAD_APK_RECEIPT"
 if len(v.get("bootstraps",[]))!=4 or any(not HEX64.fullmatch(x.get("sha256","")) for x in v["bootstraps"]):
  return "BOOTSTRAP_CUSTODY_LOST"
 if v.get("rollback")!="RAFAELIA_S15_MIDDLEWARE=0":return "ROLLBACK_NOT_REVERSIBLE"
 if set(v.get("risk_policy",[])) != {"NO_MERGE","NO_INSTALL","NO_APK_PUBLISHED","NO_DELETE","NO_FALSE_AUTHORITY","HUMAN_REVIEW_REQUIRED"}:
  return "UNAUTHORIZED_RISK_POLICY_CHANGE"
 return "PASS_SCOPED_G4_STATIC_EVIDENCE_LEDGER"
if __name__=="__main__":
 try:obj=json.loads(Path(sys.argv[1]).read_text(encoding="utf8"))
 except (OSError,ValueError,IndexError,TypeError):obj=None
 state=valid(obj)
 print(state)
 raise SystemExit(0 if state=="PASS_SCOPED_G4_STATIC_EVIDENCE_LEDGER" else 2)
