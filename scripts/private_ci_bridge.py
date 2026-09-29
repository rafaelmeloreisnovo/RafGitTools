#!/usr/bin/env python3
"""Bounded replay of private-repository CI from the public RafGitTools control plane.

The bridge deliberately separates provider authentication from execution:
the GitHub PAT is used only by the workflow checkout/access steps. This module
must execute with no PAT/token secret in its environment. It never prints raw
private stdout/stderr and emits hash-only public evidence.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import platform
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

REGISTRY_SCHEMA = "rafgittools.private-ci-execution-registry.v1"
MANIFEST_SCHEMA = "rafaelia.private-ci-manifest.v1"
RECEIPT_SCHEMA = "rafgittools.private-ci-replay-receipt.v1"

SHA40_RE = re.compile(r"^[0-9a-f]{40}$")
REPO_RE = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
ID_RE = re.compile(r"^[a-z0-9][a-z0-9_.-]{0,127}$")
ENV_RE = re.compile(r"^[A-Z_][A-Z0-9_]*$")
FORBIDDEN_SECRET_NAMES = {
    "PAT_ACTIONS",
    "PROVIDER_ACTIONS_TOKEN",
    "GITHUB_TOKEN",
    "GH_TOKEN",
}
FORBIDDEN_KEY_FRAGMENTS = (
    "secret_value",
    "token_value",
    "credential_value",
    "private_key",
    "password",
)


def canonical_bytes(obj: Any) -> bytes:
    return (json.dumps(obj, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")


def load_json(path: Path) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"{path}: root must be an object")
    return data


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for block in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def git_blob_sha1_file(path: Path) -> str:
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\x00"
    return hashlib.sha1(header + data).hexdigest()


def _assert_no_secret_values(obj: Any, ctx: str = "root") -> None:
    if isinstance(obj, dict):
        for key, value in obj.items():
            key_norm = str(key).casefold()
            if any(fragment in key_norm for fragment in FORBIDDEN_KEY_FRAGMENTS):
                raise ValueError(f"{ctx}: forbidden secret-bearing key {key!r}")
            _assert_no_secret_values(value, f"{ctx}.{key}")
    elif isinstance(obj, list):
        for idx, value in enumerate(obj):
            _assert_no_secret_values(value, f"{ctx}[{idx}]")


def _safe_relative(root: Path, raw: str, *, ctx: str) -> Path:
    p = Path(raw)
    if p.is_absolute() or ".." in p.parts or not raw:
        raise ValueError(f"{ctx}: path must be non-empty and relative")
    root_resolved = root.resolve()
    resolved = (root / p).resolve()
    if resolved != root_resolved and root_resolved not in resolved.parents:
        raise ValueError(f"{ctx}: path escapes source root")
    return resolved


def validate_registry(doc: dict[str, Any]) -> None:
    _assert_no_secret_values(doc)
    if doc.get("schema") != REGISTRY_SCHEMA:
        raise ValueError("registry schema mismatch")
    if doc.get("claim_allowed") is not False:
        raise ValueError("registry claim_allowed must be false")
    if doc.get("secret_value_persisted") is not False:
        raise ValueError("registry must assert secret_value_persisted=false")
    targets = doc.get("targets")
    if not isinstance(targets, list) or not targets:
        raise ValueError("registry targets must be a non-empty list")

    seen: set[str] = set()
    for item in targets:
        if not isinstance(item, dict):
            raise ValueError("registry target must be an object")
        target_id = str(item.get("target_id", ""))
        repository = str(item.get("repository", ""))
        manifest_path = str(item.get("manifest_path", ""))
        if not ID_RE.fullmatch(target_id) or target_id in seen:
            raise ValueError(f"invalid/duplicate target_id: {target_id!r}")
        seen.add(target_id)
        if not REPO_RE.fullmatch(repository):
            raise ValueError(f"{target_id}: invalid repository")
        _safe_relative(Path("."), manifest_path, ctx=f"{target_id}.manifest_path")

        workflows = item.get("allowed_workflow_ids")
        if not isinstance(workflows, list) or not workflows:
            raise ValueError(f"{target_id}: allowed_workflow_ids must be non-empty")
        if len(set(workflows)) != len(workflows):
            raise ValueError(f"{target_id}: duplicate workflow id")
        if not all(isinstance(x, str) and ID_RE.fullmatch(x) for x in workflows):
            raise ValueError(f"{target_id}: invalid workflow id")

        executables = item.get("allowed_executables")
        if not isinstance(executables, list) or not executables:
            raise ValueError(f"{target_id}: allowed_executables must be non-empty")
        if not all(
            isinstance(x, str)
            and x == Path(x).name
            and re.fullmatch(r"[A-Za-z0-9_.+-]+", x)
            for x in executables
        ):
            raise ValueError(f"{target_id}: invalid allowed executable")


def resolve_public(
    registry: dict[str, Any],
    *,
    target_id: str,
    workflow_id: str,
    commit: str,
) -> dict[str, Any]:
    validate_registry(registry)
    if not SHA40_RE.fullmatch(commit):
        raise ValueError("target commit must be an exact lowercase 40-hex SHA")
    target = next(
        (x for x in registry["targets"] if x["target_id"] == target_id),
        None,
    )
    if target is None:
        raise ValueError(f"unknown target_id: {target_id}")
    if workflow_id not in target["allowed_workflow_ids"]:
        raise ValueError(f"{target_id}: workflow is not publicly allowlisted: {workflow_id}")
    return {
        "target_id": target_id,
        "repository": target["repository"],
        "manifest_path": target["manifest_path"],
        "workflow_id": workflow_id,
        "commit": commit,
        "allowed_executables": target["allowed_executables"],
        "secret_reference": registry.get("secret_reference", "PAT_ACTIONS"),
        "claim_allowed": False,
    }


def _validate_step(
    step: dict[str, Any],
    *,
    allowed_executables: set[str],
    source_root: Path | None,
    ctx: str,
) -> None:
    step_id = str(step.get("id", ""))
    if not ID_RE.fullmatch(step_id):
        raise ValueError(f"{ctx}: invalid step id")
    argv = step.get("argv")
    if (
        not isinstance(argv, list)
        or not argv
        or not all(isinstance(x, str) and x for x in argv)
    ):
        raise ValueError(f"{ctx}: argv must be a non-empty string array")
    executable = Path(argv[0]).name
    if executable not in allowed_executables:
        raise ValueError(f"{ctx}: executable {executable!r} is not allowlisted")

    cwd = str(step.get("cwd", "."))
    if source_root is not None:
        _safe_relative(source_root, cwd, ctx=f"{ctx}.cwd")
    elif Path(cwd).is_absolute() or ".." in Path(cwd).parts:
        raise ValueError(f"{ctx}: cwd must be relative")

    timeout = step.get("timeout_seconds", 300)
    if not isinstance(timeout, int) or not 1 <= timeout <= 1800:
        raise ValueError(f"{ctx}: timeout_seconds must be 1..1800")

    env = step.get("env", {})
    if not isinstance(env, dict):
        raise ValueError(f"{ctx}: env must be an object")
    for key, value in env.items():
        if not isinstance(key, str) or not ENV_RE.fullmatch(key):
            raise ValueError(f"{ctx}: invalid env key")
        if key in FORBIDDEN_SECRET_NAMES or any(
            marker in key for marker in ("TOKEN", "SECRET", "PASSWORD", "PRIVATE_KEY")
        ):
            raise ValueError(f"{ctx}: secret-like env key is forbidden: {key}")
        if not isinstance(value, str):
            raise ValueError(f"{ctx}: env value for {key} must be a string")


def validate_manifest(
    doc: dict[str, Any],
    *,
    expected_target_id: str,
    expected_repository: str,
    allowed_workflow_ids: set[str],
    allowed_executables: set[str],
    requested_workflow_id: str | None = None,
    source_root: Path | None = None,
) -> dict[str, Any] | None:
    _assert_no_secret_values(doc)
    if doc.get("schema") != MANIFEST_SCHEMA:
        raise ValueError("private manifest schema mismatch")
    if doc.get("claim_allowed") is not False:
        raise ValueError("private manifest claim_allowed must be false")
    if doc.get("target_id") != expected_target_id:
        raise ValueError("private manifest target_id mismatch")
    if doc.get("repository") != expected_repository:
        raise ValueError("private manifest repository mismatch")

    workflows = doc.get("workflows")
    if not isinstance(workflows, list) or not workflows:
        raise ValueError("private manifest workflows must be non-empty")

    seen: set[str] = set()
    selected: dict[str, Any] | None = None
    for workflow in workflows:
        if not isinstance(workflow, dict):
            raise ValueError("workflow entry must be object")
        workflow_id = str(workflow.get("workflow_id", ""))
        if not ID_RE.fullmatch(workflow_id) or workflow_id in seen:
            raise ValueError(f"invalid/duplicate private workflow id: {workflow_id!r}")
        seen.add(workflow_id)
        if workflow_id not in allowed_workflow_ids:
            raise ValueError(f"private manifest workflow not public-allowlisted: {workflow_id}")
        source_yaml = str(workflow.get("source_yaml", ""))
        source_yaml_path: Path | None = None
        if source_root is not None:
            source_yaml_path = _safe_relative(source_root, source_yaml, ctx=f"{workflow_id}.source_yaml")
            if not source_yaml_path.is_file():
                raise ValueError(f"{workflow_id}: source_yaml is missing from exact checkout")
            expected_blob = workflow.get("source_yaml_git_blob_sha1")
            if expected_blob is not None:
                if not isinstance(expected_blob, str) or not SHA40_RE.fullmatch(expected_blob):
                    raise ValueError(f"{workflow_id}: invalid source_yaml_git_blob_sha1")
                observed_blob = git_blob_sha1_file(source_yaml_path)
                if observed_blob != expected_blob:
                    raise ValueError(
                        f"{workflow_id}: source YAML git blob mismatch "
                        f"(expected {expected_blob}, observed {observed_blob})"
                    )
        elif (
            not source_yaml
            or Path(source_yaml).is_absolute()
            or ".." in Path(source_yaml).parts
        ):
            raise ValueError(f"{workflow_id}: source_yaml must be relative")

        steps = workflow.get("steps")
        if not isinstance(steps, list) or not steps:
            raise ValueError(f"{workflow_id}: steps must be non-empty")
        step_ids: set[str] = set()
        for idx, step in enumerate(steps):
            if not isinstance(step, dict):
                raise ValueError(f"{workflow_id}.steps[{idx}]: must be object")
            _validate_step(
                step,
                allowed_executables=allowed_executables,
                source_root=source_root,
                ctx=f"{workflow_id}.steps[{idx}]",
            )
            sid = step["id"]
            if sid in step_ids:
                raise ValueError(f"{workflow_id}: duplicate step id {sid}")
            step_ids.add(sid)

        artifacts = workflow.get("artifact_hash_paths", [])
        if not isinstance(artifacts, list):
            raise ValueError(f"{workflow_id}: artifact_hash_paths must be a list")
        for idx, artifact in enumerate(artifacts):
            if not isinstance(artifact, dict):
                raise ValueError(f"{workflow_id}.artifact_hash_paths[{idx}]: must be object")
            raw = str(artifact.get("path", ""))
            if source_root is not None:
                _safe_relative(source_root, raw, ctx=f"{workflow_id}.artifact[{idx}]")
            elif not raw or Path(raw).is_absolute() or ".." in Path(raw).parts:
                raise ValueError(f"{workflow_id}: artifact path must be relative")
            if artifact.get("required") not in (True, False):
                raise ValueError(f"{workflow_id}: artifact required must be boolean")

        if workflow_id == requested_workflow_id:
            selected = workflow

    if requested_workflow_id is not None and selected is None:
        raise ValueError(f"requested workflow missing from private manifest: {requested_workflow_id}")
    return selected


def _safe_execution_env(step_env: dict[str, str]) -> dict[str, str]:
    inherited: dict[str, str] = {}
    for key in ("PATH", "HOME", "LANG", "LC_ALL", "TMPDIR", "TZ"):
        value = os.environ.get(key)
        if value:
            inherited[key] = value
    inherited["CI"] = "true"
    inherited.update(step_env)
    return inherited


def _assert_execution_has_no_provider_secret() -> None:
    leaked = [
        name
        for name in FORBIDDEN_SECRET_NAMES
        if os.environ.get(name)
    ]
    if leaked:
        raise ValueError(
            "provider secret is present in execution environment: " + ",".join(sorted(leaked))
        )


def execute_plan(
    *,
    registry: dict[str, Any],
    manifest: dict[str, Any],
    target_id: str,
    workflow_id: str,
    commit: str,
    source_root: Path,
    receipt_path: Path,
) -> dict[str, Any]:
    resolved = resolve_public(
        registry,
        target_id=target_id,
        workflow_id=workflow_id,
        commit=commit,
    )
    allowed_workflows = set(
        next(x for x in registry["targets"] if x["target_id"] == target_id)["allowed_workflow_ids"]
    )
    allowed_executables = set(resolved["allowed_executables"])
    workflow = validate_manifest(
        manifest,
        expected_target_id=target_id,
        expected_repository=resolved["repository"],
        allowed_workflow_ids=allowed_workflows,
        allowed_executables=allowed_executables,
        requested_workflow_id=workflow_id,
        source_root=source_root,
    )
    assert workflow is not None

    _assert_execution_has_no_provider_secret()
    source_yaml = _safe_relative(
        source_root,
        workflow["source_yaml"],
        ctx=f"{workflow_id}.source_yaml",
    )
    if not source_yaml.is_file():
        raise ValueError(f"source workflow YAML missing: {workflow['source_yaml']}")

    step_receipts: list[dict[str, Any]] = []
    result = "PASS"
    gaps: list[str] = ["TOKEN_VAZIO_NETWORK_EGRESS_ISOLATION_NOT_ENFORCED"]

    for step in workflow["steps"]:
        cwd = _safe_relative(source_root, str(step.get("cwd", ".")), ctx=step["id"])
        run = subprocess.run(
            step["argv"],
            cwd=cwd,
            env=_safe_execution_env(dict(step.get("env", {}))),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=int(step.get("timeout_seconds", 300)),
            check=False,
        )
        step_receipts.append(
            {
                "id": step["id"],
                "argv_sha256": sha256_bytes(
                    canonical_bytes(step["argv"])
                ),
                "cwd": str(step.get("cwd", ".")),
                "returncode": int(run.returncode),
                "stdout_sha256": sha256_bytes(run.stdout),
                "stderr_sha256": sha256_bytes(run.stderr),
                "stdout_bytes": len(run.stdout),
                "stderr_bytes": len(run.stderr),
                "raw_stdout_persisted": False,
                "raw_stderr_persisted": False,
            }
        )
        if run.returncode != 0:
            result = "FAIL"
            break

    artifact_receipts: list[dict[str, Any]] = []
    if result == "PASS":
        for artifact in workflow.get("artifact_hash_paths", []):
            path = _safe_relative(
                source_root,
                artifact["path"],
                ctx=f"{workflow_id}.artifact",
            )
            if path.is_file():
                artifact_receipts.append(
                    {
                        "path": artifact["path"],
                        "state": "PRESENT_HASHED",
                        "sha256": sha256_file(path),
                        "size": path.stat().st_size,
                        "content_persisted_publicly": False,
                    }
                )
            else:
                artifact_receipts.append(
                    {
                        "path": artifact["path"],
                        "state": "MISSING",
                        "sha256": None,
                        "size": None,
                        "content_persisted_publicly": False,
                    }
                )
                if artifact["required"]:
                    result = "FAIL"
                    gaps.append(f"MISSING_REQUIRED_ARTIFACT:{artifact['path']}")

    receipt = {
        "schema": RECEIPT_SCHEMA,
        "target_id": target_id,
        "repository": resolved["repository"],
        "source_commit": commit,
        "workflow_id": workflow_id,
        "source_yaml": workflow["source_yaml"],
        "source_yaml_sha256": sha256_file(source_yaml),
        "private_manifest_sha256": sha256_bytes(canonical_bytes(manifest)),
        "result": result,
        "steps": step_receipts,
        "artifact_hashes": artifact_receipts,
        "runner": {
            "system": platform.system(),
            "machine": platform.machine(),
            "python": platform.python_version(),
        },
        "github_execution": {
            "control_repository": os.environ.get("GITHUB_REPOSITORY", "TOKEN_VAZIO"),
            "run_id": os.environ.get("GITHUB_RUN_ID", "TOKEN_VAZIO"),
            "run_attempt": os.environ.get("GITHUB_RUN_ATTEMPT", "TOKEN_VAZIO"),
            "job": os.environ.get("GITHUB_JOB", "TOKEN_VAZIO"),
        },
        "privacy_boundary": {
            "secret_reference": resolved["secret_reference"],
            "secret_value_persisted": False,
            "secret_available_to_private_subprocess": False,
            "shell_execution": False,
            "raw_private_stdout_persisted": False,
            "raw_private_stderr_persisted": False,
            "private_source_uploaded_as_public_artifact": False,
            "network_egress_isolation": "TOKEN_VAZIO_NOT_ENFORCED",
        },
        "claim_allowed": False,
        "f_gap": gaps,
        "f_next": (
            "Promote this exact target/workflow/commit only after the receipt is reviewed; "
            "network egress isolation remains an explicit gap."
        ),
    }
    receipt_path.parent.mkdir(parents=True, exist_ok=True)
    receipt_path.write_bytes(canonical_bytes(receipt))
    return receipt


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    sub = ap.add_subparsers(dest="command", required=True)

    p = sub.add_parser("validate-registry")
    p.add_argument("--registry", type=Path, required=True)

    p = sub.add_parser("resolve")
    p.add_argument("--registry", type=Path, required=True)
    p.add_argument("--target-id", required=True)
    p.add_argument("--workflow-id", required=True)
    p.add_argument("--commit", required=True)

    p = sub.add_parser("validate-manifest")
    p.add_argument("--registry", type=Path, required=True)
    p.add_argument("--manifest", type=Path, required=True)
    p.add_argument("--target-id", required=True)
    p.add_argument("--workflow-id", required=True)
    p.add_argument("--commit", required=True)
    p.add_argument("--source-root", type=Path, required=True)

    p = sub.add_parser("execute")
    p.add_argument("--registry", type=Path, required=True)
    p.add_argument("--manifest", type=Path, required=True)
    p.add_argument("--target-id", required=True)
    p.add_argument("--workflow-id", required=True)
    p.add_argument("--commit", required=True)
    p.add_argument("--source-root", type=Path, required=True)
    p.add_argument("--receipt", type=Path, required=True)

    args = ap.parse_args()

    try:
        registry = load_json(args.registry)
        if args.command == "validate-registry":
            validate_registry(registry)
            print("PASS private-ci-registry claim_allowed=false")
            return 0

        resolved = resolve_public(
            registry,
            target_id=args.target_id,
            workflow_id=args.workflow_id,
            commit=args.commit,
        )
        if args.command == "resolve":
            print(json.dumps(resolved, sort_keys=True))
            return 0

        manifest = load_json(args.manifest)
        target = next(
            x for x in registry["targets"] if x["target_id"] == args.target_id
        )
        if args.command == "validate-manifest":
            validate_manifest(
                manifest,
                expected_target_id=args.target_id,
                expected_repository=resolved["repository"],
                allowed_workflow_ids=set(target["allowed_workflow_ids"]),
                allowed_executables=set(target["allowed_executables"]),
                requested_workflow_id=args.workflow_id,
                source_root=args.source_root,
            )
            print(
                json.dumps(
                    {
                        "result": "PASS",
                        "target_id": args.target_id,
                        "workflow_id": args.workflow_id,
                        "commit": args.commit,
                        "claim_allowed": False,
                    },
                    sort_keys=True,
                )
            )
            return 0

        receipt = execute_plan(
            registry=registry,
            manifest=manifest,
            target_id=args.target_id,
            workflow_id=args.workflow_id,
            commit=args.commit,
            source_root=args.source_root,
            receipt_path=args.receipt,
        )
        print(
            json.dumps(
                {
                    "result": receipt["result"],
                    "target_id": receipt["target_id"],
                    "workflow_id": receipt["workflow_id"],
                    "source_commit": receipt["source_commit"],
                    "receipt_sha256": sha256_file(args.receipt),
                    "claim_allowed": False,
                    "f_gap": receipt["f_gap"],
                },
                sort_keys=True,
            )
        )
        return 0 if receipt["result"] == "PASS" else 1
    except (ValueError, OSError, json.JSONDecodeError, subprocess.TimeoutExpired) as exc:
        print(f"[FALHA] private CI bridge: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
