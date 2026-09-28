#!/usr/bin/env python3
"""Read-only adapter: RAFAELIA Navigator SQLite -> corpus logistics JSONL."""
from __future__ import annotations
import argparse, json, sqlite3
from pathlib import Path

def emit(database: Path, output: Path, max_records: int|None=None) -> dict:
    if output.exists() and output.stat().st_size:
        raise RuntimeError("output must be new or empty")
    uri=f"file:{database.resolve()}?mode=ro"
    con=sqlite3.connect(uri,uri=True)
    con.execute("pragma query_only=on")
    counts={"CONVERSATIONS":0,"CODEX":0}
    written=0
    output.parent.mkdir(parents=True,exist_ok=True)
    with output.open("x",encoding="utf-8",newline="\n") as out:
        q=("select message_id,conversation_id,node_id,parent_id,role,content_type,text,text_hash,"
           "source_path,source_pointer,privacy_class,epistemic_state,claim_allowed "
           "from messages order by source_path,conversation_id,node_id,message_id")
        for row in con.execute(q):
            if max_records is not None and written>=max_records: break
            (mid,cid,nid,parent,role,ctype,text,th,spath,sptr,privacy,epi,claim)=row
            record={
              "source_family":"CONVERSATIONS","book_id":str(cid or "TOKEN_VAZIO"),
              "session_id":str(spath or "TOKEN_VAZIO"),"text":str(text or ""),
              "privacy_class":str(privacy or "PRIVATE_DEFAULT_DENY"),
              "materialization_state":"MATERIALIZED",
              "characteristics":[
                {"name":"gymnasium","value":"GYM-CORPUS-MEMORY"},
                {"name":"message_id","value":mid},{"name":"node_id","value":nid},
                {"name":"role","value":role},{"name":"content_type","value":ctype},
                {"name":"epistemic_state","value":epi},{"name":"source_pointer","value":sptr}
              ],
              "relations":([{"type":"PREVIOUS","to_source_node":parent}] if parent else []),
              "metrics":{"byte_cost":len(str(text or "").encode("utf-8"))}
            }
            out.write(json.dumps(record,ensure_ascii=False,sort_keys=True,separators=(",",":"))+"\n")
            counts["CONVERSATIONS"]+=1; written+=1
        if max_records is None or written<max_records:
            q=("select record_id,task,repository,branch,commit_sha,pr,path,text,text_hash,"
               "source_path,source_pointer,privacy_class,epistemic_state,claim_allowed "
               "from codex_records order by source_path,record_id")
            for row in con.execute(q):
                if max_records is not None and written>=max_records: break
                (rid,task,repository,branch,commit,pr,path,text,th,spath,sptr,privacy,epi,claim)=row
                record={
                  "source_family":"CODEX","book_id":str(repository or "TOKEN_VAZIO"),
                  "session_id":str(task or spath or "TOKEN_VAZIO"),"text":str(text or ""),
                  "privacy_class":str(privacy or "PRIVATE_DEFAULT_DENY"),
                  "materialization_state":"MATERIALIZED",
                  "characteristics":[
                    {"name":"gymnasium","value":"GYM-SYSTEMS"},
                    {"name":"record_id","value":rid},{"name":"branch","value":branch},
                    {"name":"commit_sha","value":commit},{"name":"pr","value":pr},
                    {"name":"path","value":path},{"name":"epistemic_state","value":epi},
                    {"name":"source_pointer","value":sptr}
                  ],
                  "relations":[],
                  "metrics":{"byte_cost":len(str(text or "").encode("utf-8"))}
                }
                out.write(json.dumps(record,ensure_ascii=False,sort_keys=True,separators=(",",":"))+"\n")
                counts["CODEX"]+=1; written+=1
    con.close()
    return {"records":written,"counts":counts,"source_database":database.name,"raw_source_mutated":False}

def main()->int:
    p=argparse.ArgumentParser()
    p.add_argument("database",type=Path); p.add_argument("output",type=Path)
    p.add_argument("--max-records",type=int)
    a=p.parse_args()
    print(json.dumps(emit(a.database,a.output,a.max_records),sort_keys=True))
    return 0
if __name__=="__main__": raise SystemExit(main())
