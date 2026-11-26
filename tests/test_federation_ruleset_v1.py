import copy
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("federation_ruleset", ROOT / "scripts/federation_ruleset_v1.py")
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
REPO = "rafaelmeloreisnovo/example"
SHA = "a" * 40
BASE = "/repos/" + REPO


class Fake:
    def __init__(self):
        self.branch = None
        self.write_attempted = False
        self.details = []
        self.private = False
        self.head = SHA
        self.writes = 0
        self.bad_readback = False

    def get(self, p):
        if p == BASE:
            return 200, {"id": 42, "full_name": REPO, "default_branch": "main",
                "private": self.private, "archived": False, "disabled": False,
                "permissions": {"admin": True}}, False
        if p == BASE + "/branches/main":
            return 200, {"commit": {"sha": self.head}}, False
        if p == BASE + "/branches/main/protection":
            return 404, None, False
        if p == BASE + "/rulesets?includes_parents=true&per_page=100":
            return 200, [{"id": x["id"]} for x in self.details], False
        if p.startswith(BASE + "/rulesets/"):
            return 200, copy.deepcopy(next(x for x in self.details if x["id"] == int(p.rsplit("/", 1)[1]))), False
        raise AssertionError(p)

    def request(self, method, path, data):
        assert method == "POST" and path == BASE + "/rulesets" and m.valid_payload(data)
        self.writes += 1
        self.write_attempted = True
        d = dict(copy.deepcopy(data), id=99)
        if self.bad_readback:
            d["bypass_actors"] = [{"actor_id": 1}]
        self.details.append(d)
        return 201, {"id": 99}, False


class FederationTests(unittest.TestCase):
    def get_plan(self, f):
        return m.plan(m.state(f, REPO, 42, SHA))

    def test_plan_and_apply(self):
        f = Fake()
        p = self.get_plan(f)
        self.assertEqual(f.writes, 0)
        self.assertTrue(m.valid_payload(p["new_ruleset"]))
        with tempfile.TemporaryDirectory() as d:
            out = m.apply(f, REPO, 42, SHA, p["plan_sha256"], Path(d) / "receipt.json")
            self.assertEqual(out["state"], "CONFIG_READBACK_SCOPED_BASELINE_ONLY")
            self.assertTrue((Path(d) / "federation-ruleset-intent.json").exists())
        self.assertEqual(f.writes, 1)
        self.assertIsNone(self.get_plan(f)["new_ruleset"])

    def test_stale_plan(self):
        f = Fake()
        p = self.get_plan(f)
        f.head = "b" * 40
        with tempfile.TemporaryDirectory() as d, self.assertRaises(m.Hold):
            m.apply(f, REPO, 42, SHA, p["plan_sha256"], Path(d) / "receipt.json")
        self.assertEqual(f.writes, 0)

    def test_wrong_digest(self):
        f = Fake()
        with tempfile.TemporaryDirectory() as d, self.assertRaises(m.Hold):
            m.apply(f, REPO, 42, SHA, "0" * 64, Path(d) / "receipt.json")
        self.assertEqual(f.writes, 0)

    def test_private_denied_before_rules(self):
        f = Fake()
        f.private = True
        with self.assertRaisesRegex(m.Hold, "PRIVATE_TARGET"):
            self.get_plan(f)

    def test_existing_guard_preserved(self):
        f = Fake()
        f.details = [dict(id=1, name="existing", target="branch", enforcement="active",
                          conditions={"ref_name": {"include": ["~ALL"], "exclude": []}},
                          bypass_actors=[], rules=[{"type": "deletion"}])]
        p = self.get_plan(f)
        self.assertEqual(p["new_ruleset"]["rules"], [{"type": "non_fast_forward"}])
        with tempfile.TemporaryDirectory() as d:
            m.apply(f, REPO, 42, SHA, p["plan_sha256"], Path(d) / "receipt.json")
        self.assertEqual(f.details[0]["rules"], [{"type": "deletion"}])

    def test_bypass_is_not_proof(self):
        f = Fake()
        f.details = [dict(id=1, name="existing", target="branch", enforcement="active",
                          conditions={"ref_name": {"include": ["~ALL"], "exclude": []}},
                          bypass_actors=[{"actor_id": 5}], rules=[{"type": "deletion"}])]
        self.assertEqual(len(self.get_plan(f)["new_ruleset"]["rules"]), 2)
        f.details[0].pop("bypass_actors")
        with self.assertRaisesRegex(m.Hold, "BYPASS"):
            self.get_plan(f)

    def test_poststate_fail_closed(self):
        f = Fake()
        f.bad_readback = True
        p = self.get_plan(f)
        with tempfile.TemporaryDirectory() as d, self.assertRaisesRegex(m.Hold, "POSTSTATE"):
            m.apply(f, REPO, 42, SHA, p["plan_sha256"], Path(d) / "receipt.json")
        self.assertEqual(f.writes, 1)

    def test_default_branch_drift_same_sha_requires_recovery(self):
        class Drift(Fake):
            def get(self, p):
                if self.writes and p == BASE:
                    status, data, more = super().get(p)
                    return status, dict(data, default_branch="release"), more
                if self.writes and p.startswith(BASE + "/branches/release"):
                    p = p.replace("/branches/release", "/branches/main", 1)
                return super().get(p)
        f = Drift()
        p = self.get_plan(f)
        with tempfile.TemporaryDirectory() as d, self.assertRaisesRegex(m.Hold, "POSTSTATE_MISMATCH"):
            m.apply(f, REPO, 42, SHA, p["plan_sha256"], Path(d) / "receipt.json")
        self.assertEqual(f.writes, 1)

    def test_no_arbitrary_endpoint_or_payload(self):
        p = m.Provider("rafaelmeloreisnovo", REPO, "synthetic")
        with self.assertRaises(m.Hold):
            p.request("DELETE", BASE + "/rulesets/1")
        with self.assertRaises(m.Hold):
            p.request("POST", BASE + "/rulesets", {"enforcement": "disabled"})
        with self.assertRaises(m.Hold):
            p.get(BASE + "/actions/secrets")

    def test_owner_and_target_guard(self):
        env = dict(GITHUB_REPOSITORY=m.EXECUTOR, GITHUB_ACTOR="rafaelmeloreisnovo",
                   GITHUB_EVENT_NAME="workflow_dispatch", GITHUB_REF="refs/heads/main",
                   GITHUB_RUN_ATTEMPT="1", GITHUB_SHA=SHA)
        m.guard(env, "instituto-Rafael")
        with self.assertRaises(m.Hold):
            m.guard(dict(env, GITHUB_EVENT_NAME="pull_request"), "instituto-Rafael")
        with self.assertRaises(m.Hold):
            m.target("other", "example", "42", SHA)
        with self.assertRaises(m.Hold):
            m.target("rafaelmeloreisnovo", "../other", "42", SHA)

    def test_public_inventory_pagination(self):
        class F:
            def get(self, path):
                page = int(path.rsplit("=", 1)[1])
                return 200, [{"id": page, "full_name": "rafaelmeloreisnovo/repo" + str(page), "private": False}], page == 1
        result = m.inventory(F(), "rafaelmeloreisnovo")
        self.assertEqual(result["count"], 2)
        self.assertFalse(result["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
