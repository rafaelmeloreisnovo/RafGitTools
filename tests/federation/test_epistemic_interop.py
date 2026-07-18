#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    loaded = importlib.util.module_from_spec(spec)
    assert spec and spec.loader
    spec.loader.exec_module(loaded)
    return loaded


interop = module(
    "epistemic_interop",
    ROOT / "scripts" / "federation" / "epistemic_interop.py",
)
PROFILE = ROOT / "configs" / "epistemic-provenance-interop.json"
INDEX = ROOT / "configs" / "workflow-master-index.json"


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def test_profile_and_master_index_are_valid():
    assert interop.validate(load(PROFILE), load(INDEX)) == []


def test_required_vocabulary_is_complete_and_unique():
    terms = [entry["term"] for entry in load(PROFILE)["vocabulary"]]
    assert set(terms) >= interop.REQUIRED_TERMS
    assert len(terms) == len(set(terms))


def test_token_vazio_is_not_coerced_to_numeric_or_boolean():
    profile = load(PROFILE)
    assert profile["boundary"]["token_vazio_is_numeric"] is False
    entry = next(item for item in profile["vocabulary"] if item["term"] == "TOKEN_VAZIO")
    assert "neither zero, null, false, pass nor fail" in entry["semantics"]


def test_outputs_are_deterministic():
    profile = load(PROFILE)
    index = load(INDEX)
    first = interop.build_outputs(profile, index)
    second = interop.build_outputs(profile, index)
    assert first == second
    assert {
        name: interop.sha256_json(value) for name, value in first.items()
    } == {
        name: interop.sha256_json(value) for name, value in second.items()
    }


def test_prov_projection_preserves_four_state_axes():
    projection = interop.build_prov_projection(load(PROFILE), load(INDEX))
    entity = projection["entity"]["repo:rafgittools"]
    assert entity["rafaelia:sourceStatus"]
    assert entity["rafaelia:epistemicStatus"]
    assert entity["rafaelia:operationalStatus"]
    assert entity["rafaelia:claimGate"] == "BLOCKED"
    assert projection["rafaelia:boundary"]["claimAllowed"] is False


def test_openlineage_projection_is_bounded_and_repeatable():
    profile = load(PROFILE)
    index = load(INDEX)
    projection = interop.build_openlineage_projection(profile, index)
    repeat = interop.build_openlineage_projection(profile, index)
    assert projection["run"]["runId"] == repeat["run"]["runId"]
    assert projection["eventType"] == "OTHER"
    assert projection["run"]["facets"]["rafaelia_boundary"]["formalConformance"] == "TOKEN_VAZIO"
    assert projection["run"]["facets"]["rafaelia_boundary"]["claimAllowed"] is False


def test_slsa_projection_never_claims_attestation():
    projection = interop.build_slsa_projection(load(PROFILE), load(INDEX))
    boundary = projection["rafaeliaBoundary"]
    assert boundary["signed"] is False
    assert boundary["verifiedBuilder"] == "TOKEN_VAZIO"
    assert boundary["formalSlsaConformance"] == "TOKEN_VAZIO"
    assert projection["predicate"]["buildDefinition"]["internalParameters"]["attestationStatus"] == "TOKEN_VAZIO"


def test_spdx_projection_records_dependencies_without_truth_promotion():
    projection = interop.build_spdx_projection(load(PROFILE), load(INDEX))
    assert projection["spdxVersion"] == "SPDX-2.3"
    assert len(projection["packages"]) == len(load(INDEX)["nodes"])
    assert all(package["filesAnalyzed"] is False for package in projection["packages"])
    assert any(
        relation["relationshipType"] == "DEPENDS_ON"
        for relation in projection["relationships"]
    )
    assert "TOKEN_VAZIO" in projection["annotations"][0]["comment"]


def test_nist_crosswalk_is_conceptual_only():
    projection = interop.build_nist_crosswalk(load(PROFILE))
    assert projection["status"] == "CONCEPTUAL_CROSSWALK"
    assert projection["conformance"] == "TOKEN_VAZIO"
    assert "CLAIM" in projection["functions"]["MAP"]
    assert "TEST" in projection["functions"]["MEASURE"]
    assert "ROLLBACK" in projection["functions"]["MANAGE"]
    assert "AUTHORITY" in projection["functions"]["GOVERN"]


def test_invalid_promotion_and_attestation_are_rejected():
    profile = load(PROFILE)
    index = load(INDEX)
    profile["claim_allowed"] = True
    profile["standards"]["slsa_provenance"]["attestation"] = "VERIFIED"
    errors = interop.validate(profile, index)
    assert "profile claim_allowed must remain false" in errors
    assert "SLSA attestation must remain TOKEN_VAZIO" in errors
