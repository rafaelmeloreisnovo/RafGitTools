#!/usr/bin/env python3
from __future__ import annotations

import argparse
import collections
import datetime as dt
import json
import os
import re
import shutil
import subprocess
from pathlib import Path
from typing import Any, Iterable

SCHEMA_ID = "rafaelia_repository_view_v1"
PROFILE_ID = "RAFAELIA-REPOSITORY-VIEW-1"
TOKEN_VAZIO = "TOKEN_VAZIO"

DOC_SUFFIXES = {".md", ".markdown", ".rst", ".adoc", ".txt"}
SOURCE_SUFFIXES = {
    ".c", ".h", ".cc", ".cpp", ".cxx", ".hpp", ".hh", ".s", ".asm",
    ".py", ".pyi", ".java", ".kt", ".kts", ".rs", ".go", ".swift",
    ".js", ".jsx", ".ts", ".tsx", ".dart", ".lua", ".rb", ".php",
    ".sh", ".bash", ".zsh", ".fish", ".ps1", ".pl", ".scala", ".clj",
}
CONFIG_SUFFIXES = {
    ".yml", ".yaml", ".json", ".toml", ".ini", ".cfg", ".conf",
    ".properties", ".gradle", ".xml", ".lock", ".mk", ".cmake",
}
DATA_SUFFIXES = {
    ".csv", ".tsv", ".jsonl", ".ndjson", ".parquet", ".sqlite", ".db",
    ".sql", ".npy", ".npz", ".h5", ".hdf5", ".arrow", ".feather",
}
ARTIFACT_SUFFIXES = {
    ".zip", ".tar", ".tgz", ".gz", ".bz2", ".xz", ".7z", ".zst",
    ".apk", ".aab", ".aar", ".jar", ".war", ".so", ".a", ".o",
    ".bin", ".elf", ".exe", ".dll", ".dylib", ".class", ".dex",
    ".pdf", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg",
    ".mp3", ".wav", ".mp4", ".mov", ".webm",
}
INDEX_BASENAME_RE = re.compile(
    r"^(readme|index|contents|manifest|navigation|map|catalog|catalogue|status)([._-].*)?$",
    re.IGNORECASE,
)
STANDARD_ROOT_RE = re.compile(
    r"^(readme|license|licence|copying|notice|contributing|authors|changelog|security|code_of_conduct|agents)(\..*)?$",
    re.IGNORECASE,
)
SKIP_FALLBACK_DIRS = {".git", ".hg", ".svn", "node_modules", ".gradle", ".idea"}


