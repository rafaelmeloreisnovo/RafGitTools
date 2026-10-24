"""Structural negative checks: contract verification never claims Android runtime."""
import copy
import json
import sys
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"scripts"))
from validate_s15_android_handoff import validate
P=ROOT/"contracts"/"authorial_middleware_s15_android_handoff.v1.json"
class TestS15Handoff(unittest.TestCase):
    @classmethod
    def setUpClass(cls): cls.base=json.loads(P.read_text(encoding="utf8"))
    def deny(self,mutate,expected):
        d=copy.deepcopy(self.base);mutate(d);self.assertEqual(validate(d),expected)
    def test_pass_scoped(self): self.assertEqual(validate(self.base),"PASS_SCOPED_DOCUMENTARY_HOFF")
    def test_false_claim(self): self.deny(lambda d:d.update(claim_allowed=True),"FALSE_CLAIM")
    def test_source(self): self.deny(lambda d:d["producer"].update(c_source_blob="0"*40),"UNBOUND_SOURCE")
    def test_executed(self): self.deny(lambda d:d["producer"].update(reported_executed_value=1),"FALSE_EXECUTION_OR_ABI")
    def test_head(self): self.deny(lambda d:d["consumer"].update(exact_head="a"*40),"HEAD_DRIFT")
    def test_default(self): self.deny(lambda d:d["consumer"].update(default_enabled=True),"UNSAFE_DEFAULT_OR_ABI")
    def test_build(self): self.deny(lambda d:d["evidence"].update(android_mk_module="NOT_RUN"),"UNVERIFIED_BUILD_GATE")
    def test_apk(self): self.deny(lambda d:d["evidence"].update(android_apk="PASS"),"UNSUPPORTED_PHYSICAL_CLAIM")
    def test_device(self): self.deny(lambda d:d["evidence"].update(device_install="PASS"),"UNSUPPORTED_PHYSICAL_CLAIM")
    def test_rights(self): self.deny(lambda d:d["evidence"].update(authorization_to_release=True),"UNSUPPORTED_PHYSICAL_CLAIM")
    def test_rollback(self): self.deny(lambda d:d["rollback"].update(code="DROP_OLD_FILES"),"ROLLBACK_MISSING")
    def test_claim_gate(self): self.deny(lambda d:d.update(claim_gate="ALLOWED"),"PROMOTION_NOT_AUTHORIZED")
if __name__=="__main__":unittest.main()
