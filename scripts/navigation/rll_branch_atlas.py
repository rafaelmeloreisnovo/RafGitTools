#!/usr/bin/env python3
import argparse, json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
DEFAULT=ROOT/"data/navigation/RLL_BRANCH_ATLAS_SNAPSHOT_20260929.json"
FAMILIES={
"00-anchor","05-lab","10-science","20-data","30-math-geometry",
"40-runtime-work","50-docs-papers","60-ci-release","70-governance-security",
"80-audit-evidence","90-agent-automation","95-legacy-unclassified"
}

def validate(path: Path):
    data=json.loads(path.read_text(encoding="utf-8"))
    errors=[]
    if data.get("schema")!="rafgittools.rll-branch-atlas-snapshot.v1":
        errors.append("schema")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed")
    repos=data.get("repositories",{})
    for repo,meta in repos.items():
        rows=meta.get("branches",[])
        if meta.get("total")!=len(rows):
            errors.append(f"{repo}:total")
        names=[r.get("branch") for r in rows]
        if len(names)!=len(set(names)):
            errors.append(f"{repo}:duplicate_branch")
        for row in rows:
            if row.get("family") not in FAMILIES:
                errors.append(f"{repo}:{row.get('branch')}:family")
            if row.get("branch") in {"main","rll/lab"}:
                if row.get("rename_state")!="NO_RENAME_ANCHOR":
                    errors.append(f"{repo}:{row.get('branch')}:anchor")
            elif row.get("rename_state")!="REVIEW_REQUIRED":
                errors.append(f"{repo}:{row.get('branch')}:rename_state")
    return data,errors

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--snapshot",type=Path,default=DEFAULT)
    args=ap.parse_args()
    data,errors=validate(args.snapshot)
    print(json.dumps({
      "state":"PASS" if not errors else "FAIL",
      "errors":errors,
      "repositories":{k:v.get("total") for k,v in data.get("repositories",{}).items()},
      "claim_allowed":False
    },indent=2,sort_keys=True))
    raise SystemExit(1 if errors else 0)

if __name__=="__main__":
    main()
