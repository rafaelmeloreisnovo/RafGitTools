#!/usr/bin/env python3
"""Local-only bounded file/ZIP/line locator for private RAFAELIA sources.

No Drive credentials, uploads or source mutation. No source text by default.
A locator is navigation evidence, not proof of runtime or scientific claims.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import re
import sqlite3
import stat
import zipfile
from pathlib import Path, PurePosixPath

SCHEMA = "rafgittools.archive-line-index/v1"
COMMAND = re.compile(rb"^\s*(?:\$\s+|#!|(?:python3?|bash|sh|git|gh|make|ninja|gradle|adb|termux-[\w-]+|curl|wget|npm|pnpm|cargo|clang|gcc|cmake|\.\/)[ \t/])")
CODE = re.compile(rb"^\s*(?:def |class |async def |import |from |function |#include|(?:static |inline |extern )?(?:void|int|long|char|float|double|size_t)\s|(?:public|private|package|fun|val|var|const|let|if|for|while)\s)")


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def line_kind(data: bytes) -> str:
    if COMMAND.search(data):
        return "COMMAND_CANDIDATE"
    if CODE.search(data):
        return "CODE_CANDIDATE"
    return "OTHER_LINE"


def safe_member(name: str) -> bool:
    if not name or "\\" in name or "\x00" in name:
        return False
    p = PurePosixPath(name)
    return not p.is_absolute() and all(x not in ("..", "") for x in p.parts) and not re.match(r"^[A-Za-z]:", name)


class BudgetExceeded(Exception):
    pass


class Indexer:
    def __init__(self, root: Path, output: Path, *, max_files: int, max_bytes: int,
                 max_entry_bytes: int, max_ratio: int, max_depth: int, private_text: bool):
        self.root, self.output = root.resolve(strict=True), output.resolve()
        if not self.root.is_dir():
            raise ValueError("SOURCE_ROOT_NOT_DIRECTORY")
        if self.output == self.root or self.root in self.output.parents or self.output in self.root.parents:
            raise ValueError("OUTPUT_AND_SOURCE_MUST_BE_DISJOINT")
        if self.output.exists():
            raise ValueError("OUTPUT_MUST_NOT_EXIST_APPEND_ONLY_RUN")
        if min(max_files, max_bytes, max_entry_bytes, max_ratio, max_depth) < 1:
            raise ValueError("BOUNDS_MUST_BE_POSITIVE")
        self.max_files, self.max_bytes = max_files, max_bytes
        self.max_entry_bytes, self.max_ratio, self.max_depth = max_entry_bytes, max_ratio, max_depth
        self.private_text = private_text
        self.bytes_seen = 0
        self.files_seen = 0
        self.partial = False
        self.errors: list[str] = []
        self.output.mkdir(parents=True, exist_ok=False, mode=0o700)
        os.chmod(self.output, 0o700)
        self.db = sqlite3.connect(self.output / "ATLAS.sqlite3")
        self.db.executescript("""
          create table entries(id text primary key, locator text unique, parent_id text,
             kind text, size_bytes integer, sha256 text, status text, depth integer);
          create table lines(entry_id text, line_no integer, fragment_no integer, offset_bytes integer,
             length_bytes integer, sha256 text, kind text, private_text text,
             primary key(entry_id,line_no,fragment_no));
          create index lines_kind on lines(kind);
          create index entries_parent on entries(parent_id);
        """)
        self.db.commit()

    def spend(self, n: int) -> None:
        if self.bytes_seen + n > self.max_bytes:
            self.partial = True
            raise BudgetExceeded("TOTAL_BYTES_BUDGET_REACHED")
        self.bytes_seen += n

    def put(self, locator: str, parent: str | None, kind: str,
            size: int | None, sha: str | None, status: str, depth: int) -> str:
        key = digest(locator.encode("utf-8", "surrogatepass"))
        self.db.execute("insert into entries values(?,?,?,?,?,?,?,?)",
                        (key, locator, parent, kind, size, sha, status, depth))
        return key

    def process_stream(self, f, locator: str, parent: str | None, kind: str,
                       declared_size: int | None, depth: int,
                       hard_limit: int | None = None) -> str:
        entry_id = digest(locator.encode("utf-8", "surrogatepass"))
        checksum = hashlib.sha256()
        size = 0
        line_no = 1
        fragment_no = 0
        status = "INDEXED"
        try:
            while True:
                # Split huge binary or text lines into individually bounded fragments.
                block = f.readline(65536)
                if not block:
                    break
                if hard_limit is not None and size + len(block) > hard_limit:
                    status = "SKIP_ENTRY_LIMIT_PARTIAL"
                    self.partial = True
                    break
                self.spend(len(block))
                checksum.update(block)
                fragment_no += 1
                self.db.execute("insert into lines values(?,?,?,?,?,?,?,?)", (
                    entry_id, line_no, fragment_no, size, len(block), digest(block),
                    line_kind(block) if fragment_no == 1 else "LINE_CONTINUATION",
                    block.decode("utf-8", "replace").rstrip("\r\n") if self.private_text else None,
                ))
                size += len(block)
                if block.endswith(b"\n"):
                    line_no += 1
                    fragment_no = 0
        except BudgetExceeded:
            status = "PARTIAL_BYTE_BUDGET"
        except (OSError, RuntimeError, zipfile.BadZipFile) as exc:
            status = "READ_ERROR"
            self.errors.append(f"{locator}:{type(exc).__name__}")
        # A partial read MUST NOT be labeled as a complete-file SHA.
        self.put(locator, parent, kind, declared_size if declared_size is not None else size,
                 checksum.hexdigest() if status == "INDEXED" else None, status, depth)
        return entry_id

    def zip_members(self, zf: zipfile.ZipFile, container: str, parent_id: str, depth: int) -> None:
        infos = zf.infolist()
        if len(infos) > 10000:
            self.errors.append(f"{container}:ZIP_MEMBER_COUNT_LIMIT")
            self.partial = True
            return
        counts: dict[str, int] = {}
        for info in sorted(infos, key=lambda x: x.filename):
            locator = container + "!" + info.filename
            counts[info.filename] = counts.get(info.filename, 0) + 1
            if counts[info.filename] > 1:
                self.put(locator + f"#duplicate-{counts[info.filename]}", parent_id, "ZIP_MEMBER",
                         info.file_size, None, "DUPLICATE_NAME_BLOCKED", depth)
                self.partial = True
                continue
            if not safe_member(info.filename):
                self.put(locator, parent_id, "ZIP_MEMBER", info.file_size, None,
                         "UNSAFE_MEMBER_PATH", depth)
                self.partial = True
                continue
            if info.is_dir():
                self.put(locator, parent_id, "ZIP_DIRECTORY", 0, None, "INDEXED_METADATA", depth)
                continue
            unix_mode = info.external_attr >> 16
            reason = None
            if stat.S_ISLNK(unix_mode):
                reason = "SYMLINK_MEMBER_BLOCKED"
            elif info.flag_bits & 1:
                reason = "ENCRYPTED_MEMBER_BLOCKED"
            elif info.file_size > self.max_entry_bytes:
                reason = "MEMBER_TOO_LARGE"
            elif info.file_size > self.max_ratio * max(info.compress_size, 1):
                reason = "SUSPICIOUS_COMPRESSION_RATIO"
            if reason:
                self.put(locator, parent_id, "ZIP_MEMBER", info.file_size, None, reason, depth)
                self.partial = True
                continue
            try:
                with zf.open(info) as member:
                    if info.filename.lower().endswith(".zip") and depth < self.max_depth:
                        raw = member.read(self.max_entry_bytes + 1)
                        if len(raw) > self.max_entry_bytes:
                            self.put(locator, parent_id, "ZIP", info.file_size, None,
                                     "NESTED_MEMBER_LIMIT", depth)
                            self.partial = True
                            continue
                        mid = self.process_stream(io.BytesIO(raw), locator, parent_id,
                                                  "ZIP", len(raw), depth, self.max_entry_bytes)
                        if self.db.execute("select status from entries where id=?", (mid,)).fetchone()[0] != "INDEXED":
                            continue
                        try:
                            with zipfile.ZipFile(io.BytesIO(raw)) as nested:
                                self.zip_members(nested, locator, mid, depth + 1)
                        except zipfile.BadZipFile:
                            self.errors.append(f"{locator}:INVALID_NESTED_ZIP")
                            self.partial = True
                    else:
                        self.process_stream(member, locator, parent_id, "ZIP_MEMBER",
                                            info.file_size, depth, self.max_entry_bytes)
                        if info.filename.lower().endswith(".zip"):
                            self.errors.append(f"{locator}:ZIP_DEPTH_LIMIT")
                            self.partial = True
            except (OSError, RuntimeError, zipfile.BadZipFile, ValueError) as exc:
                self.errors.append(f"{locator}:{type(exc).__name__}")
                self.partial = True
                if not self.db.execute("select 1 from entries where locator=?", (locator,)).fetchone():
                    self.put(locator, parent_id, "ZIP_MEMBER", info.file_size, None, "READ_ERROR", depth)

    def visit(self) -> None:
        exhausted = False
        for current, dirs, files in os.walk(self.root, followlinks=False):
            for dirname in dirs:
                link = Path(current) / dirname
                if link.is_symlink():
                    rel = link.relative_to(self.root).as_posix()
                    self.put(rel, None, "SYMLINK_DIRECTORY", None, None, "SYMLINK_BLOCKED", 0)
                    self.partial = True
            dirs[:] = sorted(x for x in dirs if not (Path(current) / x).is_symlink())
            for name in sorted(files):
                source = Path(current) / name
                if source.is_symlink():
                    rel = source.relative_to(self.root).as_posix()
                    self.put(rel, None, "SYMLINK_FILE", None, None, "SYMLINK_BLOCKED", 0)
                    self.partial = True
                    continue
                if not source.is_file():
                    self.partial = True
                    self.errors.append(source.relative_to(self.root).as_posix() + ":NON_REGULAR_FILE")
                    continue
                if self.files_seen >= self.max_files or self.bytes_seen >= self.max_bytes:
                    self.partial = True
                    exhausted = True
                    break
                self.files_seen += 1
                rel = source.relative_to(self.root).as_posix()
                try:
                    size = source.stat().st_size
                    with source.open("rb") as f:
                        ident = self.process_stream(f, rel, None,
                                                    "ZIP" if source.suffix.lower() == ".zip" else "FILE",
                                                    size, 0)
                    if source.suffix.lower() == ".zip" and self.db.execute(
                         "select status from entries where id=?", (ident,)).fetchone()[0] == "INDEXED":
                        with zipfile.ZipFile(source) as zf:
                            self.zip_members(zf, rel, ident, 1)
                except (OSError, zipfile.BadZipFile, RuntimeError) as exc:
                    self.errors.append(f"{rel}:{type(exc).__name__}")
                    self.partial = True
            self.db.commit()
            if exhausted:
                break

    def finish(self) -> dict:
        counts = {r[0]: r[1] for r in self.db.execute(
            "select status,count(*) from entries group by status")}
        nodes = self.db.execute("select count(*) from entries").fetchone()[0]
        lines = self.db.execute("select count(*) from lines").fetchone()[0]
        report = {
            "schema": SCHEMA, "scope": "LOCAL_MIRROR_ONLY", "privacy": "PRIVATE_DEFAULT_DENY",
            "source_root": "REDACTED_LOCAL_ROOT", "files_touched": self.files_seen,
            "nodes": nodes, "line_fragments": lines, "bytes_read": self.bytes_seen,
            "statuses": counts, "complete": not self.partial and not self.errors,
            "gaps": self.errors[:32], "private_text_enabled": self.private_text,
            "claim_allowed": False, "source_mutated": False, "external_transfer": False,
        }
        self.db.commit()
        self.db.close()
        (self.output / "RECEIPT.json").write_text(json.dumps(report, indent=2, ensure_ascii=False,
                                                       sort_keys=True) + "\n", encoding="utf-8")
        (self.output / "START_HERE.md").write_text(
            "# Private file / ZIP / line Atlas\n\n"
            "LOCAL_ONLY; PRIVATE_DEFAULT_DENY. No content was uploaded.\n\n"
            "ATLAS.sqlite3: entries (hierarchy/path/hash/status), lines (line number, fragment, byte offset/hash/type).\n"
            "ZIP members use outer.zip!path!nested locators. Open original source for retrieval.\n"
            "Bytes absent, archive failures and truncation are gaps, never empty-file claims.\n"
            "Line text is omitted unless private-text was explicitly enabled.\n"
            "RECEIPT.json records bounded coverage; claim_allowed=false.\n",
            encoding="utf-8")
        return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True, help="local, explicitly authorized directory")
    parser.add_argument("--output", type=Path, required=True, help="new private directory, outside source")
    parser.add_argument("--max-files", type=int, default=500)
    parser.add_argument("--max-bytes", type=int, default=64 << 20)
    parser.add_argument("--max-entry-bytes", type=int, default=8 << 20)
    parser.add_argument("--max-ratio", type=int, default=200)
    parser.add_argument("--max-depth", type=int, default=2)
    parser.add_argument("--private-text", action="store_true")
    args = parser.parse_args(argv)
    engine = Indexer(args.source, args.output, max_files=args.max_files,
                     max_bytes=args.max_bytes, max_entry_bytes=args.max_entry_bytes,
                     max_ratio=args.max_ratio, max_depth=args.max_depth,
                     private_text=args.private_text)
    engine.visit()
    print(json.dumps(engine.finish(), sort_keys=True, ensure_ascii=False))
    return 0 if not engine.partial and not engine.errors else 2


if __name__ == "__main__":
    raise SystemExit(main())
