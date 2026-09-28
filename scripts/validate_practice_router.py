#!/usr/bin/env python3
import argparse
import json
from pathlib import Path

def die(message):
    print(json.dumps({"state":"FAIL","error":message}, ensure_ascii=False))
    raise SystemExit(1)

def load(path):
    try:
        return json.loads(Path(path).read_text(encoding="utf-8"))
    except Exception as exc:
        die(f"cannot load router: {exc}")

def validate(data):
    if data.get("schema") != "RAFGITTOOLS_PRACTICE_ROUTER_V1":
        die("unexpected schema")
    if data.get("claim_allowed") is not False:
        die("claim_allowed must be false")
    fed = data.get("federated_atlas") or {}
    if fed.get("repo") != "rafaelmeloreisnovo/Mapa":
        die("federated atlas authority must remain Mapa")
    routes = data.get("routes")
    if not isinstance(routes, list) or not routes:
        die("routes must be a non-empty list")
    ids=set()
    required={"id","atlas_area","source_min","execution_target","evidence","next"}
    for route in routes:
        missing=sorted(required-set(route))
        if missing:
            die(f"route missing keys: {missing}")
        rid=route["id"]
        if rid in ids:
            die(f"duplicate route id: {rid}")
        ids.add(rid)
        src=route["source_min"]
        if not isinstance(src,list) or not 1 <= len(src) <= 3:
            die(f"{rid}: source_min must contain 1..3 entries")
        for key in ("atlas_area","execution_target","evidence","next"):
            if not isinstance(route[key],str) or not route[key].strip():
                die(f"{rid}: empty {key}; use typed TOKEN_VAZIO instead")
    return routes

def main():
    p=argparse.ArgumentParser()
    p.add_argument("router")
    p.add_argument("--route")
    args=p.parse_args()
    data=load(args.router)
    routes=validate(data)
    if args.route:
        hit=next((r for r in routes if r["id"]==args.route),None)
        if hit is None:
            die(f"unknown route: {args.route}")
        print(json.dumps({"state":"PASS","route":hit,"claim_allowed":False},ensure_ascii=False,indent=2))
        return
    print(json.dumps({
        "state":"PASS",
        "schema":data["schema"],
        "routes":len(routes),
        "federated_atlas":data["federated_atlas"],
        "claim_allowed":False
    },ensure_ascii=False,indent=2))

if __name__=="__main__":
    main()
