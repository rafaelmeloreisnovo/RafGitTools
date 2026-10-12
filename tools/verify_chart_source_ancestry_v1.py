#!/usr/bin/env python3
"""Read-only topology check of provider *metadata snapshots*.

Input is caller-supplied, authenticated elsewhere. No Drive API calls here.
No file bytes, names, hashes or source IDs are logged. This function does not
authenticate metadata, consent, access rights or third-party licenses.
"""
from __future__ import annotations
from dataclasses import dataclass
from typing import Any

@dataclass(frozen=True)
class LineageDecision:
    state: str
    depth: int
    claim_allowed: bool = False
    scope: str = "STRUCTURAL_METADATA_ONLY"

def verify_parent_chain(
    records: Any, source_id: str, trusted_root_id: str,
    max_depth: int = 64
) -> LineageDecision:
    def result(code: str, depth: int = 0) -> LineageDecision:
        return LineageDecision(code, depth)
    if (not isinstance(records, list) or len(records) > 128 or
            not isinstance(source_id,str) or not source_id.strip() or
            not isinstance(trusted_root_id,str) or not trusted_root_id.strip() or
            source_id == trusted_root_id or
            not isinstance(max_depth,int) or isinstance(max_depth,bool) or
            max_depth < 1 or max_depth > 64):
        return result("BLOCK_BAD_INPUT")
    index = {}
    for item in records:
        if not isinstance(item,dict) or set(item)!={"id","parents","kind"}:
            return result("BLOCK_SCHEMA")
        key=item.get("id")
        parents=item.get("parents")
        kind=item.get("kind")
        if (not isinstance(key,str) or not key.strip() or len(key)>256 or
                key in index or not isinstance(parents,list) or len(parents)>1 or
                kind not in {"file","folder"}):
            return result("BLOCK_SCHEMA")
        if any(not isinstance(p,str) or not p.strip() or len(p)>256 for p in parents):
            return result("BLOCK_SCHEMA")
        index[key]=item
    if source_id not in index or trusted_root_id not in index:
        return result("BLOCK_ROOT_OR_SOURCE_MISSING")
    if index[trusted_root_id]["kind"]!="folder":
        return result("BLOCK_TRUSTED_ROOT_NOT_FOLDER")
    visited=set()
    current=source_id
    depth=0
    while depth < max_depth:
        if current in visited:
            return result("BLOCK_CYCLE",depth)
        visited.add(current)
        rec=index.get(current)
        if rec is None:
            return result("BLOCK_MISSING_PARENT",depth)
        if current==trusted_root_id:
            return result("PASS_STRUCTURAL_ONLY",depth)
        parents=rec["parents"]
        if not parents:
            return result("BLOCK_OUTSIDE_TRUSTED_ROOT",depth)
        parent=parents[0]
        if parent==current:
            return result("BLOCK_CYCLE",depth)
        if parent not in index:
            return result("BLOCK_MISSING_PARENT",depth)
        if index[parent]["kind"]!="folder":
            return result("BLOCK_PARENT_NOT_FOLDER",depth)
        current=parent
        depth+=1
    return result("BLOCK_DEPTH_LIMIT",depth)
