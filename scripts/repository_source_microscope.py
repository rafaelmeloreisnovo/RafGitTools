#!/usr/bin/env python3
"""Exact-SHA, code-first microscope over an allowlisted GitHub repository.

It resolves commit -> tree -> blobs, ranks executable source before documents,
and emits path/blob/line evidence. The target repository is never mutated.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = ROOT / "configs" / "repository-source-microscope.v1.json"
EXACT_SHA_RE = re.compile(r"^[0-9a-f]{40}$")


def canonical_json_bytes(value: Any) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_config(path: Path = CONFIG_PATH) -> dict[str, Any]:
    cfg = json.loads(path.read_text(encoding="utf-8"))
    if cfg.get("schema") != "rafgittools.repository-source-microscope/v1":
        raise ValueError("config_schema_mismatch")
    if cfg.get("auth", {}).get("secret_reference") != "PAT_ACTIONS":
        raise ValueError("source_microscope_must_bind_pat_actions")
    if cfg.get("auth", {}).get("no_fallback") is not True:
        raise ValueError("pat_actions_no_fallback_required")
    return cfg


def allowed_repo(cfg: dict[str, Any], repository: str) -> dict[str, Any]:
    for item in cfg.get("repositories", []):
        if item.get("repository") == repository:
            return item
    raise ValueError("repository_not_allowlisted")


def validate_commit(commit: str) -> str:
    if not EXACT_SHA_RE.fullmatch(commit):
        raise ValueError("exact_lowercase_commit_sha_required")
    return commit


def api_get(cfg: dict[str, Any], path: str, token: str) -> dict[str, Any]:
    base = cfg["github_api_base"].rstrip("/")
    url = base + path
    if urllib.parse.urlparse(url).hostname != "api.github.com":
        raise ValueError("github_api_host_mismatch")
    request = urllib.request.Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "RafGitTools-SourceMicroscope/1.0",
        },
        method="GET",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read())


def repo_path(repository: str) -> str:
    owner, name = repository.split("/", 1)
    return "/repos/" + urllib.parse.quote(owner, safe="") + "/" + urllib.parse.quote(name, safe="")


def resolve_tree(cfg: dict[str, Any], repository: str, commit: str, token: str) -> tuple[str, list[dict[str, Any]], bool]:
    root = repo_path(repository)
    commit_obj = api_get(cfg, f"{root}/git/commits/{commit}", token)
    if commit_obj.get("sha") != commit:
        raise RuntimeError("provider_commit_readback_mismatch")
    tree_sha = commit_obj.get("tree", {}).get("sha")
    if not isinstance(tree_sha, str) or not EXACT_SHA_RE.fullmatch(tree_sha):
        raise RuntimeError("provider_tree_sha_missing")
    tree_obj = api_get(cfg, f"{root}/git/trees/{tree_sha}?recursive=1", token)
    entries = tree_obj.get("tree", [])
    if not isinstance(entries, list):
        raise RuntimeError("provider_tree_missing")
    return tree_sha, entries, bool(tree_obj.get("truncated"))


def classify_path(cfg: dict[str, Any], path: str) -> str:
    low = path.casefold()
    scan = cfg["scan"]
    if any(marker.casefold() in low for marker in scan["test_markers"]):
        return "TEST_OR_VERIFIER"
    if any(marker.casefold() in low for marker in scan["workflow_markers"]):
        return "BUILD_OR_WORKFLOW"
    suffix = Path(path).suffix.casefold()
    if suffix in {x.casefold() for x in scan["code_extensions"]}:
        return "SOURCE_CODE"
    if suffix in {x.casefold() for x in scan["contract_extensions"]}:
        return "CONTRACT_OR_DATA"
    if suffix in {x.casefold() for x in scan["document_extensions"]}:
        return "DOCUMENT_COMPASS"
    return "OTHER"


RANK = {
    "TEST_OR_VERIFIER": 0,
    "SOURCE_CODE": 1,
    "BUILD_OR_WORKFLOW": 2,
    "CONTRACT_OR_DATA": 3,
    "DOCUMENT_COMPASS": 4,
    "OTHER": 5,
}


def ranked_blobs(cfg: dict[str, Any], entries: list[dict[str, Any]], max_blob_bytes: int) -> list[dict[str, Any]]:
    blobs: list[dict[str, Any]] = []
    for entry in entries:
        if entry.get("type") != "blob":
            continue
        size = int(entry.get("size") or 0)
        if size > max_blob_bytes:
            continue
        path = str(entry.get("path", ""))
        item = {
            "path": path,
            "blob_sha": entry.get("sha"),
            "size": size,
            "class": classify_path(cfg, path),
        }
        blobs.append(item)
    return sorted(blobs, key=lambda x: (RANK[x["class"]], x["path"].casefold()))


def fetch_blob_text(cfg: dict[str, Any], repository: str, blob_sha: str, token: str) -> str | None:
    obj = api_get(cfg, f"{repo_path(repository)}/git/blobs/{blob_sha}", token)
    if obj.get("encoding") != "base64":
        return None
    try:
        raw = base64.b64decode(obj.get("content", ""), validate=False)
        return raw.decode("utf-8")
    except (ValueError, UnicodeDecodeError):
        return None


def find_matches(text: str, needles: list[str], max_per_file: int = 20) -> list[dict[str, Any]]:
    wanted = [n.casefold() for n in needles if n]
    if not wanted:
        return []
    out: list[dict[str, Any]] = []
    for line_no, line in enumerate(text.splitlines(), start=1):
        folded = line.casefold()
        hit = [needles[i] for i, n in enumerate(wanted) if n in folded]
        if hit:
            out.append({"line": line_no, "needles": hit, "text": line[:320]})
            if len(out) >= max_per_file:
                break
    return out


def run_scan(args: argparse.Namespace) -> int:
    cfg = load_config()
    repo_meta = allowed_repo(cfg, args.repository)
    commit = validate_commit(args.commit)
    token = os.environ.get("PAT_ACTIONS", "")
    if not token:
        raise RuntimeError("required_secret_missing:PAT_ACTIONS")
    tree_sha, entries, truncated = resolve_tree(cfg, args.repository, commit, token)
    max_blob = args.max_blob_bytes or int(cfg["scan"]["max_blob_bytes_default"])
    max_files = args.max_files or int(cfg["scan"]["max_files_default"])
    files = ranked_blobs(cfg, entries, max_blob)
    needles = [x.strip() for x in args.needles.split(",") if x.strip()]

    matches: list[dict[str, Any]] = []
    scanned = 0
    for item in files[:max_files]:
        if not needles:
            break
        text = fetch_blob_text(cfg, args.repository, str(item["blob_sha"]), token)
        scanned += 1
        if text is None:
            continue
        found = find_matches(text, needles)
        if found:
            matches.append({**item, "matches": found})

    class_counts: dict[str, int] = {}
    for item in files:
        class_counts[item["class"]] = class_counts.get(item["class"], 0) + 1

    result = {
        "schema": "rafgittools.repository-source-microscope-receipt/v1",
        "repository": args.repository,
        "repository_role": repo_meta["role"],
        "authority_pointer": repo_meta["authority_pointer"],
        "commit": commit,
        "tree_sha": tree_sha,
        "tree_truncated": truncated,
        "source_priority": "CODE_FIRST_DOCUMENTS_AS_COMPASS",
        "files_total_bounded": len(files),
        "class_counts": class_counts,
        "files_scanned_for_needles": scanned,
        "needles": needles,
        "matches": matches,
        "top_ranked_paths": files[: min(80, len(files))],
        "execution_state": "SOURCE_LOCATED_NOT_EXECUTED",
        "claim_allowed": False,
    }
    if truncated:
        result["gap"] = "TOKEN_VAZIO_PROVIDER_TREE_TRUNCATED"
    encoded = canonical_json_bytes(result)
    result["receipt_sha256_without_self_hash"] = sha256_bytes(encoded)
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(canonical_json_bytes(result))
    print(json.dumps({
        "repository": args.repository,
        "commit": commit,
        "matches": len(matches),
        "tree_truncated": truncated,
        "output_sha256": sha256_bytes(output.read_bytes()),
        "claim_allowed": False,
    }, sort_keys=True))
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--needles", default="")
    parser.add_argument("--max-files", type=int, default=0)
    parser.add_argument("--max-blob-bytes", type=int, default=0)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    return run_scan(args)


if __name__ == "__main__":
    raise SystemExit(main())
