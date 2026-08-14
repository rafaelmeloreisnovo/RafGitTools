#!/usr/bin/env python3
"""Set-based FTS overlay for RAFAELIA Navigator V1.2.

Uses the full-node integrity contract while moving FTS population from one SQL
operation per message to one set-based operation per source shard.
"""
from __future__ import annotations

import argparse
import json
import sqlite3
import tempfile
from pathlib import Path

import rafaelia_navigator as nav
from rafaelia_navigator_integrity_v1 import IntegrityBuild


class BulkBuild(IntegrityBuild):
    def conv(self, path: Path, relative: str):
        fts_enabled = self.fts
        self.fts = False
        try:
            records = super().conv(path, relative)
        finally:
            self.fts = fts_enabled
        if fts_enabled:
            self.c.execute(
                "delete from messages_fts where message_id in "
                "(select message_id from messages where source_path=?)",
                (relative,),
            )
            self.c.execute(
                "insert into messages_fts "
                "select message_id,conversation_id,role,text from messages "
                "where source_path=?",
                (relative,),
            )
        return records


def selftest():
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        source = root / "NOVOexport"
        output = root / "output"
        source.mkdir()
        mapping = {
            "n0": {"parent": None, "message": None},
            "n1": {
                "parent": "n0",
                "message": {
                    "id": "m1",
                    "author": {"role": "user"},
                    "content": {"content_type": "text", "parts": ["Termux"]},
                },
            },
        }
        (source / "conversations-000.json").write_text(
            json.dumps([{"id": "c1", "mapping": mapping}]), encoding="utf-8"
        )
        assert BulkBuild(source, output).run() == 0
        connection = sqlite3.connect(output / "RAFAELIA_NAVIGATOR.sqlite3")
        assert connection.execute("select count(*) from nodes").fetchone()[0] == 2
        assert connection.execute("select count(*) from messages_fts").fetchone()[0] == 1
        assert connection.execute(
            "select count(*) from messages_fts where messages_fts match 'termux'"
        ).fetchone()[0] == 1
        connection.close()
        assert BulkBuild(source, output).run() == 0
        print("BULK_FTS_SELFTEST_PASS")
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
        return BulkBuild(
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
