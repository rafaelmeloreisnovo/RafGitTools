"""Validate and seal a whole-session capsule without collapsing atomic claims."""
from __future__ import annotations
from hashlib import sha256
import json
from pathlib import Path
from typing import Any

REQUIRED = {
    "sequence", "token_id", "domain", "statement", "modality",
    "negation", "numbers", "authority", "claim_gate"
}


def canonical_digest(value: Any) -> str:
    return sha256(
        json.dumps(value, sort_keys=True, separators=(",", ":")).encode()
    ).hexdigest()


def validate_capsule(capsule: dict) -> list[str]:
    errors: list[str] = []
    if capsule.get("interaction_model") != "ONE_CAPSULE_ORDERED_ATOMIC_SUBTOKENS":
        errors.append("invalid interaction_model")
    for key in (
        "claim_allowed", "private_payload_copied",
        "automatic_cross_repo_write", "automatic_merge"
    ):
        if capsule.get(key) is not False:
            errors.append(f"{key} must be false")
    if "digital_identity" not in capsule.get("excluded_topics", []):
        errors.append("explicit exclusion missing")

    tokens = capsule.get("tokens", [])
    if not tokens:
        return errors + ["tokens empty"]
    if [token.get("sequence") for token in tokens] != list(range(1, len(tokens) + 1)):
        errors.append("sequence must be contiguous")
    token_ids = [token.get("token_id") for token in tokens]
    if len(token_ids) != len(set(token_ids)):
        errors.append("duplicate token ids")

    for token in tokens:
        missing = REQUIRED - token.keys()
        if missing:
            errors.append(f"{token.get('token_id')}: missing {sorted(missing)}")
            continue
        if token["negation"] not in (True, False):
            errors.append(f"{token['token_id']}: negation")
        if not isinstance(token["numbers"], list):
            errors.append(f"{token['token_id']}: numbers")
        statement = token["statement"].lower()
        if token["domain"] == "digital_identity" or "digital identity" in statement:
            errors.append(f"{token['token_id']}: excluded topic leaked")
        for literal in token["numbers"]:
            if str(literal).lower() not in statement:
                errors.append(f"{token['token_id']}: numeric literal lost: {literal}")
        if token["claim_gate"] == "OPEN":
            errors.append(f"{token['token_id']}: open gate")

    routes = capsule.get("routes", {})
    if routes.get("physics") != "instituto-Rafael/relativity-living-light":
        errors.append("bad physics route")
    if routes.get("orchestration") != "rafaelmeloreisnovo/RafPolimata":
        errors.append("bad orchestration route")
    if routes.get("map") != "rafaelmeloreisnovo/Mapa":
        errors.append("bad map route")
    return errors


def seal_capsule(capsule: dict) -> dict:
    errors = validate_capsule(capsule)
    if errors:
        raise ValueError("; ".join(errors))
    base = dict(capsule)
    base.pop("seal", None)
    return {
        "schema": "rafaelia.session_single_subtokenization.seal.v1",
        "session_id": capsule["session_id"],
        "token_count": len(capsule["tokens"]),
        "token_digest_sha256": canonical_digest(capsule["tokens"]),
        "capsule_digest_sha256": canonical_digest(base),
        "claim_allowed": False
    }


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--capsule", default="configs/session-single-subtokenization-v1.json"
    )
    parser.add_argument("--report")
    args = parser.parse_args()
    capsule = json.loads(Path(args.capsule).read_text())
    report = seal_capsule(capsule)
    output = json.dumps(report, indent=2, sort_keys=True)
    if args.report:
        Path(args.report).write_text(output + "\n")
    else:
        print(output)
