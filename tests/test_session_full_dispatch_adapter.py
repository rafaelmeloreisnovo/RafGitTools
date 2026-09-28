import importlib.util
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
P=ROOT/"scripts"/"validate_session_full_dispatch_adapter.py"
spec=importlib.util.spec_from_file_location("v",P)
v=importlib.util.module_from_spec(spec); spec.loader.exec_module(v)

class AdapterTests(unittest.TestCase):
    def test_adapter(self):
        out=v.validate(ROOT/"configs"/"session-full-dispatch-adapter.v1.json")
        self.assertEqual(out["status"],"PASS")
        self.assertEqual(out["owned_workstreams"],["WS01","WS03","WS10"])
        self.assertFalse(out["claim_allowed"])

if __name__=="__main__":
    unittest.main()
