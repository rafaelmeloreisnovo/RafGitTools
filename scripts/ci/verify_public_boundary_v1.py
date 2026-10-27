#!/usr/bin/env python3
"""Fail-closed structural gate: no private or PAT-backed job on public Actions."""
import argparse
import os
import re
from pathlib import Path

DISABLED=("provider_actions","private_ci_bridge","mapa_provider_pat_preflight",
"provider_environments","release","public_data_custody",
"repository_factory_pat_preflight","promotion_hold_pat_agent")

def validate(src):
    errors=[]
    if not re.search(r"(?m)^  schedule:\s*$",src): errors.append("NO_SCHEDULE")
    if "PUBLIC_ONLY_DISABLED_PRIVATE_EXECUTION" not in src: errors.append("NO_LOCK_MARKER")
    if 'if [[ "$private_ci" == true ]]; then' not in src: errors.append("NO_ROUTE_GUARD")
    for job in DISABLED:
        m=re.search(r"(?ms)^  "+re.escape(job)+r":\n(.*?)(?=^  [a-z][\w-]*:\n|\Z)",src)
        if not m: errors.append("MISSING_"+job); continue
        head=m.group(1).split("\n    runs-on:",1)[0]
        if not re.search(r"(?m)^    if: \$\{\{ false \}\}\s*$",head):
            errors.append("ENABLED_"+job)
    return errors

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--workflow",required=True)
    p.add_argument("--static-only",action="store_true")
    args=p.parse_args()
    if not args.static_only:
        if os.getenv("GITHUB_REPOSITORY")!="rafaelmeloreisnovo/RafGitTools" or os.getenv("GITHUB_REPOSITORY_VISIBILITY")!="public":
            print("PUBLIC_BOUNDARY=BLOCKED_RUNTIME")
            return 2
    try: errs=validate(Path(args.workflow).read_text(encoding="utf-8"))
    except (OSError,UnicodeError):
        print("PUBLIC_BOUNDARY=INCOMPLETE_INPUT")
        return 3
    print("PUBLIC_BOUNDARY="+("BLOCKED" if errs else "PASS_SCOPED")+" violations="+str(len(errs)))
    return 2 if errs else 0

if __name__=="__main__":
    raise SystemExit(main())
