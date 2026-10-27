import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
SPEC=importlib.util.spec_from_file_location("hourly",ROOT/"scripts/ci/hourly_public_control_v1.py")
hourly=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(hourly)
DATA=json.loads((ROOT/"configs/federation/public-hourly-control.v1.json").read_text())

class HourlyPublicTests(unittest.TestCase):
    def test_positive_contract(self):
        self.assertEqual(hourly.check(DATA),[])
    def test_private_fetch_denied(self):
        x=dict(DATA,allow_remote_private_fetch=True)
        self.assertIn("POLICY_allow_remote_private_fetch",hourly.check(x))
    def test_pat_denied(self):
        self.assertIn("POLICY_allow_pat",hourly.check(dict(DATA,allow_pat=True)))
    def test_drive_denied(self):
        self.assertIn("POLICY_allow_drive_private_fetch",hourly.check(dict(DATA,allow_drive_private_fetch=True)))
    def test_claim_denied(self):
        self.assertIn("POLICY_claim_allowed",hourly.check(dict(DATA,claim_allowed=True)))
    def test_no_secret_receipt_fields(self):
        x=dict(DATA,result_fields=DATA["result_fields"]+["token"])
        self.assertIn("FIELDS",hourly.check(x))
    def test_invalid_state(self):
        self.assertIn("STATE",hourly.check(dict(DATA,state="ACTIVE")))
    def test_wrong_schedule(self):
        self.assertIn("POLICY_schedule_utc",hourly.check(dict(DATA,schedule_utc="* * * * *")))
    def test_no_hosted_claim(self):
        self.assertEqual(DATA["state"],"CANDIDATE_HOLD")
        self.assertFalse(DATA["private_actions_dispatched"])
if __name__=="__main__":
    unittest.main()
