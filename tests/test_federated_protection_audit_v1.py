"""Adversarial, offline tests: protection census is only an observation."""
import importlib.util
import json
import pathlib
import unittest
from urllib.parse import parse_qs, urlsplit

SOURCE = pathlib.Path(__file__).resolve().parents[1] / "scripts" / "federated_protection_audit_v1.py"
spec = importlib.util.spec_from_file_location("census", SOURCE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)


class FakeGithub:
    def __init__(self, pages, protection=200, rulesets=None):
        self.pages = pages
        self.protection = protection
        self.rulesets = [] if rulesets is None else rulesets
        self.calls = []

    def get(self, path):
        self.calls.append(path)
        if path.startswith("/user/repos?"):
            page = int(parse_qs(urlsplit(path).query)["page"][0])
            return 200, self.pages.get(page, [])
        if path.endswith("/protection"):
            return self.protection, None
        if "/rulesets?" in path:
            return 200, self.rulesets
        raise AssertionError("Unexpected endpoint")


def repo(name="owner/private-repository", branch="main"):
    return {"full_name": name, "default_branch": branch, "private": True}


class CensusTests(unittest.TestCase):
    def test_success_is_only_terminal_scoped_observation(self):
        result = mod.census(FakeGithub({1: [repo()]}, rulesets=[{"id": 7}]))
        self.assertEqual(result["coverage_state"], "TERMINAL_OBSERVED_NOT_GLOBAL")
        self.assertEqual(result["counts"]["observed"], 1)
        self.assertEqual(result["counts"]["ruleset_list_nonempty"], 1)
        self.assertFalse(result["claim_allowed"])
        self.assertEqual(result["protection_policy_adequacy"], "TOKEN_VAZIO_NOT_VERIFIED")
        self.assertNotIn("private-repository", json.dumps(result))

    def test_404_does_not_prove_unprotected(self):
        result = mod.census(FakeGithub({1: [repo()]}, protection=404))
        self.assertEqual(result["counts"]["branch_protection_404_unknown"], 1)

    def test_403_does_not_prove_permission(self):
        result = mod.census(FakeGithub({1: [repo()]}, protection=403))
        self.assertEqual(result["counts"]["branch_protection_403_unknown"], 1)

    def test_bounded_scan_not_full(self):
        result = mod.census(FakeGithub({1: [repo("o/a"), repo("o/b")]}), max_repos=1)
        self.assertFalse(result["page_terminal_observed"])
        self.assertEqual(result["coverage_state"], "PARTIAL_OR_BLOCKED")

    def test_deduplication(self):
        result = mod.census(FakeGithub({1: [repo(), repo()]}))
        self.assertEqual(result["counts"]["observed"], 1)
        self.assertEqual(result["counts"]["duplicates"], 1)

    def test_invalid_identity_fails_closed(self):
        with self.assertRaises(mod.Blocked):
            mod.census(FakeGithub({1: [{"full_name": "not-a-repo", "default_branch": "main"}]}))

    def test_missing_token_fails_closed(self):
        with self.assertRaises(mod.Blocked):
            mod.Github("")

    def test_no_client_http_mutation_methods(self):
        # Exposed GitHub transport is a single GET method only.
        self.assertFalse(hasattr(mod.Github, "post"))
        self.assertFalse(hasattr(mod.Github, "put"))
        self.assertFalse(hasattr(mod.Github, "patch"))
        self.assertFalse(hasattr(mod.Github, "delete"))


if __name__ == "__main__":
    unittest.main()
