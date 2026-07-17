#!/usr/bin/env python3
"""Run a real local rollback drill and bounded remote-node simulations."""
from __future__ import annotations
import argparse, hashlib, json, tempfile
from pathlib import Path

def sha(path:Path)->str:return hashlib.sha256(path.read_bytes()).hexdigest()
def load(path:Path)->dict:return json.loads(path.read_text(encoding='utf-8'))
def local_file_rollback()->dict:
    with tempfile.TemporaryDirectory(prefix='rafaelia-rollback-') as td:
        p=Path(td)/'state.txt';known=b'KNOWN_GOOD\n';p.write_bytes(known);before=sha(p)
        p.write_bytes(b'FAILURE_INJECTED\n');mutated=sha(p);p.write_bytes(known);after=sha(p)
        return {'test':'local-file-restore','before_sha256':before,'mutated_sha256':mutated,'after_sha256':after,'pass':before==after and before!=mutated}
def expand(index:dict,node:dict)->dict:
    out=dict(node); defaults=index.get('defaults',{})
    for key in ('states','temporal','runtime_evidence','supply_chain','recovery'):
        merged=dict(defaults.get(key,{}));merged.update(node.get(key,{}));out[key]=merged
    return out
def simulate(index:dict,node_id:str)->dict:
    expanded=[expand(index,n) for n in index['nodes']];by={n['id']:n for n in expanded};node=by.get(node_id)
    if not node:return {'node':node_id,'status':'BLOCKED','reason':'node not found'}
    dependents=sorted(n['id'] for n in expanded if node_id in n['dependencies'])
    return {'node':node_id,'status':'SAFE_STATE_REACHED','safe_state':node['recovery']['safe_state'],'rollback_plan':node['recovery']['rollback'],'dependents_isolated':dependents,'remote_runtime_recovery':'TOKEN_VAZIO','claim_policy':'failure cannot promote claims'}
def main()->int:
    p=argparse.ArgumentParser();p.add_argument('--index',type=Path,required=True);p.add_argument('--node',required=True);p.add_argument('--report',type=Path);a=p.parse_args()
    index=load(a.index);local=local_file_rollback();simulation=simulate(index,a.node)
    out={'schema':'rafaelia.recovery-drill.v1','status':'PASS' if local['pass'] and simulation['status']=='SAFE_STATE_REACHED' else 'FAIL','local_control_plane_rollback':local,'node_simulation':simulation,'boundary':'Only local file restoration is executed. Remote device, VM and model recovery remain TOKEN_VAZIO until repository-local drills run.'}
    text=json.dumps(out,indent=2,ensure_ascii=False)+'\n';print(text,end='')
    if a.report:a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(text,encoding='utf-8')
    return 0 if out['status']=='PASS' else 1
if __name__=='__main__':raise SystemExit(main())
