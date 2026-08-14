#!/usr/bin/env python3
"""Cross-check the five-repository RAFCODEPHI runtime stack fail-closed."""
from __future__ import annotations

import argparse
import importlib.util
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.termux.rafacodephi"
API_PACKAGE = f"{PACKAGE}.api"
PREFIX = f"/data/data/{PACKAGE}/files/usr"
RECEIVER = f"{API_PACKAGE}/com.termux.api.TermuxApiReceiver"
REPOSITORIES = {
    "app": "rafaelmeloreisnovo/termux-app-rafacodephi",
    "api": "rafaelmeloreisnovo/termux-api_rafcodephi",
    "packages": "rafaelmeloreisnovo/termux-packages",
    "polimata": "rafaelmeloreisnovo/RafPolimata",
}

LOCK_SPEC = importlib.util.spec_from_file_location(
    "runtime_lock_contract", ROOT / "scripts/runtime_lock_contract.py"
)
assert LOCK_SPEC is not None and LOCK_SPEC.loader is not None
LOCK_MODULE = importlib.util.module_from_spec(LOCK_SPEC)
LOCK_SPEC.loader.exec_module(LOCK_MODULE)


class StackError(RuntimeError):
    pass


def require(condition: bool, token: str) -> None:
    if not condition:
        raise StackError(token)


def read(root: Path, relative: str) -> str:
    path = root / relative
    require(path.is_file(), f"FILE_MISSING:{path}")
    return path.read_text(encoding="utf-8")


def git_head(root: Path) -> str:
    result = subprocess.run(
        ["git", "-C", str(root), "rev-parse", "HEAD"],
        text=True,
        capture_output=True,
        check=False,
    )
    require(result.returncode == 0, f"GIT_HEAD_UNAVAILABLE:{root}")
    return result.stdout.strip()


