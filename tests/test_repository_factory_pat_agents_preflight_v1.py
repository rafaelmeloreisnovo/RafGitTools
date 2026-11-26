"""No-secret mocked tests for repository factory PAT_AGENTS identity preflight."""
import importlib.util
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
spec = importlib.util.spec_from_file_location(
    "repository_factory_pat_agents_preflight_v1",
    ROOT / "scripts" / "repository_factory_pat_agents_preflight_v1.py",
)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class Client:
    def __init__(self, responses):
        self.responses = iter(responses)
        self.calls = []

    def __call__(self, method, path):
        self.calls.append((method, path))
        return next(self.responses)


class PatAgentsPreflightTests(unittest.TestCase):
    def test_unknown_owner_is_fail_closed_and_offline(self):
        client = Client([])
        result = module.preflight("other-owner", client)
        self.assertEqual(result["status"], "BLOCKED_OWNER")
        self.assertEqual(client.calls, [])

    def test_missing_secret(self):
        result = module.preflight("rafaelmeloreisnovo")
        self.assertEqual(result["status"], "BLOCKED_SECRET_UNAVAILABLE")

    def test_actor_identity_match(self):
        client = Client([(200, {"login": "rafaelmeloreisnovo"})])
        result = module.preflight("rafaelmeloreisnovo", client)
        self.assertEqual(result["status"], "IDENTITY_READBACK_ONLY")
        self.assertEqual(client.calls, [("GET", "/user")])
        self.assertEqual(result["repository_creation_permission"], "TOKEN_VAZIO_NOT_TESTED")

    def test_actor_mismatch_blocks(self):
        client = Client([(200, {"login": "another"})])
        self.assertEqual(module.preflight("rafaelmeloreisnovo", client)["status"], "BLOCKED_ACTOR_MISMATCH")
        self.assertEqual(len(client.calls), 1)

    def test_malformed_actor_blocks(self):
        for body in (None, {}, {"login": ""}, {"login": 3}):
            client = Client([(200, body)])
            self.assertEqual(module.preflight("rafaelmeloreisnovo", client)["status"],
                             "PROVIDER_IDENTITY_TOKEN_VAZIO")

    def test_org_member_not_admin(self):
        client = Client([(200, {"login": "member"}), (200, {"state": "active", "role": "member"})])
        r = module.preflight("instituto-Rafael", client)
        self.assertEqual(r["status"], "MEMBERSHIP_READBACK_ONLY")
        self.assertEqual(r["organization_membership"], "ACTIVE_ORG_MEMBER_OBSERVED")
        self.assertEqual(r["repository_creation_permission"], "TOKEN_VAZIO_NOT_TESTED")

    def test_org_admin_still_not_creation_permission(self):
        client = Client([(200, {"login": "admin"}), (200, {"state": "active", "role": "admin"})])
        r = module.preflight("instituto-Rafael", client)
        self.assertEqual(r["organization_membership"], "ACTIVE_ORG_ADMIN_OBSERVED")
        self.assertEqual(r["repository_creation_permission"], "TOKEN_VAZIO_NOT_TESTED")
        self.assertFalse(r["http_mutation_allowed"])

    def test_org_denied_membership_is_typed_gap(self):
        client = Client([(200, {"login": "member"}), (403, None)])
        r = module.preflight("instituto-Rafael", client)
        self.assertEqual(r["status"], "ORG_MEMBERSHIP_TOKEN_VAZIO")
        self.assertEqual(r["organization_membership"], "TOKEN_VAZIO")

    def test_org_pending_is_not_promoted(self):
        client = Client([(200, {"login": "member"}), (200, {"state": "pending", "role": "admin"})])
        self.assertEqual(module.preflight("instituto-Rafael", client)["status"],
                         "ORG_MEMBERSHIP_TOKEN_VAZIO")

    def test_transport_denies_post_or_other_paths(self):
        client = Client([])
        safe = module.ReadOnlyTransport(client)
        for verb, path in (("POST", "/user"), ("GET", "/repos/acme/repo"),
                           ("GET", "/user/memberships/orgs/other")):
            with self.assertRaises(ValueError):
                safe(verb, path)
        self.assertEqual(client.calls, [])



class PublicTargetPreflightTests(unittest.TestCase):
    SHA = "a" * 40
    REPO = "instituto-Rafael/relativity-living-light"

    def responses(self):
        return [(200, {"login": "member"}),
                (200, {"state": "active", "role": "member"}),
                (200, {"full_name": self.REPO, "private": False, "visibility": "public"}),
                (200, {"ref": "refs/heads/main", "object": {"type": "commit", "sha": self.SHA}}),
                (200, {"sha": self.SHA})]

    def check(self, responses, sha=None, name="relativity-living-light"):
        client = Client(responses)
        result = module.target_preflight("instituto-Rafael", name, self.SHA if sha is None else sha, client)
        return result, client.calls

    def test_public_exact_main_no_write_or_scope_claim(self):
        r, calls = self.check(self.responses())
        self.assertEqual(r["status"], "PUBLIC_EXACT_MAIN_READBACK_ONLY")
        self.assertTrue(all(method == "GET" for method, _ in calls))
        self.assertEqual(calls[-1][1], "/repos/" + self.REPO + "/git/commits/" + self.SHA)
        self.assertFalse(r["http_mutation_allowed"])
        self.assertFalse(r["claim_allowed"])
        self.assertEqual(r["effective_pat_scope"], "TOKEN_VAZIO_PUBLIC_READ_NOT_SCOPE_PROOF")

    def test_unallowlisted_target_or_invalid_sha_never_requests(self):
        for name, sha in (("other", self.SHA), ("../relativity-living-light", self.SHA),
                          ("relativity-living-light", "main"), ("relativity-living-light", "")):
            with self.subTest(name=name, sha=sha):
                r, calls = self.check([], sha=sha, name=name)
                self.assertEqual(r["status"], "BLOCKED_TARGET_OR_SHA")
                self.assertEqual(calls, [])

    def test_private_or_wrong_repo_stops_before_commit(self):
        for body in ({"full_name": self.REPO, "private": True, "visibility": "private"},
                     {"full_name": "other/repo", "private": False, "visibility": "public"},
                     {"full_name": self.REPO, "visibility": "public"}):
            responses = self.responses()
            responses[2] = (200, body)
            r, calls = self.check(responses)
            self.assertEqual(r["status"], "BLOCKED_PUBLIC_TARGET_IDENTITY")
            self.assertEqual(len(calls), 3)

    def test_main_drift_and_commit_mismatch(self):
        responses = self.responses()
        responses[3][1]["object"]["sha"] = "b" * 40
        r, calls = self.check(responses)
        self.assertEqual(r["status"], "BLOCKED_TARGET_MAIN_DRIFT")
        self.assertEqual(len(calls), 4)
        responses = self.responses()
        responses[4] = (200, {"sha": "c" * 40})
        self.assertEqual(self.check(responses)[0]["status"], "BLOCKED_COMMIT_READBACK")

    def test_denied_provider_and_identity(self):
        responses = self.responses()
        responses[2] = (403, None)
        self.assertEqual(self.check(responses)[0]["status"], "BLOCKED_PUBLIC_TARGET_IDENTITY")
        r, calls = self.check([(401, None)])
        self.assertEqual(r["status"], "BLOCKED")
        self.assertEqual(len(calls), 1)

if __name__ == "__main__":
    unittest.main()
