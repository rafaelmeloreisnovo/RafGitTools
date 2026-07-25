#!/usr/bin/env python3
"""Dependency-free structural gate for RafGitFS Prompt 2 / Room v6."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

EXPECTED_TABLES = {
    "storage_profiles",
    "repository_refs",
    "virtual_tree_entries",
    "content_cache",
    "workspaces",
    "transfer_jobs",
    "staged_operations",
    "sync_conflicts",
    "operation_receipts",
}

EXPECTED_DAO_ACCESSORS = {
    "storageProfileDao",
    "repositoryRefDao",
    "virtualTreeDao",
    "contentCacheDao",
    "workspaceDao",
    "transferJobDao",
    "stagedOperationDao",
    "syncConflictDao",
    "operationReceiptDao",
}

FILES = (
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsEntities.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsRoomV6.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsCatalogDaos.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsCacheDao.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsOperationDaos.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsCacheMaintenance.kt",
    "app/src/main/kotlin/com/rafgittools/data/cache/CacheDatabase.kt",
    "app/src/main/kotlin/com/rafgittools/di/AppModule.kt",
)

FORBIDDEN_PERSISTED_FIELD_FRAGMENTS = {
    "token",
    "password",
    "secret",
    "credential",
    "privatekey",
    "accesstoken",
    "refreshtoken",
}


class ValidationError(ValueError):
    """Raised when a Room v6 invariant is violated."""


def read(root: Path, relative: str) -> str:
    path = root / relative
    if not path.is_file():
        raise ValidationError(f"missing file: {relative}")
    return path.read_text(encoding="utf-8")


def digest(root: Path, relatives: tuple[str, ...]) -> str:
    hasher = hashlib.sha256()
    for relative in sorted(relatives):
        path = root / relative
        hasher.update(relative.encode("utf-8"))
        hasher.update(b"\0")
        hasher.update(path.read_bytes())
        hasher.update(b"\0")
    return hasher.hexdigest()


def entity_tables(source: str) -> set[str]:
    return set(re.findall(r'tableName\s*=\s*"([a-z0-9_]+)"', source))


def migration_tables(source: str) -> set[str]:
    return set(
        re.findall(
            r"CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+`?([a-z0-9_]+)`?",
            source,
            flags=re.IGNORECASE,
        )
    )


def persisted_property_names(source: str) -> set[str]:
    return set(re.findall(r"\bval\s+([A-Za-z][A-Za-z0-9_]*)\s*:", source))


def operation_receipt_dao_block(source: str) -> str:
    match = re.search(r"interface\s+OperationReceiptDao\s*\{(?P<body>.*?)\n\}", source, re.DOTALL)
    if not match:
        raise ValidationError("OperationReceiptDao not found")
    return match.group("body")


def validate(root: Path) -> dict[str, Any]:
    sources = {relative: read(root, relative) for relative in FILES}
    entities = sources[FILES[0]]
    migration = sources[FILES[1]]
    cache_dao = sources[FILES[3]]
    operation_daos = sources[FILES[4]]
    cache_database = sources[FILES[6]]
    app_module = sources[FILES[7]]

    if not re.search(r"version\s*=\s*6\b", cache_database):
        raise ValidationError("CacheDatabase must declare version = 6")
    if "MIGRATION_5_6" not in cache_database:
        raise ValidationError("CacheDatabase.MIGRATION_5_6 is required")
    if "RafGitFsRoomV6.createStatements.forEach" not in cache_database:
        raise ValidationError("migration must execute the canonical SQL statement list")
    if "CacheDatabase.MIGRATION_5_6" not in app_module:
        raise ValidationError("AppModule must register MIGRATION_5_6")

    entity_set = entity_tables(entities)
    migration_set = migration_tables(migration)
    if EXPECTED_TABLES - entity_set:
        raise ValidationError(f"missing Room entities: {sorted(EXPECTED_TABLES - entity_set)}")
    if EXPECTED_TABLES - migration_set:
        raise ValidationError(f"missing migration tables: {sorted(EXPECTED_TABLES - migration_set)}")
    if migration_set - EXPECTED_TABLES:
        raise ValidationError(f"unexpected migration tables: {sorted(migration_set - EXPECTED_TABLES)}")

    for accessor in EXPECTED_DAO_ACCESSORS:
        if f"fun {accessor}()" not in cache_database:
            raise ValidationError(f"CacheDatabase missing DAO accessor: {accessor}")
        provider_name = "provide" + accessor[0].upper() + accessor[1:]
        if provider_name not in app_module:
            raise ValidationError(f"AppModule missing DAO provider: {provider_name}")

    receipt_block = operation_receipt_dao_block(operation_daos)
    if re.search(r'@Query\("\s*(UPDATE|DELETE)', receipt_block, re.IGNORECASE):
        raise ValidationError("OperationReceiptDao must remain append-only")
    if "OnConflictStrategy.ABORT" not in receipt_block:
        raise ValidationError("receipt insertion must use ABORT conflict policy")

    if "pinned = 0" not in cache_dao:
        raise ValidationError("cache eviction must exclude pinned entries")
    if "deleteExpiredUnpinned" not in cache_dao:
        raise ValidationError("expired cache cleanup must be explicit and unpinned-only")
    if "cacheBudgetSatisfied" not in sources[FILES[5]]:
        raise ValidationError("cache maintenance must report whether the budget was satisfied")

    properties = {name.lower() for name in persisted_property_names(entities)}
    forbidden = sorted(
        name for name in properties
        if any(fragment in name for fragment in FORBIDDEN_PERSISTED_FIELD_FRAGMENTS)
    )
    if forbidden:
        raise ValidationError(f"secret-bearing persisted fields are prohibited: {forbidden}")

    for required in ("claimAllowed", "receiptRequired", "protectedBranchWrite", "deleteEnabled"):
        if required not in entities:
            raise ValidationError(f"storage policy field missing: {required}")

    if "idx_operation_receipts_request" not in migration or "CREATE UNIQUE INDEX" not in migration:
        raise ValidationError("requestId uniqueness index is required")
    if "idx_content_cache_lru" not in migration:
        raise ValidationError("content cache LRU index is required")
    if "ON DELETE CASCADE" not in migration:
        raise ValidationError("reconstructible profile-owned state requires cascade relationships")

    return {
        "status": "PASS",
        "database_version": 6,
        "migration": "5->6",
        "tables": len(EXPECTED_TABLES),
        "dao_accessors": len(EXPECTED_DAO_ACCESSORS),
        "receipt_append_only": True,
        "pinned_cache_evictable": False,
        "secrets_persisted": False,
        "claim_allowed": False,
        "sha256": digest(root, FILES),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--write-report", type=Path)
    args = parser.parse_args()

    try:
        report = validate(args.repo_root.resolve())
    except (OSError, ValidationError) as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc)}, ensure_ascii=False, indent=2))
        return 1

    output = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True)
    print(output)
    if args.write_report:
        args.write_report.parent.mkdir(parents=True, exist_ok=True)
        args.write_report.write_text(output + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
