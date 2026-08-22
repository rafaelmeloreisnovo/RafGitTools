from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STATUS = ROOT / "data/governance/uncertainty-ethics-license-v2.applied-status-20260822.json"
PARABLES = ROOT / "data/governance/parable-technical-links.v1.json"
RELATIONS = ROOT / "data/governance/relation-coverage.v1.json"


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def test_status_preserves_claim_and_gap_boundaries() -> None:
    data = load(STATUS)
    assert data["claim_allowed"] is False
    assert data["governance_ci"]["conclusion"] == "success"

    urgencies = data["urgencies"]
    assert urgencies["TV-V2-PARABLE-LINK-001"]["state"] == "CLOSED_MATERIALIZED_LIMITED"
    assert urgencies["TV-V2-B7-T2-001"]["state"] == "TOKEN_VAZIO_BRIDGE"
    assert urgencies["TV-V2-B7-T2-001"]["claim"] is False

    gaia = urgencies["TV-V2-GAIA-COMPLEX-EXEC-001"]
    assert gaia["source_blob"] == "1bb228392c51d3bfd0d85ae8e6ce92c40fe28dd1"
    assert "CI_RECEIPT_IN_PROGRESS" in gaia["state"]

    noise = urgencies["TV-V2-NOISE-NULL-001"]
    assert "DOMAIN_NULLS_OPEN" in noise["state"]


def test_parables_have_zero_evidence_weight_and_technical_targets() -> None:
    data = load(PARABLES)
    assert data["claim_allowed"] is False
    assert data["evidence_effect"] == "NONE"
    assert data["relations"]
    for relation in data["relations"]:
        assert relation["relation"] == "ANALOGY_OF"
        assert relation["technical_target"]
        assert relation["evidence_effect"] == "NONE"


def test_relation_coverage_is_bounded_and_typed() -> None:
    data = load(RELATIONS)
    assert data["claim_allowed"] is False
    assert data["state"] == "PARTIAL_BOUNDED_INVENTORY"
    assert data["relations"]
    for relation in data["relations"]:
        assert relation["source"]
        assert relation["relation"]
        assert relation["target"]
        assert relation["uncertainty"]
        assert relation["boundary_or_falsifier"]
        assert relation["next_gate"]
