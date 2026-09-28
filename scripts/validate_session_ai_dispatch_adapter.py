#!/usr/bin/env python3
import json,sys
from pathlib import Path
def die(x): raise SystemExit('FAIL: '+x)
def validate(d):
    if d.get('schema')!='RAFGITTOOLS_SESSION_AI_DISPATCH_ADAPTER_V1': die('schema')
    if d.get('claim_allowed') is not False: die('claim_allowed')
    inv=set(d.get('invariants',[]))
    for x in ('SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM','TOKEN_VAZIO != 0','IMPLEMENTED_UNTESTED != PASS','AGENT_ASSIGNMENT != AGENT_EXECUTION','NO_DEFAULT_BRANCH_DIRECT_MUTATION','NO_PRIVATE_PAYLOAD_IN_PUBLIC_ADAPTER'):
        if x not in inv: die('missing invariant '+x)
    fr=d.get('federated_registry',{})
    for k in ('repo','branch','expected_head','path','expected_blob_sha','relation'):
        if not fr.get(k): die('federated registry '+k)
    agents=d.get('agent_bindings',[])
    if len({a['agent_id'] for a in agents})!=len(agents): die('duplicate agent')
    for p in d.get('packet_bindings',[]):
        if len(p.get('source_min',[]))>3: die(p['packet_id']+' source_min')
        for k in ('packet_id','local_route','execution_target','evidence_rule','state'):
            if not p.get(k): die('packet missing '+k)
    forb=set(d.get('problem_policy',{}).get('prohibited',[]))
    for x in ('weaken_test','secret_log','direct_default_branch_write','auto_merge','auto_release'):
        if x not in forb: die('problem policy missing '+x)
    return {'status':'PASS','agents':len(agents),'packets':len(d.get('packet_bindings',[])),'claim_allowed':False}
def main():
    if len(sys.argv)!=2: die('usage')
    d=json.loads(Path(sys.argv[1]).read_text(encoding='utf-8')); print(json.dumps(validate(d),sort_keys=True))
if __name__=='__main__': main()
