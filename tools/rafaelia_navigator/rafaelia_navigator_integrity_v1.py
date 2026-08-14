#!/usr/bin/env python3
"""Integrity overlay for RAFAELIA Navigator V1.

Preserves every mapping node, validates parent edges against the complete tree,
keeps cumulative counts stable across resume, appends segment numbers, and
blocks a previously indexed source if its content hash changes.
"""
from __future__ import annotations

import argparse
import json
import sqlite3
import sys
import tempfile
from pathlib import Path

import rafaelia_navigator as nav


class SegmentWriter(nav.Seg):
    def __init__(self, directory: Path, prefix: str, max_records: int):
        self.d = directory
        self.prefix = prefix
        self.maxn = max_records
        self.n = 0
        self.f = None
        indexes = []
        for path in directory.glob(f"{prefix}-*.jsonl.txt"):
            try:
                indexes.append(int(path.name.split("-")[-1].split(".")[0]))
            except ValueError:
                pass
        self.i = max(indexes, default=0)


nav.Seg = SegmentWriter


class IntegrityBuild(nav.Build):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.count.update(nodes=0, empty_messages=0, orphans=0)

    def schema(self):
        super().schema()
        self.c.executescript(
            """
            create table if not exists nodes(
              node_key text primary key,
              conversation_id text,
              node_id text,
              parent_id text,
              has_message integer,
              source_path text,
              source_pointer text
            );
            create index if not exists node_conv on nodes(conversation_id);
            create index if not exists node_parent on nodes(conversation_id,parent_id);
            """
        )
        self.c.commit()

    def conv(self, path: Path, relative: str):
        records = 0
        for conversation_index, value in enumerate(nav.records(path)):
            if not isinstance(value, dict):
                continue
            records += 1
            conversation_id = str(
                value.get("id")
                or value.get("conversation_id")
                or nav.htext(f"{relative}:{conversation_index}")
            )
            mapping = value.get("mapping") if isinstance(value.get("mapping"), dict) else {}
            structural_hash = nav.htext(
                "\n".join(
                    sorted(
                        f"{node_id}>{node.get('parent')}"
                        for node_id, node in mapping.items()
                        if isinstance(node, dict)
                    )
                )
            )
            self.c.execute(
                "insert or replace into conversations values(?,?,?,?,?,?,?,?,?,0)",
                (
                    conversation_id,
                    nav.htext(str(value.get("title") or "")),
                    nav.scalar(value.get("create_time")),
                    nav.scalar(value.get("update_time")),
                    relative,
                    f"{relative}#conversation[{conversation_index}]",
                    structural_hash,
                    "PRIVATE_DEFAULT_DENY",
                    "SOURCE_OBSERVED",
                ),
            )
            for node_id, node in mapping.items():
                if not isinstance(node, dict):
                    continue
                pointer = (
                    f"{relative}#conversation[{conversation_index}]"
                    f".mapping[{json.dumps(str(node_id))}]"
                )
                node_key = nav.htext(f"{conversation_id}\x1f{node_id}")
                self.c.execute(
                    "insert or replace into nodes values(?,?,?,?,?,?,?)",
                    (
                        node_key,
                        conversation_id,
                        str(node_id),
                        nav.scalar(node.get("parent")),
                        1 if isinstance(node.get("message"), dict) else 0,
                        relative,
                        pointer,
                    ),
                )
            for node_id, node in mapping.items():
                if not isinstance(node, dict) or not isinstance(node.get("message"), dict):
                    continue
                message = node["message"]
                message_id = str(message.get("id") or node_id)
                text, content_type = nav.text_of(message)
                text_hash = nav.htext(text)
                asset_refs = nav.assets(message)
                pointer = (
                    f"{relative}#conversation[{conversation_index}]"
                    f".mapping[{json.dumps(str(node_id))}]"
                )
                error = (
                    message.get("status")
                    if message.get("status") not in (None, "finished_successfully")
                    else None
                )
                self.c.execute(
                    "insert or replace into messages values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
                    (
                        message_id,
                        conversation_id,
                        str(node_id),
                        nav.scalar(node.get("parent")),
                        nav.role(message),
                        nav.scalar(message.get("create_time")),
                        content_type,
                        text,
                        text_hash,
                        relative,
                        pointer,
                        json.dumps(asset_refs, ensure_ascii=False),
                        json.dumps(error, ensure_ascii=False) if error is not None else None,
                        "PRIVATE_DEFAULT_DENY",
                        "SOURCE_OBSERVED",
                    ),
                )
                if self.fts:
                    self.c.execute("delete from messages_fts where message_id=?", (message_id,))
                    self.c.execute(
                        "insert into messages_fts values(?,?,?,?)",
                        (message_id, conversation_id, nav.role(message), text),
                    )
                self.msgseg.put(
                    {
                        "kind": "message",
                        "conversation_id": conversation_id,
                        "message_id": message_id,
                        "node_id": str(node_id),
                        "parent_id": nav.scalar(node.get("parent")),
                        "role": nav.role(message),
                        "create_time": nav.scalar(message.get("create_time")),
                        "content_type": content_type,
                        "text": text,
                        "text_hash": text_hash,
                        "asset_refs": asset_refs,
                        "source_pointer": pointer,
                        "source_path": relative,
                        "privacy_class": "PRIVATE_DEFAULT_DENY",
                        "epistemic_state": "SOURCE_OBSERVED",
                        "target_adapter": ["GAIA_L1_L3", "RMRALPHA"],
                        "claim_allowed": False,
                    }
                )
        return records

    def refresh_totals(self):
        counts = {
            "files": "select count(*) from source_files where status='COMPLETE'",
            "conversations": "select count(*) from conversations",
            "nodes": "select count(*) from nodes",
            "messages": "select count(*) from messages",
            "codex": "select count(*) from codex_records",
            "assets": "select count(*) from assets",
            "empty_messages": "select count(*) from messages where trim(text)=''",
            "errors": "select count(*) from source_files where status<>'COMPLETE'",
        }
        for key, sql in counts.items():
            self.count[key] = self.c.execute(sql).fetchone()[0]
        self.count["duplicates"] = self.c.execute(
            "select coalesce(sum(n-1),0) from "
            "(select count(*) n from messages group by text_hash having count(*)>1)"
        ).fetchone()[0]
        self.count["orphans"] = self.c.execute(
            """
            select count(*) from nodes n
            where n.parent_id is not null
              and not exists (
                select 1 from nodes p
                where p.conversation_id=n.conversation_id
                  and p.node_id=n.parent_id
              )
            """
        ).fetchone()[0]

    def outputs(self):
        self.refresh_totals()
        super().outputs()

    def run(self):
        files = self.files()
        if not files:
            print("no eligible files", file=sys.stderr)
            return 2
        for index, path in enumerate(files, 1):
            relative = path.relative_to(self.src).as_posix()
            stat = path.stat()
            digest = nav.hfile(path)
            previous = self.c.execute(
                "select sha256,bytes,status from source_files where path=?", (relative,)
            ).fetchone()
            if previous and previous == (digest, stat.st_size, "COMPLETE"):
                self.checkpoint(
                    {
                        "event": "SKIP_UNCHANGED",
                        "cursor_initial": relative,
                        "cursor_final": relative,
                        "source_hash": digest,
                    }
                )
                print(f"SKIP {index}/{len(files)} {relative}")
                continue
            if previous and previous[0] != digest:
                message = f"immutable source hash changed: {previous[0]} -> {digest}"
                self.c.execute(
                    "update source_files set status='BLOCKED_SOURCE_CHANGED',"
                    "processed_at=?,error=? where path=?",
                    (nav.now(), message, relative),
                )
                self.c.commit()
                self.checkpoint(
                    {
                        "event": "SOURCE_CHANGED_BLOCKED",
                        "cursor_initial": relative,
                        "cursor_final": relative,
                        "previous_source_hash": previous[0],
                        "source_hash": digest,
                        "error": message,
                    }
                )
                print(f"BLOCKED {relative}: {message}", file=sys.stderr)
                continue
            kind = (
                "conversation"
                if path.name.startswith("conversations-")
                else "codex"
                if path.name.startswith("codex-")
                else "asset_manifest"
                if path.name == "conversation_asset_file_names.json"
                else "manifest"
            )
            try:
                with self.c:
                    records = (
                        self.conv(path, relative)
                        if kind == "conversation"
                        else self.codex(path, relative)
                        if kind == "codex"
                        else self.assetmanifest(path, relative)
                        if kind == "asset_manifest"
                        else sum(1 for _ in nav.records(path))
                    )
                    self.c.execute(
                        "insert into source_files values(?,?,?,?,?,'COMPLETE',?,?,null) "
                        "on conflict(path) do update set sha256=excluded.sha256,"
                        "bytes=excluded.bytes,mtime_ns=excluded.mtime_ns,kind=excluded.kind,"
                        "status='COMPLETE',processed_at=excluded.processed_at,"
                        "records=excluded.records,error=null",
                        (
                            relative,
                            digest,
                            stat.st_size,
                            stat.st_mtime_ns,
                            kind,
                            nav.now(),
                            records,
                        ),
                    )
                self.srcseg.put(
                    {
                        "path": relative,
                        "kind": kind,
                        "bytes": stat.st_size,
                        "sha256": digest,
                        "records": records,
                        "privacy_class": "PRIVATE_DEFAULT_DENY",
                        "claim_allowed": False,
                    }
                )
                self.checkpoint(
                    {
                        "event": "FILE_COMPLETE",
                        "cursor_initial": f"{relative}#record[0]",
                        "cursor_final": f"{relative}#record[{max(0, records-1)}]",
                        "source_hash": digest,
                        "records": records,
                        "bytes": stat.st_size,
                    }
                )
                print(f"CHECKPOINT {index}/{len(files)} COMPLETE {relative} records={records}")
            except Exception as error:
                self.c.execute(
                    "insert into source_files values(?,?,?,?,?,'BLOCKED',?,0,?) "
                    "on conflict(path) do update set status='BLOCKED',"
                    "processed_at=excluded.processed_at,error=excluded.error",
                    (
                        relative,
                        digest,
                        stat.st_size,
                        stat.st_mtime_ns,
                        kind,
                        nav.now(),
                        f"{type(error).__name__}: {error}",
                    ),
                )
                self.c.commit()
                print(f"BLOCKED {relative}: {error}", file=sys.stderr)
        self.msgseg.close()
        self.codseg.close()
        self.srcseg.close()
        self.outputs()
        self.c.execute("pragma wal_checkpoint(truncate)")
        self.c.close()
        print("RAFAELIA_NAVIGATOR_PASS")
        for key, value in self.count.items():
            print(f"{key}={value}")
        return 0 if not self.count["errors"] else 4


