"""Scoped invariants for RAFAELIA PR DMAIC snapshot; no live/network assertions."""
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
PATH = ROOT / "configs/rafgittools-pr-dmaic-20261023.v1.json"

class PortfolioContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(PATH.read_text(encoding="utf-8"))

    def test_status_and_source(self):
        d = self.data
        self.assertFalse(d["claim_allowed"])
        self.assertFalse(d["certification_claim"])
        self.assertEqual("SOURCE_SNAPSHOT_PROPOSED_IN_DRAFT", d["status"])
        self.assertEqual("38a20b00dbf789e6366d026a374bf63dba4a1220", d["provider_main_sha"])

    def test_coverage(self):
        d = self.data
        items = d["items"]
        self.assertEqual(23, len(items))
        self.assertEqual(23, len({x["number"] for x in items}))
        self.assertEqual(23, sum(d["family_counts"].values()))
        self.assertEqual([x["created_at"] for x in items], sorted(x["created_at"] for x in items))
        for item in items:
            with self.subTest(pr=item["number"]):
                self.assertEqual(40, len(item["observed_head_sha"]))
                self.assertEqual(40, len(item["observed_base_sha"]))
                self.assertTrue(item["observation"].startswith("★ "))
                self.assertTrue(item["bifurcation"].startswith("† "))
                self.assertTrue(item["transmutation"].startswith("‡ "))
                self.assertFalse(item["claim_allowed"])
                self.assertFalse(item["release_allowed"])
                self.assertTrue(item["gaps"])
                self.assertIn(item["ci"]["state"], ("PASS_SCOPED_HISTORICAL_OBSERVATION","NOT_ASSESSED_EXACT_HEAD"))

    def test_guarded_forks(self):
        d=self.data
        observed={x["number"]:x for x in d["items"]}
        self.assertEqual({680,681,682,683}, {x["number"] for x in observed.values() if not x["draft"]})
        self.assertEqual(set([546,547,548,550,673,689,691]),set(d["shared_paths"]["app/build.gradle"]))
        self.assertEqual(set([680,681,682,686,687,693,694,695,700]),set(d["shared_paths"][".github/workflows/START.yml"]))
        self.assertTrue(all("DRAFT_LABEL_STATE_MISMATCH" in observed[n]["gaps"] for n in (680,681,682,683)))
        self.assertTrue(all(v == "TOKEN_VAZIO_NOT_MEASURED" for v in d["baseline_metrics"].values()))

if __name__ == "__main__":
    unittest.main()
