"""Promotion-hold P0 adversarial tests. Offline; no provider tokens/network."""
import copy
import importlib.util
import json
import os
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "promotion_hold_pat_agent_v1", ROOT / "scripts/promotion_hold_pat_agent_v1.py")
agent = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = agent
SPEC.loader.exec_module(agent)
CONTRACT = json.loads(
    (ROOT / "configs/promotion-hold-agent.v1.json").read_text(encoding="utf-8"))
TARGET = CONTRACT["targets"][0]
ENV = {"GITHUB_EVENT_NAME": "workflow_dispatch", "GITHUB_REF": "refs/heads/main",
       "GITHUB_ACTOR": "rafaelmeloreisnovo",
       "GITHUB_REPOSITORY": "rafaelmeloreisnovo/RafGitTools",
       "PROMOTION_HOLD_CONFIRM": "EXPLICIT_HOLD_ONLY"}


def snapshot(title=None, sha=None, state="open", merged_at=None):
    return {"number": TARGET["pr"], "state": state, "merged_at": merged_at,
            "head": {"sha": sha or TARGET["head_sha"]},
            "base": {"ref": "main"},
            "title": title if title is not None else TARGET["title"]}


class FakeProvider:
    def __init__(self, first, second=None, patch_status=200):
        self.first = first
        self.second = second if second is not None else first
        self.patch_status = patch_status
        self.calls = []
        self.count = 0

    def __call__(self, method, path, payload):
        self.calls.append((method, path, payload))
        if method == "PATCH":
            if payload != {"title": TARGET["title"]}:
                raise AssertionError("unexpected provider mutation")
            return self.patch_status, {}
        self.count += 1
        return 200, self.first if self.count == 1 else self.second


