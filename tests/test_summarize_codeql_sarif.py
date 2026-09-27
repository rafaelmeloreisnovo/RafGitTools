from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "summarize_codeql_sarif.py"
SPEC = importlib.util.spec_from_file_location("summarize_codeql_sarif", SCRIPT)
assert SPEC and SPEC.loader
MOD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MOD)

class CodeQLSarifSummaryTests(unittest.TestCase):
    def test_groups_results_without_sensitive_fields(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "java.sarif"
            p.write_text(json.dumps({
                "runs": [{"results": [
                    {"ruleId": "java/x", "level": "warning", "message": {"text": "secret detail"}, "locations": [{"physicalLocation": {"artifactLocation": {"uri": "Sensitive.kt"}}}]},
                    {"ruleId": "java/x", "level": "warning"},
                    {"ruleId": "java/y", "level": "error"},
                ]}]
            }), encoding="utf-8")
            out = MOD.summarize([p])
        self.assertEqual(out["state"], "OBSERVED")
        self.assertEqual(out["result_count"], 3)
        self.assertEqual(out["rule_count"], 2)
        self.assertEqual(out["by_level"], {"error": 1, "warning": 2})
        self.assertEqual(out["by_rule"][0], {"rule_id": "java/x", "count": 2})
        serialized = json.dumps(out)
        self.assertNotIn("secret detail", serialized)
        self.assertNotIn("Sensitive.kt", serialized)

    def test_no_sarif_is_token_vazio_not_zero_evidence(self):
        out = MOD.summarize([])
        self.assertEqual(out["state"], "TOKEN_VAZIO_NO_SARIF")
        self.assertEqual(out["result_count"], 0)
        self.assertFalse(out["claim_allowed"])

    def test_parse_error_is_audit_state(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "bad.sarif"
            p.write_text("{bad", encoding="utf-8")
            out = MOD.summarize([p])
        self.assertEqual(out["state"], "AUDIT_PARSE_ERROR")
        self.assertTrue(out["parse_errors"])

if __name__ == "__main__":
    unittest.main()
