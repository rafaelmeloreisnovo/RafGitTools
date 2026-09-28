#!/usr/bin/env python3
"""Bounded private query over a generated Knowledge Campus plan."""
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path

def token_id(value:str)->str:
    return 'TOK-'+hashlib.sha256(value.casefold().encode('utf-8')).hexdigest()[:24]

def execute(plan:dict,gymnasium=None,tier=None,token=None,state=None,kind=None,limit=20):
    if not 1 <= limit <= 1000: raise ValueError('limit must be 1..1000')
    chunks={c['chunk_id']:c for c in plan.get('chunks') or []}
    allowed=set(chunks)
    if gymnasium:
        g=next((x for x in plan.get('gymnasia') or [] if x.get('id')==gymnasium),None)
        allowed &= set((g or {}).get('chunks') or [])
    if tier: allowed={i for i in allowed if chunks[i].get('tier')==tier}
    if state: allowed={i for i in allowed if chunks[i].get('materialization_state')==state}
    if kind: allowed={i for i in allowed if any(m.get('kind')==kind for m in chunks[i].get('marks') or [])}
    if token:
        tid=token_id(token)
        t=next((x for x in plan.get('token_index') or [] if x.get('token_id')==tid),None)
        allowed &= set((t or {}).get('chunks') or [])
    result=[]
    for cid in sorted(allowed):
        c=chunks[cid]
        result.append({k:c.get(k) for k in ('chunk_id','source_family','book_id','session_id','text_sha256','bytes','tier','materialization_state','token_count','marks')})
        if len(result)>=limit: break
    return {'count':len(result),'results':result,'claim_allowed':False}

def main()->int:
    p=argparse.ArgumentParser(); p.add_argument('plan',type=Path)
    p.add_argument('--gymnasium'); p.add_argument('--tier',choices=['HOT','WARM','COLD','ARCHIVE'])
    p.add_argument('--token'); p.add_argument('--state'); p.add_argument('--kind'); p.add_argument('--limit',type=int,default=20)
    a=p.parse_args(); plan=json.loads(a.plan.read_text(encoding='utf-8'))
    print(json.dumps(execute(plan,a.gymnasium,a.tier,a.token,a.state,a.kind,a.limit),ensure_ascii=False,sort_keys=True))
    return 0
if __name__=='__main__': raise SystemExit(main())
