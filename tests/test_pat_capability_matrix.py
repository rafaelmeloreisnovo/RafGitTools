import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PATH=ROOT/"scripts"/"validate_pat_capability_matrix.py"
SPEC=importlib.util.spec_from_file_location("validate_pat_capability_matrix",PATH)
M=importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(M)

class PatCapabilityMatrixTests(unittest.TestCase):
    def load(self):
        return json.loads((ROOT/"contracts"/"PAT_CAPABILITY_MATRIX_V1.json").read_text(encoding="utf-8"))

    def test_canonical_matrix_passes(self):
        self.assertEqual(M.validate(self.load()), [])

    def test_lowercase_secret_fails(self):
        data=self.load()
        data["secret_ids"][0]="pat_actions"
        self.assertTrue(any("secret id" in x for x in M.validate(data)))

    def test_actions_cannot_gain_write_by_documentation(self):
        data=self.load()
        next(x for x in data["capabilities"] if x["secret_id"]=="PAT_ACTIONS")["mutation_allowed"]=True
        self.assertTrue(any("read-only" in x for x in M.validate(data)))

    def test_unwired_pat_cannot_be_promoted_without_probe(self):
        data=self.load()
        next(x for x in data["capabilities"] if x["secret_id"]=="PAT_AGENTS")["current_state"]="WIRED"
        self.assertTrue(any("PAT_AGENTS" in x for x in M.validate(data)))

    def test_environment_name_is_canonical_uppercase(self):
        data=self.load()
        next(x for x in data["capabilities"] if x["secret_id"]=="PAT_ENV")["environment"]="Pat_environments"
        self.assertTrue(any("canonical uppercase" in x for x in M.validate(data)))

if __name__=="__main__":
    unittest.main()