def validate_stack(
    lock_path: Path,
    app_root: Path,
    api_root: Path,
    packages_root: Path,
    polimata_root: Path,
    *,
    check_heads: bool = True,
) -> dict[str, object]:
    lock = LOCK_MODULE.load_lock(lock_path)
    mapped = LOCK_MODULE.validate(lock)
    roots = {
        "app": app_root,
        "api": api_root,
        "packages": packages_root,
        "polimata": polimata_root,
    }
    locked_commits = {role: mapped[name]["commit"] for role, name in REPOSITORIES.items()}
    if check_heads:
        for role, root in roots.items():
            require(git_head(root) == locked_commits[role], f"LOCKED_HEAD_MISMATCH:{role}")

    app_constants = read(
        app_root,
        "termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java",
    )
    app_importer = read(app_root, "scripts/import_rafcodephi_real_bootstrap.py")
    app_prepare = read(app_root, "scripts/prepare_bootstrap_env.sh")
    app_manifest = read(app_root, "app/src/main/AndroidManifest.xml")
    app_gradle = read(app_root, "app/build.gradle")
    require(f'TERMUX_PACKAGE_NAME = "{PACKAGE}"' in app_constants, "APP_PACKAGE_IDENTITY")
    require("TERMUX_API_PACKAGE_NAME = TERMUX_PACKAGE_NAME + \".api\"" in app_constants, "APP_API_IDENTITY")
    require(
        'TERMUX_API_CODE_PACKAGE_NAME = "com.termux.api"' in app_constants,
        "APP_API_CODE_IDENTITY",
    )
    require("android:sharedUserId" not in app_manifest, "APP_SHARED_USER_ID_FORBIDDEN")
    require(
        'android:name="${TERMUX_PACKAGE_NAME}.permission.TERMUX_API"' in app_manifest
        and 'android:protectionLevel="signature"' in app_manifest,
        "APP_API_SIGNATURE_PERMISSION",
    )
    require("RAFCODEPHI_PAIRED_KEYSTORE_FILE" in app_gradle, "APP_PAIRED_SIGNING_INTERFACE")
    for token in [
        '"arm": {"elf_class": 1, "machine": 40',
        '"aarch64": {"elf_class": 2, "machine": 183',
        "libexec/termux-api-broadcast",
        "termux-api client does not target the RAFCODEPHI API receiver",
        "RAFCODEPHI_PACKAGE_REPOSITORY_NOT_PUBLISHED",
        "custom-prefix apt repository is not deterministically disabled",
    ]:
        require(token in app_importer, f"APP_IMPORT_CONTRACT:{token}")
    require("RAF_REAL_BOOTSTRAP_ZIP_ARM" in app_prepare, "APP_ARM_IMPORT_INPUT")
    require("RAF_REAL_BOOTSTRAP_ZIP_AARCH64" in app_prepare, "APP_AARCH64_IMPORT_INPUT")

    api_gradle = read(api_root, "app/build.gradle")
    api_manifest = read(api_root, "app/src/main/AndroidManifest.xml")
    api_constants = read(api_root, "app/src/main/java/com/termux/api/TermuxAPIConstants.java")
    require("applicationId rafcodephiApiPackage" in api_gradle, "API_APPLICATION_ID")
    require(f'RAFCODEPHI_APP_PACKAGE_NAME") ?: "{PACKAGE}"' in api_gradle, "API_MAIN_PACKAGE")
    require("android:sharedUserId" not in api_manifest, "API_SHARED_USER_ID_FORBIDDEN")
    require(
        'android:exported="true"' in api_manifest
        and 'android:name="com.termux.api.TermuxApiReceiver"' in api_manifest
        and 'android:permission="${RAFCODEPHI_APP_PACKAGE}.permission.TERMUX_API"' in api_manifest,
        "API_SIGNATURE_PERMISSION_ROUTE",
    )
    require("TERMUX_API_CODE_PACKAGE_NAME" in api_constants, "API_RECEIVER_CODE_IDENTITY")
    require("RAFCODEPHI_PAIRED_KEYSTORE_FILE" in api_gradle, "API_PAIRED_SIGNING_INTERFACE")
    require('rafcodephiSharedMode == "maven-local"' in api_gradle, "API_LOCAL_SHARED_BUILD_ROUTE")
    shared_pin = re.search(r'RAFCODEPHI_TERMUX_SHARED_VERSION"\) \?: "([0-9a-f]{40})"', api_gradle)
    require(shared_pin is not None, "API_TERMUX_SHARED_PIN")
    require(shared_pin.group(1) == locked_commits["app"], "API_TERMUX_SHARED_PIN_DRIFT")

    client_patch = read(packages_root, "packages/termux-api/termux-api.c.patch")
    bootstrap_builder = read(packages_root, "scripts/build-rafcodephi-real-bootstrap.sh")
    require(f'+    child_argv[5] = "{RECEIVER}";' in client_patch, "CLI_RECEIVER_ROUTE")
    require("RAFCODEPHI_SHARED_UID_FILESYSTEM_SOCKETS" in client_patch, "CLI_ABSTRACT_SOCKET_ROUTE")
    require('+    child_argv[1] = "startservice";' not in client_patch, "CLI_SERVICE_STUB_ROUTE")
    for token in [
        "--add busybox,proot,ca-certificates,termux-api",
        "termux_api_cli=EMBEDDED",
        "libexec/termux-api-broadcast",
        "Enabled: no",
        "RAFCODEPHI_PACKAGE_REPOSITORY_NOT_PUBLISHED",
        "device_runtime_proof=TOKEN_VAZIO",
    ]:
        require(token in bootstrap_builder, f"PACKAGES_BOOTSTRAP_CONTRACT:{token}")

    evidence_adapter = read(polimata_root, "scripts/validate_rafcodephi_bootstrap_evidence.py")
    evidence_schema = read(polimata_root, "contracts/rafcodephi-bootstrap-evidence.v1.schema.json")
    for token in [
        PACKAGE,
        API_PACKAGE,
        RECEIVER,
        "SIGNATURE_PERMISSION_NO_SHARED_UID",
        "RAFCODEPHI_PACKAGE_REPOSITORY_NOT_PUBLISHED",
        "STRUCTURAL_PASS_LIMITED",
        "claim_allowed",
    ]:
        require(token in evidence_adapter or token in evidence_schema, f"EVIDENCE_ADAPTER_CONTRACT:{token}")

    return {
        "schema": "rafcodephi.runtime-stack/v1",
        "locked_commits": locked_commits,
        "contracts": {
            "app_identity": "PASS",
            "api_identity": "PASS",
            "cli_route": "PASS",
            "arm_pair": "PASS",
            "package_repo_guard": "PASS",
            "evidence_adapter": "PASS",
        },
        "runtime_boundaries": {
            "custom_binary_repository": "BLOCKED_CUSTOM_REPOSITORY_NOT_PUBLISHED",
            "arm32_device": "TOKEN_VAZIO",
            "arm64_device": "TOKEN_VAZIO",
            "paired_apk_install": "TOKEN_VAZIO",
            "termux_api_call": "TOKEN_VAZIO",
        },
        "structural_state": "PASS",
        "claim_allowed": False,
        "release_allowed": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    workspace = ROOT.parent
    parser.add_argument("--lock", type=Path, default=ROOT / "runtime-lock.json")
    parser.add_argument("--app", type=Path, default=workspace / "termux-app-rafacodephi")
    parser.add_argument("--api", type=Path, default=workspace / "termux-api_rafcodephi")
    parser.add_argument("--packages", type=Path, default=workspace / "termux-packages")
    parser.add_argument("--polimata", type=Path, default=workspace / "RafPolimata")
    parser.add_argument("--skip-head-check", action="store_true")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        report = validate_stack(
            args.lock,
            args.app,
            args.api,
            args.packages,
            args.polimata,
            check_heads=not args.skip_head_check,
        )
    except (OSError, LOCK_MODULE.ContractError, StackError) as exc:
        print(json.dumps({"ok": False, "state": "TOKEN_VAZIO", "error": str(exc)}, sort_keys=True))
        return 1
    payload = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload, encoding="utf-8")
    print(payload, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
