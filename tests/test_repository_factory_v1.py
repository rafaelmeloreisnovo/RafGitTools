"""Focused offline no-secret/no-network provider adapter contracts."""
import importlib.util
import pathlib
import unittest

PATH = pathlib.Path(__file__).resolve().parents[1] / "scripts/repository_factory_v1.py"
spec = importlib.util.spec_from_file_location("repository_factory_v1", PATH)
rf = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rf)


class FakeGitHub:
    def __init__(self, replies):
        self.replies = list(replies)
        self.calls = []

    def __call__(self, method, path, payload=None):
        self.calls.append((method, path, payload))
        return self.replies.pop(0)


class FactoryTests(unittest.TestCase):
    OWNER = "rafaelmeloreisnovo"
    NAME = "voynich-corpus-experiments"

    def create(self, fake, owner=OWNER, name=NAME):
        return rf.plan_or_create(owner, name, operation="create", approval=True,
                                 rights=True, confirm="CREATE:" + owner + "/" + name, client=fake)

    def test_plan_does_not_use_network(self):
        client = FakeGitHub([])
        self.assertEqual(rf.plan_or_create(self.OWNER, self.NAME, client=client)["status"], "PLAN_ONLY")
        self.assertFalse(client.calls)

    def test_owner_allowlist(self):
        self.assertEqual(rf.check_target("unknown", "safe"), "UNAUTHORIZED_OWNER")

    def test_invalid_names(self):
        for name in ("../x", "a/b", "bad name", ".git", "name.git"):
            self.assertEqual(rf.check_target(self.OWNER, name), "INVALID_NAME")

    def test_confirmation_and_rights_gate(self):
        client = FakeGitHub([])
        self.assertEqual(rf.plan_or_create(self.OWNER, self.NAME, operation="create", client=client)["status"],
                         "BLOCKED_APPROVAL")
        self.assertFalse(client.calls)

    def test_missing_capability_token_gate(self):
        self.assertEqual(rf.plan_or_create(self.OWNER, self.NAME, operation="create", rights=True,
                         approval=True, confirm="CREATE:" + self.OWNER + "/" + self.NAME)["status"],
                         "BLOCKED_TOKEN")

    def test_personal_actor_mismatch(self):
        client = FakeGitHub([(200, {"login": "wrong-actor"})])
        self.assertEqual(self.create(client)["status"], "BLOCKED_ACTOR_MISMATCH")
        self.assertEqual(len(client.calls), 1)

    def test_existing_private_is_idempotent(self):
        client = FakeGitHub([(200, {"login": self.OWNER}),
                             (200, {"private": True, "full_name": self.OWNER + "/" + self.NAME})])
        self.assertEqual(self.create(client)["status"], "ALREADY_EXISTS_PRIVATE_NO_MUTATION")
        self.assertEqual([x[0] for x in client.calls], ["GET", "GET"])

    def test_existing_public_denied(self):
        client = FakeGitHub([(200, {"login": self.OWNER}),
                             (200, {"private": False, "full_name": self.OWNER + "/" + self.NAME})])
        self.assertEqual(self.create(client)["status"], "BLOCKED_EXISTING_VISIBILITY")

    def test_private_create_has_readback(self):
        client = FakeGitHub([(200, {"login": self.OWNER}), (404, None), (201, {}),
                             (200, {"owner": {"login": self.OWNER}, "private": True,
                                    "full_name": self.OWNER + "/" + self.NAME,
                                    "id": 12, "html_url": "https://github.com/safe"})])
        result = self.create(client)
        self.assertEqual(result["status"], "CREATED_READBACK_VERIFIED")
        self.assertEqual([x[0] for x in client.calls], ["GET", "GET", "POST", "GET"])
        self.assertTrue(client.calls[2][2]["private"])

    def test_post_accepted_but_get_failed_never_retries(self):
        client = FakeGitHub([(200, {"login": self.OWNER}), (404, None), (201, {}), (0, None)])
        self.assertEqual(self.create(client)["status"], "CREATED_READBACK_TOKEN_VAZIO")
        self.assertEqual(sum(1 for x in client.calls if x[0] == "POST"), 1)

    def test_org_uses_distinct_endpoint(self):
        owner = "instituto-Rafael"
        client = FakeGitHub([(200, {"login": self.OWNER}), (404, None), (403, None)])
        self.assertEqual(self.create(client, owner=owner, name="safe-org")["status"],
                         "PROVIDER_REJECTED_OR_UNKNOWN")
        self.assertEqual(client.calls[2][1], "/orgs/instituto-Rafael/repos")

    def test_readback_public_is_not_verified(self):
        client = FakeGitHub([(200, {"login": self.OWNER}), (404, None), (201, {}),
                             (200, {"owner": {"login": self.OWNER}, "private": False,
                                    "full_name": self.OWNER + "/" + self.NAME})])
        self.assertEqual(self.create(client)["status"], "CREATED_READBACK_MISMATCH")


if __name__ == "__main__":
    unittest.main()
