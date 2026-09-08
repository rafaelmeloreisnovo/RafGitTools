#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
GATE = HERE / "rafaelia_navigator_canonical_gate.py"


def conversation(shard: int) -> list[dict]:
    cid = f"conv-{shard}"
    mid = f"msg-{shard}"
    nid = f"node-{shard}"
    return [
        {
            "id": cid,
            "title": f"fixture {shard}",
            "create_time": 1 + shard,
            "update_time": 2 + shard,
            "mapping": {
                nid: {
                    "id": nid,
                    "parent": None,
                    "message": {
                        "id": mid,
                        "author": {"role": "user"},
                        "create_time": 3 + shard,
                        "content": {"content_type": "text", "parts": [f"hello {shard}"]},
                        "status": "finished_successfully",
                    },
                }
            },
        }
    ]


class CanonicalGateTest(unittest.TestCase):
    def write_shards(self, root: Path, count: int) -> int:
        total = 0
        for i in range(count):
            p = root / f"conversations-{i:03d}.json"
            p.write_text(json.dumps(conversation(i), ensure_ascii=False), encoding="utf-8")
            total += p.stat().st_size
        return total

    def run_gate(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(GATE), *args],
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )

    def test_exact_fixture_builds_and_verifies(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "src"
            out = root / "out"
            src.mkdir()
            total = self.write_shards(src, 3)
            cp = self.run_gate(
                str(src),
                str(out),
                "--start", "0",
                "--end", "2",
                "--segment-records", "2",
                "--expected-total-bytes", str(total),
            )
            self.assertEqual(cp.returncode, 0, cp.stderr + cp.stdout)
            self.assertIn("CANONICAL_GATE=PASS", cp.stdout)
            manifest = json.loads((out / "CANONICAL_NAVIGATOR_MANIFEST_V1.json").read_text(encoding="utf-8"))
            self.assertEqual(manifest["source_range"], {"start": 0, "end": 2, "count": 3})
            self.assertEqual(manifest["navigator"]["counts"]["messages"], 3)
            self.assertEqual(manifest["messages_projection"]["records"], 3)
            self.assertEqual(manifest["messages_projection"]["segment_count"], 2)
            self.assertEqual(manifest["messages_projection"]["source_paths_without_messages"], [])
            for seg in manifest["messages_projection"]["segments"]:
                self.assertEqual(len(seg["sha256"]), 64)

    def test_missing_shard_fails_before_build(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "src"
            out = root / "out"
            src.mkdir()
            self.write_shards(src, 2)
            cp = self.run_gate(str(src), str(out), "--start", "0", "--end", "2")
            self.assertNotEqual(cp.returncode, 0)
            self.assertIn("canonical_source_set_not_exact", cp.stderr)
            self.assertFalse((out / "RAFAELIA_NAVIGATOR.sqlite3").exists())

    def test_duplicate_canonical_name_fails(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "src"
            out = root / "out"
            (src / "a").mkdir(parents=True)
            (src / "b").mkdir(parents=True)
            for parent in (src / "a", src / "b"):
                (parent / "conversations-000.json").write_text(
                    json.dumps(conversation(0), ensure_ascii=False), encoding="utf-8"
                )
            cp = self.run_gate(str(src), str(out), "--start", "0", "--end", "0", "--preflight-only")
            self.assertNotEqual(cp.returncode, 0)
            self.assertIn("canonical_source_set_not_exact", cp.stderr)
            self.assertIn("duplicates", cp.stderr)

    def test_expected_byte_drift_fails(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            src = root / "src"
            out = root / "out"
            src.mkdir()
            total = self.write_shards(src, 1)
            cp = self.run_gate(
                str(src), str(out), "--start", "0", "--end", "0",
                "--expected-total-bytes", str(total + 1), "--preflight-only"
            )
            self.assertNotEqual(cp.returncode, 0)
            self.assertIn("canonical_total_bytes_mismatch", cp.stderr)


if __name__ == "__main__":
    unittest.main()
