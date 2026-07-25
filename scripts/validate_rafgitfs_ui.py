#!/usr/bin/env python3
"""Dependency-free structural gate for RafGitFS Prompt 4."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

FILES = (
    "app/src/main/kotlin/com/rafgittools/RafGitFsActivity.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/RafGitFsUiModels.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/RafGitFsComponents.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/StorageProfilesViewModel.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/StorageProfilesScreen.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/RepositoryStorageViewModel.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/RepositoryStorageScreen.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/VirtualFileBrowserViewModel.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/VirtualFileBrowserScreen.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/VirtualFileViewerViewModel.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/VirtualFileViewerScreen.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/StorageSettingsViewModel.kt",
    "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/StorageSettingsScreen.kt",
    "app/src/main/AndroidManifest.xml",
    ".github/workflows/rafgitfs-room-v6-validation.yml",
)

EXPECTED_SCREENS = {
    "StorageProfilesScreen",
    "RepositoryStorageScreen",
    "VirtualFileBrowserScreen",
    "VirtualFileViewerScreen",
    "StorageSettingsScreen",
}
EXPECTED_VIEW_MODELS = {
    "StorageProfilesViewModel",
    "RepositoryStorageViewModel",
    "VirtualFileBrowserViewModel",
    "VirtualFileViewerViewModel",
    "StorageSettingsViewModel",
}
FORBIDDEN_MUTATIONS = re.compile(
    r"@(POST|PUT|PATCH|DELETE)|createPullRequest\s*\(|\bpush\s*\(|\bcommit\s*\(|"
    r"deleteRepository\s*\(|openPullRequest\s*\(|stageFiles\s*\(",
    re.IGNORECASE,
)


class ValidationError(ValueError):
    pass


def read(root: Path, relative: str) -> str:
    path = root / relative
    if not path.is_file():
        raise ValidationError(f"missing file: {relative}")
    return path.read_text(encoding="utf-8")


def digest(root: Path) -> str:
    hasher = hashlib.sha256()
    for relative in sorted(FILES):
        hasher.update(relative.encode("utf-8"))
        hasher.update(b"\0")
        hasher.update((root / relative).read_bytes())
        hasher.update(b"\0")
    return hasher.hexdigest()


def validate(root: Path) -> dict[str, Any]:
    source = {path: read(root, path) for path in FILES}
    activity = source[FILES[0]]
    models = source[FILES[1]]
    components = source[FILES[2]]
    profile_vm = source[FILES[3]]
    browser_vm = source[FILES[7]]
    browser_screen = source[FILES[8]]
    viewer_vm = source[FILES[9]]
    settings_vm = source[FILES[11]]
    manifest = source[FILES[13]]
    workflow = source[FILES[14]]
    ui_sources = "\n".join(source[path] for path in FILES[:13])

    for screen in EXPECTED_SCREENS:
        if f"fun {screen}" not in ui_sources:
            raise ValidationError(f"screen missing: {screen}")
    for view_model in EXPECTED_VIEW_MODELS:
        if f"class {view_model}" not in ui_sources or "@HiltViewModel" not in ui_sources:
            raise ValidationError(f"Hilt view model missing: {view_model}")

    for state in (
        "OBSERVED", "NOT_MODIFIED", "TOKEN_VAZIO", "RATE_LIMITED", "ERROR"
    ):
        if state not in models:
            raise ValidationError(f"evidence UI state missing: {state}")

    for marker in (
        "RafGitFsReadOnlyBadge", "RafGitFsStatusBanner", "RafGitFsBreadcrumbBar"
    ):
        if marker not in components:
            raise ValidationError(f"shared UI component missing: {marker}")

    for route in (
        "RafGitFsRoute.Profiles", "RafGitFsRoute.Repositories", "RafGitFsRoute.Browser",
        "RafGitFsRoute.Viewer", "RafGitFsRoute.Settings",
    ):
        if route not in activity:
            raise ValidationError(f"navigation route missing: {route}")
    if "RafGitFsActivity" not in manifest or 'android:host="storage"' not in manifest:
        raise ValidationError("RafGitFS activity/deep link is not registered")
    if 'android:exported="true"' not in manifest:
        raise ValidationError("RafGitFS launcher must declare exported explicitly")

    if FORBIDDEN_MUTATIONS.search(ui_sources):
        raise ValidationError("Prompt 4 UI contains a remote mutation capability")
    if re.search(r"claimAllowed\s*=\s*true", ui_sources):
        raise ValidationError("claim promotion must remain blocked")
    for marker in (
        'accessMode = "READ_ONLY"', 'writePolicy = "BLOCKED"',
        "receiptRequired = true", "protectedBranchWrite = false",
        "deleteEnabled = false", "claimAllowed = false",
    ):
        if marker not in profile_vm or marker not in settings_vm:
            raise ValidationError(f"profile safety invariant missing: {marker}")

    for marker in (
        "observeChildren", "refreshRefs", "refreshTree", "setFavorite",
        "RafGitFsBreadcrumbBar", "Search this folder",
    ):
        if marker not in browser_vm + browser_screen:
            raise ValidationError(f"browser capability missing: {marker}")
    if "readContent" not in viewer_vm or "VirtualFileViewerUiState" not in viewer_vm:
        raise ValidationError("bounded read-only viewer connection missing")
    if "OutlinedTextField" not in source[FILES[6]] or "OutlinedTextField" not in source[FILES[12]]:
        raise ValidationError("repository search or settings input missing")

    for marker in (
        "validate_rafgitfs_ui.py", "test_validate_rafgitfs_ui.py",
        "RafGitFsUiPathsTest",
    ):
        if marker not in workflow:
            raise ValidationError(f"Prompt 4 workflow gate missing: {marker}")

    return {
        "status": "PASS",
        "prompt": "4/8",
        "screens": len(EXPECTED_SCREENS),
        "view_models": len(EXPECTED_VIEW_MODELS),
        "evidence_states_visible": True,
        "breadcrumbs": True,
        "favorites": True,
        "read_only_viewer": True,
        "remote_write_enabled": False,
        "claim_allowed": False,
        "sha256": digest(root),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--write-report", type=Path)
    args = parser.parse_args()
    try:
        report = validate(args.repo_root.resolve())
    except (OSError, ValidationError) as error:
        print(json.dumps({"status": "FAIL", "error": str(error)}, indent=2, ensure_ascii=False))
        return 1
    output = json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False)
    print(output)
    if args.write_report:
        args.write_report.parent.mkdir(parents=True, exist_ok=True)
        args.write_report.write_text(output + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
