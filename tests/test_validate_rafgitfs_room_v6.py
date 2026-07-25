from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate_rafgitfs_room_v6.py"
SPEC = importlib.util.spec_from_file_location("validate_rafgitfs_room_v6", SCRIPT)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class RafGitFsRoomV6GateTest(unittest.TestCase):
    def test_canonical_room_v6_passes(self) -> None:
        report = MODULE.validate(ROOT)
        self.assertEqual("PASS", report["status"])
        self.assertEqual(6, report["database_version"])
        self.assertEqual(9, report["tables"])
        self.assertEqual(9, report["dao_accessors"])
        self.assertTrue(report["receipt_append_only"])
        self.assertFalse(report["pinned_cache_evictable"])
        self.assertFalse(report["secrets_persisted"])
        self.assertFalse(report["claim_allowed"])

    def test_database_version_regression_is_rejected(self) -> None:
        with self.mutated_root(
            "app/src/main/kotlin/com/rafgittools/data/cache/CacheDatabase.kt",
            lambda value: value.replace("version = 6", "version = 5"),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_missing_migration_table_is_rejected(self) -> None:
        with self.mutated_root(
            "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsRoomV6.kt",
            lambda value: value.replace(
                "CREATE TABLE IF NOT EXISTS `sync_conflicts`",
                "CREATE VIEW IF NOT EXISTS `sync_conflicts`",
            ),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_receipt_update_api_is_rejected(self) -> None:
        with self.mutated_root(
            "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsOperationDaos.kt",
            lambda value: value.replace(
                "interface OperationReceiptDao {",
                'interface OperationReceiptDao {\n    @Query("UPDATE operation_receipts SET result = \'X\'")\n    suspend fun mutate(): Int',
            ),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_pinned_cache_eviction_is_rejected(self) -> None:
        with self.mutated_root(
            "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsCacheDao.kt",
            lambda value: value.replace(" AND pinned = 0", ""),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_secret_bearing_room_field_is_rejected(self) -> None:
        with self.mutated_root(
            "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsEntities.kt",
            lambda value: value.replace(
                "val updatedAt: Long\n)",
                "val updatedAt: Long,\n    val accessToken: String\n)",
                1,
            ),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_unregistered_hilt_migration_is_rejected(self) -> None:
        with self.mutated_root(
            "app/src/main/kotlin/com/rafgittools/di/AppModule.kt",
            lambda value: value.replace(",\n                CacheDatabase.MIGRATION_5_6", ""),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_missing_dao_provider_is_rejected(self) -> None:
        with self.mutated_root(
            "app/src/main/kotlin/com/rafgittools/di/AppModule.kt",
            lambda value: value.replace("fun provideOperationReceiptDao", "fun missingOperationReceiptDao"),
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