class HoldTests(unittest.TestCase):
    def test_exact_source_contract(self):
        self.assertEqual(agent.validate_contract(CONTRACT), [])

    def test_three_scoped_targets_only(self):
        self.assertEqual({x["id"] for x in CONTRACT["targets"]},
                         {"mapa750", "rafpolimata431", "rafpolimata432"})
        self.assertTrue(all(x["title"].startswith("[NÃO PROMOVER — P0: ")
                            for x in CONTRACT["targets"]))

    def test_fake_new_target_denied(self):
        d = copy.deepcopy(CONTRACT)
        d["targets"].append(dict(TARGET, id="evil", repository="other/repo"))
        self.assertIn("TARGET_ALLOWLIST_INVALID", agent.validate_contract(d))

    def test_boolean_pr_rejected(self):
        d = copy.deepcopy(CONTRACT)
        d["targets"][0]["pr"] = True
        self.assertIn("TARGET_PR_INVALID", agent.validate_contract(d))

    def test_unknown_repo_rejected(self):
        d = copy.deepcopy(CONTRACT)
        d["targets"][0]["repository"] = "attacker/repo"
        self.assertIn("TARGET_REPOSITORY_INVALID", agent.validate_contract(d))

    def test_non_blocking_title_rejected(self):
        d = copy.deepcopy(CONTRACT)
        d["targets"][0]["title"] = "READY TO MERGE"
        self.assertIn("NON_PROMOTION_TITLE_INVALID", agent.validate_contract(d))

    def test_contract_cannot_grant_merge_or_ruleset(self):
        d = copy.deepcopy(CONTRACT)
        d["automatic_merge"] = True
        d["approval_or_ruleset_mutation"] = True
        self.assertTrue(any(e.startswith("INVALID_CONTRACT") for e in
                            agent.validate_contract(d)))

    def test_good_hold_inspect_no_patch(self):
        provider = FakeProvider(snapshot())
        receipt, code = agent.evaluate(TARGET, "inspect", provider, ENV)
        self.assertEqual(code, 0)
        self.assertEqual(receipt["state"],
                         "HOLD_TITLE_CONFIRMED_WRITE_PERMISSION_UNPROVEN")
        self.assertEqual([x[0] for x in provider.calls], ["GET"])
        self.assertFalse(receipt["promotion_allowed"])

    def test_head_drift_fails_without_patch(self):
        provider = FakeProvider(snapshot(sha="0"*40, title="old"))
        receipt, code = agent.evaluate(TARGET, "mark", provider, ENV)
        self.assertEqual(code, 3)
        self.assertEqual(receipt["state"], "BLOCKED_HEAD_DRIFT")
        self.assertEqual(len(provider.calls), 1)

    def test_closed_pr_fails_without_patch(self):
        provider = FakeProvider(snapshot(state="closed", title="old"))
        receipt, code = agent.evaluate(TARGET, "mark", provider, ENV)
        self.assertEqual((receipt["state"], code), ("BLOCKED_PR_NOT_OPEN", 3))
        self.assertEqual(len(provider.calls), 1)

    def test_untrusted_event_never_reaches_provider(self):
        provider = FakeProvider(snapshot(title="old"))
        for k,v in (("GITHUB_EVENT_NAME", "push"),
                    ("GITHUB_REF", "refs/heads/untrusted"),
                    ("GITHUB_ACTOR", "another-user")):
            env = dict(ENV, **{k: v})
            receipt, code = agent.evaluate(TARGET, "mark", provider, env)
            self.assertEqual((receipt["state"], code),
                             ("BLOCKED_UNTRUSTED_DISPATCH",3))
        self.assertEqual(provider.calls, [])

    def test_no_manual_confirmation_no_patch(self):
        provider = FakeProvider(snapshot(title="old"))
        receipt, code = agent.evaluate(TARGET, "mark", provider,
                                        dict(ENV, PROMOTION_HOLD_CONFIRM=""))
        self.assertEqual((receipt["state"], code),
                         ("BLOCKED_EXPLICIT_CONFIRMATION", 3))
        self.assertEqual(provider.calls, [])

    def test_title_drift_inspect_never_mutates(self):
        provider = FakeProvider(snapshot(title="unmarked"))
        receipt, code = agent.evaluate(TARGET, "inspect", provider, ENV)
        self.assertEqual((receipt["state"], code), ("BLOCKED_HOLD_TITLE_DRIFT", 3))
        self.assertEqual([x[0] for x in provider.calls], ["GET"])

    def test_mark_only_title_with_readback(self):
        provider = FakeProvider(snapshot(title="unmarked"), snapshot())
        receipt, code = agent.evaluate(TARGET, "mark", provider, ENV)
        self.assertEqual(code,0)
        self.assertEqual(receipt["state"], "HOLD_TITLE_PATCHED_AND_READBACK")
        self.assertEqual([x[0] for x in provider.calls], ["GET", "PATCH", "GET"])
        self.assertEqual(provider.calls[1][2], {"title": TARGET["title"]})

    def test_mark_write_denied_fail_closed(self):
        provider = FakeProvider(snapshot(title="unmarked"), patch_status=403)
        receipt, code = agent.evaluate(TARGET, "mark", provider, ENV)
        self.assertEqual((receipt["state"], code),
                         ("BLOCKED_PROVIDER_TITLE_PATCH",3))
        self.assertTrue(receipt["provider_write_attempted"])

    def test_post_patch_drift_fails(self):
        provider = FakeProvider(snapshot(title="unmarked"),snapshot(title="unmarked"))
        receipt, code = agent.evaluate(TARGET, "mark", provider, ENV)
        self.assertEqual((receipt["state"], code),
                         ("BLOCKED_POSTPATCH_TITLE_MISMATCH",3))

    def test_post_patch_sha_drift_fails(self):
        provider = FakeProvider(snapshot(title="unmarked"),snapshot(sha="a"*40))
        receipt, code = agent.evaluate(TARGET, "mark", provider, ENV)
        self.assertEqual((receipt["state"], code),("BLOCKED_HEAD_DRIFT",3))

    def test_wrong_base_fails(self):
        bad = snapshot(title="unmarked")
        bad["base"]["ref"] = "feature"
        provider = FakeProvider(bad)
        receipt, code = agent.evaluate(TARGET, "mark", provider, ENV)
        self.assertEqual((receipt["state"], code),("BLOCKED_BASE_DRIFT",3))
        self.assertEqual(len(provider.calls),1)

    def test_unexpected_provider_status_fails(self):
        def forbidden(method, path, payload):
            return 403, None
        receipt, code = agent.evaluate(TARGET, "inspect", forbidden, ENV)
        self.assertEqual((receipt["state"], code), ("BLOCKED_PROVIDER_READ",3))

    def test_cli_missing_pat_fails_without_fallback(self):
        fake = dict(ENV, RAFAELIA_AGENTS_TOKEN="")
        with patch.dict(os.environ, fake, clear=True):
            with patch("sys.stdout"):
                status = agent.main(["--operation", "mark", "--target", "mapa750"])
        self.assertEqual(status,3)

    def test_validate_cli_requires_no_pat(self):
        with patch.dict(os.environ, {}, clear=True):
            with patch("sys.stdout"):
                status = agent.main(["--operation", "validate"])
        self.assertEqual(status,0)


if __name__ == "__main__":
    unittest.main()
