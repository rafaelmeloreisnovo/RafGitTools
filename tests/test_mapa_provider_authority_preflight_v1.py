"""Negative no-token tests for Mapa PAT_ENV read-only provider preflight."""
import copy
import importlib.util
import json
import pathlib
import unittest
from unittest.mock import patch

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate_mapa_provider_authority_preflight_v1.py"
spec = importlib.util.spec_from_file_location("mapa_provider_preflight", SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
CONTRACT = json.loads(
    (ROOT / "contracts/MAPA_GITHUB_PROVIDER_AUTHORITY_PREFLIGHT_20261011.v1.json").read_text(encoding="utf-8")
)


class FakeResponse:
    status = 200

    def __init__(self, payload):
        self._body = json.dumps(payload).encode("utf-8")

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return None

    def read(self, length):
        return self._body[:length]


class FakeOpener:
    def __init__(self, main_sha):
        self.main_sha = main_sha
        self.verbs = []
        self.urls = []
        self.headers = []

    def open(self, req, timeout=0):
        self.verbs.append(req.get_method())
        self.urls.append(req.full_url)
        self.headers.append(dict(req.headers))
        if req.full_url.endswith("/branches/main"):
            obj = {"commit": {"sha": self.main_sha}, "protected": False}
        elif req.full_url.endswith("/rulesets/21909304"):
            obj = {"enforcement": "disabled", "rules": [
                {"type": "pull_request", "parameters": {"required_approving_review_count": 0}}]}
        elif req.full_url.endswith("/actions/secrets"):
            obj = {"secrets": [{"name": "CODESCAN_AUTH_TOKEN"}]}
        elif req.full_url.endswith("/protection"):
            obj = {"required_status_checks": None}
        elif req.full_url.endswith("/rulesets"):
            obj = []
        else:
            obj = {"permissions": {"admin": True}}
        return FakeResponse(obj)


class MapaPatEnvPreflightTests(unittest.TestCase):
    def invalid(self, altered):
        self.assertNotEqual(module.validate_contract(altered), [])

    def test_canonical_contract_is_valid(self):
        self.assertEqual(module.validate_contract(CONTRACT), [])

    def test_alternate_repo_denied(self):
        d = copy.deepcopy(CONTRACT)
        d["target_repository"] = "untrusted/other"
        self.assertIn("UNAUTHORIZED_TARGET", module.validate_contract(d))

    def test_no_apply(self):
        d = copy.deepcopy(CONTRACT)
        d["operation"] = "APPLY"
        d["mutation_allowed"] = True
        self.assertIn("MUTATION_NOT_AUTHORIZED", module.validate_contract(d))

    def test_no_fake_closed_gate(self):
        d = copy.deepcopy(CONTRACT)
        d["all_closure_gates_pass"] = True
        self.assertIn("UNSUPPORTED_CLOSURE_PROMOTION", module.validate_contract(d))

    def test_no_pat_actions_substitute(self):
        d = copy.deepcopy(CONTRACT)
        d["credential"]["secret_id"] = "PAT_ACTIONS"
        self.assertIn("WRONG_PAT_SCOPE", module.validate_contract(d))

    def test_no_pat_agents_substitute(self):
        d = copy.deepcopy(CONTRACT)
        d["credential"]["secret_id"] = "PAT_AGENTS"
        self.assertIn("WRONG_PAT_SCOPE", module.validate_contract(d))

    def test_no_pat_environment_alias(self):
        d = copy.deepcopy(CONTRACT)
        d["credential"]["secret_id"] = "PAT_ENVIRONMENTS"
        self.assertIn("WRONG_PAT_SCOPE", module.validate_contract(d))

    def test_no_fallback(self):
        d = copy.deepcopy(CONTRACT)
        d["credential"]["forbidden_fallbacks"].remove("PAT_ACTIONS")
        self.assertIn("PAT_FALLBACK_NOT_DENIED", module.validate_contract(d))

    def test_no_secret_value_persistence(self):
        d = copy.deepcopy(CONTRACT)
        d["credential"]["value_persisted"] = True
        self.assertIn("SECRET_SUBSTITUTION_OR_EXPOSURE", module.validate_contract(d))

    def test_unreviewed_bypass_not_promoted(self):
        d = copy.deepcopy(CONTRACT)
        d["provider_prestate"]["bypass_actor_rationales"] = "APPROVED"
        self.assertIn("BYPASS_JUSTIFICATION_FABRICATED", module.validate_contract(d))

    def test_four_bypasses_not_silently_removed(self):
        d = copy.deepcopy(CONTRACT)
        d["provider_prestate"]["always_bypass_integrations"].pop()
        self.assertIn("BYPASS_INVENTORY_NOT_GROUNDED", module.validate_contract(d))

    def test_negative_rejection_not_fabricated(self):
        d = copy.deepcopy(CONTRACT)
        d["zero_approval_rejection_test"] = "PASS"
        self.assertIn("NEGATIVE_CONTROL_FABRICATED", module.validate_contract(d))

    def test_gates_cannot_be_omitted(self):
        d = copy.deepcopy(CONTRACT)
        d["required_before_any_apply"].pop()
        self.assertIn("REQUIRED_GATES_CHANGED", module.validate_contract(d))

    def test_unsafe_endpoint_is_rejected(self):
        d = copy.deepcopy(CONTRACT)
        d["readback_endpoints"].append("PATCH /repos/rafaelmeloreisnovo/Mapa/rulesets/21909304")
        self.assertIn("PROVIDER_ENDPOINT_ALLOWLIST_DRIFT", module.validate_contract(d))

    def test_invalid_main_sha(self):
        d = copy.deepcopy(CONTRACT)
        d["expected_target_main_sha"] = "HEAD"
        self.assertIn("EXPECTED_TARGET_MAIN_SHA_INVALID", module.validate_contract(d))

    def test_synthetic_provider_probe_get_only_and_sanitized(self):
        mock = FakeOpener(CONTRACT["expected_target_main_sha"])
        with patch.object(module.urllib.request, "build_opener", return_value=mock):
            report = module.probe_read_only(CONTRACT, "FAKE_TEST_TOKEN_NOT_REAL")
        self.assertEqual(report["state"], "READ_ONLY_API_REACHABLE_NO_WRITE_PERMISSION_PROOF")
        self.assertEqual(set(mock.verbs), {"GET"})
        self.assertTrue(all(x.startswith("https://api.github.com/repos/rafaelmeloreisnovo/Mapa")
                            for x in mock.urls))
        self.assertFalse(report["mutation_allowed"])
        self.assertNotIn("FAKE_TEST_TOKEN_NOT_REAL", json.dumps(report))
        self.assertNotIn("Authorization", json.dumps(report))
        self.assertEqual(len(report["endpoints"]), 6)

    def test_live_main_sha_drift_fail_closed(self):
        mock = FakeOpener("f" * 40)
        with patch.object(module.urllib.request, "build_opener", return_value=mock):
            report = module.probe_read_only(CONTRACT, "FAKE_TEST_TOKEN_NOT_REAL")
        self.assertEqual(report["state"], "BLOCKED_MAIN_SHA_UNVERIFIED_OR_DRIFTED")


if __name__ == "__main__":
    unittest.main()
