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

    def test_overlapping_pages_same_total_fail_closed_even_if_all_green(self):
        # A moving listing may repeat an ID and still reach the exact total.
        # The old collector overwrote it and falsely marked the set stable.
        def fetch(page, per_page):
            self.assertEqual(100, per_page)
            if page == 1:
                return mod.Page(101, [run(i) for i in range(1, 101)])
            if page == 2:
                return mod.Page(101, [run(100), run(101)])
            self.fail("unexpected extra page")

        out = mod.collect_check_runs(fetch)
        self.assertEqual(101, out["returned_unique_count"])
        self.assertTrue(out["complete_enumeration"])
        self.assertEqual([100], out["repeated_check_ids"])
        self.assertTrue(out["snapshot_unstable"])
        self.assertEqual("AUDIT_SNAPSHOT_UNSTABLE", out["state"])

    def test_total_count_drift_is_audit(self):
        def fetch(page, per_page):
            if page == 1:
                return mod.Page(101, [run(i) for i in range(1, 101)])
            return mod.Page(102, [run(101), run(102)])

        out = mod.collect_check_runs(fetch)
        self.assertTrue(out["complete_enumeration"])
        self.assertTrue(out["snapshot_unstable"])
        self.assertEqual("AUDIT_SNAPSHOT_UNSTABLE", out["state"])



class StrictGateTests(unittest.TestCase):
    SHA = "a" * 40

    @classmethod
    def checks(cls, *items):
        def fetch(page, per_page):
            return mod.Page(len(items), list(items) if page == 1 else [])
        return mod.collect_check_runs(fetch)

    @staticmethod
    def legacy(state="pending", count=0):
        return {"combined_state": state, "status_count": count, "statuses": []}

    @classmethod
    def checked(cls, number=1, status="completed", conclusion="success", sha=None):
        value = run(number, status, conclusion)
        value["head_sha"] = cls.SHA if sha is None else sha
        return value

    def gate(self, items, required=("check-1",), legacy=None):
        return mod.evaluate_strict_gate(
            self.checks(*items),
            self.legacy() if legacy is None else legacy,
            self.SHA,
            required,
        )

    def test_zero_checks_never_green(self):
        checks = self.checks()
        self.assertEqual("NO_CHECKS_OBSERVED", checks["state"])
        self.assertFalse(self.gate(())["gate_pass"])

    def test_all_success_exact_sha_and_named_check_pass_scoped(self):
        out = self.gate((self.checked(), self.checked(2)),
                        required=("check-1", "check-2"))
        self.assertTrue(out["gate_pass"])
        self.assertEqual("PASS_OBSERVED_REQUIRED_CHECKS", out["state"])
        self.assertFalse(out["claim_allowed"])

    def test_missing_required_name_fails(self):
        out = self.gate((self.checked(),), required=("check-1", "security"))
        self.assertIn("MISSING_REQUIRED_CHECK:security", out["reasons"])

    def test_empty_required_set_never_passes(self):
        out = self.gate((self.checked(),), required=())
        self.assertIn("REQUIRED_CHECK_SET_EMPTY_OR_INVALID", out["reasons"])

    def test_sha_absent_or_mismatched_fails(self):
        for sha in ("", "b" * 40):
            with self.subTest(sha=sha):
                self.assertFalse(self.gate((self.checked(sha=sha),))["gate_pass"])

    def test_pending_skipped_cancelled_and_failed_never_pass(self):
        for status, conclusion in (("in_progress", None),
                                   ("completed", "skipped"),
                                   ("completed", "cancelled"),
                                   ("completed", "failure")):
            with self.subTest(status=status, conclusion=conclusion):
                self.assertFalse(self.gate(
                    (self.checked(status=status, conclusion=conclusion),)
                )["gate_pass"])

    def test_legacy_failure_or_pending_never_masked(self):
        for state in ("failure", "pending", "error"):
            with self.subTest(state=state):
                self.assertFalse(self.gate((self.checked(),),
                    legacy=self.legacy(state, 1))["gate_pass"])

    def test_legacy_status_success_is_independent_and_valid(self):
        self.assertTrue(self.gate((self.checked(),),
            legacy=self.legacy("success", 1))["gate_pass"])

    def test_drift_and_partial_enumeration_fail(self):
        def fetch(page, per_page):
            return mod.Page(4, [self.checked(1)] if page == 1 else [])
        checks = mod.collect_check_runs(fetch)
        out = mod.evaluate_strict_gate(checks, self.legacy(),
                                       self.SHA, ("check-1",))
        self.assertFalse(out["gate_pass"])
        self.assertIn("CHECK_ENUMERATION_UNTRUSTED", out["reasons"])


if __name__ == "__main__":
    unittest.main()
