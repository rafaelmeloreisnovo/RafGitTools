"""Source-level tests for scoped provider security gate. No network, no PAT."""
import importlib.util
import json
import pathlib
import tempfile
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("gate",ROOT/"scripts/provider_fleet_gate_v1.py")
gate=importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)
SHA="a"*40
ENTRY={"name":"owner/demo","id":44,"default_branch":"main","head_sha":SHA,"visibility":"public"}

class Fake:
    def __init__(self):
        self.writes=0
        self.protected=False
        self.rules=[]
        self.admin=True
    def request(self,method,path,data=None):
        if path=="/repos/owner/demo":
            return 200,{"id":44,"full_name":"owner/demo","default_branch":"main","visibility":"public","permissions":{"admin":self.admin}}
        if path=="/repos/owner/demo/branches/main":
            return 200,{"commit":{"sha":SHA},"protected":self.protected}
        if path=="/repos/owner/demo/rulesets":
            return 200,self.rules
        if path=="/repos/owner/demo/collaborators/independent/permission":
            return 200,{"permission":"write"}
        if path=="/repos/owner/demo/branches/main/protection":
            if method=="PUT":
                assert data==gate.POLICY
                self.writes+=1
                self.protected=True
            if not self.protected:
                return 404,None
            return 200,{"enforce_admins":{"enabled":True},"allow_force_pushes":{"enabled":False},
                        "allow_deletions":{"enabled":False},"required_conversation_resolution":{"enabled":True},
                        "required_pull_request_reviews":{"required_approving_review_count":1,
                        "dismiss_stale_reviews":True,"require_last_push_approval":True}}
        return 403,None

class Tests(unittest.TestCase):
    def test_manifest_duplicates_blocked(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=pathlib.Path(tmp)/"x.json"
            p.write_text(json.dumps({"schema":gate.SCHEMA,"claim_allowed":False,"repositories":[ENTRY]}))
            self.assertEqual(gate.load_manifest(p)["owner/demo"]["id"],44)
            p.write_text(json.dumps({"schema":gate.SCHEMA,"claim_allowed":False,"repositories":[ENTRY,ENTRY]}))
            with self.assertRaises(gate.Hold):
                gate.load_manifest(p)
    def test_audit_has_zero_writes(self):
        api=Fake()
        result=gate.execute(api,ENTRY,SHA,"independent",False)
        self.assertEqual(result["status"],"AUDIT_ELIGIBLE_NOT_APPLIED")
        self.assertEqual(api.writes,0)
    def test_one_target_policy_readback(self):
        api=Fake()
        result=gate.execute(api,ENTRY,SHA,"independent",True)
        self.assertEqual(result["status"],"PROVIDER_POLICY_READBACK_PASS_NEGATIVE_TEST_PENDING")
        self.assertTrue(result["provider_write_verified"])
        self.assertEqual(api.writes,1)
    def test_negative_blockers(self):
        cases=(("admin",lambda p:setattr(p,"admin",False)),
               ("existing",lambda p:setattr(p,"protected",True)),
               ("rules",lambda p:setattr(p,"rules",[{"enforcement":"active"}])))
        for label,mutate in cases:
            with self.subTest(label=label):
                api=Fake();mutate(api)
                result=gate.execute(api,ENTRY,SHA,"independent",True)
                self.assertTrue(result["status"].startswith("HOLD_"))
                self.assertEqual(api.writes,0)
        api=Fake()
        result=gate.execute(api,ENTRY,SHA,"owner",True)
        self.assertEqual(result["status"],"HOLD_INDEPENDENT_REVIEWER_NOT_VERIFIED")
        self.assertEqual(api.writes,0)
    def test_no_secret_values_in_receipts(self):
        api=Fake();result=gate.execute(api,ENTRY,SHA,"independent",False)
        self.assertNotIn("token",json.dumps(result).lower())
        self.assertFalse(result["secret_value_recorded"])
    def test_exclusive_receipt(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=pathlib.Path(tmp)/"receipt.json"
            data={"state":"HOLD"}
            with path.open("x") as f:
                json.dump(data,f)
            with self.assertRaises(FileExistsError):
                with path.open("x"):
                    pass
if __name__=="__main__":
    unittest.main()
