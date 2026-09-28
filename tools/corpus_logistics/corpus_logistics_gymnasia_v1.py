#!/usr/bin/env python3
"""Deterministic corpus-logistics and Knowledge Gymnasia planner.

This module does not publish secrets or mutate remote providers. It converts
normalized private records into content-addressed chunks, typed relations,
shelf tiers and campus/gymnasium manifests suitable for governed publication.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable, Iterator

SCHEMA = "rafgittools.corpus-logistics-gymnasia.v1"
TIERS = ("HOT", "WARM", "COLD", "ARCHIVE")
KINDS = {
    "CONCEPT","PROJECT","REPOSITORY","FILE","CODE","FORMULA","VARIABLE","UNIT",
    "PARABLE","SYMBOL","CLAIM","EVIDENCE","GAP","ROUTE","DECISION","QUESTION",
    "RESULT","CONTRADICTION","SUPERSESSION","TOKEN_VAZIO"
}
TOKEN_RE = re.compile(r"[\wÀ-ÿ]+|[√πφΩ∆Σμψ]+|[^\s]", re.UNICODE)
FORMULA_RE = re.compile(r"(?:[=<>≤≥±√∑ΣπφΩ∆]|\b(?:sin|cos|tan|sqrt|log|exp)\s*\()", re.I)

def canonical(value: object) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))+"\n").encode()

def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()

def text_hash(text: str) -> str:
    return sha256_bytes(text.encode("utf-8"))

def content_address(source_family: str, book_id: str, session_id: str, text: str) -> str:
    payload={"source_family":source_family,"book_id":book_id,"session_id":session_id,"text_sha256":text_hash(text)}
    return "CHK-"+sha256_bytes(canonical(payload))[:32]

def stable_tokens(text: str) -> list[dict]:
    out=[]
    for i, token in enumerate(TOKEN_RE.findall(text)):
        kind="SYMBOL" if re.fullmatch(r"[√πφΩ∆Σμψ]+", token) else "CONCEPT"
        if token=="TOKEN_VAZIO":
            kind="TOKEN_VAZIO"
        out.append({"i":i,"value":token,"normalized":token.casefold(),"kind":kind})
    return out

def inferred_marks(record: dict) -> list[dict]:
    """Conservative deterministic marks. Semantic promotion remains external."""
    text=str(record.get("text") or "")
    marks=[]
    if record.get("kind") in KINDS:
        marks.append({"kind":record["kind"],"source":"EXPLICIT"})
    if "TOKEN_VAZIO" in text:
        marks.append({"kind":"GAP","source":"LEXICAL_SENTINEL"})
    if FORMULA_RE.search(text):
        marks.append({"kind":"FORMULA","source":"SYNTAX_CANDIDATE","claim_allowed":False})
    tags={str(x).upper() for x in (record.get("tags") or [])}
    if "PARABLE" in tags:
        marks.append({"kind":"PARABLE","source":"EXPLICIT_TAG","evidence_effect":"NONE"})
    return marks

def slot_score(metrics: dict) -> float:
    access=max(0,int(metrics.get("access_count",0)))
    active=max(0,int(metrics.get("active_project_links",0)))
    degree=max(0,int(metrics.get("relation_degree",0)))
    byte_cost=max(0,int(metrics.get("byte_cost",0)))
    duplicate=max(0,int(metrics.get("duplicate_penalty",0)))
    return access*4 + active*6 + degree*2 - byte_cost/65536 - duplicate*5

def tier_for(score: float) -> str:
    if score >= 40: return "HOT"
    if score >= 15: return "WARM"
    if score >= 0: return "COLD"
    return "ARCHIVE"

@dataclass
class Chunk:
    chunk_id: str
    source_family: str
    book_id: str
    session_id: str
    text_sha256: str
    bytes: int
    privacy_class: str
    materialization_state: str
    tier: str
    score: float
    token_count: int
    marks: list[dict]
    characteristics: list[dict]
    relations: list[dict]

def normalize_record(record: dict) -> Chunk:
    source=str(record.get("source_family") or "TOKEN_VAZIO")
    book=str(record.get("book_id") or "TOKEN_VAZIO")
    session=str(record.get("session_id") or "TOKEN_VAZIO")
    text=str(record.get("text") or "")
    metrics=dict(record.get("metrics") or {})
    score=slot_score(metrics)
    relations=list(record.get("relations") or [])
    characteristics=list(record.get("characteristics") or [])
    return Chunk(
        chunk_id=content_address(source,book,session,text),
        source_family=source,
        book_id=book,
        session_id=session,
        text_sha256=text_hash(text),
        bytes=len(text.encode("utf-8")),
        privacy_class=str(record.get("privacy_class") or "PRIVATE_DEFAULT_DENY"),
        materialization_state=str(record.get("materialization_state") or "MATERIALIZED"),
        tier=tier_for(score),
        score=score,
        token_count=len(stable_tokens(text)),
        marks=inferred_marks(record),
        characteristics=characteristics,
        relations=relations,
    )

def build(records: Iterable[dict]) -> dict:
    chunks=[normalize_record(x) for x in records]
    chunks.sort(key=lambda x:(x.source_family,x.book_id,x.session_id,x.chunk_id))
    # adjacency replaces duplicated overlap bytes
    edges=[]
    groups={}
    for c in chunks:
        groups.setdefault((c.source_family,c.book_id,c.session_id),[]).append(c)
    for values in groups.values():
        for a,b in zip(values,values[1:]):
            edges.append({"type":"NEXT","from":a.chunk_id,"to":b.chunk_id})
            edges.append({"type":"PREVIOUS","from":b.chunk_id,"to":a.chunk_id})
    for c in chunks:
        for rel in c.relations:
            edges.append({"from":c.chunk_id,**rel})
    tier_counts={t:sum(1 for c in chunks if c.tier==t) for t in TIERS}
    gymnasia={}
    for c in chunks:
        gym=str(next((x.get("value") for x in c.characteristics if x.get("name")=="gymnasium"),"GENERAL"))
        item=gymnasia.setdefault(gym,{"id":gym,"chunks":[],"kinds":set(),"states":set()})
        item["chunks"].append(c.chunk_id)
        item["kinds"].update(m.get("kind") for m in c.marks)
        item["states"].add(c.materialization_state)
    gyms=[{"id":v["id"],"chunks":v["chunks"],"kinds":sorted(x for x in v["kinds"] if x),"materialization_states":sorted(v["states"])} for v in gymnasia.values()]
    gyms.sort(key=lambda x:x["id"])
    body={
        "schema":SCHEMA,
        "privacy_class":"PRIVATE_DEFAULT_DENY",
        "claim_allowed":False,
        "raw_body_embedded":False,
        "chunks":[asdict(c) for c in chunks],
        "edges":edges,
        "gymnasia":gyms,
        "tier_counts":tier_counts,
        "publication_rule":"POINTERS_CHUNKS_INDEXES_ATLAS_ROUTES_RECEIPTS",
    }
    body["manifest_sha256"]=sha256_bytes(canonical(body))
    return body

def read_jsonl(path: Path) -> Iterator[dict]:
    with path.open("r",encoding="utf-8") as f:
        for line_no,line in enumerate(f,1):
            if not line.strip(): continue
            value=json.loads(line)
            if not isinstance(value,dict): raise ValueError(f"{path}:{line_no}: object required")
            yield value

def write_plan(source: Path, output: Path) -> dict:
    if output.exists() and output.stat().st_size:
        raise RuntimeError("output must be new or empty")
    plan=build(read_jsonl(source))
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(plan,ensure_ascii=False,indent=2,sort_keys=True)+"\n",encoding="utf-8")
    return plan

def selftest() -> int:
    records=[
        {"source_family":"CONVERSATIONS","book_id":"b1","session_id":"s1","text":"h/r = sqrt(1-x)","metrics":{"access_count":12,"active_project_links":2},"characteristics":[{"name":"gymnasium","value":"MATHEMATICS"}]},
        {"source_family":"CONVERSATIONS","book_id":"b1","session_id":"s1","text":"TOKEN_VAZIO evidence","metrics":{"access_count":1},"characteristics":[{"name":"gymnasium","value":"MATHEMATICS"}]},
        {"source_family":"CODEX","book_id":"c1","session_id":"s2","text":"component relation","tags":["PARABLE"],"metrics":{"byte_cost":999999},"characteristics":[{"name":"gymnasium","value":"SYSTEMS"}]},
    ]
    a=build(records); b=build(records)
    assert a==b
    assert a["raw_body_embedded"] is False
    assert len(a["chunks"])==3
    assert any(m["kind"]=="FORMULA" for m in a["chunks"][0]["marks"])
    assert any(m["kind"]=="GAP" for m in a["chunks"][1]["marks"])
    assert any(m["kind"]=="PARABLE" for m in a["chunks"][2]["marks"])
    assert any(e["type"]=="NEXT" for e in a["edges"])
    print("CORPUS_LOGISTICS_GYMNASIA_SELFTEST_PASS")
    return 0

def main() -> int:
    p=argparse.ArgumentParser()
    sub=p.add_subparsers(dest="cmd",required=True)
    b=sub.add_parser("build"); b.add_argument("source",type=Path); b.add_argument("output",type=Path)
    sub.add_parser("selftest")
    a=p.parse_args()
    if a.cmd=="selftest": return selftest()
    plan=write_plan(a.source,a.output)
    print(json.dumps({"manifest_sha256":plan["manifest_sha256"],"chunks":len(plan["chunks"]),"tier_counts":plan["tier_counts"]},sort_keys=True))
    return 0

if __name__=="__main__":
    raise SystemExit(main())
