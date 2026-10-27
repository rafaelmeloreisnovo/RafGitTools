#!/usr/bin/env python3
"""Public C0-only hourly receipt; stdlib and zero network/private reads."""
import argparse
import json
import os
from pathlib import Path
import re

ALLOW_FIELDS = {"schema", "state", "public_source_sha", "claim_allowed"}
SCHEMA = "rafaelia.public-hourly-control.v1"
EXPECTED = {
    "runner_scope": "PUBLIC_ONLY",
    "source_access": "CURRENT_PUBLIC_REPOSITORY_ONLY",
    "private_coordination": "GPT_CONNECTORS_SEPARATE_TRUST_DOMAIN",
    "private_actions_dispatched": False,
    "public_payload_classes": ["C0_PUBLIC"],
    "allow_remote_private_fetch": False,
    "allow_pat": False,
    "allow_drive_private_fetch": False,
    "allow_secrets_in_logs": False,
    "release_approved": False,
    "claim_allowed": False,
    "schedule_utc": "43 * * * *",
}

def check(data):
    errors=[]
    if not isinstance(data,dict):
        return ["INVALID_OBJECT"]
    if data.get("schema")!=SCHEMA:
        errors.append("SCHEMA")
    if data.get("state")!="CANDIDATE_HOLD":
        errors.append("STATE")
    for k,value in EXPECTED.items():
        if data.get(k)!=value:
            errors.append("POLICY_"+k)
    if set(data.get("result_fields",[]))!=ALLOW_FIELDS:
        errors.append("FIELDS")
    return errors

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--manifest",required=True)
    p.add_argument("--output",required=True)
    p.add_argument("--static-only",action="store_true")
    args=p.parse_args()
    state="BLOCKED"
    try:
        obj=json.loads(Path(args.manifest).read_text(encoding="utf-8"))
        failures=check(obj)
        if not args.static_only:
            if os.getenv("GITHUB_REPOSITORY")!="rafaelmeloreisnovo/RafGitTools":
                failures.append("WRONG_REPO")
            if os.getenv("GITHUB_REPOSITORY_VISIBILITY")!="public":
                failures.append("WRONG_VISIBILITY")
            if os.getenv("GITHUB_EVENT_NAME")!="schedule":
                failures.append("WRONG_TRIGGER")
            if os.getenv("PUBLIC_SCHEDULE")!="43 * * * *":
                failures.append("WRONG_SCHEDULE")
        sha=os.getenv("GITHUB_SHA","TOKEN_VAZIO")
        if not re.fullmatch(r"[0-9a-f]{40}",sha):
            sha="TOKEN_VAZIO"
            if not args.static_only:
                failures.append("NO_SHA")
        if not failures:
            state="PASS_SCOPED_PUBLIC_ONLY"
        record=dict(schema=SCHEMA,state=state,public_source_sha=sha,claim_allowed=False)
        assert set(record)==ALLOW_FIELDS
        Path(args.output).write_text(json.dumps(record,sort_keys=True,separators=(",",":"))+"\n",encoding="utf-8")
    except (OSError,UnicodeError,ValueError,TypeError,AssertionError):
        # Never echo exception values which could include sensitive inputs.
        print("PUBLIC_HOURLY=INCOMPLETE")
        return 3
    print("PUBLIC_HOURLY="+state)
    return 0 if state=="PASS_SCOPED_PUBLIC_ONLY" else 2

if __name__=="__main__":
    raise SystemExit(main())
