import copy
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"scripts"))
from validate_s16_seven_gates_v2 import valid
P=ROOT/"contracts"/"s16_seven_gate_closure.v2.json"
class TestS16V2(unittest.TestCase):
 @classmethod
 def setUpClass(cls):cls.orig=json.loads(P.read_text(encoding="utf8"))
 def deny(self,modify,reason):
  d=copy.deepcopy(self.orig);modify(d);self.assertEqual(valid(d),reason)
 def test_ok(self):self.assertEqual(valid(self.orig),"PASS_SCOPED_G4_STATIC_EVIDENCE_LEDGER")
 def test_false_claim(self):self.deny(lambda d:d.update(claim_allowed=True),"FALSE_PROMOTION")
 def test_missing_hardware(self):self.deny(lambda d:d["gates"][4].update(status="PASS_ON_PHONE"),"GATE_STATE_DRIFT")
 def test_real_apt_false(self):self.deny(lambda d:d["classification"].update(real_apt_pkg_proot=True),"UNVERIFIED_EXECUTION_OR_RIGHTS")
 def test_device_false(self):self.deny(lambda d:d["classification"].update(device=True),"UNVERIFIED_EXECUTION_OR_RIGHTS")
 def test_license_false(self):self.deny(lambda d:d["classification"].update(license_certified=True),"UNVERIFIED_EXECUTION_OR_RIGHTS")
 def test_fake_profile(self):self.deny(lambda d:d["classification"].update(bootstrap="REAL_APT"),"MISLABELED_DIAGNOSTIC_ARTIFACT")
 def test_wrong_head(self):self.deny(lambda d:d["provenance"].update(android_commit="0"*40),"INVALID_EXACT_HEAD_BINDING")
 def test_custody(self):self.deny(lambda d:d["provenance"].update(drive_readback_sha256_equal=False),"CUSTODY_UNATTESTED")
 def test_bad_sha(self):self.deny(lambda d:d["apk_artifacts"][0].update(sha256="0"),"BAD_APK_RECEIPT")
 def test_missing_abi(self):self.deny(lambda d:d["apk_artifacts"].pop(),"APK_ABI_MISSING")
 def test_bootstrap(self):self.deny(lambda d:d["bootstraps"][1].update(sha256="nope"),"BOOTSTRAP_CUSTODY_LOST")
 def test_recovery(self):self.deny(lambda d:d.update(rollback="DELETE"),"ROLLBACK_NOT_REVERSIBLE")
 def test_risk(self):self.deny(lambda d:d["risk_policy"].remove("NO_APK_PUBLISHED"),"UNAUTHORIZED_RISK_POLICY_CHANGE")
 def test_untracked(self):self.deny(lambda d:d["gates"][4].update(still_open=[]),"UNTRACKED_BLOCKER")
if __name__=="__main__":unittest.main()
