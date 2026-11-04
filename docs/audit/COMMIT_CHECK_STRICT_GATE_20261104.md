# Commit Checks strict gate — scoped, fail-closed (2026-11-04)

Authority: `rafaelmeloreisnovo/RafGitTools`, source `scripts/audit_commit_checks.py`; no branch-protection, release or other-repo mutation. This is a **candidate** until the exact PR HEAD CI has finished.

## Defect / reversible change

The historical Checks API enumerator accepted `total_count=0` as `COMPLETE_ALL_SUCCESS`. A green check list also did not prove that a required context existed or belonged to the requested commit. A green Checks API summary could hide a separate non-success legacy commit status.

The enumerator now labels a zero check set `NO_CHECKS_OBSERVED`, emits each check run's `head_sha`, and adds an explicit **opt-in** strict gate. Historical audit mode keeps its pre-existing exit behavior for compatibility; it still writes JSON even when checks fail. Strict mode fails closed unless all of the following are true:

1. Check pagination is complete/stable, there is at least one check, no check-run ID repeats across pages, and every observed check has `status=completed` and `conclusion=success`.
2. Every observed Checks API `head_sha` matches the specified SHA exactly; missing SHA is rejected.
3. The caller supplies at least one exact `--required-check`, and all named checks are present. A skipped, cancelled, timed-out, pending or failed check cannot count as success.
4. If legacy commit statuses exist, their **combined latest state** is `success`. If no legacy statuses exist, they do not fabricate a failure when Check Runs evidence is present.

```bash
python3 scripts/test_audit_commit_checks.py -v
python3 scripts/audit_commit_checks.py \
  --owner rafaelmeloreisnovo --repo RafGitTools \
  --sha "$EXACT_COMMIT_SHA" --output checks-receipt.json \
  --strict-gate --required-check '03 · Python / deterministic tests'
```

Exit code 4 means strict gate denied. Exit code 3 means snapshot incomplete or unstable in audit mode. Exit code 2 means API/read failure. Never treat a successful **audit enumeration** (exit 0 without `--strict-gate`) as CI approval.

## Evidence and limits

- Tests exercise: empty check sets, missing expected names, empty policy, head mismatch/absent, pending, skipped, cancelled, failed, legacy pending/failure, paging incompleteness, and a scoped success.
- Strict `PASS_OBSERVED_REQUIRED_CHECKS` proves only the observed check names and SHA. It does **not** validate trusted check-app identity, provider required-check rulesets, source licensing, uploaded artifacts, APK installed, real ARM hardware, device/VM boot, security posture or release readiness. `claim_allowed=false`, `release_allowed=false`.
- Actual GitHub remote test and independent review must be observed on the exact PR head before a merge decision. Provider protections remain independent gates.
- Rollback: close this draft PR. No main branch, protected settings or existing receipts are rewritten.

R3: `F_ok=source+tests proposed`; `F_gap=exact-head CI/provider+physical receipts unobserved`; `F_next=inspect terminal PR checks and review before further action`.
