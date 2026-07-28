import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "preflight", ROOT / "scripts/validate_contextual_execution_request.py"
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class ContextualExecutionPreflightTest(unittest.TestCase):
    def request(self):
        return json.loads(
            (ROOT / "examples/contextual-execution-request.wine-formula.json")
            .read_text(encoding="utf-8")
        )

    def test_valid_contract_can_be_blocked(self):
        result = MODULE.validate_request(self.request())
        self.assertEqual("PASS", result["contract_status"])
        self.assertEqual("BLOCKED", result["decision"])
        self.assertFalse(result["claim_allowed"])

    def test_tool_availability_does_not_override_semantic_gate(self):
        result = MODULE.validate_request(self.request())
        self.assertIn("semantic_gate_closed", result["reasons"])

    def test_mutation_needs_human_review(self):
        request = self.request()
        request["semantic_gate"] = {"answer_allowed": True, "blocking_gaps": []}
        for source in request["sources"]:
            source["observed"] = True
        result = MODULE.validate_request(request)
        self.assertEqual("BLOCKED", result["decision"])
        self.assertIn("human_review_not_approved", result["reasons"])

    def test_authorized_request_is_still_not_execution(self):
        request = self.request()
        request["semantic_gate"] = {"answer_allowed": True, "blocking_gaps": []}
        for source in request["sources"]:
            source["observed"] = True
        request["authorization"]["human_review_status"] = "approved"
        result = MODULE.validate_request(request)
        self.assertEqual("AUTHORIZED", result["decision"])
        self.assertEqual("authorized_not_yet_executed", result["safe_state"])
        self.assertFalse(result["claim_allowed"])

    def test_direct_default_branch_write_is_blocked(self):
        request = self.request()
        request["semantic_gate"] = {"answer_allowed": True, "blocking_gaps": []}
        for source in request["sources"]:
            source["observed"] = True
        request["authorization"]["human_review_status"] = "approved"
        request["rollback"]["direct_default_branch_write"] = True
        result = MODULE.validate_request(request)
        self.assertEqual("BLOCKED", result["decision"])
        self.assertIn("default_branch_write_not_denied", result["reasons"])

    def test_invalid_hash_is_rejected(self):
        request = self.request()
        request["packet_sha256"] = "bad"
        with self.assertRaises(MODULE.PreflightError):
            MODULE.validate_request(request)


if __name__ == "__main__":
    unittest.main()
