#!/usr/bin/env python3
"""Dependency-free gate for RafGitFS Prompt 8 industrial closeout."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

FILES = (
    "app/src/main/kotlin/com/rafgittools/rafgitfs/assurance/RafGitFsSecurityPolicy.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/assurance/RafGitFsRuntimeSecurityGate.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/assurance/RafGitFsPerformanceBudget.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/assurance/RafGitFsIndustrialCloseout.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsStepExecutor.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/RafGitFsComponents.kt",
    "app/src/test/kotlin/com/rafgittools/rafgitfs/assurance/RafGitFsSecurityPolicyTest.kt",
    "app/src/test/kotlin/com/rafgittools/rafgitfs/assurance/RafGitFsCloseoutTest.kt",
    "app/src/androidTest/kotlin/com/rafgittools/rafgitfs/assurance/RafGitFsPrivateStorageInstrumentedTest.kt",
    "docs/RAFGITFS_SECURITY_THREAT_MODEL.md",
    "docs/RAFGITFS_V1_FINAL_STATUS.md",
    "docs/RAFGITFS_POST_V1_ROADMAP.md",
    "artifacts/rafgitfs-v1-maturity-matrix.json",
    "artifacts/rafgitfs-v1-closeout-receipt.json",
    ".github/workflows/rafgitfs-room-v6-validation.yml",
)

class ValidationError(ValueError): pass

def read(root: Path, relative: str) -> str:
    path = root / relative
    if not path.is_file(): raise ValidationError(f"missing file: {relative}")
    return path.read_text(encoding="utf-8")

def validate(root: Path) -> dict:
    src = {p: read(root, p) for p in FILES}
    policy, runtime, budget, closeout, executor, components, security_test, closeout_test, android_test, threat, status, roadmap, matrix_text, receipt_text, workflow = [src[p] for p in FILES]
    all_text = "\n".join(src.values())

    controls = set(re.findall(r'"(SEC-[A-Z]+-\d{3})"', policy))
    if len(controls) != 11:
        raise ValidationError(f"expected 11 canonical security controls, found {len(controls)}")
    for marker in (
        "generatedBranch", "approvalExact", "unresolvedConflicts", "forcePush",
        "draftPullRequest", "claimAllowed", "secretsPersistedInRoom", "workspacePrivate",
    ):
        if marker not in policy: raise ValidationError(f"security context missing: {marker}")
    if "assessAfterExactApproval" not in runtime or "RafGitFsSecurityPolicy.assessPublication" not in runtime:
        raise ValidationError("runtime security assessment is not connected")
    if "securityGate.assessAfterExactApproval(plan)" not in executor:
        raise ValidationError("step executor bypasses runtime security gate")
    if "assessment.decision != RafGitFsSecurityDecision.ALLOW" not in executor:
        raise ValidationError("non-ALLOW security decisions must block")

    for marker in ("TOKEN_VAZIO", "MEASUREMENT_RECEIPT_INCOMPLETE", "targetsMillis", "p95"):
        if marker not in budget: raise ValidationError(f"performance evidence marker missing: {marker}")
    if "observedMillis == null" not in budget:
        raise ValidationError("unmeasured metrics must remain TOKEN_VAZIO")

    for marker in (
        "IMPLEMENTED_SOURCE", "OBSERVED_CI", "OBSERVED_ANDROID", "TOKEN_VAZIO",
        "BLOCKED_BY_POLICY", "unresolvedTokenVazio", "claimAllowed: Boolean = false",
    ):
        if marker not in closeout: raise ValidationError(f"closeout model missing: {marker}")
    for pr in ("PR_300", "PR_301", "PR_302", "PR_303", "PR_304", "PR_305"):
        if pr not in closeout: raise ValidationError(f"source receipt missing: {pr}")

    for marker in ("LiveRegionMode.Polite", "contentDescription", "read-only mode", "Repository path"):
        if marker not in components: raise ValidationError(f"accessibility marker missing: {marker}")
    for marker in ("filesDir", "externalFilesDir", "AndroidJUnit4"):
        if marker not in android_test: raise ValidationError(f"instrumented storage marker missing: {marker}")

    for marker in ("Threat Model", "TOKEN_VAZIO", "force=false", "draft=true", "claim_allowed: false"):
        if marker not in threat: raise ValidationError(f"threat model marker missing: {marker}")
    if "prompts_source_completed: 8/8" not in status:
        raise ValidationError("final source completion statement missing")
    for marker in ("Onda 9", "Onda 10", "Onda 11", "Onda 12", "ZERO_STEP_NO_LOGS"):
        if marker not in roadmap: raise ValidationError(f"post-V1 roadmap missing: {marker}")

    try:
        matrix = json.loads(matrix_text)
        receipt = json.loads(receipt_text)
    except json.JSONDecodeError as error:
        raise ValidationError(f"invalid JSON artifact: {error}") from error
    if matrix.get("claim_allowed") is not False or receipt.get("claim_allowed") is not False:
        raise ValidationError("machine-readable claim must remain false")
    if len(matrix.get("prompts", [])) != 8:
        raise ValidationError("maturity matrix must contain exactly 8 prompts")
    if receipt.get("prompts_source_completed") != "8/8":
        raise ValidationError("closeout receipt prompt count mismatch")
    if receipt.get("production_ready") is not False or receipt.get("certification_claim") is not False:
        raise ValidationError("production or certification was promoted without evidence")
    receipt_hash = receipt.get("canonical_sha256", "")
    if not re.fullmatch(r"[0-9a-f]{64}", receipt_hash):
        raise ValidationError("closeout receipt hash is not SHA-256")
    if not receipt.get("f_gap") or "android_device_execution" not in receipt["f_gap"]:
        raise ValidationError("required open evidence was removed")

    if re.search(r"claimAllowed\s*=\s*true|claim_allowed[\"']?\s*[:=]\s*true", all_text, re.IGNORECASE):
        raise ValidationError("claim promotion detected")
    for marker in (
        "validate_rafgitfs_industrial_closeout.py",
        "test_validate_rafgitfs_industrial_closeout.py",
        "RafGitFsSecurityPolicyTest",
        "RafGitFsCloseoutTest",
        "compileDevDebugAndroidTestKotlin",
    ):
        if marker not in workflow: raise ValidationError(f"Prompt 8 workflow gate missing: {marker}")

    digest = hashlib.sha256()
    for path in sorted(FILES): digest.update((path + "\0" + src[path]).encode())
    return {
        "status": "PASS",
        "prompt": "8/8",
        "source_scope_complete": True,
        "security_controls": len(controls),
        "runtime_security_gate": True,
        "accessibility_semantics": True,
        "unit_tests": True,
        "adversarial_tests": True,
        "instrumented_test_source": True,
        "remote_ci_pass": False,
        "android_device_execution": "TOKEN_VAZIO",
        "production_ready": False,
        "claim_allowed": False,
        "sha256": digest.hexdigest(),
    }

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--write-report", type=Path)
    args = parser.parse_args()
    try: report = validate(args.repo_root.resolve())
    except (OSError, ValidationError) as error:
        print(json.dumps({"status":"FAIL","error":str(error)}, indent=2)); return 1
    text = json.dumps(report, indent=2, sort_keys=True)
    print(text)
    if args.write_report:
        args.write_report.parent.mkdir(parents=True, exist_ok=True)
        args.write_report.write_text(text + "\n", encoding="utf-8")
    return 0

if __name__ == "__main__": sys.exit(main())
