import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SNAP=ROOT/"data/navigation/RLL_BRANCH_ATLAS_SNAPSHOT_20260929.json"
CFG=ROOT/"configs/rll-branch-atlas.v1.yml"
HTML=ROOT/"docs/site/rll-atlas/index.html"

class BranchAtlasTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data=json.loads(SNAP.read_text(encoding="utf-8"))
        cls.cfg=CFG.read_text(encoding="utf-8")
        cls.html=HTML.read_text(encoding="utf-8")

    def test_totals_match_records(self):
        for repo,meta in self.data["repositories"].items():
            self.assertEqual(meta["total"],len(meta["branches"]),repo)

    def test_observed_census(self):
        self.assertEqual(self.data["repositories"]["instituto-Rafael/relativity-living-light"]["total"],872)
        self.assertEqual(self.data["repositories"]["rafaelmeloreisnovo/RafGitTools"]["total"],325)

    def test_anchor_no_rename(self):
        rll={r["branch"]:r for r in self.data["repositories"]["instituto-Rafael/relativity-living-light"]["branches"]}
        self.assertEqual(rll["main"]["rename_state"],"NO_RENAME_ANCHOR")
        self.assertEqual(rll["rll/lab"]["rename_state"],"NO_RENAME_ANCHOR")

    def test_no_pat_fallback_contract(self):
        self.assertIn("NO_PAT_FALLBACK",self.cfg)
        self.assertIn("SECRET_VALUE_NEVER_PERSISTED_OR_PRINTED",self.cfg)
        self.assertIn("PAT_AGENTS:",self.cfg)
        self.assertIn("TOKEN_VAZIO_PERMISSION_PROBE_REQUIRED",self.cfg)

    def test_comboboxes_present(self):
        self.assertIn('id="repo"',self.html)
        self.assertIn('id="family"',self.html)
        self.assertIn('id="branch"',self.html)

if __name__=="__main__":
    unittest.main()
