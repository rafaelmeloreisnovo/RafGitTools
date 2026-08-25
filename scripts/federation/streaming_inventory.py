#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json, os
from pathlib import Path

ARCHIVE_SUFFIXES = {".zip", ".zst", ".gz", ".tgz", ".tar", ".7z", ".zqs"}


def hash_file(path: Path, chunk_size: int) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        while True:
            chunk = handle.read(chunk_size)
            if not chunk:
                break
            digest.update(chunk)
    return digest.hexdigest()


def iter_files(root: Path):
    for base, dirs, files in os.walk(root, followlinks=False):
        dirs[:] = sorted(d for d in dirs if not (Path(base) / d).is_symlink())
        for name in sorted(files):
            path = Path(base) / name
            if not path.is_symlink() and path.is_file():
                yield path


def scan(root: Path, chunk_size: int, max_files: int | None = None, after_path: str | None = None):
    records = []
    for path in iter_files(root):
        relative = path.relative_to(root).as_posix()
        if after_path is not None and relative <= after_path:
            continue
        if max_files is not None and len(records) >= max_files:
            break
        stat = path.stat()
        suffix = path.suffix.lower()
        records.append({
            "relative_path": relative,
            "size_bytes": stat.st_size,
            "mtime_ns": stat.st_mtime_ns,
            "sha256": hash_file(path, chunk_size),
            "suffix": suffix,
            "is_archive_candidate": suffix in ARCHIVE_SUFFIXES,
            "payload_copied": False,
        })
    return records


def load_resume_path(checkpoint: Path | None) -> str | None:
    if checkpoint is None or not checkpoint.exists():
        return None
    data = json.loads(checkpoint.read_text(encoding="utf-8"))
    value = data.get("last_relative_path")
    if value is not None and not isinstance(value, str):
        raise ValueError("checkpoint last_relative_path must be a string or null")
    return value


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--checkpoint", type=Path)
    parser.add_argument("--resume-checkpoint", type=Path)
    parser.add_argument("--chunk-size", type=int, default=1024 * 1024)
    parser.add_argument("--max-files", type=int)
    args = parser.parse_args()
    root = args.root.resolve()
    if not root.is_dir():
        raise SystemExit("root must be a directory")
    if args.chunk_size < 4096:
        raise SystemExit("chunk-size must be at least 4096")
    after_path = load_resume_path(args.resume_checkpoint)
    records = scan(root, args.chunk_size, args.max_files, after_path)
    report = {
        "profile_id": "RAFAELIA-STREAMING-INVENTORY-1",
        "root_name": root.name,
        "resumed_after": after_path,
        "records": records,
        "record_count": len(records),
        "payload_copied": False,
        "archives_extracted": False,
        "claim_allowed": False,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if args.checkpoint:
        checkpoint = {
            "profile_id": report["profile_id"],
            "record_count": len(records),
            "last_relative_path": records[-1]["relative_path"] if records else after_path,
            "complete_for_requested_scope": args.max_files is None,
        }
        args.checkpoint.parent.mkdir(parents=True, exist_ok=True)
        args.checkpoint.write_text(json.dumps(checkpoint, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"status":"PASS", "records":len(records), "resumed_after":after_path, "archives_extracted":False}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
