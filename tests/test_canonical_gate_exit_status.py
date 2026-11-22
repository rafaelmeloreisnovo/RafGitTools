"""Execute the real POSIX wrapper with synthetic validator processes.

These tests prove error propagation, not the mocked validators' correctness.
"""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
REPORTS = {
    "scripts/check_gap_closure_ledger.py": "gap-closure-execution-report.json",
    "scripts/check_agent_entry_kernel.py": "agent-entry-kernel-report.json",
    "scripts/validate_knowledge_work_seven_guards.py": "knowledge-work-seven-guards-report.json",
    "scripts/check_program_mission_source_cohesion.py": "program-mission-source-cohesion-report.json",
    "scripts/validate_main_provider_enforcement_plan_v3.py": "main-provider-enforcement-plan-v3-report.json",
}
LAST = "scripts/validate_rafgitfs_cache_offline.py"


class CanonicalGateExitStatus(unittest.TestCase):
    def run_gate(self, fail_target="", fail_code=23, bad_report=False):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            (root / "scripts").mkdir()
            (root / "artifacts").mkdir()
            script = root / "scripts/validate_rafaelia_workflow.sh"
            script.write_bytes((ROOT / "scripts/validate_rafaelia_workflow.sh").read_bytes())
            bindir = root / "bin"
            bindir.mkdir()
            # No repository validator, build, Android tool or network is executed.
            stub = '''#!/bin/sh
printf '%s\n' "$*" >> "$TRACE"
if [ "$1" = "$FAIL_TARGET" ]; then
    printf '{"synthetic_failure":true}\n'
    printf 'synthetic validator stderr\n' >&2
    exit "$FAIL_CODE"
fi
printf '{"synthetic_success":true}\n'
'''
            for tool in ("python3", "make", "bash"):
                path = bindir / tool
                path.write_text(stub)
                path.chmod(0o755)
            if bad_report:
                (root / "artifacts" / next(iter(REPORTS.values()))).mkdir()
            env = dict(os.environ, PATH=str(bindir) + os.pathsep + os.environ["PATH"],
                       TRACE=str(root / "trace"), FAIL_TARGET=fail_target,
                       FAIL_CODE=str(fail_code))
            result = subprocess.run(["/bin/sh", str(script)], env=env,
                                    capture_output=True, text=True, timeout=15)
            trace = (root / "trace").read_text()
            reports = {name: p.read_text() for name in REPORTS.values()
                       if (p := root / "artifacts" / name).is_file()}
            return result, trace, reports

    def test_each_reported_validator_failure_stops_real_wrapper(self):
        for target, report in REPORTS.items():
            with self.subTest(target=target):
                result, trace, reports = self.run_gate(target)
                self.assertEqual(result.returncode, 23, result.stderr)
                self.assertIn(target, trace)
                self.assertNotIn(LAST, trace)
                self.assertIn('"synthetic_failure":true', reports[report])
                self.assertIn('"synthetic_failure":true', result.stdout)
                self.assertIn("synthetic validator stderr", result.stderr)

    def test_success_retains_all_reports_and_reaches_last_validator(self):
        result, trace, reports = self.run_gate()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(LAST, trace)
        self.assertEqual(len(reports), 5)

    def test_preserves_failure_codes_including_missing_tool_and_signal_status(self):
        for code in (1, 64, 126, 127, 143):
            with self.subTest(code=code):
                result, trace, _ = self.run_gate(next(iter(REPORTS)), code)
                self.assertEqual(result.returncode, code)
                self.assertNotIn(LAST, trace)

    def test_unreportable_output_fails_before_validator(self):
        result, trace, _ = self.run_gate(bad_report=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn(next(iter(REPORTS)), trace)
        self.assertNotIn(LAST, trace)


if __name__ == "__main__":
    unittest.main()
