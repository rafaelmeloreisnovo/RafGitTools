#!/usr/bin/env python3
"""Offline bounded metadata inventory: no raw corpus transfer, opaque JSUM."""
import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path

CLASSES = {
    "shader": ("shader", "glsl", "wgsl", "vulkan", "fragment"),
    "ledger": ("ledger", "leader", "receipt", "recibo", "commit"),
    "atlas": ("atlas", "mapa", "map", "rota", "route", "indice", "index"),
    "benchmark": ("benchmark", "bench", "perf", "neon", "arm32", "arm64"),
    "conversations": ("convers", "jejum", "jesum", "novexport", "novoexport"),
}
EXTENSIONS = {".json", ".jsonl", ".jsum", ".js", ".txt", ".md",
              ".glsl", ".wgsl", ".frag", ".csv"}


def classify(name):
    key = name.casefold()
    return next((kind for kind, signals in CLASSES.items()
                 if any(k in key for k in signals)), None)


def scan(root, max_depth=1, max_files=400, hash_max_bytes=16 * 1024 * 1024):
    root = Path(root).expanduser().resolve(strict=True)
    if not root.is_dir() or root == Path("/") or root == Path.home():
        raise ValueError("BLOCKED_UNSCOPED_ROOT")
    if not 0 <= max_depth <= 2 or not 1 <= max_files <= 1000:
        raise ValueError("BLOCKED_SCAN_BUDGET")
    if not 0 <= hash_max_bytes <= 128 * 1024 * 1024:
        raise ValueError("BLOCKED_HASH_BUDGET")
    records = []
    for current, dirs, files in os.walk(root, followlinks=False):
        here = Path(current)
        depth = len(here.relative_to(root).parts)
        dirs[:] = sorted(d for d in dirs if not d.startswith(".")
                          and not (here / d).is_symlink()
                          and depth < max_depth)
        for name in sorted(files):
            path = here / name
            if path.is_symlink() or path.suffix.lower() not in EXTENSIONS:
                continue
            kind = classify(name)
            if kind is None:
                continue
            if len(records) >= max_files:
                raise ValueError("BLOCKED_MAX_FILES")
            stat = path.stat()
            rel = path.relative_to(root).as_posix()
            record = {
                "path_id_sha256": hashlib.sha256(rel.encode("utf-8")).hexdigest(),
                "kind": kind,
                "bytes": stat.st_size,
                "format": path.suffix.lower(),
                "state": "METADATA_ONLY",
                "sha256": None,
                "parser": "OPAQUE_NOT_PARSED" if path.suffix.lower() == ".jsum" else
                          "NOT_PARSED_NO_SCHEMA"
            }
            if stat.st_size <= hash_max_bytes:
                h = hashlib.sha256()
                with path.open("rb") as stream:
                    for chunk in iter(lambda: stream.read(65536), b""):
                        h.update(chunk)
                record["sha256"] = h.hexdigest()
                record["state"] = "HASHED_LOCAL_ONLY"
            else:
                record["state"] = "TOKEN_VAZIO_HASH_SIZE_GATED"
            records.append(record)
    return {
        "schema": "rafaelia.lab.novoexport.inventory.v1",
        "source": "USER_SELECTED_LOCAL_ROOT",
        "depth_limit": max_depth,
        "file_budget": max_files,
        "hash_budget_bytes": hash_max_bytes,
        "records": records,
        "summary": dict(sorted(Counter(r["kind"] for r in records).items())),
        "raw_corpus_uploaded": False,
        "claim_allowed": False
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("root", help="Explicit local directory; never a provider URL")
    ap.add_argument("--max-depth", type=int, default=1)
    ap.add_argument("--max-files", type=int, default=400)
    ap.add_argument("--hash-max-mib", type=int, default=16)
    ap.add_argument("--write-local", action="store_true",
                    help="Write only a private metadata inventory inside lab/build/")
    a = ap.parse_args()
    result = scan(a.root, a.max_depth, a.max_files, a.hash_max_mib * 1048576)
    if a.write_local:
        output = Path(__file__).resolve().parents[1] / "build/private_novoexport_inventory.json"
        output.parent.mkdir(parents=True, exist_ok=True)
        fd = os.open(output, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        with os.fdopen(fd, "w", encoding="utf-8") as target:
            json.dump(result, target, indent=2)
            target.write("\n")
            target.flush()
            os.fsync(target.fileno())
        print("PASS_PRIVATE_LOCAL_INVENTORY", len(result["records"]))
    else:
        print(json.dumps({"matched": len(result["records"]), "by_kind": result["summary"],
                          "persisted": False, "uploaded": False}))
    print("TOKEN_VAZIO_JSUM_SCHEMA / NO_CONTENT_PUBLISHED / NO_NETWORK")


if __name__ == "__main__":
    main()
