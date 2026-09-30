#!/usr/bin/env python3
import argparse
import hashlib
import json
from pathlib import Path

EXPECTED = {
    "rafaelmeloreisnovo/Rafaelia_Private",
    "rafaelmeloreisnovo/GAIA_phi",
    "rafaelmeloreisnovo/RafPolimata",
    "rafaelmeloreisnovo/Vectras-VM-Android",
    "rafaelmeloreisnovo/termux-app-rafacodephi",
    "rafaelmeloreisnovo/termux-packages",
    "rafaelmeloreisnovo/RafGitTools",
}
EXPECTED_SHARED_BLOB = "132f948d199f6679fcae4f33912e0a3ad69691a3"

def load(path):
    return json.loads(path.read_text(encoding="utf-8"))

def git_blob_sha(data):
    header = ("blob " + str(len(data)) + "\0").encode("ascii")
    return hashlib.sha1(header + data).hexdigest()

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--registry", default="configs/ecosystem-federation.v1.json")
    ap.add_argument("--state", default="configs/ecosystem-operational-state.v2.json")
    ap.add_argument("--root", action="append", default=[], metavar="REPOSITORY=PATH")
    args = ap.parse_args()

    registry = load(Path(args.registry))
    state = load(Path(args.state))
    roots = {}
    for item in args.root:
        repo, path = item.split("=", 1)
        roots[repo] = Path(path)

    failures = []
    members = registry.get("members", [])
    observed = {m.get("repository") for m in members}
    if observed != EXPECTED:
        failures.append("seven-member registry mismatch")

    if registry.get("claim_allowed") is not False:
        failures.append("registry claim_allowed must remain false")
    if state.get("claim_allowed") is not False:
        failures.append("operational-state claim_allowed must remain false")

    for member in members:
        repo = member.get("repository")
        if repo not in roots:
            failures.append(repo + ": root not supplied")
            continue
        root = roots[repo]
        manifest_path = root / member.get("member_manifest", "")
        if not manifest_path.is_file():
            failures.append(repo + ": member manifest missing")
            continue
        manifest = load(manifest_path)
        if manifest.get("repository") != repo:
            failures.append(repo + ": member manifest repository mismatch")
        if manifest.get("authority") != member.get("authority"):
            failures.append(repo + ": authority mismatch")
        if manifest.get("claim_allowed") is not False:
            failures.append(repo + ": member claim_allowed must remain false")

        fed = manifest.get("federation", {})
        if fed.get("authority_transfer_allowed") is not False:
            failures.append(repo + ": authority transfer must remain disabled")

        receipt = root / "federation/receipts/2026-09-30-ecosystem-hotfix.v2.json"
        if not receipt.is_file():
            failures.append(repo + ": successor hotfix receipt missing")
        else:
            rec = load(receipt)
            if rec.get("claim_allowed") is not False:
                failures.append(repo + ": hotfix receipt claim_allowed must remain false")
            if not rec.get("rollback"):
                failures.append(repo + ": rollback missing from hotfix receipt")

        for shared in manifest.get("shared_components", []):
            if shared.get("id") != "raf_bl0.c":
                continue
            rel = shared.get("path", "")
            file_path = root / rel
            if not file_path.is_file():
                failures.append(repo + ": shared raf_bl0 path missing")
                continue
            actual = git_blob_sha(file_path.read_bytes())
            declared = shared.get("blob_sha")
            if actual != declared:
                failures.append(repo + ": raf_bl0 manifest/blob mismatch")
            if actual != EXPECTED_SHARED_BLOB:
                failures.append(repo + ": raf_bl0 diverged from observed federation blob")

    if state.get("materialized", {}).get("raf_bl0", {}).get("authority_repo") != "TOKEN_VAZIO":
        failures.append("raf_bl0 authority must remain unresolved until provenance gate closes")

    if failures:
        for f in failures:
            print("FAIL:", f)
        return 1

    print("PASS: seven-member ecosystem federation v2 is structurally coherent")
    print("members=7 shared_blob=" + EXPECTED_SHARED_BLOB + " claim_allowed=false")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
