#!/usr/bin/env python3
"""Bounded, dependency-free CheckValidation fast gate."""
from __future__ import annotations
import argparse, datetime as dt, json, pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
MUTABLE = re.compile(r"^\s*(?:-|)\s*uses:\s*[^#\s]+@(v\d+|main|master|latest)\s*(?:#.*)?$")
NODE20 = re.compile(r"\bnode(?:-version)?\s*[:=]\s*['\"]?20(?:\.|['\"\s]|$)", re.I)

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--profile", default="checkvalidation/profiles/rafgittools.v1.json")
    ns = ap.parse_args()
    findings = []
    profile_path = ROOT / ns.profile
    try:
        profile = json.loads(profile_path.read_text(encoding="utf-8"))
    except Exception as exc:
        findings.append({"code": "PROFILE_INVALID", "path": ns.profile, "detail": str(exc)})
        profile = {}

    required = {
        "schema": "rafaelia.checkvalidation.profile.v1",
        "integration_branch": "develop",
        "publication_branch": "main",
        "claim_allowed": False,
        "node_baseline": "24",
    }
    for key, expected in required.items():
        if profile.get(key) != expected:
            findings.append({"code": "PROFILE_CONTRACT", "field": key, "expected": expected,
                             "observed": profile.get(key, "TOKEN_VAZIO")})

    for path in sorted((ROOT / ".github/workflows").glob("*.y*ml")):
        text = path.read_text(encoding="utf-8", errors="replace")
        for line_no, line in enumerate(text.splitlines(), 1):
            if MUTABLE.match(line):
                findings.append({"code": "MUTABLE_ACTION_REF", "path": str(path.relative_to(ROOT)), "line": line_no})
            if NODE20.search(line):
                findings.append({"code": "NODE20_ACTIVE_REFERENCE", "path": str(path.relative_to(ROOT)), "line": line_no})
        if "pull_request_target:" in text and "actions/checkout@" in text:
            findings.append({"code": "PR_TARGET_CHECKOUT", "path": str(path.relative_to(ROOT))})

    for rel in ["checkvalidation/README.md", "docs/governance/BRANCH_PUBLICATION_MODEL_V1.md"]:
        if not (ROOT / rel).is_file():
            findings.append({"code": "REQUIRED_PATH_ABSENT", "path": rel})

    status = "PASS_SCOPED" if not findings else "FAIL"
    receipt = {
        "schema": "rafaelia.checkvalidation.receipt.v1",
        "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "scope": "static-fast-gate",
        "status": status,
        "claim_allowed": False,
        "profile": ns.profile,
        "findings": findings,
        "unobserved": ["compilation", "device", "scientific-validation", "publication-authorization"],
    }
    out = ROOT / "build/checkvalidation/receipt.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(receipt, indent=2, sort_keys=True))
    return 0 if status == "PASS_SCOPED" else 1

if __name__ == "__main__":
    sys.exit(main())
