import copy
import json
import unittest
from pathlib import Path
from scripts.latin.validate_latin_authority import validate

ROOT = Path(__file__).resolve().parents[2]
POLICY = json.loads((ROOT/"contracts/latin/latin-authority.v1.json").read_text())

class LatinAuthorityTest(unittest.TestCase):
    def test_default_is_structural_only(self):
        x=validate(POLICY)
        self.assertEqual(x["result"], "PASS_POLICY_STRUCTURE_ONLY")
        self.assertFalse(x["claim_allowed"])
    def test_admin_escalation_rejected(self):
        p=copy.deepcopy(POLICY); p["security"]["network_admin_calls"]=True
        with self.assertRaises(ValueError): validate(p)
    def test_k_secrets_not_equivalent_to_provider_grant(self):
        p=copy.deepcopy(POLICY); p["capabilities"]["K_SECRETS"]="ADMIN"
        with self.assertRaises(ValueError): validate(p)
    def test_no_fallback(self):
        p=copy.deepcopy(POLICY); p["security"]["allow_secret_fallback"]=True
        with self.assertRaises(ValueError): validate(p)
    def test_no_unreviewed_promotion(self):
        p=copy.deepcopy(POLICY); p["claim_allowed"]=True
        with self.assertRaises(ValueError): validate(p)
    def test_license_gate_cannot_be_dropped(self):
        p=copy.deepcopy(POLICY); p["required_gates"].remove("SOURCE_LICENSE_AND_AUTHORSHIP")
        with self.assertRaises(ValueError): validate(p)

if __name__ == "__main__":
    unittest.main()
