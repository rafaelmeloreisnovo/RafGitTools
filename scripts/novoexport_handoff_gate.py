#!/usr/bin/env python3
"""Bounded, offline verifier for a four-node NOVOexport plan (not an executor)."""
import hashlib
import json
import re
import sys
from pathlib import Path

SHA1=re.compile(r"^[0-9a-f]{40}$")
SHA256=re.compile(r"^[0-9a-f]{64}$")
VAZIO=re.compile(r"^TOKEN_VAZIO(?:_[A-Z0-9_]+)?$")

def require(value,code):
    if not value: raise ValueError(code)

def check(p):
    require(isinstance(p,dict),"NOT_AN_OBJECT")
    require(set(p)=={"schema","intent","origin","snapshot","data_boundary","plan","gates","claim_allowed"},"TOP_FIELDS")
    require(p["schema"]=="rafaelia.novoexport-four-node-handoff.v1","SCHEMA")
    require(p["intent"]=="NAME_CATALOG_METADATA" and p["claim_allowed"] is False,"CLAIM")
    require(p["data_boundary"]=={"transfer_mode":"METADATA_ONLY","raw_corpus_in_payload":False,
      "secret_values_in_payload":False,"destination_visibility":"private"},"DATA_BOUNDARY")
    require(p["plan"]=={"state":"PLAN_ONLY","write_requires_human_approval":True,
      "execution_authorized":False,"name_relation_state":"CANDIDATE_ONLY",
      "rollback":"APPEND_ONLY_SUCCESSOR"},"PLAN")
    o=p["origin"]
    require(set(o)=={"provider","file_name","source_ref","source_sha256","role"},"ORIGIN_FIELDS")
    require(o["provider"] in {"ANDROID_SAF","GOOGLE_DRIVE","GITHUB"},"PROVIDER")
    require(o["role"] in {"METADATA_OBSERVATION","CUSTODY_POINTER"},"ROLE")
    require(isinstance(o["file_name"],str) and 0<len(o["file_name"])<=512,"FILE_NAME")
    require(isinstance(o["source_ref"],str) and bool(re.fullmatch(
      r"(?:drive:[A-Za-z0-9_-]+|github:[A-Za-z0-9_./:@-]+|sha256:[0-9a-f]{64})",o["source_ref"])),"OPAQUE_REF")
    require(isinstance(o["source_sha256"],str) and
      (bool(SHA256.fullmatch(o["source_sha256"])) or bool(VAZIO.fullmatch(o["source_sha256"]))),"SOURCE_DIGEST")
    s=p["snapshot"]
    require(set(s)=={"novoexport_repository","novoexport_commit","rafgittools_repository",
      "rafgittools_commit","drive_receipt_ref"},"SNAPSHOT_FIELDS")
    require(s["rafgittools_repository"]=="rafaelmeloreisnovo/RafGitTools","EXECUTOR")
    require(bool(re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+",s["novoexport_repository"])),"PRODUCER")
    for key in ("novoexport_commit","rafgittools_commit"):
        require(isinstance(s[key],str) and bool(SHA1.fullmatch(s[key])) and len(set(s[key]))>1,"PIN_"+key)
    require(isinstance(s["drive_receipt_ref"],str) and
      bool(re.fullmatch(r"drive:[A-Za-z0-9_-]+",s["drive_receipt_ref"])),"RECEIPT_REF")
    g=p["gates"]
    require(set(g)=={"physical_device","github_exact_head_ci","drive_readback","independent_review"},"GATES")
    require(all(x in {"TOKEN_VAZIO_NOT_RUN","TOKEN_VAZIO_UNVERIFIED","TOKEN_VAZIO_UNRESOLVED"}
      for x in g.values()),"UNPROVEN_GATE_PROMOTION")
    raw=json.dumps(p,ensure_ascii=False,sort_keys=True,separators=(",",":")).encode()
    return {"state":"PASS_SCOPED_PLAN_ONLY","claim_allowed":False,"execution_authorized":False,
      "packet_sha256":hashlib.sha256(raw).hexdigest()}

def main():
    try:
        path=Path(sys.argv[1])
        require(path.stat().st_size<=65536,"OVERSIZED_PACKET")
        output=check(json.loads(path.read_text(encoding="utf-8")))
    except (IndexError,KeyError,ValueError,TypeError,OSError,AttributeError) as e:
        print(json.dumps({"state":"BLOCKED","reason":str(e),"claim_allowed":False}))
        return 2
    print(json.dumps(output,sort_keys=True))
    return 0

if __name__=="__main__":raise SystemExit(main())
