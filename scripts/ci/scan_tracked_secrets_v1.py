#!/usr/bin/env python3
"""Tracked-tree redacted scan; oversize and binary files fail closed; no path logging."""
import argparse
import json
from pathlib import Path
import re
import subprocess

LIMIT=2*1024*1024
MARKERS=(
 re.compile(rb"\bghp_[A-Za-z0-9]{36}\b"),
 re.compile(rb"\bgithub_pat_[A-Za-z0-9_]{50,}\b"),
 re.compile(rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
 re.compile(rb"https?://[^\s/:@]+:[^\s/@]+@[A-Za-z0-9.-]+"),
)
def scan(root,names,limit=LIMIT):
    base=Path(root).resolve()
    counts=dict(scanned=0,oversize=0,symlink=0,unreadable=0,binary=0,suspicious=0)
    for name in names:
        try:
            p=base/name
            if p.is_symlink(): counts["symlink"]+=1; continue
            if not p.resolve().is_relative_to(base): counts["unreadable"]+=1; continue
            if p.stat().st_size>limit: counts["oversize"]+=1; continue
            with p.open("rb") as f: raw=f.read(limit+1)
            if len(raw)>limit: counts["oversize"]+=1; continue
            if b"\x00" in raw: counts["binary"]+=1; continue
            counts["scanned"]+=1
            if any(rx.search(raw) for rx in MARKERS): counts["suspicious"]+=1
        except (OSError,UnicodeError,ValueError): counts["unreadable"]+=1
    state="BLOCKED" if counts["suspicious"] else ("INCOMPLETE" if any(counts[k] for k in ("oversize","symlink","unreadable","binary")) else "PASS_SCOPED")
    return {"state":state,"counts":counts,"history":"NOT_SCANNED","remote_secrets":"NOT_SCANNED","claim_repository_clean":False}
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--root",default=".")
    p.add_argument("--max-bytes",type=int,default=LIMIT)
    a=p.parse_args()
    if not 1024<=a.max_bytes<=128*1024*1024:
        print('{"state":"BLOCKED_PARAMETER"}'); return 2
    try:
        root=Path(a.root).resolve()
        data=subprocess.run(["git","-C",str(root),"ls-files","-z"],capture_output=True,check=True,timeout=20).stdout
        names=[x.decode("utf-8","surrogateescape") for x in data.split(b"\x00") if x]
        report=scan(root,names,a.max_bytes)
    except (OSError,subprocess.SubprocessError,UnicodeError):
        print('{"state":"INCOMPLETE_GIT"}'); return 3
    print(json.dumps(report,sort_keys=True,separators=(",",":")))
    return {"PASS_SCOPED":0,"BLOCKED":2,"INCOMPLETE":3}[report["state"]]
if __name__=="__main__":
    raise SystemExit(main())
