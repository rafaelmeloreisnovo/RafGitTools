#!/usr/bin/env python3
"""Bounded public-safe PostgreSQL matrix gate: exact fixture names, no secrets.

The SQL fixture is SELECT-only. This adapter turns its textual result into a
nonzero exit status. A successful psql exit alone is NOT proof of expectation.
It does not read Drive, modify the database or claim provider snapshot isolation.
"""
import argparse
from pathlib import Path
import subprocess
import sys

EXPECTED_CASES = frozenset({
    "valid", "page_gap", "missing_page_zero", "broken_cursor",
    "early_terminal", "two_terminal_pages", "not_terminal",
    "terminal_cursor_mismatch", "null_digest", "wrong_digest", "claim_true",
    "provider_drift", "account_drift", "query_contract_drift",
    "duplicate_page_index",
})


class MatrixGateError(ValueError):
    pass


def verify_output(output: str) -> dict:
    """Accept only the exact 15 rows from the declared synthetic matrix."""
    seen = set()
    accepted = set()
    for raw in output.splitlines():
        if not raw.strip():
            continue
        fields = raw.split("|")
        if len(fields) != 8:
            raise MatrixGateError("UNEXPECTED_PSQL_ROW_OR_OUTPUT")
        (case_name, expected_accept, strict_accept, expectation_met,
         _legacy_terminal, _legacy_hash_failures, _legacy_cursor_breaks,
         entire_matrix_pass) = fields
        if (_legacy_terminal not in ("t", "f")
            or (_legacy_hash_failures and not _legacy_hash_failures.isdecimal())
            or (_legacy_cursor_breaks and not _legacy_cursor_breaks.isdecimal())):
            raise MatrixGateError("LEGACY_FIELD_MALFORMED")
        if case_name not in EXPECTED_CASES or case_name in seen:
            raise MatrixGateError("UNKNOWN_OR_DUPLICATE_CASE")
        seen.add(case_name)
        expected = case_name == "valid"
        if (expected_accept != ("t" if expected else "f")
            or strict_accept != ("t" if expected else "f")
            or expectation_met != "t" or entire_matrix_pass != "t"):
            raise MatrixGateError("FALSIFIER_NOT_REJECTED_OR_CONTROL_FAILED")
        if strict_accept == "t":
            accepted.add(case_name)
    if seen != EXPECTED_CASES or accepted != {"valid"}:
        raise MatrixGateError("MISSING_CASE_OR_INVALID_ACCEPTANCE_SET")
    return {"matrix_cases": len(seen), "negative_rejected": 14,
            "valid_accepted": 1, "claim_allowed": False,
            "scope": "SYNTHETIC_PG_MATRIX_ONLY"}


def run_psql(sql_path: Path) -> str:
    if not sql_path.is_file():
        raise MatrixGateError("SQL_FIXTURE_MISSING")
    args = ["psql", "-X", "-q", "-A", "-t", "-F", "|",
            "-v", "ON_ERROR_STOP=1", "-c", "BEGIN READ ONLY;",
            "-f", str(sql_path), "-c", "ROLLBACK;"]
    try:
        proc = subprocess.run(args, capture_output=True, text=True,
                              timeout=60, check=False)
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise MatrixGateError("PSQL_UNAVAILABLE_OR_TIMEOUT") from exc
    if proc.returncode != 0:
        # Never emit stderr: it can contain private connection details.
        raise MatrixGateError("POSTGRES_QUERY_FAILED")
    if len(proc.stdout) > 65536:
        raise MatrixGateError("UNBOUNDED_PSQL_OUTPUT")
    return proc.stdout


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--psql", action="store_true",
                        help="Execute the synthetic SQL fixture through psql")
    opts = parser.parse_args(argv)
    try:
        # Exact repo-owned fixture, never an arbitrary SQL path on private DB.
        fixture = (Path(__file__).resolve().parents[2] / "docs/supabase" /
                   "test_drive_folder_capture_scoped_gate_v2_synthetic.sql")
        raw = run_psql(fixture) if opts.psql else sys.stdin.read()
        result = verify_output(raw)
    except MatrixGateError as exc:
        print(f"FAIL_SCOPED_MATRIX:{exc}", file=sys.stderr)
        return 2
    print(f"PASS_SCOPED_SYNTHETIC:{result['matrix_cases']}/15;"
          f"negative={result['negative_rejected']};claim_allowed=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
