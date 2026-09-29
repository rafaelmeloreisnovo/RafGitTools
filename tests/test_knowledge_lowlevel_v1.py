import importlib.util, json, pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location("v",ROOT/"scripts/validate_knowledge_lowlevel_v1.py")
v=importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(v)

class LowLevelKnowledgeTests(unittest.TestCase):
    def test_real_files_pass(self):
        p=json.loads((ROOT/"configs/knowledge-campus/lowlevel-profile.v1.json").read_text())
        m=json.loads((ROOT/"configs/knowledge-campus/gymnasiums/mechanical-powertrain.v1.json").read_text())
        self.assertEqual([],v.validate_profile(p))
        self.assertEqual([],v.validate_mech(m))
    def test_kotlin_java_cannot_claim_freestanding(self):
        p=json.loads((ROOT/"configs/knowledge-campus/lowlevel-profile.v1.json").read_text())
        p["profiles"]["L1_KOTLIN_JAVA_RESTRICTED"]["freestanding_claim"]=True
        self.assertIn("kotlin_java_freestanding_claim_must_be_false",v.validate_profile(p))
    def test_hazard_boundary_required(self):
        m=json.loads((ROOT/"configs/knowledge-campus/gymnasiums/mechanical-powertrain.v1.json").read_text())
        del m["systems"]["SYS-FUEL-AIR-OXIDIZER"]["safety_boundary"]
        self.assertIn("missing_hazard_boundary",v.validate_mech(m))

if __name__=="__main__": unittest.main()
