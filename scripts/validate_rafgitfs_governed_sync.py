#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

FILES = (
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsSyncModels.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsCanonical.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsDiffPlanner.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsReceiptFactory.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsStepExecutor.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsGovernedSyncEngine.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsSyncModule.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/data/RafGitFsOperationDaos.kt",
    ".github/workflows/rafgitfs-room-v6-validation.yml",
)
PHASES = ("SCAN", "DIFF", "PLAN", "DRY_RUN", "APPROVE", "EXECUTE", "RECEIPT")

class ValidationError(ValueError): pass

def read(root: Path, path: str) -> str:
    target = root / path
    if not target.is_file(): raise ValidationError(f"missing file: {path}")
    return target.read_text(encoding="utf-8")

def validate(root: Path) -> dict:
    src = {p: read(root, p) for p in FILES}
    models, canonical, planner, receipt, executor, engine, module, dao, workflow = [src[p] for p in FILES]
    for phase in PHASES:
        if phase not in models: raise ValidationError(f"missing phase {phase}")
    for marker in ("planHash", "requiresApproval", "claimAllowed: Boolean = false", "PROTECTED_BRANCH_WRITE", "DESTRUCTIVE_REMOTE"):
        if marker not in models: raise ValidationError(f"model invariant missing: {marker}")
    for marker in ("SHA-256", "REDACTED", "MessageDigest", "MAX_EVENTS"):
        if marker not in canonical: raise ValidationError(f"canonical/log invariant missing: {marker}")
    for marker in ("TOKEN_VAZIO_CAPABILITY_PROMPT_7", "DESTRUCTIVE_REMOTE_PERMANENTLY_BLOCKED"):
        if marker not in executor: raise ValidationError(f"executor block missing: {marker}")
    for marker in ("validateApproval", "PLAN_HASH_MISMATCH", "APPROVAL_REQUIRED", "UNRESOLVED_CONFLICTS", "RETRY_LIMIT_REACHED", "receiptDao.append"):
        if marker not in engine: raise ValidationError(f"engine gate missing: {marker}")
    if "confirmation == \"APPROVE ${plan.planHash.take(12)}\"" not in engine:
        raise ValidationError("exact-plan confirmation missing")
    for marker in ("compareAndSetState", "pause", "cancel", "syncState NOT IN ('CANCELLED','COMPLETE')"):
        if marker not in dao: raise ValidationError(f"DAO transition missing: {marker}")
    if "@Insert(onConflict = OnConflictStrategy.ABORT)" not in dao:
        raise ValidationError("append-only receipt insert missing")
    if re.search(r"claimAllowed\s*=\s*true", "\n".join(src.values())):
        raise ValidationError("claim promotion detected")
    if "RafGitFsBlockedRemoteWriteCapability" not in module:
        raise ValidationError("Prompt 6 must bind blocked remote writer")
    for marker in ("validate_rafgitfs_governed_sync.py", "test_validate_rafgitfs_governed_sync.py", "RafGitFsGovernedSyncTest"):
        if marker not in workflow: raise ValidationError(f"workflow gate missing: {marker}")
    digest = hashlib.sha256()
    for p in sorted(FILES): digest.update((p + "\0" + src[p]).encode())
    return {
        "status":"PASS", "prompt":"6/8", "phases":list(PHASES),
        "persistent_jobs":True, "immutable_receipts":True, "sanitized_logs":True,
        "remote_write_enabled":False, "claim_allowed":False,
        "sha256":digest.hexdigest(),
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
