import importlib.util, json, sqlite3, tempfile, unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def load(name,path):
    s=importlib.util.spec_from_file_location(name,path); m=importlib.util.module_from_spec(s); assert s and s.loader; s.loader.exec_module(m); return m

A=load("adapter",ROOT/"tools"/"corpus_logistics"/"navigator_to_gymnasia_v1.py")
P=load("planner",ROOT/"tools"/"corpus_logistics"/"corpus_logistics_gymnasia_v1.py")
T=load("publisher",ROOT/"tools"/"corpus_logistics"/"campus_publication_v1.py")

class CorpusLogisticsEndToEndTests(unittest.TestCase):
    def test_navigator_to_tree_canary(self):
        with tempfile.TemporaryDirectory() as d:
            r=Path(d); db=r/"n.sqlite3"
            c=sqlite3.connect(db)
            c.executescript('''
              create table messages(message_id text,conversation_id text,node_id text,parent_id text,role text,content_type text,text text,text_hash text,source_path text,source_pointer text,privacy_class text,epistemic_state text,claim_allowed integer);
              create table codex_records(record_id text,task text,repository text,branch text,commit_sha text,pr text,path text,text text,text_hash text,source_path text,source_pointer text,privacy_class text,epistemic_state text,claim_allowed integer);
              insert into messages values('m1','c1','n1',null,'user','text','x = sqrt(y)','h','conversations-000.json','ptr','PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED',0);
              insert into messages values('m2','c1','n2','n1','assistant','text','TOKEN_VAZIO','h2','conversations-000.json','ptr2','PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED',0);
              insert into codex_records values('r1','task','o/r','main','abc','1','a.py','code','h3','codex-000.json','p3','PRIVATE_DEFAULT_DENY','SOURCE_OBSERVED',0);
            '''); c.commit(); c.close()
            normalized=r/"normalized.jsonl"; A.emit(db,normalized)
            plan=P.write_plan(normalized,r/"plan.json")
            receipt=T.materialize(r/"plan.json",r/"campus")
            self.assertEqual(receipt["chunks"],3)
            self.assertEqual(receipt["state"],"PASS_TREE_MATERIALIZED_NOT_GITHUB_PUBLISHED")
            self.assertTrue((r/"campus"/"00_INDEX"/"FORMULAS.json").exists())
            self.assertTrue((r/"campus"/"00_INDEX"/"TOKENS.json").exists())
            self.assertTrue((r/"campus"/"00_INDEX"/"BOOKS.json").exists())
            self.assertTrue((r/"campus"/"00_INDEX"/"SESSIONS.json").exists())
            self.assertTrue((r/"campus"/"06_GAPS"/"TOKEN_VAZIO_AND_GAPS.json").exists())
            self.assertFalse(json.loads((r/"campus"/"07_EVIDENCE"/"BOUNDARY.json").read_text())["claim_allowed"])

if __name__=="__main__": unittest.main()
