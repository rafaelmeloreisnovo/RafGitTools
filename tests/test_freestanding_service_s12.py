"""Hosted, negative-test oriented contract gate; no production L0 dependency."""
import copy
import json
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"scripts"))
from validate_freestanding_service_s12 import validate
SOURCE=Path(__file__).resolve().parents[1]/"contracts"/"freestanding_service_s12.v1.json"
class S12Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls): cls.good=json.loads(SOURCE.read_text(encoding="utf8"))
    def bad(self, edit, code):
        d=copy.deepcopy(self.good); edit(d); self.assertEqual(validate(d),code)
    def test_accept_scoped(self): self.assertEqual(validate(self.good),"PASS_SCOPED_CONTRACT_ONLY")
    def test_false_promotion(self): self.bad(lambda d:d.update(claim_allowed=True),"PROMOTION_OR_DEPENDENCY_LEAK")
    def test_runtime_dependency(self): self.bad(lambda d:d.update(runtime_external_dependencies=["libc"]),"PROMOTION_OR_DEPENDENCY_LEAK")
    def test_libc(self): self.bad(lambda d:d["constraints"].update(libc=True),"CORE_EXTERNAL_CAPABILITY_LEAK")
    def test_jni(self): self.bad(lambda d:d["constraints"].update(jni=True),"CORE_EXTERNAL_CAPABILITY_LEAK")
    def test_missing(self): self.bad(lambda d:d.pop("rollback"),"MISSING_CONTRACT_FIELDS")
    def test_lanes(self): self.bad(lambda d:d["friction_lanes"].pop(),"MATRIX_TOPOLOGY_DRIFT")
    def test_family(self): self.bad(lambda d:d["binary_prefix_families"].append("APK_VERIFIED"),"MATRIX_TOPOLOGY_DRIFT")
    def test_names(self): self.bad(lambda d:d["matrix_cells"].__setitem__(0,d["matrix_cells"][1]),"ANONYMOUS_OR_INVALID_MATRIX")
    def test_review(self): self.bad(lambda d:d["promotion_requires"].remove("human_review"),"HUMAN_PROMOTION_GATE_MISSING")
    def test_android(self): self.bad(lambda d:d["test_evidence"].update(physical_android="PASS"),"UNSUPPORTED_ANDROID_CLAIM")
    def test_rollback(self): self.bad(lambda d:d.update(rollback="DISCARD"),"ROLLBACK_MISSING")
if __name__=="__main__":unittest.main()
