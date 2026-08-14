#!/usr/bin/env python3
"""Deterministic publication of RAFAELIA Navigator SQLite into private segments."""
from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import tempfile
from pathlib import Path
from typing import Iterator

CHUNK = 1 << 20
SCHEMA = "RAFAELIA_NAVIGATOR_PUBLICATION_V1"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(CHUNK), b""):
            digest.update(block)
    return digest.hexdigest()


def merkle_root(hashes: list[str]) -> str:
    if not hashes:
        return hashlib.sha256(b"").hexdigest()
    level = [bytes.fromhex(value) for value in hashes]
    while len(level) > 1:
        if len(level) % 2:
            level.append(level[-1])
        level = [
            hashlib.sha256(level[index] + level[index + 1]).digest()
            for index in range(0, len(level), 2)
        ]
    return level[0].hex()


def dict_rows(cursor: sqlite3.Cursor) -> Iterator[dict]:
    names = [column[0] for column in cursor.description]
    for values in cursor:
        yield dict(zip(names, values))


def normalized_row(kind: str, row: dict) -> dict:
    value = {
        "kind": kind,
        **row,
        "privacy_class": row.get("privacy_class", "PRIVATE_DEFAULT_DENY"),
        "claim_allowed": bool(row.get("claim_allowed", 0)),
    }
    for key in ("asset_refs_json", "error_json"):
        if isinstance(value.get(key), str):
            try:
                value[key.removesuffix("_json")] = json.loads(value.pop(key))
            except json.JSONDecodeError:
                value[key.removesuffix("_json")] = value.pop(key)
    return value


class SegmentSink:
    def __init__(self, output: Path, prefix: str, records_per_file: int):
        self.output = output
        self.prefix = prefix
        self.records_per_file = records_per_file
        self.index = 0
        self.count = 0
        self.current_count = 0
        self.stream = None
        self.paths: list[Path] = []

    def write(self, value: dict):
        if self.stream is None or self.current_count >= self.records_per_file:
            self.close_current()
            self.index += 1
            self.current_count = 0
            path = self.output / f"{self.prefix}-{self.index:05d}.jsonl.txt"
            self.stream = path.open("x", encoding="utf-8", newline="\n")
            self.paths.append(path)
        self.stream.write(
            json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
            + "\n"
        )
        self.current_count += 1
        self.count += 1

    def close_current(self):
        if self.stream is not None:
            self.stream.close()
            self.stream = None

    def close(self):
        self.close_current()


def require_empty(output: Path):
    if output.exists() and any(output.iterdir()):
        raise RuntimeError(f"publication target is not empty: {output}")
    output.mkdir(parents=True, exist_ok=True)


def publish(database: Path, output: Path, records_per_file: int = 5000) -> dict:
    require_empty(output)
    uri = f"file:{database.resolve()}?mode=ro"
    connection = sqlite3.connect(uri, uri=True)
    connection.execute("pragma query_only=on")
    required = {
        "source_files",
        "conversations",
        "nodes",
        "messages",
        "codex_records",
        "assets",
    }
    existing = {
        row[0]
        for row in connection.execute(
            "select name from sqlite_master where type='table'"
        )
    }
    missing = sorted(required - existing)
    if missing:
        raise RuntimeError(f"missing committed tables: {missing}")

    queries = [
        (
            "SOURCES",
            "source",
            "select path,sha256,bytes,mtime_ns,kind,status,processed_at,records,error "
            "from source_files order by path",
        ),
        (
            "CONVERSATIONS",
            "conversation",
            "select conversation_id,title_hash,create_time,update_time,source_path,"
            "source_pointer,structural_hash,privacy_class,epistemic_state,claim_allowed "
            "from conversations order by source_path,conversation_id",
        ),
        (
            "NODES",
            "node",
            "select conversation_id,node_id,parent_id,has_message,source_path,source_pointer "
            "from nodes order by source_path,conversation_id,node_id",
        ),
        (
            "MESSAGES",
            "message",
            "select message_id,conversation_id,node_id,parent_id,role,create_time,"
            "content_type,text,text_hash,source_path,source_pointer,asset_refs_json,error_json,"
            "privacy_class,epistemic_state,claim_allowed from messages "
            "order by source_path,conversation_id,node_id,message_id",
        ),
        (
            "CODEX",
            "codex",
            "select record_id,task,repository,branch,commit_sha,pr,path,diff_hash,text,"
            "text_hash,source_path,source_pointer,privacy_class,epistemic_state,claim_allowed "
            "from codex_records order by source_path,record_id",
        ),
        (
            "ASSETS",
            "asset",
            "select asset_key,original_name,stored_name,conversation_id,message_id,"
            "source_path,source_pointer,claim_allowed from assets order by source_path,asset_key",
        ),
    ]

    sinks: list[SegmentSink] = []
    counts: dict[str, int] = {}
    try:
        for prefix, kind, sql in queries:
            sink = SegmentSink(output, prefix, records_per_file)
            sinks.append(sink)
            cursor = connection.execute(sql)
            for row in dict_rows(cursor):
                sink.write(normalized_row(kind, row))
            sink.close()
            counts[kind] = sink.count
    finally:
        for sink in sinks:
            sink.close()
        connection.close()

    artifacts = []
    for path in sorted(output.glob("*.jsonl.txt")):
        artifacts.append(
            {
                "path": path.name,
                "bytes": path.stat().st_size,
                "sha256": sha256_file(path),
            }
        )
    manifest = {
        "schema": SCHEMA,
        "privacy_class": "PRIVATE_DEFAULT_DENY",
        "claim_allowed": False,
        "training_executed": False,
        "source_database": database.name,
        "source_database_bytes": database.stat().st_size,
        "source_database_sha256": sha256_file(database),
        "records_per_file": records_per_file,
        "counts": counts,
        "artifacts": artifacts,
        "merkle_root": merkle_root([item["sha256"] for item in artifacts]),
        "F_ok": "segments rebuilt only from committed SQLite tables",
        "F_gap": "publication does not include image bytes or graph/memory reconciliation",
        "F_next": "publish the private segments and validate byte-identical round-trip",
    }
    manifest_path = output / "PUBLICATION_MANIFEST.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print("NAVIGATOR_PUBLICATION_PASS")
    for key, value in counts.items():
        print(f"{key}={value}")
    print(f"segments={len(artifacts)}")
    print(f"merkle_root={manifest['merkle_root']}")
    return manifest


