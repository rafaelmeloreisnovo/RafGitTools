import unittest

from scripts.validate_knowledge_work_seven_guards import (
    validate_config,
    validate_envelope,
)

def base_unit():
    return {
        "id": "RGT-KW7-001",
        "intent": "validate one bounded control-plane transition",
        "authority": "rafaelmeloreisnovo/RafGitTools",
        "source_contract_ref": "github:rafaelmeloreisnovo/Mapa@3a2821d44d555c28cd041ba029cfa940fe3d4c0a:data/control-plane/knowledge-work-house.v1.json",
        "provenance": {
            "source_provider": "GitHub",
            "repository": "rafaelmeloreisnovo/RafGitTools",
            "ref": "main",
            "path": "examples/knowledge-work-seven-guards.example.json",
            "object_hash": "sha256:example-fixture",
            "observed_at": "2026-09-13T00:45:00-03:00",
            "authority": "rafaelmeloreisnovo/RafGitTools"
        },
        "context": {
            "intent": "bounded structural example",
            "scope": "seven-guard adapter only",
            "boundary": "no producer/runtime/scientific claim",
            "observed_at": "2026-09-13T00:45:00-03:00",
            "dependencies": ["Mapa:knowledge-work-house.v1"]
        },
        "evidence": [
            {"ref": "GitHub:Mapa#619", "type": "SOURCE_CONTRACT", "scope": "seven-guard vocabulary"}
        ],
        "contradictions": [],
        "uncertainty": [
            {
                "id": "U0",
                "state": "CLOSED",
                "evidence_needed": "none",
                "falsifier": "fixture no longer matches source contract",
                "next_probe": "revalidate on source contract change"
            }
        ],
        "reproduction": {
            "status": "PASS",
            "procedure": "python3 scripts/validate_knowledge_work_seven_guards.py examples/knowledge-work-seven-guards.example.json",
            "environment_ref": "python:stdlib",
            "input_ref": "examples/knowledge-work-seven-guards.example.json",
            "output_ref": "stdout:report"
        },
        "rollback": {
            "state": "NOT_APPLICABLE",
            "predecessor": "none",
            "procedure": "read-only fixture validation",
            "verification": "no mutation performed"
        },
        "reconstruction_pointer": "Mapa#619 -> RafGitTools seven-guard adapter -> validation report",
        "mutation_performed": False,
        "claim_allowed": False,
        "next": "domain authority reviews any producer claim"
    }

class SevenGuardAdapterTests(unittest.TestCase):
    def test_config_contract(self):
        cfg = {
            "schema": "rafaelia.rafgittools.knowledge-work-seven-guards.v1",
            "claim_allowed": False,
            "source_contract": {"repository": "rafaelmeloreisnovo/Mapa", "pull_request": 619},
            "guards": [
                {"key": "provenance"}, {"key": "context"}, {"key": "evidence"},
                {"key": "contradictions"}, {"key": "uncertainty"},
                {"key": "reproduction"}, {"key": "rollback"}
            ]
        }
        self.assertEqual(validate_config(cfg), [])

    def test_complete_unit_ready_for_domain_review(self):
        errors, blockers, decision = validate_envelope(base_unit())
        self.assertEqual(errors, [])
        self.assertEqual(blockers, [])
        self.assertEqual(decision, "READY_FOR_DOMAIN_REVIEW")

    def test_missing_provenance_blocks(self):
        u = base_unit()
        del u["provenance"]["repository"]
        errors, _, decision = validate_envelope(u)
        self.assertIn("provenance:missing_repository", errors)
        self.assertEqual(decision, "BLOCKED")

    def test_missing_context_blocks(self):
        u = base_unit()
        u["context"]["boundary"] = ""
        errors, _, decision = validate_envelope(u)
        self.assertIn("context:empty_boundary", errors)
        self.assertEqual(decision, "BLOCKED")

    def test_empty_evidence_is_blocker(self):
        u = base_unit()
        u["evidence"] = []
        errors, blockers, decision = validate_envelope(u)
        self.assertEqual(errors, [])
        self.assertIn("evidence:empty", blockers)
        self.assertEqual(decision, "BLOCKED")

    def test_open_contradiction_is_blocker(self):
        u = base_unit()
        u["contradictions"] = [{
            "id": "C1", "state": "OPEN",
            "comparison_scope": "same object/ref", "ref": "receipt:C1"
        }]
        _, blockers, decision = validate_envelope(u)
        self.assertIn("contradictions:C1:OPEN", blockers)
        self.assertEqual(decision, "BLOCKED")

    def test_token_vazio_uncertainty_is_blocker(self):
        u = base_unit()
        u["uncertainty"] = [{
            "id": "U1", "state": "TOKEN_VAZIO",
            "evidence_needed": "device execution",
            "falsifier": "device receipt exists and matches exact ref",
            "next_probe": "run on device"
        }]
        _, blockers, decision = validate_envelope(u)
        self.assertIn("uncertainty:U1:TOKEN_VAZIO", blockers)
        self.assertEqual(decision, "BLOCKED")

    def test_reproduction_missing_is_blocker(self):
        u = base_unit()
        u["reproduction"]["status"] = "TOKEN_VAZIO"
        _, blockers, decision = validate_envelope(u)
        self.assertIn("reproduction:TOKEN_VAZIO", blockers)
        self.assertEqual(decision, "BLOCKED")

    def test_mutation_requires_rollback(self):
        u = base_unit()
        u["mutation_performed"] = True
        u["rollback"]["state"] = "TOKEN_VAZIO"
        _, blockers, decision = validate_envelope(u)
        self.assertIn("rollback:mutation_without_ready_rollback", blockers)
        self.assertEqual(decision, "BLOCKED")

    def test_claim_promotion_is_rejected(self):
        u = base_unit()
        u["claim_allowed"] = True
        errors, _, decision = validate_envelope(u)
        self.assertIn("claim_gate:RafGitTools_claim_allowed_must_remain_false", errors)
        self.assertEqual(decision, "BLOCKED")

if __name__ == "__main__":
    unittest.main()
