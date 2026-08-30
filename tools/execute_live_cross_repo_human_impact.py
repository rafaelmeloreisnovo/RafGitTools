#!/usr/bin/env python3
import hashlib
import json
import os
import sys
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASE = ROOT / "audits/HUMAN_IMPACT_CASE_CHIPQUANTUM_BOUNDARY_20260830.v1.json"
CONTRACT = ROOT / "contracts/human_impact_cross_repo.v1.json"
DEFAULT_OUTPUT = ROOT / "artifacts/live-human-impact-receipt.json"


def require(condition, message):
    if not condition:
        raise SystemExit("FAIL: " + message)


def raw_url(repo, revision, path):
    return f"https://raw.githubusercontent.com/{repo}/{revision}/{path}"


def fetch_exact(repo, revision, path):
    url = raw_url(repo, revision, path)
    request = urllib.request.Request(url, headers={"User-Agent": "RafGitTools-live-human-impact-v1"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            body = response.read()
            status = response.status
    except Exception as exc:
        raise SystemExit(f"FAIL: exact revision unreadable: {repo}@{revision}:{path}: {exc}") from exc
    require(status == 200, f"unexpected HTTP status {status} for {url}")
    return body, url


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def main():
    output = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUTPUT
    output.parent.mkdir(parents=True, exist_ok=True)

    case = json.loads(CASE.read_text(encoding="utf-8"))
    contract = json.loads(CONTRACT.read_text(encoding="utf-8"))

    required_fields = set(contract["required_for_material_human_impact"])
    missing = sorted(required_fields - set(case))
    require(not missing, "case missing required fields: " + ", ".join(missing))
    require(case.get("claim_allowed") is False, "case must not self-promote")
    require(case.get("deployment_authorized") is False, "case must not authorize deployment")
    require(case.get("independent_review") == "TOKEN_VAZIO", "independent review must remain TOKEN_VAZIO unless externally evidenced")
    require(case["decision_state"] in contract["accepted_states"], "invalid decision state")

    local_invariants = set(contract["invariants"])
    required_invariants = {
        "LOCAL_PASS != HUMAN_IMPACT_PASS",
        "TECHNICAL_CORRECTNESS != ETHICAL_PERMISSION",
        "MODEL_RECOMMENDATION != HUMAN_VALUE_DECISION",
        "TOKEN_VAZIO != PASS",
        "UNKNOWN_RISK != SAFE",
        "PERFORMANCE_GAIN != RIGHTS_OVERRIDE",
        "FIXTURE != LIVE",
    }
    require(required_invariants.issubset(local_invariants), "local contract invariant drift")
    require(contract["claim_allowed"] is False, "local contract self-promoted")

    cp = case["control_plane"]
    cp_bytes, cp_url = fetch_exact(cp["repository"], cp["revision"], cp["path"])
    cp_policy = json.loads(cp_bytes.decode("utf-8"))
    require(cp_policy.get("claim_allowed") is False, "Mapa policy self-promoted claim_allowed")
    require(cp_policy.get("promotion_allowed") is False, "Mapa policy self-promoted promotion_allowed")
    require(cp_policy.get("autonomous_human_value_decision_allowed") is False, "Mapa policy allows autonomous human-value decision")
    require(cp_policy.get("single_actor_final_authority_for_high_impact") is False, "Mapa policy allows single-actor high-impact authority")
    cp_invariants = set(cp_policy.get("core_invariants", []))
    for item in (
        "PERSON != RESOURCE != TOKEN != DATASET != COST_FUNCTION",
        "MODEL_RECOMMENDATION != HUMAN_VALUE_DECISION",
        "TOKEN_VAZIO != PASS",
        "UNKNOWN_RISK != SAFE",
    ):
        require(item in cp_invariants, f"Mapa invariant missing: {item}")

    consumer = case["consumer"]
    consumer_bytes, consumer_url = fetch_exact(consumer["repository"], consumer["revision"], consumer["path"])
    consumer_text = consumer_bytes.decode("utf-8")
    for phrase in (
        "PERSON != RESOURCE != TOKEN != DATASET != COST_FUNCTION",
        "MODEL_RECOMMENDATION != HUMAN_VALUE_DECISION",
        "RIGHTS_CONSTRAINTS -> FEASIBLE_SET -> OPTIMIZATION",
        "BEST_INTEREST_OF_CHILD = HARD_CONSTRAINT",
        "UNKNOWN_RISK != SAFE",
        "TOKEN_VAZIO != PASS",
        "COMPUTABLE_OPTIMUM != ETHICAL_LEGITIMACY",
    ):
        require(phrase in consumer_text, f"consumer boundary missing: {phrase}")

    transport_revision = os.environ.get("TRANSPORT_REVISION") or os.environ.get("GITHUB_SHA") or "TOKEN_VAZIO"
    require(transport_revision != "TOKEN_VAZIO", "transport revision unavailable")

    receipt = {
        "schema": "rafaelia.live-cross-repo-human-impact-receipt.v1",
        "receipt_id": f"LIVE-HI-{case['decision_id']}",
        "observed_at": datetime.now(timezone.utc).isoformat(),
        "receipt_state": "LIVE_CROSS_REPO_READBACK_LIMITED",
        "claim_allowed": False,
        "ethical_certification": False,
        "legal_certification": False,
        "clinical_certification": False,
        "child_safety_certification": False,
        "deployment_authorized": False,
        "decision_state": case["decision_state"],
        "decision_id": case["decision_id"],
        "scope": case["scope"],
        "control_plane": {
            **cp,
            "source_url": cp_url,
            "sha256": sha256(cp_bytes),
            "readback": "OBSERVED_HTTP_200_AND_VALIDATED"
        },
        "transport": {
            "repository": os.environ.get("GITHUB_REPOSITORY", "rafaelmeloreisnovo/RafGitTools"),
            "revision": transport_revision,
            "contract_path": "contracts/human_impact_cross_repo.v1.json",
            "contract_sha256": sha256(CONTRACT.read_bytes()),
            "readback": "LOCAL_EXACT_CHECKOUT_AND_VALIDATED"
        },
        "consumer": {
            **consumer,
            "source_url": consumer_url,
            "sha256": sha256(consumer_bytes),
            "readback": "OBSERVED_HTTP_200_AND_VALIDATED"
        },
        "human_impact_record": {key: case[key] for key in contract["required_for_material_human_impact"]},
        "external_authority_gaps": {
            "independent_human_review": "TOKEN_VAZIO",
            "provider_main_enforcement": "TOKEN_VAZIO",
            "affected_community_review": "TOKEN_VAZIO_WHEN_APPLICABLE",
            "specialist_review": "TOKEN_VAZIO_WHEN_APPLICABLE"
        },
        "execution_evidence": {
            "github_run_id": os.environ.get("GITHUB_RUN_ID", "TOKEN_VAZIO"),
            "github_run_attempt": os.environ.get("GITHUB_RUN_ATTEMPT", "TOKEN_VAZIO"),
            "github_workflow": os.environ.get("GITHUB_WORKFLOW", "TOKEN_VAZIO"),
            "github_actor": os.environ.get("GITHUB_ACTOR", "TOKEN_VAZIO")
        },
        "falsifier": case["falsifier"],
        "guards": [
            "FIXTURE != LIVE",
            "LOCAL_PASS != HUMAN_IMPACT_PASS",
            "CI_PASS != ETHICAL_CERTIFICATION",
            "SELF_REVIEW != INDEPENDENT_REVIEW",
            "TOKEN_VAZIO != PASS"
        ]
    }

    output.write_text(json.dumps(receipt, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({
        "result": "PASS",
        "receipt_state": receipt["receipt_state"],
        "receipt_id": receipt["receipt_id"],
        "control_plane_revision": cp["revision"],
        "transport_revision": transport_revision,
        "consumer_revision": consumer["revision"],
        "claim_allowed": False,
        "external_review": "TOKEN_VAZIO"
    }, indent=2))


if __name__ == "__main__":
    main()
