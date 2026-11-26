import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("preflight", ROOT / "scripts/termux_ruleset_preflight_v1.py")
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
REPO = "rafaelmeloreisnovo/termux-app-rafacodephi"
SHA = "a" * 40


class Fake:
    def __init__(self):
        self.data = {
            "": (200, {"id": 1082194563, "full_name": REPO, "default_branch": "master", "private": False, "permissions": {"admin": True}}),
            "/branches/master": (200, {"commit": {"sha": SHA}}),
            "/branches/master/protection": (404, None),
            "/rulesets?includes_parents=true&per_page=100": (200, [{"id": 21908888}]),
            "/rulesets/21908888": (200, {"id": 21908888, "target": "branch", "enforcement": "active", "conditions": {"ref_name": {"include": ["~ALL"], "exclude": []}}, "rules": [{"type": "deletion"}], "bypass_actors": []}),
        }

    def get(self, path):
        return self.data[path]


class PreflightTests(unittest.TestCase):
    def test_manual_guard(self):
        env = {"GITHUB_REPOSITORY": m.EXECUTOR, "GITHUB_ACTOR": m.OWNER, "GITHUB_EVENT_NAME": "workflow_dispatch", "GITHUB_REF": "refs/heads/main", "GITHUB_SHA": SHA, "GITHUB_RUN_ATTEMPT": "1"}
        m.guard(REPO, SHA, env)
        for key, val in (("GITHUB_EVENT_NAME", "pull_request"), ("GITHUB_RUN_ATTEMPT", "2"), ("GITHUB_REF", "refs/heads/pr"), ("GITHUB_ACTOR", "other"), ("GITHUB_REPOSITORY", "other/repo")):
            with self.subTest(key=key), self.assertRaises(m.Hold):
                m.guard(REPO, SHA, dict(env, **{key: val}))
        with self.assertRaises(m.Hold):
            m.guard("other/repo", SHA, env)

    def test_read_only(self):
        d = m.inventory(Fake(), REPO, SHA)
        self.assertFalse(d["apply_attempted"])
        self.assertFalse(d["claim_allowed"])
        self.assertIn("pull_request", d["app_missing_rule_types_only"])
        self.assertEqual(d["administration_write_permission"], "TOKEN_VAZIO_NOT_TESTED")
        self.assertTrue(d["rule_inventory"][0]["covers_default_proven"])

    def test_head_drift(self):
        f = Fake()
        f.data["/branches/master"][1]["commit"]["sha"] = "b" * 40
        with self.assertRaises(m.Hold):
            m.inventory(f, REPO, SHA)

    def test_classic_forbidden(self):
        f = Fake()
        f.data["/branches/master/protection"] = (403, None)
        with self.assertRaises(m.Hold):
            m.inventory(f, REPO, SHA)

    def test_exclusion(self):
        f = Fake()
        f.data["/rulesets/21908888"][1]["conditions"]["ref_name"]["exclude"] = ["refs/heads/master"]
        self.assertFalse(m.inventory(f, REPO, SHA)["rule_inventory"][0]["covers_default_proven"])

    def test_unknown_glob(self):
        f = Fake()
        f.data["/rulesets/21908888"][1]["conditions"]["ref_name"]["exclude"] = ["refs/heads/m*"]
        self.assertEqual(m.inventory(f, REPO, SHA)["app_missing_rule_types_only"], "TOKEN_VAZIO")

    def test_no_unknown_endpoint(self):
        p = m.Provider(REPO, "synthetic-test-only")
        with self.assertRaises(m.Hold):
            p.get("/actions/secrets")


if __name__ == "__main__":
    unittest.main()
