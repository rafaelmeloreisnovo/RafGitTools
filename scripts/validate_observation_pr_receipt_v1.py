#!/usr/bin/env python3
"""Pure, read-only PR evidence gate; never authorizes a provider mutation."""
import json
import re
import sys

EXPECTED = {
    ("rafaelmeloreisnovo/Mapa", 758): "ccd25c9cf256f3b7cee221cc28954b0185e33e3a",
    ("rafaelmeloreisnovo/RafPolimata", 437): "9c5b6657841aea96128ab360e952f0cfad09a69c",
    ("rafaelmeloreisnovo/EstudioAudio", 70): "9be018f47df528059470f87fc7657b8707911732",
}
SHA = re.compile(r"^[0-9a-f]{40}$")


def assess(source):
    """Data is a caller-supplied observation, not an authenticated GitHub response."""
    gaps = []
    if not isinstance(source, dict):
        return {"state": "BLOCKED_INPUT", "gaps": ["NOT_OBJECT"], "claim_allowed": False}
    identity = (source.get("repository"), source.get("pr"))
    pinned = EXPECTED.get(identity)
    if pinned is None or type(identity[1]) is not int:
        gaps.append("TARGET_NOT_PINNED")
    if not isinstance(source.get("head_sha"), str) or not SHA.fullmatch(source["head_sha"]):
        gaps.append("MALFORMED_HEAD")
    elif pinned is not None and source["head_sha"] != pinned:
        gaps.append("HEAD_DRIFT")
    if source.get("draft") is not True or source.get("merged") is not False:
        gaps.append("NOT_DRAFT_OPEN")
    if not isinstance(source.get("title"), str) or "NÃO PROMOVER" not in source["title"]:
        gaps.append("MISSING_HOLD_TITLE")
    checks = source.get("checks")
    if not isinstance(checks, list) or not checks:
        gaps.append("CHECKS_UNOBSERVED")
    else:
        names = set()
        for check in checks:
            if not isinstance(check, dict) or not isinstance(check.get("name"), str):
                gaps.append("MALFORMED_CHECK")
                continue
            if check["name"] in names:
                gaps.append("DUPLICATE_CHECK_NAME")
            names.add(check["name"])
            if check.get("conclusion") != "success":
                gaps.append("CHECK_NOT_PASS")
    if source.get("provider_enforcement") is not True:
        gaps.append("PROVIDER_ENFORCEMENT_UNVERIFIED")
    if source.get("independent_review") is not True:
        gaps.append("INDEPENDENT_REVIEW_UNVERIFIED")
    if source.get("license_verified") is not True:
        gaps.append("RIGHTS_UNVERIFIED")
    # Green cannot grant merge in this lane. A separate owner-approved,
    # provider-enforced gate is required after fresh exact-head evidence.
    return {"state": "EVIDENCE_OBSERVABLE_NO_PROMOTION" if not gaps else "BLOCKED",
            "gaps": sorted(set(gaps)), "claim_allowed": False,
            "promotion_allowed": False, "mutation_allowed": False}


def main(argv):
    if len(argv) != 2:
        print("USAGE: validate_observation_pr_receipt_v1.py SNAPSHOT.json")
        return 2
    try:
        with open(argv[1], encoding="utf-8") as f:
            data = json.load(f)
    except (OSError, ValueError):
        print('{"state":"BLOCKED_READ","claim_allowed":false}')
        return 3
    result = assess(data)
    print(json.dumps(result, sort_keys=True))
    return 0 if result["state"] == "EVIDENCE_OBSERVABLE_NO_PROMOTION" else 3


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
