from __future__ import annotations

import hashlib
import importlib.util
import os
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "private_ci_bridge.py"
SPEC = importlib.util.spec_from_file_location("private_ci_bridge", MODULE_PATH)
assert SPEC and SPEC.loader
bridge = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(bridge)

SHA = "a" * 40


def registry() -> dict:
    return {
        "schema": bridge.REGISTRY_SCHEMA,
        "secret_reference": "PAT_ACTIONS",
        "secret_value_persisted": False,
        "claim_allowed": False,
        "targets": [
            {
                "target_id": "private-core",
                "repository": "owner/private-core",
                "manifest_path": ".rafaelia/private-ci/manifest.v1.json",
                "allowed_workflow_ids": ["unit"],
                "allowed_executables": ["python3"],
            }
        ],
    }


def manifest(step: dict | None = None) -> dict:
    return {
        "schema": bridge.MANIFEST_SCHEMA,
        "target_id": "private-core",
        "repository": "owner/private-core",
        "claim_allowed": False,
        "workflows": [
            {
                "workflow_id": "unit",
                "source_yaml": ".github/workflows/unit.yml",
                "steps": [
                    step
                    or {
                        "id": "run",
                        "argv": ["python3", "emit.py"],
                        "cwd": ".",
                        "timeout_seconds": 30,
                        "env": {},
                    }
                ],
                "artifact_hash_paths": [],
            }
        ],
    }


class PrivateCiBridgeTests(unittest.TestCase):
    def test_registry_rejects_secret_bearing_field_names(self):
        doc = registry()
        doc["token_value"] = False
        with self.assertRaisesRegex(ValueError, "forbidden secret-bearing key"):
            bridge.validate_registry(doc)

    def test_resolve_requires_exact_sha_and_allowlisted_workflow(self):
        with self.assertRaisesRegex(ValueError, "exact lowercase 40-hex"):
            bridge.resolve_public(
                registry(),
                target_id="private-core",
                workflow_id="unit",
                commit="main",
            )
        out = bridge.resolve_public(
            registry(),
            target_id="private-core",
            workflow_id="unit",
            commit=SHA,
        )
        self.assertEqual(out["repository"], "owner/private-core")
        self.assertFalse(out["claim_allowed"])

    def test_manifest_rejects_path_escape(self):
        doc = manifest()
        doc["workflows"][0]["source_yaml"] = "../escape.yml"
        with self.assertRaisesRegex(ValueError, "source_yaml"):
            bridge.validate_manifest(
                doc,
                expected_target_id="private-core",
                expected_repository="owner/private-core",
                allowed_workflow_ids={"unit"},
                allowed_executables={"python3"},
            )

    def test_manifest_rejects_secret_like_step_env_name(self):
        doc = manifest(
            {
                "id": "run",
                "argv": ["python3", "emit.py"],
                "cwd": ".",
                "timeout_seconds": 30,
                "env": {"PAT_ACTIONS": ""},
            }
        )
        with self.assertRaisesRegex(ValueError, "secret-like env key"):
            bridge.validate_manifest(
                doc,
                expected_target_id="private-core",
                expected_repository="owner/private-core",
                allowed_workflow_ids={"unit"},
                allowed_executables={"python3"},
            )

    def test_execution_hashes_private_output_without_persisting_it(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            (root / ".github/workflows").mkdir(parents=True)
            (root / ".github/workflows/unit.yml").write_text(
                "name: unit\n", encoding="utf-8"
            )
            (root / "emit.py").write_text(
                "print('PRIVATE_PAYLOAD_SHOULD_NOT_APPEAR')\n",
                encoding="utf-8",
            )
            receipt = root / "receipt.json"
            clean_env = {
                k: v
                for k, v in os.environ.items()
                if k not in bridge.FORBIDDEN_SECRET_NAMES
            }
            with mock.patch.dict(os.environ, clean_env, clear=True):
                out = bridge.execute_plan(
                    registry=registry(),
                    manifest=manifest(),
                    target_id="private-core",
                    workflow_id="unit",
                    commit=SHA,
                    source_root=root,
                    receipt_path=receipt,
                )
            self.assertEqual(out["result"], "PASS")
            raw = receipt.read_text(encoding="utf-8")
            self.assertNotIn("PRIVATE_PAYLOAD_SHOULD_NOT_APPEAR", raw)
            expected = hashlib.sha256(
                b"PRIVATE_PAYLOAD_SHOULD_NOT_APPEAR\n"
            ).hexdigest()
            self.assertEqual(out["steps"][0]["stdout_sha256"], expected)
            self.assertFalse(
                out["privacy_boundary"]["raw_private_stdout_persisted"]
            )
            self.assertFalse(out["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
