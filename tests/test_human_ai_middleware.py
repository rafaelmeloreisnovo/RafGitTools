import copy
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from validate_human_ai_middleware import validate_adapter, validate_request  # noqa: E402

BASE = json.loads((ROOT / "examples/human-ai-middleware/request.safe.json").read_text(encoding="utf-8"))


def adapter(repo="rafaelmeloreisnovo/RafPolimata", effects=None):
    return {
        "schema": "raf.human-ai.adapter.v1",
        "repository": repo,
        "default_branch": "main",
        "role": "DETERMINISTIC_STRUCTURER",
        "contract": {
            "repository": "rafaelmeloreisnovo/RafGitTools",
            "path": "contracts/human-ai-middleware-v1.schema.json",
            "version": "1.0.0",
            "commit_pin": "a" * 40,
        },
        "allowed_inputs": ["bounded_job"],
        "allowed_outputs": ["segment", "receipt"],
        "allowed_effects": effects or ["READ_ONLY", "LOCAL_WRITE"],
        "forbidden_effects": ["SENSOR_READ", "GIT_WRITE", "NETWORK_WRITE", "PUBLIC_WRITE"],
        "privacy": {
            "mode": "LOCAL_FIRST",
            "raw_secrets_allowed": False,
            "public_private_data_allowed": False,
            "minimization_required": True,
        },
        "human_control": {
            "approval_for_write": True,
            "approval_for_sensitive_data": True,
            "revocation": True,
            "appeal": True,
        },
        "token_vazio": {
            "preserved": True,
            "requires_reason": True,
            "requires_next_step": True,
            "loop_budget_enforced": True,
        },
        "evidence": {
            "implementation_state": "IMPLEMENTED",
            "runtime_state": "TOKEN_VAZIO",
            "claim_allowed": False,
        },
    }


class MiddlewareTests(unittest.TestCase):
    def findings(self, doc, adapters=None):
        return {finding.code for finding in validate_request(doc, adapters)}

    def test_safe_request_passes(self):
        self.assertEqual([], validate_request(BASE, {"rafaelmeloreisnovo/RafPolimata": adapter()}))

    def test_ai_cannot_execute(self):
        doc = copy.deepcopy(BASE)
        doc["ai_lane"]["may_execute"] = True
        self.assertIn("AI_AUTHORITY", self.findings(doc))

    def test_public_private_data_blocked(self):
        doc = copy.deepcopy(BASE)
        doc["data_boundary"]["destination_visibility"] = "PUBLIC"
        self.assertIn("PUBLIC_PRIVATE_DATA", self.findings(doc))

    def test_secret_material_blocked(self):
        doc = copy.deepcopy(BASE)
        doc["access_token"] = "abc"
        self.assertIn("SECRET_MATERIAL", self.findings(doc))

    def test_write_requires_dry_run(self):
        doc = copy.deepcopy(BASE)
        doc["execution"]["dry_run_first"] = False
        self.assertIn("DRY_RUN", self.findings(doc))

    def test_write_requires_rollback(self):
        doc = copy.deepcopy(BASE)
        doc["execution"]["rollback"] = {"available": False, "strategy": None}
        self.assertIn("ROLLBACK", self.findings(doc))

    def test_token_vazio_requires_next(self):
        doc = copy.deepcopy(BASE)
        doc["evidence"]["F_next"] = []
        self.assertIn("TOKEN_VAZIO_CONTEXT", self.findings(doc))

    def test_loop_budget_enforced(self):
        doc = copy.deepcopy(BASE)
        doc["friction"]["current_loop"] = 3
        self.assertIn("LOOP_BUDGET", self.findings(doc))

    def test_measured_usefulness_requires_evidence(self):
        doc = copy.deepcopy(BASE)
        doc["intent"]["measured_usefulness"] = 0.94
        self.assertIn("USEFULNESS_EVIDENCE", self.findings(doc))

    def test_sensitive_category_requires_rights_reviewer(self):
        doc = copy.deepcopy(BASE)
        doc["data_boundary"]["categories"] = ["VOICE"]
        self.assertIn("RIGHTS_REVIEW", self.findings(doc))

    def test_minor_requires_two_review_domains(self):
        doc = copy.deepcopy(BASE)
        doc["people"]["affected_people"][0]["minor"] = True
        self.assertIn("MINOR_REVIEW", self.findings(doc))

    def test_critical_is_blocked(self):
        doc = copy.deepcopy(BASE)
        doc["risk"]["level"] = "CRITICAL"
        self.assertIn("CRITICAL_BLOCK", self.findings(doc))

    def test_adapter_effect_enforced(self):
        doc = copy.deepcopy(BASE)
        doc["execution"]["effect_class"] = "GIT_WRITE"
        self.assertIn(
            "ADAPTER_EFFECT",
            self.findings(doc, {"rafaelmeloreisnovo/RafPolimata": adapter()}),
        )

    def test_adapter_overlap_rejected(self):
        value = adapter()
        value["forbidden_effects"].append("LOCAL_WRITE")
        self.assertIn("ADAPTER_EFFECT_OVERLAP", {finding.code for finding in validate_adapter(value)})

    def test_adapter_bad_pin_rejected(self):
        value = adapter()
        value["contract"]["commit_pin"] = "TOKEN_VAZIO"
        self.assertIn("ADAPTER_PIN", {finding.code for finding in validate_adapter(value)})

    def test_irreversible_requires_two_step(self):
        doc = copy.deepcopy(BASE)
        doc["execution"]["irreversible"] = True
        self.assertIn("IRREVERSIBLE", self.findings(doc))


if __name__ == "__main__":
    unittest.main()
