#!/usr/bin/env python3
"""Materialize a private campus publication tree from a corpus-logistics plan."""
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path

DIRS=["00_INDEX","01_ATLAS","02_ROUTES","03_MANIFOLD","04_SCAFFOLDS","05_EDGES","06_GAPS","07_EVIDENCE","08_RECEIPTS","09_CONVERSATION_CHUNKS"]

def canonical(v): return (json.dumps(v,ensure_ascii=False,sort_keys=True,separators=(",",":"))+"\n").encode()
def sha(v): return hashlib.sha256(canonical(v)).hexdigest()

def write_json(path:Path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2,sort_keys=True)+"\n",encoding="utf-8")

def materialize(plan_path:Path,root:Path)->dict:
    if root.exists() and any(root.iterdir()): raise RuntimeError("publication root must be empty")
    root.mkdir(parents=True,exist_ok=True)
    for d in DIRS:(root/d).mkdir()
    plan=json.loads(plan_path.read_text(encoding="utf-8"))
    if plan.get("schema")!="rafgittools.corpus-logistics-gymnasia.v1": raise ValueError("schema mismatch")
    if plan.get("raw_body_embedded") is not False: raise ValueError("raw bodies forbidden")
    chunks=plan.get("chunks") or []; edges=plan.get("edges") or []; gyms=plan.get("gymnasia") or []
    by_tier={t:[] for t in ("HOT","WARM","COLD","ARCHIVE")}
    gaps=[]; formulas=[]; parables=[]
    for c in chunks:
        by_tier.setdefault(c["tier"],[]).append(c["chunk_id"])
        for m in c.get("marks") or []:
            if m.get("kind")=="GAP": gaps.append({"chunk_id":c["chunk_id"],"mark":m})
            if m.get("kind")=="FORMULA": formulas.append({"chunk_id":c["chunk_id"],"mark":m})
            if m.get("kind")=="PARABLE": parables.append({"chunk_id":c["chunk_id"],"mark":m})
        prefix=c["chunk_id"].replace("CHK-","")[:2]
        write_json(root/"09_CONVERSATION_CHUNKS"/prefix/(c["chunk_id"]+".json"),c)
    write_json(root/"00_INDEX"/"TIERS.json",by_tier)
    write_json(root/"00_INDEX"/"FORMULAS.json",formulas)
    write_json(root/"00_INDEX"/"PARABLES.json",parables)
    write_json(root/"01_ATLAS"/"GYMNASIA.json",gyms)
    write_json(root/"02_ROUTES"/"EDGES.json",edges)
    write_json(root/"03_MANIFOLD"/"CAMPUS.json",{"gymnasia":gyms,"tiers":by_tier})
    write_json(root/"05_EDGES"/"ALL_EDGES.json",edges)
    write_json(root/"06_GAPS"/"TOKEN_VAZIO_AND_GAPS.json",gaps)
    receipt={
      "schema":"rafgittools.campus-publication-receipt.v1",
      "claim_allowed":False,"raw_body_embedded":False,
      "source_plan":plan_path.name,"source_plan_sha256":hashlib.sha256(plan_path.read_bytes()).hexdigest(),
      "manifest_sha256":plan.get("manifest_sha256"),"chunks":len(chunks),"edges":len(edges),
      "gymnasia":len(gyms),"gaps":len(gaps),
      "state":"PASS_TREE_MATERIALIZED_NOT_GITHUB_PUBLISHED"
    }
    write_json(root/"08_RECEIPTS"/"PUBLICATION_TREE_RECEIPT.json",receipt)
    write_json(root/"07_EVIDENCE"/"BOUNDARY.json",{
      "source_body":"EXTERNAL_PRIVATE_AUTHORITY","publication":"DERIVED_METADATA_ONLY",
      "github_publication":"NOT_RUN","claim_allowed":False
    })
    write_json(root/"04_SCAFFOLDS"/"README.json",{
      "next":"stage through RafGitFS -> dry-run -> exact approval -> private PR -> readback receipt",
      "rollback":"move active generation pointer; preserve this tree"
    })
    return receipt

def main()->int:
    p=argparse.ArgumentParser(); p.add_argument("plan",type=Path); p.add_argument("root",type=Path)
    a=p.parse_args(); print(json.dumps(materialize(a.plan,a.root),sort_keys=True)); return 0
if __name__=="__main__": raise SystemExit(main())
