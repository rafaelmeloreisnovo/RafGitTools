from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "tools" / "document_completion_scan.py"
SPEC = importlib.util.spec_from_file_location("document_completion_scan", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class DocumentCompletionScanTests(unittest.TestCase):
    def test_manifest_is_non_destructive_and_records_duplicate(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            canonical = root / "docs" / "CANONICAL.md"
            source_root = root / "fazer"
            canonical.parent.mkdir(parents=True)
            source_root.mkdir(parents=True)
            canonical.write_text("# Canonical\n", encoding="utf-8")
            duplicate = source_root / "copy.md"
            duplicate.write_text("# Canonical\n", encoding="utf-8")

            before = canonical.read_bytes()
            manifest = MODULE.build_manifest(
                canonical=canonical,
                source_roots=[source_root],
                repository="owner/repo",
                ref="branch",
                repository_root=root,
            )

            self.assertEqual(before, canonical.read_bytes())
            self.assertEqual(manifest["schema"], "raf.document-completion.v1")
            self.assertFalse(manifest["result"]["claim_allowed"])
            self.assertEqual(len(manifest["candidates"]), 1)
            candidate = manifest["candidates"][0]
            self.assertEqual(candidate["classification"], "DUPLICATE")
            self.assertEqual(candidate["decision"], "REJECT_DUPLICATE")
            self.assertEqual(manifest["conflicts"][0]["kind"], "DUPLICATE")

    def test_unknown_and_historical_material_are_quarantined(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            canonical = root / "README.md"
            incoming = root / "_incoming"
            legacy = root / "fazer"
            incoming.mkdir()
            legacy.mkdir()
            canonical.write_text("canonical", encoding="utf-8")
            (incoming / "opaque.bin").write_bytes(b"\x00\x01\x02")
            (legacy / "notes.md").write_text("draft", encoding="utf-8")

            manifest = MODULE.build_manifest(
                canonical=canonical,
                source_roots=[incoming, legacy],
                repository="owner/repo",
                ref="branch",
                repository_root=root,
            )

            decisions = {item["source_path"]: item["decision"] for item in manifest["candidates"]}
            self.assertEqual(decisions["_incoming/opaque.bin"], "QUARANTINE")
            self.assertEqual(decisions["fazer/notes.md"], "QUARANTINE")
            self.assertEqual(manifest["result"]["status"], "BLOCKED")

    def test_code_and_evidence_are_reference_only_until_review(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            canonical = root / "README.md"
            sources = root / "sources"
            evidence = sources / "evidence"
            evidence.mkdir(parents=True)
            canonical.write_text("canonical", encoding="utf-8")
            (sources / "module.c").write_text("int x;\n", encoding="utf-8")
            (evidence / "result.json").write_text("{}\n", encoding="utf-8")

            manifest = MODULE.build_manifest(
                canonical=canonical,
                source_roots=[sources],
                repository="owner/repo",
                ref="branch",
                repository_root=root,
            )

            by_path = {item["source_path"]: item for item in manifest["candidates"]}
            self.assertEqual(by_path["sources/module.c"]["classification"], "CODE")
            self.assertEqual(by_path["sources/module.c"]["decision"], "REFERENCE_ONLY")
            self.assertEqual(by_path["sources/evidence/result.json"]["classification"], "EVIDENCE")
            self.assertEqual(by_path["sources/evidence/result.json"]["epistemic_status"], "REFERENCE")
            self.assertEqual(manifest["result"]["status"], "PARTIAL")


if __name__ == "__main__":
    unittest.main()
