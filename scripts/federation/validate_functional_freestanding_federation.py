#!/usr/bin/env python3
import argparse
import json
from pathlib import Path

EXPECTED = {
    "rafaelmeloreisnovo/termux-packages",
    "rafaelmeloreisnovo/GAIA_phi",
    "rafaelmeloreisnovo/termux-app-rafacodephi",
    "rafaelmeloreisnovo/RafPolimata",
    "rafaelmeloreisnovo/RafGitTools",
}

def load(path):
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--registry", default="configs/functional-freestanding-federation.v1.json")
    parser.add_argument("--root", action="append", default=[], metavar="REPOSITORY=PATH")
    args = parser.parse_args()

    registry = load(Path(args.registry))
    roots = {}
    for item in args.root:
        repository, path = item.split("=", 1)
        roots[repository] = Path(path)

    failures = []
    observed = {item["repository"] for item in registry["members"]}
    if observed != EXPECTED:
        failures.append("registry member set differs from the concrete five-repository federation")

    for member in registry["members"]:
        repository = member["repository"]
        if repository not in roots:
            failures.append(repository + ": root not supplied")
            continue
        root = roots[repository]
        leaf_path = root / member["leaf"]
        if not leaf_path.is_file():
            failures.append(repository + ": leaf missing: " + str(leaf_path))
            continue
        leaf = load(leaf_path)
        if leaf.get("repository") != repository:
            failures.append(repository + ": leaf repository mismatch")
        if leaf.get("authority") != member["authority"]:
            failures.append(repository + ": authority mismatch")
        conditions = leaf.get("conditions", {})
        required = {
            "relationship_is_not_authority_merge": True,
            "source_body_copy_required": False,
            "missing_evidence_may_be_promoted": False,
            "freestanding_must_be_evidenced": True,
            "local_gate_required_before_promotion": True,
            "claim_promotion_by_failover": False,
        }
        if conditions != required:
            failures.append(repository + ": federation conditions changed")
        integration = leaf.get("integration", {})
        if integration.get("body_copy_allowed") is not False:
            failures.append(repository + ": body copy must remain disabled")
        if integration.get("authority_transfer_allowed") is not False:
            failures.append(repository + ": authority transfer must remain disabled")
        for relative in leaf.get("required_paths", []):
            if not (root / relative).exists():
                failures.append(repository + ": required path missing: " + relative)
        evidence = leaf.get("freestanding_evidence")
        if not evidence or not (root / evidence).exists():
            failures.append(repository + ": freestanding evidence missing: " + str(evidence))

    if failures:
        for failure in failures:
            print("FAIL:", failure)
        return 1
    print("PASS: concrete freestanding federation contract is structurally consistent")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
