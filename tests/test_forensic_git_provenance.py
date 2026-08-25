import importlib.util
import json
import pathlib
import shutil
import subprocess
import tempfile
import unittest

MODULE_PATH = (
    pathlib.Path(__file__).resolve().parents[1]
    / "tools"
    / "forensic_git_provenance"
    / "forensic_git.py"
)
SPEC = importlib.util.spec_from_file_location("forensic_git", MODULE_PATH)
fg = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(fg)


class ForensicGitTests(unittest.TestCase):
    def base_commit(self):
        return {
            "sha": "1" * 40,
            "parents": ["0" * 40],
            "tree": "2" * 40,
            "parent_trees": ["2" * 40],
            "author": {
                "name": "GitHub Copilot",
                "email_fingerprint": "sha256:" + "a" * 64,
                "date": "2025-11-25T00:04:02Z",
            },
            "committer": {
                "name": "GitHub Copilot",
                "email_fingerprint": "sha256:" + "b" * 64,
                "date": "2025-11-25T00:04:02Z",
            },
            "signature": {"git_status": "N", "signer": None, "key_id": None},
            "message": "Initial plan",
            "refs_containing": ["refs/heads/master"],
            "diff": {
                "files_changed": 0,
                "files": [],
                "patch_id_stable": None,
                "empty_against_first_parent": True,
                "empty_root": False,
            },
            "platform": {
                "first_seen_at": "2025-11-25T00:42:22Z",
                "pusher": None,
                "source": "pull_request",
            },
            "external_receipt": None,
            "agent": {"producer": "copilot", "task_receipt": None},
        }

    def evidence(self):
        return {
            "schema_version": fg.SCHEMA_VERSION,
            "case_id": "case-f7c01ff",
            "generated_at": "2026-08-03T12:00:00Z",
            "collection_mode": "ENRICHED_FIXTURE",
            "claim_allowed": False,
            "repository": {"repository_name": "BLAKE3-fork"},
            "collector": {"tool_version": fg.TOOL_VERSION},
            "revision": "fixture",
            "commits": [self.base_commit()],
            "external_events": [],
            "limitations": ["COMMENTS_EDIT_HISTORY_MISSING"],
        }

    def test_empty_agent_commit_is_proved_but_intent_remains_empty(self):
        report = fg.audit_evidence(
            self.evidence(),
            generated_at="2026-08-03T12:01:00Z",
        )
        findings = report["commit_reports"][0]["findings"]
        states = {f["code"]: f["state"] for f in findings}
        self.assertEqual(states["EMPTY_COMMIT"], fg.CLAIM_PROVADO)
        self.assertEqual(states["AGENT_PROVENANCE_INDICATOR"], fg.CLAIM_EVIDENCIADO)
        self.assertEqual(states["AGENT_TASK_RECEIPT_MISSING"], fg.CLAIM_TOKEN_VAZIO)
        self.assertEqual(states["DELIBERATE_DATE_SHIFT"], fg.CLAIM_TOKEN_VAZIO)
        self.assertEqual(states["HISTORY_FALSIFICATION"], fg.CLAIM_TOKEN_VAZIO)
        self.assertTrue(report["verdict"]["anomaly_is_not_intent"])
        self.assertFalse(report["claim_allowed"])

    def test_token_vazio_findings_do_not_raise_score(self):
        report = fg.audit_evidence(
            self.evidence(),
            generated_at="2026-08-03T12:01:00Z",
        )
        counted = report["anomaly_score"]["counted_findings"]
        self.assertNotIn("DELIBERATE_DATE_SHIFT", counted)
        self.assertNotIn("HISTORY_FALSIFICATION", counted)
        self.assertNotIn("MALICIOUS_COORDINATION", counted)

    def test_report_hash_verifies_and_breaks_on_tamper(self):
        report = fg.audit_evidence(
            self.evidence(),
            previous_event_hash="f" * 64,
            generated_at="2026-08-03T12:01:00Z",
        )
        self.assertTrue(fg.verify_report(report))
        report["verdict"]["status"] = "TAMPERED"
        self.assertFalse(fg.verify_report(report))

    def test_invalid_sha_is_rejected(self):
        evidence = self.evidence()
        evidence["commits"][0]["sha"] = "not-a-sha"
        with self.assertRaises(fg.ForensicError):
            fg.validate_evidence(evidence)

    @unittest.skipUnless(shutil.which("git"), "git executable required")
    def test_local_collection_is_read_only_and_detects_empty_commit(self):
        with tempfile.TemporaryDirectory() as td:
            repo = pathlib.Path(td) / "repo"
            repo.mkdir()
            subprocess.run(["git", "-C", str(repo), "init"], check=True, stdout=subprocess.PIPE)
            subprocess.run(["git", "-C", str(repo), "config", "user.name", "Test User"], check=True)
            subprocess.run(["git", "-C", str(repo), "config", "user.email", "test@example.invalid"], check=True)
            (repo / "a.txt").write_text("one\n", encoding="utf-8")
            subprocess.run(["git", "-C", str(repo), "add", "a.txt"], check=True)
            subprocess.run(
                ["git", "-C", str(repo), "commit", "-m", "first"],
                check=True,
                stdout=subprocess.PIPE,
                env={**dict(__import__("os").environ), "GIT_AUTHOR_DATE": "2025-01-01T00:00:00Z", "GIT_COMMITTER_DATE": "2025-01-01T00:00:00Z"},
            )
            subprocess.run(
                ["git", "-C", str(repo), "commit", "--allow-empty", "-m", "sentinel"],
                check=True,
                stdout=subprocess.PIPE,
                env={**dict(__import__("os").environ), "GIT_AUTHOR_DATE": "2025-01-01T00:01:00Z", "GIT_COMMITTER_DATE": "2025-01-01T00:01:00Z"},
            )
            before = subprocess.run(
                ["git", "-C", str(repo), "status", "--porcelain=v1"],
                check=True,
                stdout=subprocess.PIPE,
                text=True,
            ).stdout
            evidence = fg.collect_repository(
                str(repo),
                "HEAD",
                10,
                generated_at="2026-08-03T12:00:00Z",
            )
            after = subprocess.run(
                ["git", "-C", str(repo), "status", "--porcelain=v1"],
                check=True,
                stdout=subprocess.PIPE,
                text=True,
            ).stdout
            self.assertEqual(before, after)
            self.assertEqual(len(evidence["commits"]), 2)
            self.assertTrue(evidence["commits"][-1]["diff"]["empty_against_first_parent"])
            self.assertFalse(evidence["collector"]["working_tree_mutated"])
            self.assertNotIn("repository_path", evidence["repository"])

    def test_cli_round_trip(self):
        with tempfile.TemporaryDirectory() as td:
            inp = pathlib.Path(td) / "evidence.json"
            out = pathlib.Path(td) / "report.json"
            inp.write_text(json.dumps(self.evidence()), encoding="utf-8")
            rc = fg.main([
                "audit",
                "--input", str(inp),
                "--output", str(out),
                "--generated-at", "2026-08-03T12:01:00Z",
            ])
            self.assertEqual(rc, 0)
            report = json.loads(out.read_text(encoding="utf-8"))
            self.assertTrue(fg.verify_report(report))
            self.assertEqual(fg.main(["verify", "--input", str(out)]), 0)


if __name__ == "__main__":
    unittest.main()
