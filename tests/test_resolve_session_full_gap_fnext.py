import json
import subprocess
import tempfile
import unittest
from pathlib import Path

from scripts.resolve_session_full_gap_fnext import collect, Blocked


class FullGapFnextCollectorTests(unittest.TestCase):
    def make_checkout(self, mutate_routes=None):
        td = tempfile.TemporaryDirectory()
        root = Path(td.name)
        routes = [json.loads(x) for x in Path(
            "tests/fixtures/session_full_gap_fnext_routes.fixture.jsonl"
        ).read_text(encoding="utf-8").splitlines() if x.strip()]
        if mutate_routes:
            mutate_routes(routes)
        route_path = root / "routes.jsonl"
        route_path.write_text(
            "\n".join(json.dumps(x, sort_keys=True) for x in routes) + "\n",
            encoding="utf-8"
        )
        for name in ("evidence.json","receipt.json","parity.json"):
            (root / name).write_text("{}\n", encoding="utf-8")
        subprocess.run(["git","init","-q",str(root)],check=True)
        subprocess.run(["git","-C",str(root),"config","user.name","Fixture"],check=True)
        subprocess.run(["git","-C",str(root),"config","user.email","fixture@example.invalid"],check=True)
        subprocess.run(["git","-C",str(root),"add","."],check=True)
        subprocess.run(["git","-C",str(root),"commit","-qm","fixture"],check=True)
        head=subprocess.check_output(["git","-C",str(root),"rev-parse","HEAD"],text=True).strip()
        blobs={}
        for key,path in {
            "routes":"routes.jsonl","evidence":"evidence.json","receipt":"receipt.json","drive_parity":"parity.json"
        }.items():
            blobs[key]=subprocess.check_output(
                ["git","-C",str(root),"rev-parse",f"HEAD:{path}"],text=True
            ).strip()
        cfg={
          "claim_allowed":False,
          "authority":{"repo":"rafaelmeloreisnovo/Mapa","commit":head,"files":{
            "routes":{"path":"routes.jsonl","blob_sha1":blobs["routes"]},
            "evidence":{"path":"evidence.json","blob_sha1":blobs["evidence"]},
            "receipt":{"path":"receipt.json","blob_sha1":blobs["receipt"]},
            "drive_parity":{"path":"parity.json","blob_sha1":blobs["drive_parity"]}
          }},
          "semantics":{
            "exact_route_count":18,
            "required_route_fields":[
              "route_id","workstream_id","source_ref","target_ref","relation_type","owner",
              "authority","predecessor","successor","dependency_edges","evidence_ref",
              "receipt_ref","rollback_ref","replay_recipe","reproduction_state",
              "privacy_class","retention_class","claim_allowed","hash_ref"
            ]
          },
          "waves":{"0":["MC-W17","MC-W18","MC-W03"],"3":["MC-W11","MC-W12"]}
        }
        cfg_path=root/"collector.json"
        cfg_path.write_text(json.dumps(cfg),encoding="utf-8")
        return td,root,cfg_path

    def test_all_18_routes_resolve_without_execution_claim(self):
        td,root,cfg=self.make_checkout()
        with td:
            out=collect(root,config_path=cfg)
        self.assertEqual(out["count"],18)
        self.assertFalse(out["assignment_is_execution"])
        self.assertFalse(out["claim_allowed"])

    def test_wave_zero_selects_three_routes(self):
        td,root,cfg=self.make_checkout()
        with td:
            out=collect(root,wave="0",config_path=cfg)
        self.assertEqual({x["workstream_id"] for x in out["routes"]},{"MC-W17","MC-W18","MC-W03"})

    def test_workstream_selection_is_exact(self):
        td,root,cfg=self.make_checkout()
        with td:
            out=collect(root,workstream="MC-W11",config_path=cfg)
        self.assertEqual(out["count"],1)
        self.assertEqual(out["routes"][0]["workstream_id"],"MC-W11")

    def test_duplicate_workstream_blocks(self):
        def mutate(rows):
            rows[1]["workstream_id"]=rows[0]["workstream_id"]
        td,root,cfg=self.make_checkout(mutate)
        with td:
            with self.assertRaisesRegex(Blocked,"duplicate workstream_id"):
                collect(root,config_path=cfg)

    def test_claim_promotion_blocks(self):
        def mutate(rows):
            rows[0]["claim_allowed"]=True
        td,root,cfg=self.make_checkout(mutate)
        with td:
            with self.assertRaisesRegex(Blocked,"claim_allowed must be false"):
                collect(root,config_path=cfg)

    def test_head_mismatch_blocks(self):
        td,root,cfg=self.make_checkout()
        with td:
            data=json.loads(cfg.read_text())
            data["authority"]["commit"]="0"*40
            cfg.write_text(json.dumps(data))
            with self.assertRaisesRegex(Blocked,"HEAD mismatch"):
                collect(root,config_path=cfg)


if __name__ == "__main__":
    unittest.main()
