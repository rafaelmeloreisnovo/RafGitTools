#!/usr/bin/env python3
import argparse,json,pathlib,sys
P=argparse.ArgumentParser();P.add_argument("--contract",required=True);P.add_argument("--repo-root",default=".");a=P.parse_args()
c=json.loads(pathlib.Path(a.contract).read_text());errors=[]
if c.get("claim_allowed") is not False: errors.append("claim_allowed must be false")
if c.get("mode")!="READ_ONLY": errors.append("mode must be READ_ONLY")
req=c.get("required_trace_fields",[])
if len(req)!=7 or len(set(req))!=7: errors.append("seven unique trace fields required")
for t in c.get("self_test_cases",[]):
 rec=t["record"];ok=all(k in rec and rec[k] not in (None,"") for k in req) and rec.get("result_state") in c.get("allowed_result_states",[])
 got="PASS" if ok else "FAIL"
 if got!=t["expect"]:errors.append("self-test:"+t["name"]+":"+got)
root=pathlib.Path(a.repo_root)
observed={p:str((root/p).exists()).lower() for p in ["README.md","AGENTS.md","scripts",".github/workflows"]}
print(json.dumps({"state":"PASS_LIMITED" if not errors else "FAIL","errors":errors,"observed":observed,"claim_allowed":False},indent=2))
sys.exit(1 if errors else 0)
