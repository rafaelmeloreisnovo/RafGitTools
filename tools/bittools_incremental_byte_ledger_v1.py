#!/usr/bin/env python3
"""RafGitTools BitTools: bounded read -> append receipt -> resume, no network.

Reads an explicitly selected local/staged tree. Each source byte in each
committed chunk is hashed once per successful pass, including whitespace.
This is a chunk-ledger integrity proof, NOT a SHA-256 of the whole file,
not a Google Drive crawler, and not proof of every semantic interpretation.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys
import time
import zipfile

SCHEMA = "rafgittools.bittools.byte-ledger.v1"
ZERO = "0" * 64
MAX_EVENTS_PER_CYCLE = 4096
MAX_CHUNK = 8 * 1024 * 1024
MAX_ZIP_MEMBERS = 200000
MAX_RECOVERY_EVENTS = 2000000


def canon(obj):
    return json.dumps(obj, ensure_ascii=True, sort_keys=True,
                      separators=(",", ":")).encode("ascii")


def h256(data):
    return hashlib.sha256(data).hexdigest()


def chain_step(current, offset, count, digest):
    return h256(bytes.fromhex(current) + offset.to_bytes(8, "big") +
                count.to_bytes(8, "big") + bytes.fromhex(digest))


def safe_relative(path):
    p = Path(path)
    if (not path or p.is_absolute() or "\\" in path or
        any(c in ("", ".", "..") for c in path.split("/")) or
        any(ord(c) < 32 for c in path)):
        raise ValueError("DENY_UNSAFE_RELATIVE_PATH")
    return path


def file_stat(path):
    x = path.lstat()
    if not stat.S_ISREG(x.st_mode):
        raise ValueError("DENY_NONREGULAR_FILE")
    return {"size": x.st_size, "mtime_ns": x.st_mtime_ns,
            "inode": x.st_ino, "device": x.st_dev}


class Ledger:
    def __init__(self, dest, root):
        self.dest = dest
        self.root_id = h256(os.fsencode(str(root.resolve(strict=True))))
        self.seq = 0
        self.head = ZERO
        self.files = {}
        self.dirs = set()
        self.skips = set()
        self.events = 0
        self.done_zips = set()
        self.zip_progress = {}
        self.cycles = 0
        self.started = False
        if dest.exists():
            self.replay()
        else:
            dest.parent.mkdir(parents=True, exist_ok=True)
            self.append({"kind": "root", "root_id_sha256": self.root_id})
            self.started = True

    def replay(self):
        with self.dest.open("rb") as stream:
            for line in stream:
                self.events += 1
                if self.events > MAX_RECOVERY_EVENTS or len(line) > 256 * 1024:
                    raise ValueError("DENY_LEDGER_RECOVERY_LIMIT")
                if not line.endswith(b"\n"):
                    raise ValueError("DENY_TRUNCATED_APPEND")
                try:
                    record = json.loads(line)
                    digest = record.pop("event_sha256")
                    if (record.get("seq") != self.seq + 1 or
                        record.get("prev_event_sha256") != self.head or
                        digest != h256(canon(record)) or
                        record.get("schema") != SCHEMA):
                        raise ValueError("DENY_LEDGER_CHAIN")
                    self.seq += 1
                    self.head = digest
                    self.apply(record)
                except (ValueError, KeyError, TypeError, UnicodeDecodeError) as ex:
                    raise ValueError("DENY_LEDGER_INVALID") from ex
        if not self.started:
            raise ValueError("DENY_LEDGER_ROOT_MISSING")

    def apply(self, event):
        kind = event["kind"]
        if self.seq == 1:
            if kind != "root" or event["root_id_sha256"] != self.root_id:
                raise ValueError("DENY_LEDGER_ROOT_DRIFT")
            self.started = True
            return
        if kind == "directory":
            self.dirs.add(safe_relative(event["path"]))
        elif kind == "skip":
            self.skips.add(safe_relative(event["path"]))
        elif kind == "file_begin":
            path = safe_relative(event["path"])
            if path in self.files:
                raise ValueError("DENY_DUPLICATE_FILE_BEGIN")
            state = event["stat"]
            if (type(state.get("size")) is not int or state["size"] < 0 or
                any(type(state.get(k)) is not int for k in ("mtime_ns", "inode", "device"))):
                raise ValueError("DENY_SOURCE_STAT")
            self.files[path] = {"stat": state, "offset": 0,
                                "chain": ZERO, "done": False, "chunks": 0}
        elif kind == "chunk":
            path = safe_relative(event["path"])
            if path not in self.files:
                raise ValueError("DENY_CHUNK_BEFORE_SOURCE")
            state = self.files[path]
            off, count, digest = event["offset"], event["length"], event["sha256"]
            if (state["done"] or off != state["offset"] or count <= 0 or
                count > MAX_CHUNK or off + count > state["stat"]["size"] or
                not re.fullmatch(r"[a-f0-9]{64}", digest)):
                raise ValueError("DENY_CHUNK_SEQUENCE")
            state["offset"] += count
            state["chunks"] += 1
            state["chain"] = chain_step(state["chain"], off, count, digest)
            if event["chunk_chain_sha256"] != state["chain"]:
                raise ValueError("DENY_CHUNK_CHAIN")
        elif kind == "file_done":
            path = safe_relative(event["path"])
            state = self.files[path]
            if (state["done"] or state["offset"] != state["stat"]["size"] or
                event["chunk_chain_sha256"] != state["chain"]):
                raise ValueError("DENY_PREMATURE_FILE_CLOSURE")
            state["done"] = True
        elif kind == "zip_member":
            path = safe_relative(event["path"])
            if path not in self.files or not self.files[path]["done"]:
                raise ValueError("DENY_ZIP_MEMBER_BEFORE_FILE")
            if path in self.done_zips:
                raise ValueError("DENY_ZIP_MEMBER_AFTER_DONE")
            index = event["index"]
            if index != self.zip_progress.get(path, 0):
                raise ValueError("DENY_ZIP_MEMBER_SEQUENCE")
            self.zip_progress[path] = index + 1
        elif kind == "zip_done":
            path = safe_relative(event["path"])
            if path in self.done_zips:
                raise ValueError("DENY_ZIP_DONE_DUPLICATE")
            if event["entries"] != self.zip_progress.get(path, 0):
                raise ValueError("DENY_ZIP_DONE_COUNT")
            self.done_zips.add(path)
        elif kind == "cycle":
            self.cycles += 1
        else:
            raise ValueError("DENY_UNKNOWN_EVENT_KIND")

    def append(self, data):
        record = dict(data, seq=self.seq + 1,
                      prev_event_sha256=self.head, schema=SCHEMA)
        digest = h256(canon(record))
        encoded = canon(dict(record, event_sha256=digest)) + b"\n"
        # No rewriting: a crash between append and fsync is either recovered by
        # replay or blocks with DENY_TRUNCATED_APPEND, never silently skipped.
        with self.dest.open("ab", buffering=0) as stream:
            stream.write(encoded)
            os.fsync(stream.fileno())
        self.seq += 1
        self.head = digest
        self.events += 1
        self.apply(record)


def anomalies(buf, offset, previous):
    window = (previous + buf) if previous else buf
    base = offset - len(previous)
    positions = []
    count = 0
    ix = 0
    while True:
        ix = window.find(b"  ", ix)
        if ix < 0:
            break
        count += 1
        if len(positions) < 8:
            positions.append(base + ix)
        ix += 1
    return {"ascii_double_space_pairs": count,
            "ascii_double_space_offsets_sample": positions,
            "nul_bytes": buf.count(b"\x00"),
            "line_feeds": buf.count(b"\x0a"),
            "crlf_pairs": window.count(b"\r\n")}


def zip_members(ledger, full, relative, max_events):
    count = ledger.zip_progress.get(relative, 0)
    try:
        with zipfile.ZipFile(full) as archive:
            items = archive.infolist()
            if len(items) > MAX_ZIP_MEMBERS:
                # No pretending to have indexed all ZIP entries.
                raise ValueError("TOKEN_VAZIO_ZIP_ENTRY_CAP")
            for i in range(count, len(items)):
                if max_events <= 0:
                    return 0, False
                item = items[i]
                name = item.filename
                clean = (not name.startswith("/") and
                         ".." not in name.replace("\\", "/").split("/") and
                         "\x00" not in name)
                mode = item.external_attr >> 16
                ledger.append({
                    "kind": "zip_member", "path": relative, "index": i,
                    "member_name": name[:2048], "member_name_truncated": len(name) > 2048,
                    "entry_name_safe": clean,
                    "file_size_claimed": item.file_size,
                    "compressed_size_claimed": item.compress_size,
                    "crc32_header": f"{item.CRC:08x}",
                    "is_dir": item.is_dir(),
                    "is_symlink": stat.S_ISLNK(mode),
                    "encrypted": bool(item.flag_bits & 1),
                    "member_content_read": False,
                    "member_content_sha256": "TOKEN_VAZIO_NOT_READ",
                    "entry_verified": False
                })
                max_events -= 1
            if max_events <= 0:
                return 0, False
            ledger.append({"kind": "zip_done", "path": relative, "entries": len(items),
                           "scope": "ZIP_CENTRAL_DIRECTORY_ONLY"})
            return max_events - 1, True
    except (OSError, zipfile.BadZipFile, zipfile.LargeZipFile, ValueError) as e:
        # A corrupt ZIP is not silently interpreted as an empty ZIP.
        if isinstance(e, ValueError) and str(e) != "TOKEN_VAZIO_ZIP_ENTRY_CAP":
            raise
        raise ValueError("TOKEN_VAZIO_ZIP_CENTRAL_DIRECTORY_UNREADABLE") from e


def run(root, dest, *, chunk_bytes, max_bytes, max_events, zip_index=True):
    root = root.resolve(strict=True)
    if not root.is_dir() or dest.resolve().is_relative_to(root):
        raise ValueError("DENY_ROOT_NOT_DIRECTORY_OR_LEDGER_INSIDE_ROOT")
    if not (1 <= chunk_bytes <= MAX_CHUNK and
            1 <= max_bytes <= 2**40 and 2 <= max_events <= MAX_EVENTS_PER_CYCLE):
        raise ValueError("DENY_BUDGET")
    ledger = Ledger(dest, root)
    started = time.monotonic_ns()
    remaining_bytes, remaining_events = max_bytes, max_events - 1
    read_bytes = 0
    scanned = 0
    traversal_complete = True
    for base, folders, filenames in os.walk(root, topdown=True, followlinks=False):
        folders.sort()
        filenames.sort()
        for directory in list(folders):
            full = Path(base) / directory
            rel = full.relative_to(root).as_posix()
            if full.is_symlink():
                folders.remove(directory)
                if rel not in ledger.skips:
                    if remaining_events <= 0:
                        traversal_complete = False
                        break
                    ledger.append({"kind": "skip", "path": rel, "reason": "SYMLINK_DIRECTORY"})
                    remaining_events -= 1
        if not traversal_complete:
            break
        base_relative = Path(base).relative_to(root).as_posix()
        if base_relative != "." and base_relative not in ledger.dirs:
            if remaining_events <= 0:
                traversal_complete = False
                break
            ledger.append({"kind": "directory", "path": base_relative})
            remaining_events -= 1
        for filename in filenames:
            full = Path(base) / filename
            rel = full.relative_to(root).as_posix()
            try:
                safe_relative(rel)
                kind = full.lstat().st_mode
            except (OSError, ValueError):
                raise ValueError("TOKEN_VAZIO_UNSAFE_OR_MISSING_SOURCE")
            if not stat.S_ISREG(kind):
                if rel not in ledger.skips:
                    if remaining_events <= 0:
                        traversal_complete = False
                        break
                    ledger.append({"kind": "skip", "path": rel, "reason": "NONREGULAR_SOURCE"})
                    remaining_events -= 1
                continue
            meta = file_stat(full)
            state = ledger.files.get(rel)
            if state is None:
                if remaining_events <= 0:
                    traversal_complete = False
                    break
                ledger.append({"kind": "file_begin", "path": rel, "stat": meta})
                remaining_events -= 1
                state = ledger.files[rel]
            if meta != state["stat"]:
                raise ValueError("DENY_SOURCE_MUTATED_DURING_RESUME:" + rel)
            if not state["done"]:
                if state["offset"] < meta["size"]:
                    with full.open("rb", buffering=0) as stream:
                        stream.seek(state["offset"])
                        while state["offset"] < meta["size"]:
                            if remaining_bytes <= 0 or remaining_events <= 0:
                                traversal_complete = False
                                break
                            off = state["offset"]
                            n = min(chunk_bytes, remaining_bytes, meta["size"] - off)
                            part = stream.read(n)
                            if len(part) != n:
                                raise ValueError("DENY_SHORT_SOURCE_READ")
                            prior = b""
                            if off:
                                stream.seek(off - 1)
                                prior = stream.read(1)
                                stream.seek(off + n)
                            digest = h256(part)
                            ledger.append({
                                "kind": "chunk", "path": rel, "offset": off,
                                "length": len(part), "sha256": digest,
                                "chunk_chain_sha256":
                                    chain_step(state["chain"], off, len(part), digest),
                                "anomalies": anomalies(part, off, prior)
                            })
                            read_bytes += n
                            remaining_bytes -= n
                            remaining_events -= 1
                        if not traversal_complete:
                            break
                if remaining_events <= 0:
                    traversal_complete = False
                    break
                if file_stat(full) != meta:
                    raise ValueError("DENY_SOURCE_CHANGED_DURING_SCAN")
                ledger.append({"kind": "file_done", "path": rel,
                               "chunk_chain_sha256": state["chain"],
                               "scope": "RAW_BYTE_CHUNKS_COMPLETE",
                               "file_sha256": "TOKEN_VAZIO_NOT_COMPUTED"})
                remaining_events -= 1
                scanned += 1
            if zip_index and full.suffix.lower() == ".zip" and rel not in ledger.done_zips:
                if remaining_events <= 0:
                    traversal_complete = False
                    break
                remaining_events, complete = zip_members(ledger, full, rel, remaining_events)
                if not complete:
                    traversal_complete = False
                    break
        if not traversal_complete:
            break
    # Never pronounce full coverage when an earlier indexed source disappeared.
    if traversal_complete:
        for rel in ledger.files:
            full = root / rel
            if not full.exists() or not full.is_file():
                raise ValueError("DENY_PREVIOUS_SOURCE_DISAPPEARED")
    state = "DONE_SCOPED" if traversal_complete else "PARTIAL_CHECKPOINT"
    elapsed_ms = (time.monotonic_ns() - started) // 1000000
    ledger.append({"kind": "cycle", "result": state,
                   "read_bytes": read_bytes, "files_closed_this_cycle": scanned,
                   "elapsed_ms": elapsed_ms, "claim_allowed": False})
    return {"schema": SCHEMA, "state": state, "claim_allowed": False,
            "ledger_event_sha256": ledger.head, "events_persisted": ledger.seq,
            "files_observed": len(ledger.files),
            "files_completed": sum(s["done"] for s in ledger.files.values()),
            "bytes_committed": sum(s["offset"] for s in ledger.files.values()),
            "zip_indexes_completed": len(ledger.done_zips),
            "cycle_read_bytes": read_bytes, "cycle_elapsed_ms": elapsed_ms,
            "coverage": "LOCAL_SELECTED_TREE_ONLY",
            "zip_member_content": "TOKEN_VAZIO_NOT_READ",
            "whole_file_sha256": "TOKEN_VAZIO_NOT_COMPUTED",
            "drive_source": "TOKEN_VAZIO_NOT_CONNECTED",
            "release_allowed": False}


def main():
    p = argparse.ArgumentParser(description="Bounded local BitTools byte-to-JSONL inventory")
    p.add_argument("--root", type=Path, required=True, help="explicit local/staged source tree")
    p.add_argument("--ledger", type=Path, required=True, help="append-only JSONL outside source")
    p.add_argument("--chunk-bytes", type=int, default=1024*1024)
    p.add_argument("--max-bytes", type=int, default=32*1024*1024)
    p.add_argument("--max-events", type=int, default=512)
    p.add_argument("--no-zip", action="store_true")
    p.add_argument("--verify", action="store_true", help="verify chain, without reading source bytes")
    a = p.parse_args()
    try:
        root = a.root.resolve(strict=True)
        dest = a.ledger.resolve()
        if dest.is_relative_to(root):
            raise ValueError("DENY_LEDGER_INSIDE_SOURCE")
        if a.verify:
            l = Ledger(dest, root) if dest.exists() else None
            if l is None:
                raise ValueError("TOKEN_VAZIO_LEDGER_MISSING")
            result = {"state": "PASS_LEDGER_STRUCTURE_ONLY", "events": l.seq,
                      "head": l.head, "claim_allowed": False}
        else:
            result = run(root, dest, chunk_bytes=a.chunk_bytes,
                         max_bytes=a.max_bytes, max_events=a.max_events,
                         zip_index=not a.no_zip)
        print(json.dumps(result, ensure_ascii=True, sort_keys=True))
        return 0
    except (ValueError, OSError) as e:
        print(json.dumps({"state": "BLOCKED", "reason": str(e),
                          "claim_allowed": False}), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
