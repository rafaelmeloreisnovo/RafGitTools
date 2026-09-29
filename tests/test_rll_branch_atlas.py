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

    def test_totals_are_dynamic_and_match_records(self):
        for repo,meta in self.data["repositories"].items():
            self.assertEqual(meta["total"],len(meta["branches"]),repo)
            self.assertGreater(meta["total"],0)

    def test_main_is_root_and_unchanged(self):
        self.assertEqual(self.data["root_branch"],"main")
        for repo,meta in self.data["repositories"].items():
            rows={r["branch"]:r for r in meta["branches"]}
            if "main" in rows:
                self.assertEqual(rows["main"]["family"],"main")
                self.assertEqual(rows["main"]["display_label"],"main")
                self.assertEqual(rows["main"]["rename_state"],"NO_RENAME_ROOT")

    def test_rll_maturity_order(self):
        rows={r["branch"]:r for r in self.data["repositories"]["instituto-Rafael/relativity-living-light"]["branches"]}
        self.assertEqual(rows["rll/lab"]["family"],"05-lab")
        self.assertEqual(rows["rll/integration"]["family"],"06-integration")
        self.assertEqual(rows["rll/release"]["family"],"07-release")
        for name in ("rll/lab","rll/integration","rll/release"):
            self.assertEqual(rows[name]["rename_state"],"NO_RENAME_MATURITY_REF")

    def test_navigation_numbering_is_not_physical_rename(self):
        self.assertIn("numbering_is_navigation_only: true",self.cfg)
        self.assertIn("main_label: main",self.cfg)

    def test_no_pat_fallback_contract(self):
        self.assertIn("NO_PAT_FALLBACK",self.cfg)
        self.assertIn("SECRET_VALUE_NEVER_PERSISTED_OR_PRINTED",self.cfg)

    def test_comboboxes_present(self):
        self.assertIn('id="repo"',self.html)
        self.assertIn('id="family"',self.html)
        self.assertIn('id="branch"',self.html)

if __name__=="__main__":
    unittest.main()
