"""Source-side falsifiers for semantic routing; do not assert ontology truth."""
import copy
import importlib.util
import json
import math
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

ROOT=Path(__file__).resolve().parents[1]
SCRIPT=ROOT/"tools"/"semantic_multifilament_v1.py"
DATA=ROOT/"data/governance/complex-network-design-relations.v1.json"
SPEC=importlib.util.spec_from_file_location("semantic_multifilament_v1",SCRIPT)
M=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(M)

class SemanticMultifilamentTests(unittest.TestCase):
    def setUp(self):
        self.graph=json.loads(DATA.read_text(encoding="utf-8"))
        self.nodes,self.edges=M.validate_graph(self.graph)

    def test_canonical_source_readback_and_scope(self):
        nodes,edges,sha=M.read_graph(DATA)
        self.assertEqual(len(sha),64)
        self.assertGreater(len(nodes),5)
        self.assertGreater(len(edges),5)
        r=M.inspect(DATA,"N:RAFAELIA:B7","N:T2")
        self.assertIs(r["claim_allowed"],False)
        self.assertIs(r["source_receipt_authenticated"],False)
        self.assertIs(r["transitive_evidence_inference"],False)
        self.assertGreaterEqual(r["path_count"],1)

    def test_gap_bridge_cannot_promote_to_proof(self):
        routes=M.route(self.nodes,self.edges,"N:RAFAELIA:B7","N:T2")
        self.assertTrue(routes)
        for p in routes:
            self.assertEqual(p["epistemic_state"],"UNRESOLVED_OR_NON_EVIDENTIARY")
            self.assertIs(p["not_an_authenticated_evidence_chain"],True)
            self.assertIn("REL:010",p["relation_ids"])

    def test_analogy_is_not_evidence(self):
        r=M.route(self.nodes,self.edges,"N:PARABLE:VERBO_VIVO","N:RAFGITTOOLS:V3")
        self.assertEqual(r[0]["evidence_effects"],["NONE"])
        self.assertEqual(r[0]["epistemic_state"],"UNRESOLVED_OR_NON_EVIDENTIARY")

    def test_contradiction_is_preserved(self):
        r=M.route(self.nodes,self.edges,"N:GAIA:PR98_RUN","N:GAIA:COGNITIVE_SYMBIOTIC")
        self.assertEqual(len(r),1)
        self.assertTrue(r[0]["has_contradiction"])
        self.assertEqual(r[0]["contradiction_edges"],["REL:004"])

    def test_unknown_route_does_not_mean_false(self):
        r=M.inspect(DATA,"N:T2","N:DRIVE:LONGITUDINAL")
        self.assertEqual(r["paths"],[])
        self.assertEqual(r["no_path_state"],"NOT_OBSERVED_NOT_NEGATIVE_PROOF")

    def test_dangling_endpoint_rejected(self):
        graph=copy.deepcopy(self.graph)
        graph["relations"][0]["to"]="N:FORGED"
        with self.assertRaisesRegex(M.Invalid,"DANGLING_ENDPOINT"):
            M.validate_graph(graph)

    def test_duplicated_ids_rejected(self):
        graph=copy.deepcopy(self.graph)
        graph["nodes"].append(copy.deepcopy(graph["nodes"][0]))
        with self.assertRaisesRegex(M.Invalid,"DUPLICATE_NODE_ID"):
            M.validate_graph(graph)
        graph=copy.deepcopy(self.graph)
        graph["relations"].append(copy.deepcopy(graph["relations"][0]))
        with self.assertRaisesRegex(M.Invalid,"DUPLICATE_EDGE_ID"):
            M.validate_graph(graph)

    def test_fail_closed_on_evidence_promotion(self):
        graph=copy.deepcopy(self.graph)
        edge=next(e for e in graph["relations"] if e["relation_type"]=="ANALOGY_OF")
        edge["evidence_effect"]="EVIDENCES_BOUNDED"
        with self.assertRaisesRegex(M.Invalid,"NON_EVIDENTIARY_EDGE_PROMOTED"):
            M.validate_graph(graph)
        graph=copy.deepcopy(self.graph)
        edge=next(e for e in graph["relations"] if e["uncertainty_state"].startswith("TOKEN_VAZIO"))
        edge["evidence_effect"]="SUPPORT_LIMITED"
        with self.assertRaisesRegex(M.Invalid,"TOKEN_VAZIO_PROMOTED"):
            M.validate_graph(graph)

    def test_false_policy_numeric_zero_rejected(self):
        graph=copy.deepcopy(self.graph)
        graph["graph_policy"]["decorative_edges_allowed"]=0
        with self.assertRaisesRegex(M.Invalid,"GRAPH_POLICY_WEAKENED"):
            M.validate_graph(graph)
        graph=copy.deepcopy(self.graph)
        graph["claim_allowed"]=0
        with self.assertRaisesRegex(M.Invalid,"GRAPH_CLAIM_MUST_BE_FALSE"):
            M.validate_graph(graph)

    def test_duplicate_json_key_and_symlink_rejected(self):
        with TemporaryDirectory() as temp:
            p=Path(temp)/"bad.json"
            p.write_text('{"schema":"x","schema":"y"}',encoding="utf-8")
            with self.assertRaisesRegex(M.Invalid,"DUPLICATE_JSON_KEY"):
                M.read_graph(p)
            link=Path(temp)/"link.json"
            link.symlink_to(DATA)
            with self.assertRaisesRegex(M.Invalid,"GRAPH_MISSING_OR_SYMLINK"):
                M.read_graph(link)

    def test_loop_is_bounded_and_no_inferred_transitivity(self):
        graph=copy.deepcopy(self.graph)
        graph["relations"].append({
            "relation_id":"REL:LOOP",
            "from":"N:T2","to":"N:RAFAELIA:B7",
            "relation_type":"ANALOGY_OF","source_ref":"candidate fixture",
            "authority_domain":"hypothesis only",
            "uncertainty_state":"TOKEN_VAZIO_BRIDGE","evidence_effect":"NONE",
            "boundary_or_falsifier":"unknown","next_gate":"specific test"
        })
        nodes,edges=M.validate_graph(graph)
        r=M.route(nodes,edges,"N:RAFAELIA:B7","N:T2",max_depth=5,max_paths=3)
        self.assertLessEqual(len(r),3)
        self.assertTrue(all(len(p["nodes"])==len(set(p["nodes"])) for p in r))
        with self.assertRaises(M.Invalid):
            M.route(nodes,edges,"N:T2","N:RAFAELIA:B7",max_depth=6)

    def test_combinatorial_search_budget_fail_closed(self):
        graph=copy.deepcopy(self.graph)
        root="N:SEARCH:ROOT"
        target="N:SEARCH:UNREACHABLE"
        graph["nodes"].extend([
            {"id":root,"kind":"SYNTHETIC_TEST","authority":"test only"},
            {"id":target,"kind":"SYNTHETIC_TEST","authority":"test only"},
        ])
        previous=[root]
        for layer in range(4):
            current=["N:SEARCH:L%s:%s"%(layer,j) for j in range(8)]
            for node in current:
                graph["nodes"].append({
                    "id":node,"kind":"SYNTHETIC_TEST","authority":"test only"})
            for src in previous:
                for dest in current:
                    graph["relations"].append({
                        "relation_id":"REL:SEARCH:%s:%s"%(src,dest),
                        "from":src,"to":dest,
                        "relation_type":"ANALOGY_OF",
                        "source_ref":"bounded test fixture",
                        "authority_domain":"synthetic",
                        "uncertainty_state":"TOKEN_VAZIO_TEST",
                        "evidence_effect":"NONE",
                        "boundary_or_falsifier":"route explosion limit",
                        "next_gate":"block excess expansion"
                    })
            previous=current
        nodes,edges=M.validate_graph(graph)
        with self.assertRaisesRegex(M.Invalid,"ROUTE_SEARCH_BUDGET_EXCEEDED"):
            M.route(nodes,edges,root,target,max_depth=5,max_paths=1)

    def test_domain_rejections_real_log(self):
        for value in (0,1,-4,float("nan"),float("inf"),float("-inf")):
            with self.subTest(value=value):
                with self.assertRaises(M.Invalid):
                    M.nested_log(value,2)
                with self.assertRaises(M.Invalid):
                    M.nested_log_derivative(value,2)
        with self.assertRaises(M.Invalid):
            M.nested_log(math.e,3)

    def test_log_log_derivative_against_finite_differences(self):
        for depth, xs in ((1,(0.25,2,10)),(2,(1.5,2.5,7,50)),(3,(4,6,12))):
            for x in xs:
                h=1e-5*min(1.0,x-1.0) if depth>1 else 1e-5*min(1.0,x)
                numerical=(M.nested_log(x+h,depth)-M.nested_log(x-h,depth))/(2*h)
                analytical=M.nested_log_derivative(x,depth)
                self.assertAlmostEqual(numerical,analytical,delta=max(1e-8,abs(analytical)*5e-8))

    def test_inverse_direct_roundtrip(self):
        for depth,xs in ((1,(0.5,2,100)),(2,(1.2,math.e,7,70)),(3,(4,8,15))):
            for x in xs:
                y=M.nested_log(x,depth)
                inverse=M.nested_exp_inverse(y,depth)
                self.assertAlmostEqual(inverse,x,delta=max(1e-10,x*1e-12))

    def test_antiderivative_of_derivative_and_constant(self):
        x=3.0
        a=M.loglog_derivative_antiderivative(x,constant=5.0)
        self.assertAlmostEqual(a-5.0,M.nested_log(x,2))
        h=1e-5
        diff=(M.loglog_derivative_antiderivative(x+h,5)-
              M.loglog_derivative_antiderivative(x-h,5))/(2*h)
        self.assertAlmostEqual(diff,M.nested_log_derivative(x,2),places=7)
        with self.assertRaises(M.Invalid):
            M.loglog_derivative_antiderivative(x,float("nan"))

    def test_numeric_overflow_and_no_claim(self):
        with self.assertRaisesRegex(M.Invalid,"INVERSE_NUMERIC_OVERFLOW"):
            M.nested_exp_inverse(100,3)
        r=M.math_probe(2.0,2)
        self.assertIs(r["claim_allowed"],False)
        self.assertIs(r["release_allowed"],False)
        self.assertTrue(r["mathematical_domain_validated"])
        self.assertLess(r["inverse_roundtrip_residual"],1e-12)

if __name__=="__main__":
    unittest.main()
