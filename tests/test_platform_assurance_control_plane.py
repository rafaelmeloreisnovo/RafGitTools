from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VALIDATOR_PATH = ROOT / "scripts" / "platform_assurance_control_plane.py"
INDEX_PATH = ROOT / "configs" / "platform-assurance" / "index.json"

SPEC = importlib.util.spec_from_file_location("platform_assurance", VALIDATOR_PATH)
assert SPEC and SPEC.loader
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


def assembled() -> dict:
    return validator.load_bundle(INDEX_PATH)


class PlatformAssuranceControlPlaneTests(unittest.TestCase):
    def invalid(self, data: dict, message: str) -> None:
        with self.assertRaises(validator.ValidationError) as context:
            validator.validate(data)
        self.assertIn(message, str(context.exception))

    def test_canonical_bundle_valid(self) -> None:
        result = validator.validate(assembled())
        self.assertEqual("PASS", result["status"])
        self.assertEqual(12, result["work_item_count"])
        self.assertEqual(0, result["promotion_ready_count"])
        self.assertFalse(result["claim_allowed"])

    def test_claim_allowed_cannot_be_promoted(self) -> None:
        data = assembled()
        data["policy"]["claim_allowed"] = True
        self.invalid(data, "policy.claim_allowed")

    def test_duplicate_work_item_rejected(self) -> None:
        data = assembled()
        data["work_items"][1]["id"] = data["work_items"][0]["id"]
        self.invalid(data, "duplicate")

    def test_unknown_repository_rejected(self) -> None:
        data = assembled()
        data["work_items"][0]["repository"] = "unknown/repository"
        self.invalid(data, "unknown repository")

    def test_security_blocker_cannot_be_compensated(self) -> None:
        data = assembled()
        item = data["work_items"][1]
        item["promotion_ready"] = True
        item["dimensions"]["ci"] = "PASS"
        self.invalid(data, "promotion cannot compensate")

    def test_zero_step_ci_cannot_promote(self) -> None:
        data = assembled()
        item = data["work_items"][0]
        item["promotion_ready"] = True
        self.invalid(data, "observable CI")

    def test_merged_limited_requires_merge_sha(self) -> None:
        data = assembled()
        data["work_items"][0]["source_ref"] = "PR #289"
        self.invalid(data, "merge SHA required")

    def test_blocked_item_requires_exit_criteria(self) -> None:
        data = assembled()
        data["work_items"][1]["exit_criteria"] = []
        self.invalid(data, "required for BLOCKED")

    def test_dependency_cycle_rejected(self) -> None:
        data = assembled()
        data["work_items"][0]["blocked_by"] = ["WI-TERMUX-DEVICE-RECEIPT"]
        self.invalid(data, "cycle detected")

    def test_unknown_blocker_rejected(self) -> None:
        data = assembled()
        data["work_items"][0]["blocked_by"] = ["WI-NOT-FOUND"]
        self.invalid(data, "unknown")

    def test_dimension_set_is_exact(self) -> None:
        data = assembled()
        del data["work_items"][0]["dimensions"]["rights"]
        self.invalid(data, "dimensions: mismatch")

    def test_derived_counts_cannot_drift(self) -> None:
        data = assembled()
        data["derived"]["open_blocking_count"] += 1
        self.invalid(data, "derived.open_blocking_count")

    def test_file_digest_tampering_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            config_dir = root / "configs" / "platform-assurance"
            config_dir.mkdir(parents=True)
            source_dir = ROOT / "configs" / "platform-assurance"
            for source in source_dir.iterdir():
                (config_dir / source.name).write_bytes(source.read_bytes())
            repositories = config_dir / "repositories.json"
            repositories.write_text(
                repositories.read_text(encoding="utf-8") + " ",
                encoding="utf-8",
            )
            with self.assertRaises(validator.ValidationError) as context:
                validator.load_bundle(config_dir / "index.json")
            self.assertIn("file digest mismatch", str(context.exception))


if __name__ == "__main__":
    unittest.main()
