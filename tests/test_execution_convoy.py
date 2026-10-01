import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "execution_convoy.py"
spec = importlib.util.spec_from_file_location("execution_convoy", SCRIPT)
convoy = importlib.util.module_from_spec(spec)
assert spec.loader
spec.loader.exec_module(convoy)


def stage(sid, path, *, needs=None, score=(1,1,1,1,1,1), op="ASSERT_FILE"):
    U,R,D,A,E,C=score
    return {
        "id": sid, "needs": needs or [], "operation": op, "mutation_level": "READ_ONLY",
        "heuristic": {"U":U,"R":R,"D":D,"A":A,"E":E,"C":C}, "config": {"path": path}
    }


def plan(stages):
    return {"schema": convoy.SCHEMA, "workgroup_id": "test", "claim_allowed": False, "stages": stages}


class ExecutionConvoyTests(unittest.TestCase):
    def test_dependency_and_leverage_order(self):
        p = plan([
            stage("a", "a", score=(1,1,1,1,1,1)),
            stage("b", "b", score=(5,5,5,5,5,1)),
            stage("c", "c", needs=["a"], score=(5,5,5,5,5,1)),
        ])
        self.assertEqual(convoy.stage_order(p), ["b", "a", "c"])

    def test_receipt_chain_resume_and_tamper_detection(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td); (root/"a.txt").write_text("A", encoding="utf-8"); (root/"b.txt").write_text("B", encoding="utf-8")
            pp=root/"plan.json"; pp.write_text(json.dumps(plan([stage("a","a.txt"),stage("b","b.txt",needs=["a"])])), encoding="utf-8")
            out=root/"out"
            first=convoy.execute_plan(pp, root, out, "a"*40)
            self.assertEqual(first["state"], "PASS_LIMITED")
            receipts,_=convoy.verify_receipt_chain(out/"receipts")
            self.assertEqual(len(receipts), 2)
            second=convoy.execute_plan(pp, root, out, "a"*40, resume=True)
            self.assertEqual(second["executed_this_run"], [])
            rp=sorted((out/"receipts").glob("*.receipt.json"))[0]
            obj=json.loads(rp.read_text()); obj["state"]="FAIL"; rp.write_text(json.dumps(obj), encoding="utf-8")
            with self.assertRaises(convoy.IntegrityError):
                convoy.verify_receipt_chain(out/"receipts")

    def test_failure_blocks_dependents_but_independent_stage_runs(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td); (root/"ok.txt").write_text("ok", encoding="utf-8")
            p=plan([stage("missing","missing.txt",score=(5,5,5,5,5,1)),stage("dependent","ok.txt",needs=["missing"]),stage("independent","ok.txt")])
            pp=root/"plan.json"; pp.write_text(json.dumps(p), encoding="utf-8")
            result=convoy.execute_plan(pp, root, root/"out", "b"*40)
            self.assertEqual(result["states"]["missing"], "FAIL")
            self.assertEqual(result["states"]["dependent"], "BLOCKED")
            self.assertEqual(result["states"]["independent"], "PASS")
            self.assertEqual(result["state"], "FAIL")

    def test_path_escape_is_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            root=Path(td)
            pp=root/"plan.json"; pp.write_text(json.dumps(plan([stage("escape","../x")])), encoding="utf-8")
            result=convoy.execute_plan(pp, root, root/"out", "c"*40)
            self.assertEqual(result["states"]["escape"], "FAIL")

    def test_claim_promotion_rejected(self):
        p=plan([stage("a","a")]); p["claim_allowed"]=True
        with self.assertRaises(convoy.ConvoyError):
            convoy.validate_plan(p)

    def test_cycle_rejected(self):
        p=plan([stage("a","a",needs=["b"]),stage("b","b",needs=["a"])])
        with self.assertRaises(convoy.ConvoyError):
            convoy.validate_plan(p)

if __name__ == "__main__":
    unittest.main()
