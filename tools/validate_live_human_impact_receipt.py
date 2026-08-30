#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path

SHA40 = re.compile(r"^[0-9a-f]{40}$")


def require(condition, message):
    if not condition:
        raise SystemExit("FAIL: " + message)


def main():
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("artifacts/live-human-impact-receipt.json")
    require(path.is_file(), f"receipt not found: {path}")
    r = json.loads(path.read_text(encoding="utf-8"))

    require(r.get("schema") == "rafaelia.live-cross-repo-human-impact-receipt.v1", "unexpected receipt schema")
    require(r.get("receipt_state") == "LIVE_CROSS_REPO_READBACK_LIMITED", "receipt must remain limited live readback")
    require(r.get("claim_allowed") is False, "receipt self-promoted claim")
    require(r.get("ethical_certification") is False, "receipt fabricated ethical certification")
    require(r.get("legal_certification") is False, "receipt fabricated legal certification")
    require(r.get("clinical_certification") is False, "receipt fabricated clinical certification")
    require(r.get("child_safety_certification") is False, "receipt fabricated child-safety certification")
    require(r.get("deployment_authorized") is False, "receipt fabricated deployment authorization")

    for section in ("control_plane", "transport", "consumer"):
        require(section in r and isinstance(r[section], dict), f"missing section: {section}")
        revision = r[section].get("revision")
        require(isinstance(revision, str) and SHA40.match(revision), f"{section} revision must be exact 40-hex SHA")

    external = r.get("external_authority_gaps", {})
    require(external.get("independent_human_review") == "TOKEN_VAZIO", "independent review fabricated closed")
    require(external.get("provider_main_enforcement") == "TOKEN_VAZIO", "provider enforcement fabricated closed")

    evidence = r.get("execution_evidence", {})
    require(str(evidence.get("github_run_id", "TOKEN_VAZIO")) != "TOKEN_VAZIO", "run id missing")
    require(str(evidence.get("github_workflow", "TOKEN_VAZIO")) != "TOKEN_VAZIO", "workflow identity missing")

    guards = set(r.get("guards", []))
    for item in (
        "FIXTURE != LIVE",
        "LOCAL_PASS != HUMAN_IMPACT_PASS",
        "CI_PASS != ETHICAL_CERTIFICATION",
        "SELF_REVIEW != INDEPENDENT_REVIEW",
        "TOKEN_VAZIO != PASS",
    ):
        require(item in guards, f"receipt guard missing: {item}")

    print("PASS: live cross-repo human-impact receipt is bounded and non-self-certifying")


if __name__ == "__main__":
    main()
