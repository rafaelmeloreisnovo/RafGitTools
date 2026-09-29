from __future__ import annotations

import json
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GATE = ROOT / "scripts" / "validate_rafaelia_signing_contract.sh"


class SigningPageContractTests(unittest.TestCase):
    def setUp(self):
        self.receipt = {
            "schema": "rafaelia.signed-release/v1",
            "source_sha": "TOKEN_VAZIO",
            "signed_apk_sha256": "TOKEN_VAZIO",
            "certificate_sha256": "TOKEN_VAZIO",
            "signer_id": "TOKEN_VAZIO",
            "certificate_match": "TOKEN_VAZIO",
            "state": "AWAITING_REAL_SIGNING_RECEIPT",
        }
        self.variables = {
            "schema": "rafaelia.signing-variables/v1",
            "public_variables": {
                "RAFAELIA_SIGNER_ID": {"state": "TOKEN_VAZIO"},
                "RAFAELIA_EXPECTED_CERT_SHA256": {"state": "TOKEN_VAZIO"},
                "RAFAELIA_ANDROID_KEY_ALIAS": {"state": "TOKEN_VAZIO"},
                "RAFAELIA_PAGES_REPO": {
                    "recommended_value": "rafaelmeloreisnovo/RafGitTools"
                },
            },
            "secrets": {
                "RAFAELIA_ANDROID_KEYSTORE_B64": "PRIVATE; never publish",
                "RAFAELIA_ANDROID_STORE_PASSWORD": "PRIVATE; never publish",
                "RAFAELIA_ANDROID_KEY_PASSWORD": "PRIVATE; never publish",
                "RAFAELIA_PAGES_PAT": "PRIVATE; cross-repository receipt publishing only",
            },
        }

    def run_gate(self, receipt=None, variables=None, json_text=None,
                 text=None, extra_page_file=None):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp) / "page"
            root.mkdir()
            receipt = self.receipt if receipt is None else receipt
            variables = self.variables if variables is None else variables
            serialized = json.dumps(receipt)
            (root / "latest.json").write_text(
                serialized if json_text is None else json_text, encoding="utf-8"
            )
            rendered = "\n".join(
                f"{key}={value}" for key, value in receipt.items()
            ) + "\nprivate_key_material=NEVER_PUBLISH\n"
            (root / "latest.txt").write_text(
                rendered if text is None else text, encoding="utf-8"
            )
            if extra_page_file is not None:
                (root / "extra.txt").write_text(extra_page_file, encoding="utf-8")
            vars_path = Path(temp) / "variables.json"
            vars_path.write_text(json.dumps(variables), encoding="utf-8")
            return subprocess.run(
                ["bash", str(GATE), str(root), str(vars_path)],
                cwd=ROOT, text=True, capture_output=True, check=False
            )

    def test_current_unbound_receipt_passes(self):
        result = self.run_gate()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("RAFAELIA_SIGNING_PAGE_CONTRACT=PASS", result.stdout)

    def test_malformed_json_fails(self):
        result = self.run_gate(json_text='{"schema": ')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("SIGNING_PAGE_JSON_INVALID", result.stderr)

    def test_unknown_schema_fails(self):
        receipt = dict(self.receipt, schema="rafaelia.signed-release/v0")
        result = self.run_gate(receipt=receipt)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("SIGNING_PAGE_SCHEMA_INVALID", result.stderr)

    def test_unbound_values_cannot_be_promoted(self):
        receipt = dict(self.receipt, state="SIGNED_RELEASE_VERIFIED")
        result = self.run_gate(receipt=receipt)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("SIGNING_PAGE_STATE_UNSUPPORTED", result.stderr)

    def test_text_and_json_must_match(self):
        result = self.run_gate(
            text="schema=rafaelia.signed-release/v1\nprivate_key_material=NEVER_PUBLISH\n"
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("SIGNING_PAGE_TEXT_FIELDS_INVALID", result.stderr)

    def test_private_key_material_is_rejected(self):
        result = self.run_gate(
            extra_page_file="-----BEGIN PRIVATE KEY-----\nsynthetic-test-only"
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("PRIVATE_KEY_MATERIAL_DETECTED_IN_PAGES", result.stdout)

    def test_secret_values_must_remain_placeholders(self):
        variables = json.loads(json.dumps(self.variables))
        variables["secrets"]["RAFAELIA_PAGES_PAT"] = "synthetic-secret-value"
        result = self.run_gate(variables=variables)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("SIGNING_SECRET_VALUE_MUST_NOT_BE_PUBLISHED", result.stderr)


if __name__ == "__main__":
    unittest.main()
