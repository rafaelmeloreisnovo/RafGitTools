#!/usr/bin/env python3
"""Dependency-free structural gate for RafGitFS Prompt 3."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

FILES = (
    "app/src/main/kotlin/com/rafgittools/rafgitfs/remote/RafGitFsGithubApiService.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/remote/RafGitFsGithubDtos.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/remote/RafGitFsRemoteResult.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/remote/RafGitFsPagination.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/remote/RafGitFsRateLimit.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/remote/RafGitFsGithubRemoteDataSource.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/index/RafGitFsGithubIndexer.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/index/RafGitFsTreeMapper.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/index/RafGitFsContentDecoder.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsCatalogDaos.kt",
    "app/src/main/kotlin/com/rafgittools/di/AppModule.kt",
)

REQUIRED_GET_ROUTES = {
    "user/repos",
    "repos/{owner}/{repo}/branches",
    "repos/{owner}/{repo}/tags",
    "repos/{owner}/{repo}/commits/{ref}",
    "repos/{owner}/{repo}/git/trees/{treeSha}",
    "repos/{owner}/{repo}/git/blobs/{blobSha}",
    "search/code",
}


class ValidationError(ValueError):
    pass


def read(root: Path, relative: str) -> str:
    path = root / relative
    if not path.is_file():
        raise ValidationError(f"missing file: {relative}")
    return path.read_text(encoding="utf-8")


def digest(root: Path) -> str:
    hasher = hashlib.sha256()
    for relative in sorted(FILES):
        hasher.update(relative.encode())
        hasher.update(b"\0")
        hasher.update((root / relative).read_bytes())
        hasher.update(b"\0")
    return hasher.hexdigest()


def validate(root: Path) -> dict[str, Any]:
    source = {path: read(root, path) for path in FILES}
    api = source[FILES[0]]
    result = source[FILES[2]]
    pagination = source[FILES[3]]
    rate = source[FILES[4]]
    remote = source[FILES[5]]
    indexer = source[FILES[6]]
    decoder = source[FILES[8]]
    daos = source[FILES[9]]
    module = source[FILES[10]]

    if any(annotation in api for annotation in ("@POST", "@PUT", "@PATCH", "@DELETE")):
        raise ValidationError("RafGitFS API must remain read-only")
    routes = set(re.findall(r'@GET\("([^"]+)"\)', api))
    missing_routes = REQUIRED_GET_ROUTES - routes
    if missing_routes:
        raise ValidationError(f"missing GET routes: {sorted(missing_routes)}")
    if api.count("Response<") < len(REQUIRED_GET_ROUTES):
        raise ValidationError("all dedicated API endpoints must expose Retrofit Response headers")

    for state in ("Observed", "NotModified", "TokenVazio", "RateLimited", "Failure"):
        if f"data class {state}" not in result:
            raise ValidationError(f"remote result state missing: {state}")

    if "rel=\\\"next\\\"" not in pagination or "DEFAULT_PAGE_SIZE = 100" not in pagination:
        raise ValidationError("bounded Link-header pagination is required")
    for header in ("X-RateLimit-Remaining", "X-RateLimit-Reset", "Retry-After"):
        if header not in rate:
            raise ValidationError(f"rate-limit header missing: {header}")

    for marker in (
        "PAGE_BUDGET_EXHAUSTED",
        "GITHUB_TREE_TRUNCATED",
        "GITHUB_SEARCH_INCOMPLETE",
        "X-GitHub-Request-Id",
        "splitRepository",
    ):
        if marker not in remote:
            raise ValidationError(f"remote fail-closed marker missing: {marker}")

    for method in (
        "refreshRepositories",
        "refreshRefs",
        "refreshTree",
        "readContent",
        "searchLocal",
        "searchRemote",
    ):
        if f"fun {method}" not in indexer:
            raise ValidationError(f"indexer method missing: {method}")
    for marker in (
        "cachedRef?.gitSha == commit.sha",
        "countForRef",
        "listFavoritePaths",
        "deleteStale(profileId, repositoryFullName, refName, now)",
        "PROFILE_CLAIM_PROMOTION_BLOCKED",
    ):
        if marker not in indexer:
            raise ValidationError(f"incremental/index safety marker missing: {marker}")

    if "DEFAULT_MAX_IN_MEMORY_BYTES" not in decoder or "blob.size <= maxBytes" not in decoder:
        raise ValidationError("blob decoding must be bounded")
    if "countForRef" not in daos or "listFavoritePaths" not in daos:
        raise ValidationError("Room navigation helpers are required")
    if "repositoryFullName = :repositoryFullName" not in daos:
        raise ValidationError("stale ref cleanup must be repository-scoped")
    if "provideRafGitFsGithubApiService" not in module:
        raise ValidationError("Hilt provider for read-only API missing")

    forbidden_write_words = re.findall(
        r"@(POST|PUT|PATCH|DELETE)|createPullRequest\(|push\(|commit\(",
        "\n".join(source[path] for path in FILES[:9]),
    )
    if forbidden_write_words:
        raise ValidationError("remote/index layer contains a mutation capability")

    return {
        "status": "PASS",
        "prompt": "3/8",
        "read_only_routes": len(REQUIRED_GET_ROUTES),
        "pagination_bounded": True,
        "rate_limit_observed": True,
        "incremental_sha_index": True,
        "tree_truncation_token_vazio": True,
        "blob_memory_limit_bytes": 5 * 1024 * 1024,
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
