#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "termux_runtime_handoff.sh"
LOCK = ROOT / "runtime-lock.json"
FIXED_RAFGITTOOLS_SHA = "b" * 40


class TermuxRuntimeHandoffTest(unittest.TestCase):
    def run_handoff(
        self,
        build_dir: Path,
        out_dir: Path,
        *,
        require_artifacts: bool = False,
    ) -> subprocess.CompletedProcess[str]:
        command = [
            "bash",
            str(SCRIPT),
            str(LOCK),
            str(build_dir),
            str(out_dir),
        ]
        if require_artifacts:
            command.append("--require-artifacts")

        environment = os.environ.copy()
        environment["GITHUB_SHA"] = FIXED_RAFGITTOOLS_SHA
        environment["GITHUB_RUN_ID"] = "handoff-unit-test"
        environment.pop("PREFIX", None)
        return subprocess.run(
            command,
            cwd=ROOT,
            env=environment,
            text=True,
            capture_output=True,
            check=False,
        )

    @staticmethod
    def only_file(out_dir: Path, pattern: str) -> Path:
        matches = sorted(out_dir.glob(pattern))
        if len(matches) != 1:
            raise AssertionError(f"expected one {pattern}, found {matches}")
        return matches[0]

    def test_source_only_handoff_writes_verified_receipts(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            build_dir = root / "empty-build"
            out_dir = root / "receipts"
            build_dir.mkdir()

            result = self.run_handoff(build_dir, out_dir)
            self.assertEqual(result.returncode, 0, result.stderr)

            manifest_path = self.only_file(out_dir, "RUNTIME_MANIFEST_*.json")
            receipt_path = self.only_file(out_dir, "TERMUX_RUNTIME_HANDOFF_*.json")
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            receipt = json.loads(receipt_path.read_text(encoding="utf-8"))

            self.assertEqual(manifest["artifact_state"], "SOURCE_ONLY")
            self.assertEqual(receipt["runtime_manifest"]["artifact_state"], "SOURCE_ONLY")
            self.assertEqual(receipt["runtime_manifest"]["artifact_count"], 0)
            self.assertEqual(receipt["checks"]["lock_contract"], "PASS")
            self.assertEqual(receipt["checks"]["manifest_generated"], "PASS")
            self.assertEqual(
                receipt["checks"]["shared_library_execution"],
                "TOKEN_VAZIO_NOT_EXECUTED",
            )
            self.assertFalse(receipt["claim_allowed"])

            for path in (manifest_path, receipt_path):
                sidecar = Path(f"{path}.sha256")
                self.assertTrue(sidecar.is_file())
                self.assertEqual(
                    sidecar.read_text(encoding="utf-8").split()[0],
                    hashlib.sha256(path.read_bytes()).hexdigest(),
                )

    def test_require_artifacts_propagates_fail_closed_exit(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            build_dir = root / "empty-build"
            out_dir = root / "receipts"
            build_dir.mkdir()

            result = self.run_handoff(
                build_dir,
                out_dir,
                require_artifacts=True,
            )
            self.assertEqual(result.returncode, 3)
            self.assertIn("promoção exige artefatos", result.stderr)
            self.assertEqual(list(out_dir.glob("TERMUX_RUNTIME_HANDOFF_*.json")), [])

    def test_artifact_handoff_records_hash_without_execution_claim(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            build_dir = root / "build"
            out_dir = root / "receipts"
            artifact = build_dir / "armeabi-v7a" / "libraf_fixture.so"
            artifact.parent.mkdir(parents=True)
            artifact.write_bytes(b"RAFAELIA-TERMUX-HANDOFF-FIXTURE")

            result = self.run_handoff(
                build_dir,
                out_dir,
                require_artifacts=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)

            receipt_path = self.only_file(out_dir, "TERMUX_RUNTIME_HANDOFF_*.json")
            receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
            self.assertEqual(
                receipt["runtime_manifest"]["artifact_state"],
                "ARTIFACTS_PRESENT",
            )
            self.assertEqual(receipt["runtime_manifest"]["artifact_count"], 1)
            self.assertEqual(
                receipt["checks"]["device_runtime_smoke"],
                "TOKEN_VAZIO_NOT_EXECUTED",
            )
            self.assertFalse(receipt["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
