#!/usr/bin/env python3
"""Dependency-free structural gate for RafGitFS Prompt 5."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

FILES = (
    "app/src/main/kotlin/com/rafgittools/rafgitfs/cache/RafGitFsCacheModels.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/cache/RafGitFsChecksums.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/cache/RafGitFsCacheKeys.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/cache/RafGitFsAtomicFileStore.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/cache/RafGitFsCacheMaintenance.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/cache/RafGitFsOfflineCacheManager.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/cache/RafGitFsOfflineQueue.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsCacheDao.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsCatalogDaos.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsOperationDaos.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/VirtualFileViewerViewModel.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/VirtualFileViewerScreen.kt",
    ".github/workflows/rafgitfs-room-v6-validation.yml",
)

STATES = {
    "REMOTE_ONLY", "METADATA_CACHED", "PARTIAL", "CONTENT_CACHED",
    "PINNED_OFFLINE", "STALE", "CORRUPTED",
}


class ValidationError(ValueError):
    pass


def read(root: Path, relative: str) -> str:
    path = root / relative
    if not path.is_file():
        raise ValidationError(f"missing file: {relative}")
    return path.read_text(encoding="utf-8")


def digest(root: Path) -> str:
    h = hashlib.sha256()
    for relative in sorted(FILES):
        h.update(relative.encode())
        h.update(b"\0")
        h.update((root / relative).read_bytes())
        h.update(b"\0")
    return h.hexdigest()


def validate(root: Path) -> dict[str, Any]:
    source = {path: read(root, path) for path in FILES}
    models, checksums, keys, store, maintenance, manager, queue = [source[p] for p in FILES[:7]]
    cache_dao, tree_dao, job_dao = source[FILES[7]], source[FILES[8]], source[FILES[9]]
    viewer_vm, viewer, workflow = source[FILES[10]], source[FILES[11]], source[FILES[12]]
    all_cache = "\n".join(source[p] for p in FILES[:12])

    missing_states = sorted(state for state in STATES if state not in models)
    if missing_states:
        raise ValidationError(f"cache states missing: {missing_states}")

    for marker in ('"blob ${bytes.size}\\u0000"', '"SHA-1"', '"SHA-256"', "MessageDigest.isEqual"):
        if marker not in checksums:
            raise ValidationError(f"checksum invariant missing: {marker}")

    if "context.filesDir" not in store or ".part" not in store or "output.fd.sync()" not in store:
        raise ValidationError("private atomic file-store markers missing")
    if any(marker in store for marker in ("getExternalStorage", "Environment.getExternal", "WRITE_EXTERNAL_STORAGE")):
        raise ValidationError("cache store must not use shared external storage")
    if "canonicalPath" not in store or "CACHE_PATH_ESCAPE" not in store:
        raise ValidationError("path traversal defense missing")

    if "entry.pinned" not in maintenance or "evictionCandidates" not in maintenance:
        raise ValidationError("pinned-safe LRU missing")
    if "ensureCapacity" not in maintenance or "maxCacheBytes" not in maintenance:
        raise ValidationError("storage budget gate missing")

    for marker in (
        "verifyGitBlob", "writeAtomic", "CACHE_BUDGET_EXHAUSTED", "OFFLINE_CACHE_MISS",
        "markOlderGenerationsStale", "PINNED_ENTRY_REQUIRES_UNPIN",
    ):
        if marker not in manager:
            raise ValidationError(f"cache manager invariant missing: {marker}")
    if "partialValue = null" not in manager:
        raise ValidationError("partial remote content must not be persisted")

    for marker in ("CACHE_DOWNLOAD", "PIN_OFFLINE", "QUEUED_OFFLINE", "WAITING_RETRY", "claimAllowed = false"):
        if marker not in queue:
            raise ValidationError(f"offline queue marker missing: {marker}")
    if "listResumableCacheJobs" not in job_dao:
        raise ValidationError("persistent resume query missing")

    for marker in ("getByIdentity", "listForPath", "pinned = 0", "setPinned", "totalBytes"):
        if marker not in cache_dao:
            raise ValidationError(f"cache DAO marker missing: {marker}")
    if "setCacheState" not in tree_dao:
        raise ValidationError("tree cache-state projection missing")

    for marker in ("loadOfflineOnly", "pinOffline", "enqueueOfflinePin", "resumeOfflineQueue"):
        if marker not in viewer_vm + viewer:
            raise ValidationError(f"viewer offline action missing: {marker}")

    forbidden_remote_mutation = re.compile(
        r"@(POST|PUT|PATCH|DELETE)|createPullRequest\s*\(|\bpush\s*\(|\bcommit\s*\(",
        re.IGNORECASE,
    )
    if forbidden_remote_mutation.search(all_cache):
        raise ValidationError("Prompt 5 introduced a remote mutation capability")
    if re.search(r"claimAllowed\s*=\s*true", all_cache):
        raise ValidationError("claim promotion must remain blocked")

    for marker in (
        "validate_rafgitfs_cache_offline.py",
        "test_validate_rafgitfs_cache_offline.py",
        "RafGitFsCacheCoreTest",
    ):
        if marker not in workflow:
            raise ValidationError(f"workflow gate missing: {marker}")

    return {
        "status": "PASS",
        "prompt": "5/8",
        "cache_states": sorted(STATES),
        "private_internal_storage": True,
        "atomic_write": True,
        "git_blob_verified": True,
        "sha256_local_verified": True,
        "pinned_safe_lru": True,
        "persistent_manual_resume_queue": True,
        "http_range_resume_claimed": False,
        "remote_write_enabled": False,
        "claim_allowed": False,
        "sha256": digest(root),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--write-report", type=Path)
    args = parser.parse_args()
    try:
        report = validate(args.repo_root.resolve())
    except (OSError, ValidationError) as error:
        print(json.dumps({"status": "FAIL", "error": str(error)}, indent=2, ensure_ascii=False))
        return 1
    output = json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False)
    print(output)
    if args.write_report:
        args.write_report.parent.mkdir(parents=True, exist_ok=True)
        args.write_report.write_text(output + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
