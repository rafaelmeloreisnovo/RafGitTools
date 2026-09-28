from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "import_master_coverage.py"
SPEC = importlib.util.spec_from_file_location("import_master_coverage", MODULE_PATH)
assert SPEC and SPEC.loader
mod = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(mod)

SHA = "1" * 40
REPO = "rafaelmeloreisnovo/Rafaelia_Private"


class MasterCoverageImportTests(unittest.TestCase):
    def write_json(self, root: Path, rel: str, payload: dict) -> None:
        path = root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    def build_packet(self, root: Path) -> None:
        ws = "MC-W01"
        self.write_json(
            root,
            mod.REQUIRED_FILES["registry"],
            {
                "schema": "RAFAELIA_MASTER_WORKSTREAM_REGISTRY_V1",
                "workstreams": [{"id": ws, "name": "demo"}],
            },
        )
        self.write_json(
            root,
            mod.REQUIRED_FILES["obligations"],
            {
                "schema": "RAFAELIA_DOCUMENT_OBLIGATION_MATRIX_V1",
                "workstream_profiles": {ws: []},
            },
        )
        self.write_json(
            root,
            mod.REQUIRED_FILES["coverage"],
            {
                "schema": "RAFAELIA_DOCUMENT_COVERAGE_SNAPSHOT_V1",
                "document_state_counts": {"CURRENT": 1, "TOKEN_VAZIO": 2},
                "workstreams": [{"workstream_id": ws}],
            },
        )
        self.write_json(
            root,
            mod.REQUIRED_FILES["backlinks"],
            {
                "schema": "RAFAELIA_DOCUMENT_BACKLINK_GRAPH_V1",
                "nodes": [{"id": ws, "type": "WORKSTREAM"}],
                "edges": [],
            },
        )
        classified = [
            mod.REQUIRED_FILES["registry"],
            mod.REQUIRED_FILES["obligations"],
            mod.REQUIRED_FILES["coverage"],
            mod.REQUIRED_FILES["backlinks"],
            mod.REQUIRED_FILES["ledger"],
        ]
        self.write_json(
            root,
            mod.REQUIRED_FILES["authority"],
            {
                "schema": "RAFAELIA_DOCUMENT_AUTHORITY_REGISTRY_V2",
                "documents": [{"path": path, "state": "CURRENT"} for path in classified],
            },
        )
        self.write_json(
            root,
            mod.REQUIRED_FILES["ledger"],
            {
                "schema": "rafaelia.active-work-ledger.v2",
                "workstreams": [{"id": ws, "name": "demo"}],
                "waves": [{"id": 0, "workstreams": [ws]}],
            },
        )

    def test_valid_packet_passes_and_is_read_only(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self.build_packet(root)
            receipt = mod.validate_packet(root, source_repo=REPO, source_sha=SHA)
            self.assertEqual(receipt["state"], "PASS")
            self.assertTrue(receipt["read_only"])
            self.assertEqual(receipt["network_io"], "NONE")
            self.assertFalse(receipt["mutation_performed"])
            self.assertFalse(receipt["credentials_read"])
            self.assertFalse(receipt["claim_allowed"])
            self.assertEqual(receipt["summary"]["workstream_count"], 1)
            self.assertEqual(len(receipt["files"]), 6)
            for item in receipt["files"]:
                self.assertRegex(item["sha256"], r"^[0-9a-f]{64}$")

    def test_ledger_registry_mismatch_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self.build_packet(root)
            self.write_json(
                root,
                mod.REQUIRED_FILES["ledger"],
                {
                    "schema": "rafaelia.active-work-ledger.v2",
                    "workstreams": [{"id": "MC-W99", "name": "wrong"}],
                    "waves": [{"id": 0, "workstreams": ["MC-W99"]}],
                },
            )
            receipt = mod.validate_packet(root, source_repo=REPO, source_sha=SHA)
            self.assertEqual(receipt["state"], "FAIL")
            self.assertTrue(any("ledger workstream IDs" in e for e in receipt["errors"]))

    def test_missing_required_file_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self.build_packet(root)
            (root / mod.REQUIRED_FILES["coverage"]).unlink()
            receipt = mod.validate_packet(root, source_repo=REPO, source_sha=SHA)
            self.assertEqual(receipt["state"], "FAIL")
            self.assertTrue(any("missing required file" in e for e in receipt["errors"]))

    def test_source_sha_must_be_exact_lowercase_40_hex(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self.build_packet(root)
            with self.assertRaises(ValueError):
                mod.validate_packet(root, source_repo=REPO, source_sha="main")

    def test_source_repository_must_be_owner_name(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            self.build_packet(root)
            with self.assertRaises(ValueError):
                mod.validate_packet(root, source_repo="Rafaelia_Private", source_sha=SHA)


if __name__ == "__main__":
    unittest.main()
