import json
import subprocess
import tempfile
import unittest
from pathlib import Path

from scripts.resolve_session_ai_work_packet import (
    DISPATCH_PATH,
    MAPA_COMMIT,
    resolve,
    Blocked,
)


class SessionDispatchResolverTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixture = json.loads(Path("tests/fixtures/session_ai_dispatch.fixture.json").read_text())

    def checkout(self, mutate=None):
        temp = tempfile.TemporaryDirectory()
        root = Path(temp.name)
        path = root / DISPATCH_PATH
        path.parent.mkdir(parents=True)
        data = json.loads(json.dumps(self.fixture))
        if mutate:
            mutate(data)
        path.write_text(json.dumps(data, sort_keys=True, separators=(",", ":")) + "\n")
        subprocess.run(["git", "init", "-q", str(root)], check=True)
        subprocess.run(["git", "-C", str(root), "config", "user.name", "Fixture"], check=True)
        subprocess.run(["git", "-C", str(root), "config", "user.email", "fixture@example.invalid"], check=True)
        subprocess.run(["git", "-C", str(root), "add", DISPATCH_PATH], check=True)
        subprocess.run(["git", "-C", str(root), "commit", "-qm", "fixture"], check=True)
        commit = subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip()
        blob = subprocess.check_output(["git", "-C", str(root), "rev-parse", f"HEAD:{DISPATCH_PATH}"], text=True).strip()
        return temp, root, commit, blob

    def test_correct_packet_resolves_all_assigned_roles_without_execution_claim(self):
        temp, root, commit, blob = self.checkout()
        with temp:
            result = resolve(root, packet_id="SP08_SESSION_TO_WORK_PACKETS", expected_commit=commit, expected_blob=blob)
        self.assertEqual(result["state"], "ROUTE_RESOLVED")
        self.assertTrue(result["agents"])
        self.assertFalse(result["assignment_is_execution"])
        self.assertFalse(result["claim_allowed"])

    def test_correct_agent_resolves_role(self):
        temp, root, commit, blob = self.checkout()
        with temp:
            result = resolve(root, agent_id="AI02_CONTROL_EXECUTOR", expected_commit=commit, expected_blob=blob)
        self.assertEqual(result["agents"][0]["owner"], "rafaelmeloreisnovo/RafGitTools")

    def test_commit_mismatch_blocks(self):
        temp, root, _commit, blob = self.checkout()
        with temp:
            subprocess.run(["git", "-C", str(root), "commit", "--allow-empty", "-qm", "advance"], check=True)
            with self.assertRaisesRegex(Blocked, "HEAD mismatch"):
                resolve(root, packet_id="SP01_TEMPLO_VISUAL_PRIVACY", expected_commit=_commit, expected_blob=blob)

    def test_blob_mismatch_blocks(self):
        temp, root, commit, _blob = self.checkout()
        with temp:
            path = root / DISPATCH_PATH
            path.write_text(path.read_text() + " ")
            with self.assertRaisesRegex(Blocked, "blob mismatch"):
                resolve(root, packet_id="SP01_TEMPLO_VISUAL_PRIVACY", expected_commit=commit, expected_blob="0" * 40)

    def test_schema_mismatch_blocks(self):
        temp, root, commit, blob = self.checkout(lambda d: d.update(schema="WRONG"))
        with temp:
            with self.assertRaisesRegex(Blocked, "unexpected dispatch schema"):
                resolve(root, packet_id="SP01_TEMPLO_VISUAL_PRIVACY", expected_commit=commit, expected_blob=blob)

    def test_duplicate_agent_id_blocks(self):
        def mutate(data):
            data["agent_roles"].append(dict(data["agent_roles"][0]))
        temp, root, commit, blob = self.checkout(mutate)
        with temp:
            with self.assertRaisesRegex(Blocked, "duplicate agent id"):
                resolve(root, packet_id="SP01_TEMPLO_VISUAL_PRIVACY", expected_commit=commit, expected_blob=blob)

    def test_unknown_packet_blocks(self):
        temp, root, commit, blob = self.checkout()
        with temp:
            with self.assertRaisesRegex(Blocked, "unknown packet"):
                resolve(root, packet_id="SP99_MISSING", expected_commit=commit, expected_blob=blob)


if __name__ == "__main__":
    unittest.main()
