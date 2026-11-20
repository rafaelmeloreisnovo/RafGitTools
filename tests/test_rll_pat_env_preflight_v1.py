import unittest
from scripts import rll_pat_env_preflight_v1 as m

SHA = "a1dbcaac91b0235d21714a40b02328032869186d"
ENV = {"GITHUB_ACTOR": "rafaelmeloreisnovo", "GITHUB_EVENT_NAME": "workflow_dispatch",
       "GITHUB_REF": "refs/heads/main", "GITHUB_RUN_ATTEMPT": "1",
       "GITHUB_REPOSITORY": "rafaelmeloreisnovo/RafGitTools",
       "GITHUB_SHA": "a07efb3fe4def800a61514fd255c66cbb4243e31"}

class Fake:
    def __init__(self):
        self.calls = []
        self.responses = {
            m.BASE: (200, {"id": m.REPO_ID, "full_name": m.REPO, "private": False,
                           "default_branch": "main", "permissions": {"admin": True}}),
            m.BASE + "/branches/main": (200, {"commit": {"sha": SHA}, "protected": True}),
            m.BASE + "/rulesets": (200, [{"id": m.RULESET_ID}]),
            m.BASE + "/rulesets/" + str(m.RULESET_ID):
                (200, {"id": m.RULESET_ID, "target": "branch", "enforcement": "active",
                       "rules": [{"type": "pull_request"}], "bypass_actors": [{"actor_type": "Integration"}]}),
            m.BASE + "/branches/main/protection": (404, None),
        }
    def __call__(self, path):
        self.calls.append(path)
        return self.responses[path]

class GateTests(unittest.TestCase):
    def test_scoped_read_only(self):
        fake = Fake()
        result = m.probe(SHA, ENV, fake)
        self.assertEqual(result["state"], "EVIDENCED_SCOPED_READ_ONLY")
        self.assertFalse(result["claim_allowed"])
        self.assertEqual(result["provider_mutation"], "NONE")
        self.assertEqual(len(fake.calls), 5)
        self.assertEqual(result["bypass_actor_count"], 1)
    def test_wrong_actor_or_ref_or_attempt_rejected_before_provider(self):
        for key, value in (("GITHUB_ACTOR", "other"), ("GITHUB_REF", "refs/heads/branch"),
                           ("GITHUB_EVENT_NAME", "pull_request"), ("GITHUB_RUN_ATTEMPT", "2"),
                           ("GITHUB_REPOSITORY", "instituto-Rafael/relativity-living-light")):
            fake = Fake()
            with self.subTest(key=key), self.assertRaises(m.Blocked):
                m.probe(SHA, dict(ENV, **{key:value}), fake)
            self.assertEqual(fake.calls, [])
    def test_sha_and_admin_read_denied(self):
        fake = Fake()
        with self.assertRaises(m.Blocked): m.probe("a"*40, ENV, fake)
        fake = Fake()
        fake.responses[m.BASE][1]["permissions"]["admin"] = False
        with self.assertRaises(m.Blocked): m.probe(SHA, ENV, fake)
    def test_ruleset_and_protection_drift(self):
        for key, value in ((m.BASE+"/rulesets", (200, [{"id": 123}])),
                           (m.BASE+"/branches/main/protection", (403, None))):
            fake = Fake(); fake.responses[key] = value
            with self.subTest(key=key), self.assertRaises(m.Blocked):
                m.probe(SHA, ENV, fake)
    def test_strict_path_token(self):
        self.assertEqual(m.PATHS, {m.BASE, m.BASE+"/branches/main", m.BASE+"/branches/main/protection",
                                       m.BASE+"/rulesets", m.BASE+"/rulesets/"+str(m.RULESET_ID)})
        with self.assertRaises(m.Blocked): m.provider_get("fixture", "/repos/elsewhere")
        with self.assertRaises(m.Blocked): m.provider_get("", m.BASE)

if __name__ == "__main__": unittest.main()
