#!/usr/bin/env python3
"""Offline synthetic-only parser failures; no database or credentials required."""
import importlib.util
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/ci/check_drive_capture_gate_matrix_v2.py"
spec = importlib.util.spec_from_file_location("drive_pg_matrix", SCRIPT)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def clean_lines():
    cases = sorted(m.EXPECTED_CASES)
    return ["|".join([name, "t" if name == "valid" else "f",
                       "t" if name == "valid" else "f", "t",
                       "t", "0", "0", "t"]) for name in cases]


class MatrixParserContract(unittest.TestCase):
    def test_valid_matrix(self):
        report = m.verify_output("\n".join(clean_lines()))
        self.assertEqual(report["matrix_cases"], 15)
        self.assertEqual(report["negative_rejected"], 14)
        self.assertFalse(report["claim_allowed"])

    def test_empty(self):
        with self.assertRaisesRegex(m.MatrixGateError, "MISSING_CASE"):
            m.verify_output("")

    def test_missing_case(self):
        with self.assertRaisesRegex(m.MatrixGateError, "MISSING_CASE"):
            m.verify_output("\n".join(clean_lines()[:-1]))

    def test_duplicate_case(self):
        lines = clean_lines()
        with self.assertRaisesRegex(m.MatrixGateError, "DUPLICATE_CASE"):
            m.verify_output("\n".join(lines + lines[:1]))

    def test_unknown_case(self):
        lines = clean_lines()
        lines[0] = lines[0].replace(lines[0].split("|")[0], "unknown", 1)
        with self.assertRaisesRegex(m.MatrixGateError, "UNKNOWN"):
            m.verify_output("\n".join(lines))

    def test_negative_became_positive(self):
        lines = clean_lines()
        lines[0] = lines[0].replace("|f|f|", "|f|t|", 1)
        with self.assertRaisesRegex(m.MatrixGateError, "FALSIFIER_NOT_REJECTED"):
            m.verify_output("\n".join(lines))

    def test_control_became_negative(self):
        lines = clean_lines()
        ix = next(i for i, line in enumerate(lines) if line.startswith("valid|"))
        lines[ix] = lines[ix].replace("valid|t|t|", "valid|t|f|", 1)
        with self.assertRaisesRegex(m.MatrixGateError, "CONTROL_FAILED"):
            m.verify_output("\n".join(lines))

    def test_global_gate_false(self):
        lines = clean_lines()
        lines[-1] = lines[-1][:-1] + "f"
        with self.assertRaisesRegex(m.MatrixGateError, "FALSIFIER_NOT_REJECTED"):
            m.verify_output("\n".join(lines))

    def test_status_false(self):
        lines = clean_lines()
        lines[-1] = lines[-1].replace("|t|t|0|0|t", "|f|t|0|0|t")
        with self.assertRaisesRegex(m.MatrixGateError, "FALSIFIER_NOT_REJECTED"):
            m.verify_output("\n".join(lines))

    def test_extra_warning(self):
        with self.assertRaisesRegex(m.MatrixGateError, "UNEXPECTED_PSQL_ROW"):
            m.verify_output("WARNING: synthetic diagnostic\n" + "\n".join(clean_lines()))

    def test_truncated_row(self):
        lines = clean_lines()
        lines[0] = lines[0].split("|")[0]
        with self.assertRaisesRegex(m.MatrixGateError, "UNEXPECTED_PSQL_ROW"):
            m.verify_output("\n".join(lines))


if __name__ == "__main__":
    unittest.main()
