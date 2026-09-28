import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PATH=ROOT/"tools"/"corpus_logistics"/"corpus_logistics_gymnasia_v1.py"
SPEC=importlib.util.spec_from_file_location("corpus_logistics_gymnasia_v1",PATH)
M=importlib.util.module_from_spec(SPEC); assert SPEC and SPEC.loader; SPEC.loader.exec_module(M)

class CorpusLogisticsGymnasiaTests(unittest.TestCase):
    def sample(self):
        return [
            {"source_family":"CONVERSATIONS","book_id":"b","session_id":"s","text":"x = sqrt(y)","metrics":{"access_count":11},"characteristics":[{"name":"gymnasium","value":"MATH"}]},
            {"source_family":"CONVERSATIONS","book_id":"b","session_id":"s","text":"TOKEN_VAZIO","metrics":{},"characteristics":[{"name":"gymnasium","value":"MATH"}]},
        ]

    def test_deterministic(self):
        self.assertEqual(M.build(self.sample()),M.build(self.sample()))

    def test_overlap_is_edge(self):
        value=M.build(self.sample())
        self.assertEqual(len(value["chunks"]),2)
        self.assertTrue(any(e["type"]=="NEXT" for e in value["edges"]))

    def test_formula_is_candidate_not_claim(self):
        marks=M.build(self.sample())["chunks"][0]["marks"]
        formula=next(x for x in marks if x["kind"]=="FORMULA")
        self.assertFalse(formula["claim_allowed"])

    def test_token_vazio_marks_gap(self):
        marks=M.build(self.sample())["chunks"][1]["marks"]
        self.assertTrue(any(x["kind"]=="GAP" for x in marks))

    def test_raw_body_never_embedded(self):
        value=M.build(self.sample())
        self.assertFalse(value["raw_body_embedded"])
        raw=json.dumps(value,ensure_ascii=False)
        self.assertNotIn('"text": "x = sqrt(y)"',raw)

    def test_output_is_fail_closed_on_nonempty(self):
        with tempfile.TemporaryDirectory() as d:
            src=Path(d)/"in.jsonl"; out=Path(d)/"out.json"
            src.write_text(json.dumps(self.sample()[0])+"\n",encoding="utf-8")
            out.write_text("occupied",encoding="utf-8")
            with self.assertRaises(RuntimeError): M.write_plan(src,out)

if __name__=="__main__": unittest.main()
