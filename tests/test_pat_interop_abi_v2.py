"""Negative contract tests; deliberately no GitHub provider credentials."""
import copy
import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("pat_interop_validator", ROOT / "scripts/validate_pat_interop_abi_v2.py")
assert spec and spec.loader
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
CONTRACT = json.loads((ROOT / "contracts/PAT_INTEROP_ABI_V2.json").read_text(encoding="utf-8"))
MATRIX = {"secret_ids": list(module.EXPECTED)}
WORKFLOW = "name: Pat_environments\n" + "\n".join(
    "PROVIDER: ${{ secrets." + name + " }}"
    for name in ("PAT_ACTIONS", "PAT_AGENTS", "PAT_ENV")
)

class PATInteropTests(unittest.TestCase):
    def test_legacy_is_compatible(self):
        self.assertEqual([], module.validate(CONTRACT, MATRIX, WORKFLOW))

    def test_pat_env_never_aliases_pat_environments(self):
        x = copy.deepcopy(CONTRACT)
        x["capabilities"][-1]["route"] = "manual_provider_gate_only"
        self.assertIn("UNVERIFIED_PAT_ENVIRONMENTS_WIRED", module.validate(x, MATRIX, WORKFLOW))
        self.assertIn("UNAPPROVED_PAT_ENVIRONMENTS_BINDING",
            module.validate(CONTRACT, MATRIX, WORKFLOW + "\nfoo: ${{ secrets.PAT_ENVIRONMENTS }}"))

    def test_secret_misspelling_and_missing_binding_fail(self):
        x = copy.deepcopy(CONTRACT)
        x["secret_ids"][-1] = "PAT_ENVEROMENT"
        self.assertIn("SECRET_IDENTITY_DRIFT", module.validate(x, MATRIX, WORKFLOW))
        self.assertIn("EXISTING_WIRING_MISSING:PAT_AGENTS",
            module.validate(CONTRACT, MATRIX, WORKFLOW.replace("secrets.PAT_AGENTS", "secrets.UNKNOWN")))

    def test_cannot_enable_mutation_or_fallback(self):
        x = copy.deepcopy(CONTRACT)
        x["capabilities"][3]["mutation"] = True
        x["execution"]["no_automatic_secret_or_permission_fallback"] = False
        errors = module.validate(x, MATRIX, WORKFLOW)
        self.assertIn("ILLEGAL_PAT_MUTATION:PAT_DEPENDABOT", errors)
        self.assertIn("EXECUTION_BOUNDARY:no_automatic_secret_or_permission_fallback", errors)

    def test_no_false_provider_claim_or_environment_rename(self):
        x = copy.deepcopy(CONTRACT)
        x["claim_allowed"] = True
        x["provider_metadata_readback"] = "PASS"
        x["environment_rename_authorized"] = True
        errors = module.validate(x, MATRIX, WORKFLOW)
        self.assertIn("CLAIM_OR_SECRET_EXPOSURE", errors)
        self.assertIn("PROVIDER_READBACK_MUST_BE_EVIDENCED", errors)
        self.assertIn("ENV_RENAME_NOT_AUTHORIZED", errors)

if __name__ == "__main__":
    unittest.main()