def selftest() -> int:
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        database = root / "navigator.sqlite3"
        connection = sqlite3.connect(database)
        connection.executescript(
            """
            create table source_files(path text,sha256 text,bytes integer,mtime_ns integer,
              kind text,status text,processed_at text,records integer,error text);
            create table conversations(conversation_id text,title_hash text,create_time text,
              update_time text,source_path text,source_pointer text,structural_hash text,
              privacy_class text,epistemic_state text,claim_allowed integer);
            create table nodes(node_key text,conversation_id text,node_id text,parent_id text,
              has_message integer,source_path text,source_pointer text);
            create table messages(message_id text,conversation_id text,node_id text,parent_id text,
              role text,create_time text,content_type text,text text,text_hash text,source_path text,
              source_pointer text,asset_refs_json text,error_json text,privacy_class text,
              epistemic_state text,claim_allowed integer);
            create table codex_records(record_id text,task text,repository text,branch text,
              commit_sha text,pr text,path text,diff_hash text,text text,text_hash text,
              source_path text,source_pointer text,privacy_class text,epistemic_state text,
              claim_allowed integer);
            create table assets(asset_key text,original_name text,stored_name text,
              conversation_id text,message_id text,source_path text,source_pointer text,
              claim_allowed integer);
            insert into source_files values('c.json','aa',1,1,'conversation','COMPLETE','t',1,null);
            insert into conversations values('c','th',null,null,'c.json','p','sh',
              'PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED',0);
            insert into nodes values('k0','c','n0',null,0,'c.json','p0');
            insert into nodes values('k1','c','n1','n0',1,'c.json','p1');
            insert into messages values('m','c','n1','n0','user',null,'text','Termux','tx',
              'c.json','p1','[]',null,'PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED',0);
            insert into codex_records values('r','task','o/r','main','abc','1','a.c',null,
              'code','cx','x.json','px','PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED',0);
            insert into assets values('a','name.png','file.dat','c','m','a.json','pa',0);
            """
        )
        connection.commit()
        connection.close()
        first = root / "first"
        second = root / "second"
        manifest_a = publish(database, first, 1)
        manifest_b = publish(database, second, 1)
        assert manifest_a == manifest_b
        files_a = {p.name: sha256_file(p) for p in first.iterdir()}
        files_b = {p.name: sha256_file(p) for p in second.iterdir()}
        assert files_a == files_b
        assert manifest_a["counts"]["node"] == 2
        print("PUBLICATION_SELFTEST_PASS")
        return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)
    command = subparsers.add_parser("publish")
    command.add_argument("database", type=Path)
    command.add_argument("output", type=Path)
    command.add_argument("--records-per-file", type=int, default=5000)
    subparsers.add_parser("selftest")
    args = parser.parse_args()
    if args.command == "selftest":
        return selftest()
    publish(
        args.database.expanduser().resolve(),
        args.output.expanduser().resolve(),
        max(1, args.records_per_file),
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
