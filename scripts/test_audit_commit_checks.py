#!/usr/bin/env python3
import pathlib
import sys
import unittest

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import audit_commit_checks as mod


def run(i, status="completed", conclusion="success"):
    return {"id": i, "name": f"check-{i}", "status": status, "conclusion": conclusion}


class PaginationTests(unittest.TestCase):
    def test_exhausts_multiple_pages_and_finds_late_failure(self):
        pages = {
            1: [run(i) for i in range(1, 31)],
            2: [run(i) for i in range(31, 61)],
            3: [run(i, conclusion="failure" if i == 92 else "success") for i in range(61, 93)],
        }

        def fetch(page, per_page):
            self.assertEqual(100, per_page)
            return mod.Page(92, pages.get(page, []))

        out = mod.collect_check_runs(fetch)
        self.assertTrue(out["complete_enumeration"])
        self.assertEqual(92, out["returned_unique_count"])
        self.assertEqual(3, out["pages_fetched"])
        self.assertEqual(1, out["failure_count"])
        self.assertEqual("COMPLETE_WITH_FAILURES", out["state"])

    def test_incomplete_is_fail_closed(self):
        def fetch(page, per_page):
            return mod.Page(92, [run(i) for i in range(1, 31)] if page == 1 else [])

        out = mod.collect_check_runs(fetch)
        self.assertFalse(out["complete_enumeration"])
        self.assertEqual("INCOMPLETE_ENUMERATION", out["state"])

    def test_total_count_drift_is_audit(self):
        def fetch(page, per_page):
            if page == 1:
                return mod.Page(101, [run(i) for i in range(1, 101)])
            return mod.Page(102, [run(101), run(102)])

        out = mod.collect_check_runs(fetch)
        self.assertTrue(out["complete_enumeration"])
        self.assertTrue(out["snapshot_unstable"])
        self.assertEqual("AUDIT_SNAPSHOT_UNSTABLE", out["state"])


if __name__ == "__main__":
    unittest.main()
