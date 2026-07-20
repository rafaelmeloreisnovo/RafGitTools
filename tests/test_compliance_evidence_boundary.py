from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "compliance_boundary", ROOT / "scripts" / "validate_compliance_evidence_boundary.py")
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(module)


def fixture():
    return module.load(ROOT)


class ComplianceEvidenceBoundaryTests(unittest.TestCase):

    def assert_invalid(self, files, phrase):
        errors = module.validate_sources(files)
        self.assertTrue(errors)
        self.assertIn(phrase, "\n".join(errors))

    def test_repository_boundary_passes(self):
        self.assertEqual([], module.validate_sources(fixture()))

    def test_non_null_assertion_is_rejected(self):
        files = fixture()
        files["compliance"] += "\ngetComplianceStatus()[standard]!!\n"
        self.assert_invalid(files, "forbidden unsupported assertion")

    def test_hardcoded_implemented_true_is_rejected(self):
        files = fixture()
        files["compliance"] += "\nimplemented = true\n"
        self.assert_invalid(files, "implemented = true")

    def test_hardcoded_ieee_boolean_is_rejected(self):
        files = fixture()
        files["compliance"] += "\nval hasQAProcess = true\n"
        self.assert_invalid(files, "hasQAProcess")

    def test_missing_not_assessed_is_rejected(self):
        files = fixture()
        files["compliance"] = files["compliance"].replace(
            "ComplianceLevel.NOT_ASSESSED", "ComplianceLevel.NON_COMPLIANT")
        self.assert_invalid(files, "NOT_ASSESSED")

    def test_missing_token_vazio_is_rejected(self):
        files = fixture()
        files["compliance"] = files["compliance"].replace(
            "AssessmentState.TOKEN_VAZIO", "AssessmentState.OBSERVED")
        self.assert_invalid(files, "TOKEN_VAZIO")

    def test_missing_evidence_refs_is_rejected(self):
        files = fixture()
        files["compliance"] = files["compliance"].replace(
            "evidenceRefs.isNotEmpty()", "true")
        self.assert_invalid(files, "evidenceRefs.isNotEmpty")

    def test_current_time_cannot_be_used_as_fake_audit_date(self):
        files = fixture()
        files["compliance"] = files["compliance"].replace(
            "lastAuditDate = Date(0)", "lastAuditDate = Date()")
        self.assert_invalid(files, "audit date")

    def test_report_claim_boundary_is_required(self):
        files = fixture()
        files["compliance"] = files["compliance"].replace(
            "val claimAllowed: Boolean = false", "val claimAllowed: Boolean = true")
        self.assert_invalid(files, "claim boundary")

    def test_cpp_only_flag_is_rejected_from_cflags(self):
        files = fixture()
        files["makefile"] = files["makefile"].replace(
            "-fno-builtin", "-fno-builtin -fno-exceptions")
        self.assert_invalid(files, "C++-only")

    def test_link_library_is_rejected_from_cflags(self):
        files = fixture()
        files["makefile"] = files["makefile"].replace(
            "-fno-builtin", "-fno-builtin -lm")
        self.assert_invalid(files, "compile flags")

    def test_make_smoke_gate_is_required(self):
        files = fixture()
        files["makefile"] = files["makefile"].replace("check: all", "unchecked: all")
        self.assert_invalid(files, "check: all")


if __name__ == "__main__":
    unittest.main()
