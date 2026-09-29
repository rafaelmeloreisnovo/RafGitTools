import importlib.util, json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location("v",ROOT/"scripts/validate_aviation_propulsion_gymnasium_v1.py")
v=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(v)

class AviationGymnasiumTests(unittest.TestCase):
    def load(self):
        return json.loads((ROOT/"configs/knowledge-campus/gymnasiums/aviation-radial-hybrid-propulsion.v1.json").read_text())
    def test_canonical_passes(self):
        self.assertEqual([],v.validate(self.load()))
    def test_radial_rotary_boundary_required(self):
        x=self.load(); x["relation_rules"].remove("RADIAL_ENGINE != ROTARY_ENGINE")
        self.assertTrue(any("RADIAL_ENGINE" in e for e in v.validate(x)))
    def test_hydraulic_lock_boundary_required(self):
        x=self.load(); x["relation_rules"].remove("HYDRAULIC_LOCK != HYDRAULIC_LIFTER")
        self.assertTrue(any("HYDRAULIC_LOCK" in e for e in v.validate(x)))
    def test_no_reactive_mixture_recipe(self):
        x=self.load(); x["oxygen_enrichment_percent"]=42
        self.assertTrue(any("hazardous_setpoint" in e for e in v.validate(x)))
    def test_candidate_not_materialized_rule(self):
        x=self.load(); x["relation_rules"].remove("CANDIDATE != MATERIALIZED")
        self.assertTrue(any("CANDIDATE" in e for e in v.validate(x)))

if __name__=="__main__": unittest.main()
