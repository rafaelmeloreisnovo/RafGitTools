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
SCRIPT = ROOT / "scripts" / "generate_runtime_manifest.sh"
LOCK = ROOT / "runtime-lock.json"
FIXED_RAFGITTOOLS_SHA = "a" * 40


class RuntimeManifestGeneratorTest(unittest.TestCase):
    def run_generator(
        self,
        build_dir: Path,
        out_file: Path,
        *,
        require_artifacts: bool = False,
    ) -> subprocess.CompletedProcess[str]:
        command = [
            "bash",
            str(SCRIPT),
            str(LOCK),
            str(build_dir),
            str(out_file),
        ]
        if require_artifacts:
            command.append("--require-artifacts")

        environment = os.environ.copy()
        environment["GITHUB_SHA"] = FIXED_RAFGITTOOLS_SHA
        environment["GITHUB_RUN_ID"] = "unit-test"
        return subprocess.run(
            command,
            cwd=ROOT,
            env=environment,
            text=True,
            capture_output=True,
            check=False,
        )

    def test_source_only_manifest_is_explicit_and_not_promoted(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            build_dir = root / "empty-build"
            build_dir.mkdir()
            out_file = root / "RUNTIME_MANIFEST.json"

            result = self.run_generator(build_dir, out_file)
            self.assertEqual(result.returncode, 0, result.stderr)

            manifest = json.loads(out_file.read_text(encoding="utf-8"))
            self.assertEqual(manifest["artifact_state"], "SOURCE_ONLY")
            self.assertEqual(manifest["artifact_verification_state"], "TOKEN_VAZIO")
            self.assertEqual(manifest["artifact_count"], 0)
            self.assertEqual(manifest["artifacts"], [])
            self.assertEqual(manifest["runtime_state"], "TOKEN_VAZIO_DEVICE_EXECUTION")
            self.assertFalse(manifest["claim_allowed"])
            self.assertEqual(
                manifest["components"]["rafgittools_commit"],
                FIXED_RAFGITTOOLS_SHA,
            )

            expected_lock_sha = hashlib.sha256(LOCK.read_bytes()).hexdigest()
            self.assertEqual(manifest["source_lock"]["sha256"], expected_lock_sha)

            sidecar = Path(f"{out_file}.sha256")
            self.assertTrue(sidecar.is_file())
            expected_manifest_sha = hashlib.sha256(out_file.read_bytes()).hexdigest()
            self.assertEqual(
                sidecar.read_text(encoding="utf-8").split()[0],
                expected_manifest_sha,
            )

    def test_require_artifacts_rejects_empty_build(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            build_dir = root / "empty-build"
            build_dir.mkdir()
            out_file = root / "RUNTIME_MANIFEST.json"

            result = self.run_generator(
                build_dir,
                out_file,
                require_artifacts=True,
            )
            self.assertEqual(result.returncode, 3)
            self.assertIn("promoção exige artefatos", result.stderr)
            self.assertFalse(out_file.exists())
            self.assertFalse(Path(f"{out_file}.sha256").exists())

    def test_artifact_paths_are_relative_and_hashes_are_recorded(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            build_dir = root / "build"
            artifact = build_dir / "arm64-v8a" / "librafaelia_fixture.so"
            artifact.parent.mkdir(parents=True)
            artifact.write_bytes(b"RAFAELIA-ELF-FIXTURE-V1")
            out_file = root / "RUNTIME_MANIFEST.json"

            result = self.run_generator(
                build_dir,
                out_file,
                require_artifacts=True,
            )
            self.assertEqual(result.returncode, 0, result.stderr)

            manifest = json.loads(out_file.read_text(encoding="utf-8"))
            self.assertEqual(manifest["artifact_state"], "ARTIFACTS_PRESENT")
            self.assertEqual(
                manifest["artifact_verification_state"],
                "HASHED_NOT_PROMOTED",
            )
            self.assertEqual(manifest["artifact_count"], 1)
            self.assertEqual(
                manifest["artifacts"][0]["path"],
                "arm64-v8a/librafaelia_fixture.so",
            )
            self.assertEqual(
                manifest["artifacts"][0]["sha256"],
                hashlib.sha256(artifact.read_bytes()).hexdigest(),
            )
            self.assertEqual(manifest["artifacts"][0]["size"], artifact.stat().st_size)
            self.assertFalse(manifest["claim_allowed"])

    def test_unknown_fourth_argument_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            build_dir = root / "build"
            build_dir.mkdir()
            out_file = root / "RUNTIME_MANIFEST.json"
            environment = os.environ.copy()
            environment["GITHUB_SHA"] = FIXED_RAFGITTOOLS_SHA

            result = subprocess.run(
                [
                    "bash",
                    str(SCRIPT),
                    str(LOCK),
                    str(build_dir),
                    str(out_file),
                    "--unsafe-mode",
                ],
                cwd=ROOT,
                env=environment,
                text=True,
                capture_output=True,
                check=False,
            )
            self.assertEqual(result.returncode, 2)
            self.assertIn("quarto argumento desconhecido", result.stderr)


if __name__ == "__main__":
    unittest.main()
