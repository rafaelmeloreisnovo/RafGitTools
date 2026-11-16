"""Synthetic scoped tests only: never provider evidence."""
import copy
import unittest
from omega7_gate import (SCHEMA,AXES,REQUIRED,direta,derivativa,antiderivativa,
                          inversa,recursiva,ortogonal,sha,evaluate,Invalid,equal)

SHA="a"*40
def case(kind="rll_promotion"):
    src={"source_sha":SHA,"observed_sha":SHA} if kind=="rll_promotion" else {
        "source_checkpoint":"B0159_COMMITTED_20261115",
        "observed_checkpoint":"B0159_COMMITTED_20261115"}
    gate={"kind":kind,"predicates":{k:True for k in REQUIRED[kind]},**src}
    before={"source_identity":SHA,"x":0,"status":"review","flag":False}
    after={"source_identity":SHA,"x":1,"status":"candidate","flag":True}
    delta=derivativa(before,after)
    chain=[{"predecessor_digest":sha(before),"successor_digest":sha(after),"delta":delta}]
    witnesses=[{"axis":name,"provider":"synthetic-"+name,"receipt":"test-"+name,
                "source_identity":SHA} for name in ("source","execution","review","rollback")]
    return {"schema":SCHEMA,"gate":gate,"before":before,"after":after,
            "chain":chain,"witnesses":witnesses}

class Omega7Tests(unittest.TestCase):
    def test_seven_axes(self):
        self.assertEqual(len(AXES),7)
        self.assertEqual(set(evaluate(case())["directions"]),set(AXES))
    def test_all_synthetic_still_not_authorization(self):
        r=evaluate(case())
        self.assertEqual(r["decision"],"READY_FOR_INDEPENDENT_PROVIDER_RECHECK")
        self.assertFalse(r["claim_allowed"])
        self.assertFalse(r["provider_mutation_authorized"])
    def test_inventory_scope(self):
        self.assertEqual(evaluate(case("inventory_pointer"))["decision"],
                         "READY_FOR_INDEPENDENT_PROVIDER_RECHECK")
    def test_missing_review(self):
        c=case();c["gate"]["predicates"]["required_review_approved"]=None
        self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_single_false_each_gate(self):
        for k in REQUIRED["rll_promotion"]:
            c=case();c["gate"]["predicates"][k]=False
            self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_sha_mismatch(self):
        c=case();c["gate"]["observed_sha"]="b"*40
        self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_missing_vs_null(self):
        delta=derivativa({},{"x":None})
        self.assertEqual(len(delta),1)
        self.assertEqual(antiderivativa({},delta),{"x":None})
        self.assertEqual(inversa({"x":None},delta),{})
    def test_zero_vs_false(self):
        self.assertFalse(equal(0,False))
        self.assertEqual(len(derivativa({"x":0},{"x":False})),1)
    def test_inverse(self):
        a={"x":None};b={"x":2,"z":False};delta=derivativa(a,b)
        self.assertTrue(equal(inversa(b,delta),a))
        self.assertTrue(equal(antiderivativa(a,delta),b))
    def test_bad_delta_precondition(self):
        delta=derivativa({"x":1},{"x":2})
        with self.assertRaises(Invalid): antiderivativa({"x":0},delta)
    def test_repeated_change(self):
        delta=derivativa({},{"x":1})
        with self.assertRaises(Invalid):antiderivativa({},delta+delta)
    def test_chain_parent(self):
        c=case();c["chain"][0]["predecessor_digest"]="0"*64
        self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_chain_cycle(self):
        b={"x":1};s=[{"predecessor_digest":sha(b),"successor_digest":sha(b),"delta":[]}]
        with self.assertRaises(Invalid):recursiva(b,s)
    def test_duplicate_receipt(self):
        c=case();c["witnesses"][1]["receipt"]=c["witnesses"][0]["receipt"]
        self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_stale_witness(self):
        c=case();c["witnesses"][0]["source_identity"]="b"*40
        self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_unknown_axis(self):
        c=case();c["witnesses"][0]["axis"]="quantica"
        self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_schema_change(self):
        c=case();c["schema"]="unknown"
        self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_unknown_field(self):
        c=case();c["extra"]="unsafe"
        self.assertEqual(evaluate(c)["decision"],"BLOCKED")
    def test_no_mutation(self):
        c=case();baseline=copy.deepcopy(c);evaluate(c)
        self.assertEqual(c,baseline)

if __name__=="__main__": unittest.main()
