from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate_rafgitfs_github_engine.py"
SPEC = importlib.util.spec_from_file_location("validate_rafgitfs_github_engine", SCRIPT)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class RafGitFsGithubEngineGateTest(unittest.TestCase):
    def test_canonical_engine_passes(self) -> None:
        report = MODULE.validate(ROOT)
        self.assertEqual("PASS", report["status"])
        self.assertEqual("3/8", report["prompt"])
        self.assertEqual(7, report["read_only_routes"])
        self.assertFalse(report["remote_write_enabled"])
        self.assertFalse(report["claim_allowed"])

    def test_mutation_endpoint_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[0],
            lambda value: value.replace(
                "interface RafGitFsGithubApiService {",
                'interface RafGitFsGithubApiService {\n    @retrofit2.http.POST("repos/x/y")\n    suspend fun mutate(): Unit',
            ),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_missing_tree_truncation_guard_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[5],
            lambda value: value.replace("GITHUB_TREE_TRUNCATED", "TREE_COMPLETE"),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_unbounded_blob_decoder_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[8],
            lambda value: value.replace("require(blob.size <= maxBytes)", "check(true)"),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_missing_incremental_sha_gate_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[6],
            lambda value: value.replace(
                "cachedRef?.gitSha == commit.sha",
                "cachedRef?.gitSha != commit.sha",
            ),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_missing_hilt_provider_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[10],
            lambda value: value.replace(
                "provideRafGitFsGithubApiService",
                "missingRafGitFsGithubApiService",
            ),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def mutated_root(self, relative: str, mutate):
        class TempRoot:
            def __init__(self) -> None:
                self.temp = tempfile.TemporaryDirectory()
                self.path = Path(self.temp.name)

            def __enter__(self) -> Path:
                for item in MODULE.FILES:
                    source = ROOT / item
                    target = self.path / item
                    target.parent.mkdir(parents=True, exist_ok=True)
                    text = source.read_text(encoding="utf-8")
                    if item == relative:
                        text = mutate(text)
                    target.write_text(text, encoding="utf-8")
                return self.path

            def __exit__(self, exc_type, exc, tb) -> None:
                self.temp.cleanup()

        return TempRoot()


if __name__ == "__main__":
    unittest.main()
