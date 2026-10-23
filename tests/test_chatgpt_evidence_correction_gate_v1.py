import copy
import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate_chatgpt_evidence_correction_gate_v1.py"
spec = importlib.util.spec_from_file_location("chatgpt_correction_gate", SCRIPT)
mod = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(mod)

CONTRACT = json.loads((ROOT / "contracts/chatgpt_evidence_correction_gate.v1.json").read_text(encoding="utf-8"))

EVIDENCE = {
    "schema": "rafaelia.chatgpt-operational-evidence/v1",
    "source": {
        "source_surface": "ChatGPT",
        "training_use": False,
        "dataset_use": False,
        "operational_evidence_use": True,
        "instruction_sha256_utf8": "a" * 64,
    },
    "claims": {"allowed": False},
}

REQUEST = {
    "validated_operational_evidence": True,
    "explicit_target": {"repository": "owner/repo", "path": "src/example.py"},
    "exact_source_revision": "b" * 40,
    "deterministic_reproducer_or_falsifier": {"id": "F-001", "command": "python -m unittest"},
    "root_cause_evidence": {"state": "REPRODUCED"},
    "bounded_patch": {"work_branch_only": True, "direct_main_write": False, "force_push": False, "auto_merge": False},
    "rollback_condition": "revert bounded patch if falsifier/regression fails",
    "pre_patch_receipt": "receipt:pre",
    "required_post_patch_tests": ["F-001", "regression-suite"],
    "non_regression_invariants": ["SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM"],
}


class ChatGPTEvidenceCorrectionGateTests(unittest.TestCase):
    def test_complete_request_is_eligible(self):
        result = mod.evaluate(EVIDENCE, REQUEST, CONTRACT)
        self.assertEqual(result["state"], "AUTO_CORRECT_ELIGIBLE")
        self.assertTrue(result["eligible"])
        self.assertFalse(result["claim_allowed"])

    def test_training_use_is_blocked(self):
        evidence = copy.deepcopy(EVIDENCE)
        evidence["source"]["training_use"] = True
        result = mod.evaluate(evidence, REQUEST, CONTRACT)
        self.assertFalse(result["eligible"])
        self.assertIn("training_use must be false", result["errors"])

    def test_missing_root_cause_is_blocked(self):
        request = copy.deepcopy(REQUEST)
        request["root_cause_evidence"] = {}
        result = mod.evaluate(EVIDENCE, request, CONTRACT)
        self.assertFalse(result["eligible"])
        self.assertTrue(any("root_cause_evidence" in e for e in result["errors"]))

    def test_direct_main_is_blocked(self):
        request = copy.deepcopy(REQUEST)
        request["bounded_patch"]["direct_main_write"] = True
        result = mod.evaluate(EVIDENCE, request, CONTRACT)
        self.assertFalse(result["eligible"])
        self.assertIn("direct main write forbidden", result["errors"])

    def test_auto_merge_is_blocked(self):
        request = copy.deepcopy(REQUEST)
        request["bounded_patch"]["auto_merge"] = True
        result = mod.evaluate(EVIDENCE, request, CONTRACT)
        self.assertFalse(result["eligible"])
        self.assertIn("auto merge forbidden", result["errors"])


if __name__ == "__main__":
    unittest.main()
