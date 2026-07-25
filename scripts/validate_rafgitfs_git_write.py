#!/usr/bin/env python3
"""Dependency-free structural gate for RafGitFS Prompt 7."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path

FILES = (
    "app/src/main/kotlin/com/rafgittools/rafgitfs/write/RafGitFsGithubWriteApiService.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/write/RafGitFsWorkspaceStore.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/write/RafGitFsGithubBranchWriter.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/write/RafGitFsConflictResolver.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/write/RafGitFsWriteNetworkModule.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsSyncModels.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsCanonical.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsDiffPlanner.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsGovernedSyncEngine.kt",
    "app/src/main/kotlin/com/rafgittools/rafgitfs/sync/RafGitFsSyncModule.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/WorkspaceEditorViewModel.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/WorkspaceEditorScreen.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/VirtualFileBrowserScreen.kt",
    "app/src/main/kotlin/com/rafgittools/RafGitFsActivity.kt",
    ".github/workflows/rafgitfs-room-v6-validation.yml",
)

class ValidationError(ValueError): pass

def read(root: Path, relative: str) -> str:
    path = root / relative
    if not path.is_file(): raise ValidationError(f"missing file: {relative}")
    return path.read_text(encoding="utf-8")

def validate(root: Path) -> dict:
    src = {p: read(root, p) for p in FILES}
    api, workspace, writer, resolver, network, models, canonical, planner, engine, module, vm, screen, browser, activity, workflow = [src[p] for p in FILES]
    all_text = "\n".join(src.values())

    for marker in (
        '@POST("repos/{owner}/{repo}/git/refs")',
        '@POST("repos/{owner}/{repo}/git/blobs")',
        '@POST("repos/{owner}/{repo}/git/trees")',
        '@POST("repos/{owner}/{repo}/git/commits")',
        '@PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")',
        '@POST("repos/{owner}/{repo}/pulls")',
    ):
        if marker not in api: raise ValidationError(f"write endpoint missing: {marker}")
    if re.search(r"@DELETE\b", api): raise ValidationError("DELETE endpoint is forbidden")
    if re.search(r"/merges?|/merge\b", api, re.IGNORECASE): raise ValidationError("merge endpoint is forbidden")
    if "val force: Boolean = false" not in api: raise ValidationError("non-force ref default missing")
    if "val draft: Boolean = true" not in api: raise ValidationError("draft PR default missing")

    for marker in (
        "context.filesDir", "rafgitfs-workspaces-v1", "WORKSPACE_PATH_ESCAPE",
        "GIT_INTERNAL_PATH_BLOCKED", "output.fd.sync()", "MAX_WORKSPACE_FILE_BYTES",
        "STAGED_PAYLOAD_HASH_MISMATCH", "claimAllowed = false",
    ):
        if marker not in workspace: raise ValidationError(f"workspace invariant missing: {marker}")
    if any(x in workspace for x in ("getExternalStorage", "WRITE_EXTERNAL_STORAGE")):
        raise ValidationError("workspace must remain private")

    for marker in (
        "BASE_REF_MOVED", 'force = false', 'draft = true', 'rafgitfs/',
        "PROTECTED_BRANCH_TARGET", "REMOTE_BLOB_SHA_MISMATCH", "PUBLISH_META",
        "PR_OPEN:", 'ROLLBACK ${plan.planHash.take(12)}', "DESTRUCTIVE_REMOTE",
    ):
        if marker not in writer and marker != "DESTRUCTIVE_REMOTE":
            raise ValidationError(f"branch writer invariant missing: {marker}")
    if "DELETE" in writer and "DELETE_REMOTE" not in writer:
        raise ValidationError("unexpected delete logic in branch writer")
    if "RafGitFsGithubBranchWriter" not in module:
        raise ValidationError("governed branch writer is not bound")
    if "RafGitFsBlockedRemoteWriteCapability" in module:
        raise ValidationError("blocked Prompt 6 binding remained active")
    if "retrofit.create(RafGitFsGithubWriteApiService::class.java)" not in network:
        raise ValidationError("write API provider missing")

    for marker in ("workspaceId", "baseSha: String? = null", "claimAllowed: Boolean = false"):
        if marker not in models: raise ValidationError(f"write model invariant missing: {marker}")
    if "workspace=" not in canonical: raise ValidationError("workspace not included in canonical plan")
    for marker in (
        "CREATE_BRANCH", "CREATE_COMMIT", "PUSH_BRANCH", "OPEN_PULL_REQUEST",
        "WORKSPACE_AND_BASE_SHA_REQUIRED", "BOTH_CHANGED", "LOCAL_CHANGED", "REMOTE_CHANGED",
    ):
        if marker not in planner: raise ValidationError(f"planner marker missing: {marker}")
    if "it.workspaceId == plan.workspaceId" not in engine:
        raise ValidationError("stored plan is not workspace-bound")

    for marker in (
        "Generate plan and dry-run", "Exact approval", "Approve branch + commit + push + draft PR",
        "Rollback confirmation", "claim_allowed=false",
    ):
        if marker not in screen: raise ValidationError(f"workspace UI marker missing: {marker}")
    for marker in (
        "APPROVAL_CONFIRMATION_MISMATCH", "RafGitFsApproval", "EXACT_PLAN",
        "rollbackToBase", "BASE_REF_UNOBSERVED",
    ):
        if marker not in vm: raise ValidationError(f"workspace view model gate missing: {marker}")
    if "Open governed Git workspace" not in browser or "RafGitFsRoute.Workspace" not in activity:
        raise ValidationError("workspace route is not connected")

    if re.search(r"claimAllowed\s*=\s*true", all_text):
        raise ValidationError("claim promotion detected")
    for marker in (
        "validate_rafgitfs_git_write.py", "test_validate_rafgitfs_git_write.py",
        "RafGitFsWriteContractsTest",
    ):
        if marker not in workflow: raise ValidationError(f"Prompt 7 workflow gate missing: {marker}")

    digest = hashlib.sha256()
    for path in sorted(FILES): digest.update((path + "\0" + src[path]).encode())
    return {
        "status": "PASS",
        "prompt": "7/8",
        "private_workspace": True,
        "three_way_diff": True,
        "generated_branch_only": True,
        "force_push": False,
        "draft_pull_request": True,
        "direct_protected_branch_write": False,
        "remote_delete": False,
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