def selftest():
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        source = root / "NOVOexport"
        output = root / "output"
        source.mkdir()
        (source / "conversations-000.json").write_text(
            json.dumps(
                [
                    {
                        "id": "c1",
                        "title": "private",
                        "mapping": {
                            "n0": {"parent": None, "message": None},
                            "n1": {
                                "parent": "n0",
                                "message": {
                                    "id": "m1",
                                    "author": {"role": "user"},
                                    "content": {
                                        "content_type": "text",
                                        "parts": ["Termux ARMv7"],
                                    },
                                },
                            },
                        },
                    }
                ]
            ),
            encoding="utf-8",
        )
        (source / "codex-000.json").write_text(
            json.dumps([{"id": "t1", "repository": "r/x", "text": "RafPolimata"}]),
            encoding="utf-8",
        )
        (source / "conversation_asset_file_names.json").write_text(
            json.dumps({"a1": {"file_name": "f.dat", "message_id": "m1"}}),
            encoding="utf-8",
        )
        assert IntegrityBuild(source, output, seg=1).run() == 0
        connection = sqlite3.connect(output / "RAFAELIA_NAVIGATOR.sqlite3")
        assert connection.execute("select count(*) from nodes").fetchone()[0] == 2
        assert connection.execute(
            "select count(*) from nodes n where n.parent_id is not null and not exists "
            "(select 1 from nodes p where p.conversation_id=n.conversation_id "
            "and p.node_id=n.parent_id)"
        ).fetchone()[0] == 0
        connection.close()
        assert IntegrityBuild(source, output).run() == 0
        manifest = json.loads((output / "MANIFEST.json").read_text())
        assert manifest["counts"]["nodes"] == 2
        assert manifest["counts"]["orphans"] == 0
        print("INTEGRITY_SELFTEST_PASS")
        return 0


def main():
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    build = subparsers.add_parser("build")
    build.add_argument("source", type=Path)
    build.add_argument("output", type=Path)
    build.add_argument("--max-files", type=int)
    build.add_argument("--segment-records", type=int, default=5000)
    query = subparsers.add_parser("query")
    query.add_argument("database", type=Path)
    query.add_argument("query")
    query.add_argument("--kind", choices=["message", "codex", "all"], default="all")
    query.add_argument("--limit", type=int, default=20)
    subparsers.add_parser("selftest")
    arguments = parser.parse_args()
    if arguments.command == "build":
        arguments.output.mkdir(parents=True, exist_ok=True)
        return IntegrityBuild(
            arguments.source.expanduser().resolve(),
            arguments.output.expanduser().resolve(),
            arguments.max_files,
            max(1, arguments.segment_records),
        ).run()
    if arguments.command == "query":
        return nav.query(arguments.database, arguments.query, arguments.kind, arguments.limit)
    return selftest()


if __name__ == "__main__":
    raise SystemExit(main())
