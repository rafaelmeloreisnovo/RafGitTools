"""Offline independent tests; no network, tokens or provider mutations."""
import unittest
from scripts import federated_admin_protection_v1 as m


class Fake:
    def __init__(self, repo="rafaelmeloreisnovo/Mapa",
                 sha="0123456789abcdef0123456789abcdef01234567",
                 protected=False, rulesets=None, admin=True, repo_id=None,
                 private=False, protection_http=None):
        self.repo = repo
        self.sha = sha
        self.protected = protected
        self.rulesets = [] if rulesets is None else rulesets
        self.admin = admin
        self.repo_id = m.TARGETS.get(repo) if repo_id is None else repo_id
        self.private = private
        self.protection_http = protection_http
        self.calls = []
        self.mutation_count = 0

    def request(self, method, path, payload=None):
        self.calls.append((method, path))
        if method == "PUT":
            if path != "/repos/" + self.repo + "/branches/main/protection":
                raise AssertionError("unexpected write target")
            if payload != m.PROTECTION:
                raise AssertionError("unexpected write body")
            self.mutation_count += 1
            self.protected = True
            return 200, {}
        if method != "GET":
            raise AssertionError("unexpected method")
        if path == "/repos/" + self.repo:
            return 200, {"id": self.repo_id, "full_name": self.repo,
                         "private": self.private, "default_branch": "main",
                         "permissions": {"admin": self.admin}}
        if path == "/repos/" + self.repo + "/branches/main":
            return 200, {"commit": {"sha": self.sha}, "protected": self.protected}
        if path == "/repos/" + self.repo + "/rulesets":
            return 200, self.rulesets
        if path == "/repos/" + self.repo + "/branches/main/protection":
            status = self.protection_http
            if status is None:
                status = 200 if self.protected else 404
            if status != 200:
                return status, None
            return 200, {"enforce_admins": {"enabled": True},
                         "allow_force_pushes": {"enabled": False},
                         "allow_deletions": {"enabled": False}}
        raise AssertionError("unlisted URL")


SHA = "0123456789abcdef0123456789abcdef01234567"


class Tests(unittest.TestCase):
    def test_workflow_boundary(self):
        m.validate("rafaelmeloreisnovo/Mapa", SHA, "protect",
                   "rafaelmeloreisnovo", "workflow_dispatch",
                   "refs/heads/main", SHA, "1")
        for changed in (
            ("someone", "workflow_dispatch", "refs/heads/main", SHA, "1"),
            ("rafaelmeloreisnovo", "push", "refs/heads/main", SHA, "1"),
            ("rafaelmeloreisnovo", "workflow_dispatch", "refs/heads/feature", SHA, "1"),
            ("rafaelmeloreisnovo", "workflow_dispatch", "refs/heads/main", "bad", "1"),
            ("rafaelmeloreisnovo", "workflow_dispatch", "refs/heads/main", SHA, "2"),
        ):
            with self.subTest(changed=changed), self.assertRaises(m.Denied):
                m.validate("rafaelmeloreisnovo/Mapa", SHA, "protect", *changed)

    def test_invalid_target_sha_operation(self):
        for repo, sha, op in (
            ("rafaelmeloreisnovo/private", SHA, "preflight"),
            ("instituto-Rafael/relativity-living-light", SHA, "preflight"),
            ("rafaelmeloreisnovo/Mapa", "f" * 39, "preflight"),
            ("rafaelmeloreisnovo/Mapa", SHA, "visibility"),
            ("rafaelmeloreisnovo/actions/../../victim", SHA, "protect"),
        ):
            with self.subTest(repo=repo, op=op), self.assertRaises(m.Denied):
                m.validate(repo, sha, op, "rafaelmeloreisnovo",
                           "workflow_dispatch", "refs/heads/main", SHA, "1")

    def test_preflight_never_writes(self):
        p = Fake()
        result = m.run(p, p.repo, SHA, "preflight")
        self.assertEqual(result["status"], "PREFLIGHT_SCOPED_NOT_APPLIED")
        self.assertFalse(result["apply_attempted"])
        self.assertFalse(result["provider_write_verified"])
        self.assertEqual(p.mutation_count, 0)

    def test_protect_single_bound_repo(self):
        for repo in m.TARGETS:
            with self.subTest(repo=repo):
                p = Fake(repo=repo)
                result = m.run(p, repo, SHA, "protect")
                self.assertEqual(result["status"], "PASS_SCOPED_FORCE_PUSH_DELETE_GUARD_ONLY")
                self.assertTrue(result["provider_write_verified"])
                self.assertEqual(p.mutation_count, 1)
                self.assertEqual(len([x for x in p.calls if x[0] == "PUT"]), 1)
                self.assertTrue(p.protected)

    def test_fail_closed_stale_or_unverified(self):
        scenarios = [
            {"sha": "a" * 40},
            {"rulesets": [{"enforcement": "active"}]},
            {"rulesets": [{"name": "unknown"}]},
            {"protected": True},
            {"protection_http": 403},
            {"admin": False},
            {"repo_id": 999},
            {"private": True},
        ]
        for kwargs in scenarios:
            with self.subTest(scenario=kwargs):
                p = Fake(**kwargs)
                with self.assertRaises(m.Denied):
                    m.run(p, p.repo, SHA, "protect")
                self.assertEqual(p.mutation_count, 0)

    def test_guard_policy_never_relaxes_existing(self):
        self.assertIsNone(m.PROTECTION["required_status_checks"])
        self.assertTrue(m.PROTECTION["enforce_admins"])
        self.assertFalse(m.PROTECTION["allow_force_pushes"])
        self.assertFalse(m.PROTECTION["allow_deletions"])
        self.assertEqual(set(m.TARGETS), {
            "rafaelmeloreisnovo/Mapa", "rafaelmeloreisnovo/actions",
        })

    def test_provider_transport_path_guard(self):
        p = m.Provider("fixture-not-a-real-token")
        for path in (
            "/repos/rafaelmeloreisnovo/other/branches/main/protection",
            "/repos/rafaelmeloreisnovo/Mapa/actions/secrets",
            "/repos/rafaelmeloreisnovo/Mapa/branches/feature/protection",
            "/repos/rafaelmeloreisnovo/Mapa/branches/main/protection/other",
        ):
            with self.subTest(path=path), self.assertRaises(m.Denied):
                p.request("PUT", path, m.PROTECTION)
        with self.assertRaises(m.Denied):
            p.request("DELETE", "/repos/rafaelmeloreisnovo/Mapa/branches/main/protection")
        with self.assertRaises(m.Denied):
            p.request("PUT", "/repos/rafaelmeloreisnovo/Mapa/branches/main/protection",
                      dict(m.PROTECTION, allow_deletions=True))


if __name__ == "__main__":
    unittest.main()