def run_git(root: Path, *args: str) -> str | None:
    if shutil.which("git") is None:
        return None
    proc = subprocess.run(
        ["git", "-C", str(root), *args],
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if proc.returncode != 0:
        return None
    return proc.stdout.decode("utf-8", errors="replace").strip()


def tracked_paths(root: Path) -> tuple[list[str], str]:
    if (root / ".git").exists() or run_git(root, "rev-parse", "--is-inside-work-tree") == "true":
        proc = subprocess.run(
            ["git", "-C", str(root), "ls-files", "-z"],
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            check=False,
        )
        if proc.returncode == 0:
            raw = proc.stdout.decode("utf-8", errors="surrogateescape")
            return sorted(p for p in raw.split("\0") if p), "git_ls_files"

    result: list[str] = []
    for base, dirs, files in os.walk(root, followlinks=False):
        dirs[:] = sorted(d for d in dirs if d not in SKIP_FALLBACK_DIRS)
        base_path = Path(base)
        for name in sorted(files):
            path = base_path / name
            try:
                rel = path.relative_to(root).as_posix()
            except ValueError:
                continue
            result.append(rel)
    return sorted(result), "filesystem_walk"


def classify(path: str) -> str:
    p = Path(path)
    parts_lower = [part.lower() for part in p.parts]
    suffix = p.suffix.lower()
    name_lower = p.name.lower()

    if len(parts_lower) >= 2 and parts_lower[0] == ".github" and parts_lower[1] == "workflows":
        return "workflow"
    if any(part in {"test", "tests", "androidtest", "spec", "specs"} for part in parts_lower) or name_lower.startswith("test_"):
        return "test"
    if parts_lower and parts_lower[0] in {"docs", "doc", "documentation"}:
        return "documentation"
    if suffix in DOC_SUFFIXES:
        return "documentation"
    if suffix in SOURCE_SUFFIXES:
        return "source"
    if suffix in DATA_SUFFIXES:
        return "data"
    if suffix in CONFIG_SUFFIXES or name_lower in {"makefile", "cmakelists.txt", "dockerfile", "meson.build"}:
        return "configuration"
    if suffix in ARTIFACT_SUFFIXES:
        return "artifact_or_asset"
    if name_lower.startswith("license") or name_lower.startswith("copying"):
        return "governance"
    return "unknown"


def is_local_index(name: str) -> bool:
    return bool(INDEX_BASENAME_RE.match(name))


def yaml_scalar(value: Any) -> str:
    if value is None:
        return "null"
    if value is True:
        return "true"
    if value is False:
        return "false"
    if isinstance(value, (int, float)):
        return str(value)
    return json.dumps(str(value), ensure_ascii=False)


def emit_yaml(value: Any, indent: int = 0) -> list[str]:
    pad = " " * indent
    if isinstance(value, dict):
        if not value:
            return [pad + "{}"]
        lines: list[str] = []
        for key, item in value.items():
            key_text = str(key)
            if isinstance(item, (dict, list)):
                lines.append(f"{pad}{key_text}:")
                lines.extend(emit_yaml(item, indent + 2))
            else:
                lines.append(f"{pad}{key_text}: {yaml_scalar(item)}")
        return lines
    if isinstance(value, list):
        if not value:
            return [pad + "[]"]
        lines = []
        for item in value:
            if isinstance(item, dict):
                if not item:
                    lines.append(pad + "- {}")
                    continue
                first = True
                for key, sub in item.items():
                    if first and not isinstance(sub, (dict, list)):
                        lines.append(f"{pad}- {key}: {yaml_scalar(sub)}")
                        first = False
                    else:
                        if first:
                            lines.append(pad + "-")
                            first = False
                        if isinstance(sub, (dict, list)):
                            lines.append(f"{pad}  {key}:")
                            lines.extend(emit_yaml(sub, indent + 4))
                        else:
                            lines.append(f"{pad}  {key}: {yaml_scalar(sub)}")
            elif isinstance(item, list):
                lines.append(pad + "-")
                lines.extend(emit_yaml(item, indent + 2))
            else:
                lines.append(f"{pad}- {yaml_scalar(item)}")
        return lines
    return [pad + yaml_scalar(value)]


def write_yaml(path: Path, value: Any) -> None:
    path.write_text("\n".join(emit_yaml(value)) + "\n", encoding="utf-8")


def safe_fragment(text: str) -> str:
    text = re.sub(r"[^A-Za-z0-9._-]+", "_", text).strip("._-")
    return text[:80] or "ROOT"


def chunks(items: list[Any], n: int) -> Iterable[list[Any]]:
    for start in range(0, len(items), n):
        yield items[start:start + n]


def generated_at() -> str:
    epoch = os.environ.get("SOURCE_DATE_EPOCH")
    if epoch:
        try:
            return dt.datetime.fromtimestamp(int(epoch), tz=dt.timezone.utc).isoformat().replace("+00:00", "Z")
        except ValueError:
            pass
    return dt.datetime.now(tz=dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def build_records(root: Path, rel_paths: list[str], output_dir: Path) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]:
    output_rel = None
    try:
        output_rel = output_dir.relative_to(root).as_posix().rstrip("/")
    except ValueError:
        pass

    files: list[dict[str, Any]] = []
    dirs: dict[str, dict[str, Any]] = collections.defaultdict(lambda: {
        "direct_files": 0,
        "recursive_files": 0,
        "direct_subdirectories": set(),
        "role_counts": collections.Counter(),
        "local_indices": [],
    })
    dirs["."]

    for rel in rel_paths:
        if output_rel and (rel == output_rel or rel.startswith(output_rel + "/")):
            continue
        full = root / rel
        p = Path(rel)
        role = classify(rel)
        try:
            stat = full.lstat()
            size_bytes = stat.st_size
            kind = "symlink" if full.is_symlink() else "file"
        except OSError:
            size_bytes = None
            kind = "unreadable"

        record: dict[str, Any] = {
            "path": rel,
            "kind": kind,
            "size_bytes": size_bytes,
            "suffix": p.suffix.lower(),
            "role_guess": role,
        }
        if role == "unknown":
            record["semantic_role_state"] = TOKEN_VAZIO
        if isinstance(size_bytes, int) and size_bytes >= 5 * 1024 * 1024:
            record["large_file_candidate"] = True
        files.append(record)

        parent = p.parent.as_posix() if p.parent.as_posix() != "." else "."
        dirs[parent]["direct_files"] += 1
        dirs[parent]["role_counts"][role] += 1
        if is_local_index(p.name):
            dirs[parent]["local_indices"].append(rel)

        parts = p.parts[:-1]
        current = "."
        dirs[current]["recursive_files"] += 1
        for i, part in enumerate(parts):
            child = part if current == "." else f"{current}/{part}"
            dirs[current]["direct_subdirectories"].add(child)
            current = child
            dirs[current]["recursive_files"] += 1
            if i + 1 < len(parts):
                nxt = f"{current}/{parts[i+1]}"
                dirs[current]["direct_subdirectories"].add(nxt)

    normalized_dirs: dict[str, dict[str, Any]] = {}
    for path in sorted(dirs):
        info = dirs[path]
        roles = dict(sorted(info["role_counts"].items()))
        has_source = roles.get("source", 0) + roles.get("test", 0) > 0
        local_indices = sorted(info["local_indices"])
        normalized_dirs[path] = {
            "path": path,
            "direct_files": info["direct_files"],
            "recursive_files": info["recursive_files"],
            "direct_subdirectories": sorted(info["direct_subdirectories"]),
            "local_indices": local_indices,
            "role_counts_direct": roles,
            "index_state": "INDEXED_LOCAL" if local_indices else "TOKEN_VAZIO_LOCAL_INDEX",
            "source_without_local_index": bool(has_source and not local_indices),
        }
    return files, normalized_dirs


def clean_generated(output_dir: Path) -> None:
    if not output_dir.exists():
        return
    for child in output_dir.iterdir():
        if child.is_file() and (
            child.name in {"INDEX.yml", "TREE.md", "RECEIPT.json"}
            or child.name.startswith("FILES_") and child.suffix == ".yml"
            or child.name.startswith("DIRECTORIES_") and child.suffix == ".yml"
        ):
            child.unlink()


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate recursive repository navigation maps with no external Python dependencies.")
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--output-dir", type=Path, default=Path("docs/repository-map"))
    parser.add_argument("--max-records-per-shard", type=int, default=5000)
    parser.add_argument("--top-debt", type=int, default=100)
    parser.add_argument("--no-clean", action="store_true", help="Do not remove previously generated current-view files.")
    args = parser.parse_args()

    root = args.root.resolve()
    if not root.is_dir():
        raise SystemExit("root must be a directory")
    if args.max_records_per_shard < 100:
        raise SystemExit("max-records-per-shard must be >= 100")
    output_dir = args.output_dir if args.output_dir.is_absolute() else root / args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)
    if not args.no_clean:
        clean_generated(output_dir)

    rel_paths, collection_method = tracked_paths(root)
    files, directories = build_records(root, rel_paths, output_dir)

    role_counts = collections.Counter(r["role_guess"] for r in files)
    suffix_counts = collections.Counter(r["suffix"] or "<no_suffix>" for r in files)
    unknown_count = role_counts.get("unknown", 0)
    unindexed_source_dirs = [d for d in directories.values() if d["source_without_local_index"]]

    root_loose = []
    for rec in files:
        p = Path(rec["path"])
        if len(p.parts) == 1 and not STANDARD_ROOT_RE.match(p.name):
            root_loose.append(rec["path"])

    source_commit = run_git(root, "rev-parse", "HEAD") or TOKEN_VAZIO
    source_branch = os.environ.get("GITHUB_REF_NAME") or run_git(root, "rev-parse", "--abbrev-ref", "HEAD") or TOKEN_VAZIO
    repository = os.environ.get("GITHUB_REPOSITORY") or root.name
    generated = generated_at()

    file_groups: dict[str, list[dict[str, Any]]] = collections.defaultdict(list)
    for record in files:
        parts = Path(record["path"]).parts
        group = parts[0] if len(parts) > 1 else "_ROOT_"
        file_groups[group].append(record)

    shard_manifest: list[dict[str, Any]] = []
    shard_seq = 0
    for group in sorted(file_groups):
        group_records = sorted(file_groups[group], key=lambda x: x["path"])
        for part_no, part_records in enumerate(chunks(group_records, args.max_records_per_shard), start=1):
            shard_seq += 1
            filename = f"FILES_{shard_seq:04d}_{safe_fragment(group)}_{part_no:03d}.yml"
            payload = {
                "schema": SCHEMA_ID,
                "profile_id": PROFILE_ID,
                "kind": "file_shard",
                "repository": repository,
                "source_commit": source_commit,
                "group": group,
                "part": part_no,
                "record_count": len(part_records),
                "records": part_records,
            }
            write_yaml(output_dir / filename, payload)
            shard_manifest.append({"kind": "files", "path": filename, "group": group, "records": len(part_records)})

    directory_records = [directories[key] for key in sorted(directories)]
    for part_no, part_records in enumerate(chunks(directory_records, args.max_records_per_shard), start=1):
        filename = f"DIRECTORIES_{part_no:04d}.yml"
        payload = {
            "schema": SCHEMA_ID,
            "profile_id": PROFILE_ID,
            "kind": "directory_shard",
            "repository": repository,
            "source_commit": source_commit,
            "part": part_no,
            "record_count": len(part_records),
            "records": part_records,
        }
        write_yaml(output_dir / filename, payload)
        shard_manifest.append({"kind": "directories", "path": filename, "records": len(part_records)})

    top_groups = []
    for group in sorted(file_groups):
        group_records = file_groups[group]
        counts = collections.Counter(r["role_guess"] for r in group_records)
        top_groups.append({
            "group": group,
            "files": len(group_records),
            "role_counts": dict(sorted(counts.items())),
        })

    index = {
        "schema": SCHEMA_ID,
        "schema_version": "1.0.0",
        "profile_id": PROFILE_ID,
        "generated_at": generated,
        "repository": repository,
        "source": {
            "commit": source_commit,
            "branch": source_branch,
            "collection_method": collection_method,
            "scope": "recursive_tracked_files" if collection_method == "git_ls_files" else "recursive_filesystem_files",
        },
        "evidence_boundary": {
            "state": "STRUCTURE_ONLY",
            "claim_allowed": False,
            "semantic_meaning_of_unknown_files": TOKEN_VAZIO,
            "note": "Presence, path and local file metadata are observed. Functional correctness and semantic purpose are not inferred from structure alone.",
        },
        "statistics": {
            "files": len(files),
            "directories": len(directories),
            "unknown_semantic_role_files": unknown_count,
            "unindexed_source_directories": len(unindexed_source_dirs),
            "root_loose_file_candidates": len(root_loose),
            "role_counts": dict(sorted(role_counts.items())),
            "top_suffix_counts": dict(suffix_counts.most_common(50)),
        },
        "navigation": {
            "top_level_groups": top_groups,
            "shards": shard_manifest,
        },
        "documentation_debt": {
            "unindexed_source_directories_top": [d["path"] for d in unindexed_source_dirs[:args.top_debt]],
            "root_loose_file_candidates_top": root_loose[:args.top_debt],
            "truncation": {
                "unindexed_source_directories": len(unindexed_source_dirs) > args.top_debt,
                "root_loose_file_candidates": len(root_loose) > args.top_debt,
            },
        },
    }
    write_yaml(output_dir / "INDEX.yml", index)

    tree_lines = [
        "# Repository map / Mapa do repositório",
        "",
        f"- Repository: `{repository}`",
        f"- Source commit: `{source_commit}`",
        f"- Collection: `{collection_method}`",
        f"- Files mapped: **{len(files)}**",
        f"- Directories mapped: **{len(directories)}**",
        f"- Unindexed source directories: **{len(unindexed_source_dirs)}**",
        f"- Root loose-file candidates: **{len(root_loose)}**",
        "- Evidence boundary: `STRUCTURE_ONLY / claim_allowed=false`",
        "",
        "## Top-level groups",
        "",
        "| Group | Files | Roles |",
        "|---|---:|---|",
    ]
    for item in top_groups:
        roles = ", ".join(f"{k}={v}" for k, v in item["role_counts"].items()) or "—"
        tree_lines.append(f"| `{item['group']}` | {item['files']} | {roles} |")
    tree_lines += [
        "",
        "## Documentation debt — structural candidates",
        "",
        "Directories below contain source/test files but have no local README/INDEX/MANIFEST-like anchor. This is a navigation signal, not proof of bad design.",
        "",
    ]
    if unindexed_source_dirs:
        for item in unindexed_source_dirs[:args.top_debt]:
            tree_lines.append(f"- `{item['path']}` — {item['recursive_files']} recursive files")
    else:
        tree_lines.append("- None observed in the scanned scope.")
    tree_lines += ["", "## Loose root-file candidates", ""]
    if root_loose:
        for path in root_loose[:args.top_debt]:
            tree_lines.append(f"- `{path}`")
    else:
        tree_lines.append("- None observed in the scanned scope.")
    tree_lines += [
        "",
        "## Machine-readable navigation",
        "",
        "Start with `INDEX.yml`; follow `navigation.shards` to the file and directory shards.",
        "Unknown semantic purpose remains `TOKEN_VAZIO` until a code/doc/evidence link is established.",
        "",
    ]
    (output_dir / "TREE.md").write_text("\n".join(tree_lines), encoding="utf-8")

    receipt = {
        "profile_id": PROFILE_ID,
        "schema": SCHEMA_ID,
        "generated_at": generated,
        "repository": repository,
        "source_commit": source_commit,
        "collection_method": collection_method,
        "files_mapped": len(files),
        "directories_mapped": len(directories),
        "shards_written": len(shard_manifest),
        "claim_allowed": False,
        "status": "PASS_STRUCTURE_MAP_GENERATED",
    }
    (output_dir / "RECEIPT.json").write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(json.dumps(receipt, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
