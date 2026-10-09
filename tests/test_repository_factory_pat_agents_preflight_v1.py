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


if __name__ == "__main__":
    unittest.main()
