import copy
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"scripts"))
from validate_s16_seven_gates import validate
PATH=ROOT/"contracts"/"s16_seven_gate_closure.v1.json"
class GateRegistryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):cls.base=json.loads(PATH.read_text(encoding="utf8"))
    def deny(self,modify,expected):
        item=copy.deepcopy(self.base);modify(item)
        self.assertEqual(validate(item),expected)
    def test_baseline(self):self.assertEqual(validate(self.base),"PASS_SCOPED_SEVEN_GATE_LEDGER_ONLY")
    def test_loss(self):self.deny(lambda d:d["gates"].pop(),"INCOMPLETE_GATES")
    def test_unapproved_global(self):self.deny(lambda d:d.update(claim_allowed=True),"FALSE_GLOBAL_PROMOTION")
    def test_no_human(self):self.deny(lambda d:d.update(human_review="PASS"),"FALSE_GLOBAL_PROMOTION")
    def test_false_apk(self):self.deny(lambda d:d["gates"][3].update(state="PASS_DEVICE"),"FALSE_APK_GATE_COMPLETION")
    def test_false_apt(self):self.deny(lambda d:d["gates"][3]["blockers"].remove("REAL_SOURCE_BUILT_BOOTSTRAP"),"FALSE_PKG_CLAIM")
    def test_device(self):self.deny(lambda d:d["gates"][4].update(state="PASS"),"UNVERIFIED_HARDWARE_CLAIM")
    def test_resilience(self):self.deny(lambda d:d["gates"][5].update(state="PASS"),"UNVERIFIED_HARDWARE_CLAIM")
    def test_provider(self):self.deny(lambda d:d["gates"][6].update(state="PASS"),"FALSE_PROVIDER_ENFORCEMENT")
    def test_source(self):self.deny(lambda d:d["source"].update(producer_commit="fake"),"SOURCE_SHA_DRIFT")
    def test_security(self):self.deny(lambda d:d["nonnegotiables"].remove("NO_CREDENTIAL_UPLOAD"),"SCOPE_OR_SECURITY_LOST")
    def test_rollback(self):self.deny(lambda d:d["reversal"].update(runtime=""),"ROLLBACK_MISSING")
    def test_no_pass_without_evidence(self):self.deny(lambda d:d["gates"][0].update(evidence=[]),"PASS_WITHOUT_EVIDENCE")
if __name__=="__main__": unittest.main()
