#!/usr/bin/env python3
"""Typed ATLAS graph navigation; unproved links are opt-in and labeled."""
import argparse
from collections import deque
import json
from pathlib import Path

SOURCE = Path(__file__).resolve().parents[1] / "cases/atlas_edges.json"


def load(path=SOURCE):
    g = json.loads(Path(path).read_text(encoding="utf-8"))
    ids = {n["id"] for n in g["nodes"]}
    assert len(ids) == len(g["nodes"]), "DUPLICATED_NODES"
    for e in g["edges"]:
        assert e["from"] in ids and e["to"] in ids, "DANGLING_ROUTE"
        assert e["axis"] in g["l9"], "UNKNOWN_ROUTE_AXIS"
        assert e["state"] in g["states"], "UNKNOWN_STATE"
        if e["state"] == "HISTORICAL_DOCUMENT_ONLY":
            assert str(e["proof"]).startswith("https://"), "MISSING_HISTORICAL_SOURCE"
        if e["state"] == "TOKEN_VAZIO":
            assert not e["proof"], "UNKNOWN_PROOF_MUST_NOT_BE_INVENTED"
    return g


def route(g, start, end, include_unverified=False):
    queue = deque([(start, [])])
    seen = {start}
    while queue:
        cur, walk = queue.popleft()
        if cur == end:
            return walk
        for e in g["edges"]:
            if e["from"] != cur or e["to"] in seen:
                continue
            if not include_unverified and e["state"] != "HISTORICAL_DOCUMENT_ONLY":
                continue
            queue.append((e["to"], walk + [e]))
            seen.add(e["to"])
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("start", nargs="?", default="ARM32")
    parser.add_argument("end", nargs="?", default="TLB")
    parser.add_argument("--include-unverified", action="store_true")
    a = parser.parse_args()
    g = load()
    r = route(g, a.start, a.end, a.include_unverified)
    if r is None:
        print("BLOCKED_ROUTE_NO_PROVEN_PATH / TOKEN_VAZIO")
        return 2
    print(json.dumps({"path": r, "unverified": any(
        e["state"] != "HISTORICAL_DOCUMENT_ONLY" for e in r)}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
