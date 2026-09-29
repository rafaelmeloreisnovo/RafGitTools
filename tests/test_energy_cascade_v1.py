import importlib.util,json,pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location("v",ROOT/"scripts/validate_energy_cascade_v1.py")
v=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(v)

class EnergyCascadeTests(unittest.TestCase):
    def load(self): return json.loads((ROOT/"configs/knowledge-campus/gymnasiums/energy-cascade-recovery.v1.json").read_text())
    def test_canonical(self): self.assertEqual([],v.validate(self.load()))
    def test_no_closed_energy_loop(self):
        x=self.load()
        for c in x["characteristics"]:
            if c["name"]=="closed_energy_loop_claim": c["value"]=True
        self.assertIn("closed_energy_loop_claim_must_be_false",v.validate(x))
    def test_electrolysis_not_free(self):
        x=self.load(); x["relation_rules"].remove("ELECTROLYSIS != FREE_HYDROGEN")
        self.assertTrue(any("ELECTROLYSIS" in e for e in v.validate(x)))
    def test_no_reactive_recipe(self):
        x=self.load(); x["h2_o2_ratio"]="2:1"
        self.assertTrue(any("hazardous_recipe" in e for e in v.validate(x)))
if __name__=="__main__": unittest.main()
