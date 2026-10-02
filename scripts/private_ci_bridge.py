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
import shutil
import socket
import subprocess
import sys
import zipfile
from pathlib import Path
from typing import Any

REGISTRY_SCHEMA = "rafgittools.private-ci-execution-registry.v1"
MANIFEST_SCHEMA = "rafaelia.private-ci-manifest.v1"
RECEIPT_SCHEMA = "rafgittools.private-ci-replay-receipt.v1"
NETWORK_ISOLATION_METHOD = "LINUX_USER_NAMESPACE_PLUS_NETWORK_NAMESPACE"
NETWORK_ISOLATION_SUDO_METHOD = "LINUX_ROOT_NETNS_DROP_TO_CALLER_NO_NEW_PRIVS"
ZIPRAF_RECEIPT_SCHEMA = "zipraf.private-ci-sanitized-receipt.v1"
ZIPRAF_RECEIPT_PATH = "receipt/private-ci-replay-receipt.json"
ZIPRAF_MANIFEST_PATH = "META-INF/ZIPRAF/MANIFEST.V1.json"
ZIPRAF_SUMS_PATH = "META-INF/ZIPRAF/SHA256SUMS"

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
            if key_norm == "secret_value_persisted" and value is False:
                continue
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


def _network_isolation_backends() -> list[tuple[str, list[str]]]:
    if platform.system() != "Linux":
        raise ValueError("network egress isolation requires Linux")
    unshare = shutil.which("unshare")
    if unshare is None:
        raise ValueError("network egress isolation requires util-linux unshare")

    backends: list[tuple[str, list[str]]] = [
        (
            NETWORK_ISOLATION_METHOD,
            [unshare, "--user", "--map-root-user", "--net", "--"],
        )
    ]

    sudo = shutil.which("sudo")
    setpriv = shutil.which("setpriv")
    if sudo is not None and setpriv is not None and hasattr(os, "getuid") and hasattr(os, "getgid"):
        backends.append(
            (
                NETWORK_ISOLATION_SUDO_METHOD,
                [
                    sudo,
                    "-n",
                    unshare,
                    "--net",
                    "--",
                    setpriv,
                    f"--reuid={os.getuid()}",
                    f"--regid={os.getgid()}",
                    "--clear-groups",
                    "--no-new-privs",
                    "--",
                ],
            )
        )
    return backends


def network_isolation_argv(argv: list[str], *, method: str | None = None) -> list[str]:
    backends = _network_isolation_backends()
    if method is None:
        selected_method, prefix = backends[0]
    else:
        try:
            selected_method, prefix = next(x for x in backends if x[0] == method)
        except StopIteration as exc:
            raise ValueError(f"network isolation method unavailable: {method}") from exc
    if selected_method == NETWORK_ISOLATION_SUDO_METHOD and os.geteuid() == 0:
        raise ValueError("sudo netns fallback must drop from a non-root caller")
    return [*prefix, *argv]


def probe_network_isolation() -> dict[str, Any]:
    probe = [
        sys.executable,
        "-c",
        (
            "import socket,sys;"
            "s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);"
            "s.settimeout(1.0);"
            "rc=s.connect_ex(('1.1.1.1',443));"
            "sys.exit(0 if rc != 0 else 41)"
        ),
    ]
    attempts: list[dict[str, Any]] = []
    for method, prefix in _network_isolation_backends():
        run = subprocess.run(
            [*prefix, *probe],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=10,
            check=False,
        )
        attempt = {
            "method": method,
            "returncode": int(run.returncode),
            "stdout_sha256": sha256_bytes(run.stdout),
            "stderr_sha256": sha256_bytes(run.stderr),
        }
        attempts.append(attempt)
        if run.returncode == 0:
            return {
                "method": method,
                "state": "PASS",
                "external_ipv4_tcp_connect_blocked": True,
                "probe_stdout_sha256": attempt["stdout_sha256"],
                "probe_stderr_sha256": attempt["stderr_sha256"],
                "attempt_count": len(attempts),
                "failed_backend_hashes": [
                    {
                        "method": x["method"],
                        "returncode": x["returncode"],
                        "stderr_sha256": x["stderr_sha256"],
                    }
                    for x in attempts[:-1]
                ],
                "claim_allowed": False,
            }

    rendered = "; ".join(
        f"{x['method']}:rc={x['returncode']}:stderr_sha256={x['stderr_sha256']}"
        for x in attempts
    )
    raise ValueError(f"network namespace isolation probe failed across all backends ({rendered})")


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

    isolation = probe_network_isolation()
    step_receipts: list[dict[str, Any]] = []
    result = "PASS"
    gaps: list[str] = []

    for step in workflow["steps"]:
        cwd = _safe_relative(source_root, str(step.get("cwd", ".")), ctx=step["id"])
        isolated_argv = network_isolation_argv(
            list(step["argv"]),
            method=str(isolation["method"]),
        )
        run = subprocess.run(
            isolated_argv,
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
                "network_isolation_method": isolation["method"],
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
            "network_egress_isolation": isolation,
        },
        "claim_allowed": False,
        "f_gap": gaps,
        "f_next": (
            "Promote this exact target/workflow/commit only after the receipt is reviewed "
            "and compared with the source workflow contract."
        ),
    }
    receipt_path.parent.mkdir(parents=True, exist_ok=True)
    receipt_path.write_bytes(canonical_bytes(receipt))
    return receipt



def _validate_sanitized_receipt_for_zipraf(receipt: dict[str, Any]) -> None:
    _assert_no_secret_values(receipt)
    if receipt.get("schema") != RECEIPT_SCHEMA:
        raise ValueError("ZIPRAF input receipt schema mismatch")
    if receipt.get("claim_allowed") is not False:
        raise ValueError("ZIPRAF input receipt claim_allowed must be false")
    boundary = receipt.get("privacy_boundary")
    if not isinstance(boundary, dict):
        raise ValueError("ZIPRAF input receipt privacy_boundary missing")
    required_false = (
        "secret_value_persisted",
        "secret_available_to_private_subprocess",
        "raw_private_stdout_persisted",
        "raw_private_stderr_persisted",
        "private_source_uploaded_as_public_artifact",
    )
    for key in required_false:
        if boundary.get(key) is not False:
            raise ValueError(f"ZIPRAF input receipt requires {key}=false")
    isolation = boundary.get("network_egress_isolation")
    if not isinstance(isolation, dict) or isolation.get("state") != "PASS":
        raise ValueError("ZIPRAF input receipt requires PASS network isolation")


def _zipinfo(name: str) -> zipfile.ZipInfo:
    info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_STORED
    info.create_system = 3
    info.external_attr = 0o100644 << 16
    return info


def pack_receipt_zipraf(receipt_path: Path, output_path: Path) -> dict[str, Any]:
    receipt = load_json(receipt_path)
    _validate_sanitized_receipt_for_zipraf(receipt)
    receipt_bytes = canonical_bytes(receipt)
    receipt_sha = sha256_bytes(receipt_bytes)
    manifest = {
        "schema": ZIPRAF_RECEIPT_SCHEMA,
        "profile": "PRIVATE_CI_SANITIZED_RECEIPT_V1",
        "receipt_path": ZIPRAF_RECEIPT_PATH,
        "receipt_sha256": receipt_sha,
        "encryption": False,
        "signature_state": "TOKEN_VAZIO_NOT_CONFIGURED",
        "external_signature_required_for_authenticity_claim": True,
        "container_integrity_claim_only": True,
        "claim_allowed": False,
    }
    manifest_bytes = canonical_bytes(manifest)
    sums = (
        f"{receipt_sha}  {ZIPRAF_RECEIPT_PATH}\n"
        f"{sha256_bytes(manifest_bytes)}  {ZIPRAF_MANIFEST_PATH}\n"
    ).encode("utf-8")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output_path, "w") as zf:
        zf.writestr(_zipinfo(ZIPRAF_RECEIPT_PATH), receipt_bytes)
        zf.writestr(_zipinfo(ZIPRAF_MANIFEST_PATH), manifest_bytes)
        zf.writestr(_zipinfo(ZIPRAF_SUMS_PATH), sums)

    return {
        "schema": ZIPRAF_RECEIPT_SCHEMA,
        "output_sha256": sha256_file(output_path),
        "receipt_sha256": receipt_sha,
        "signature_state": "TOKEN_VAZIO_NOT_CONFIGURED",
        "encryption": False,
        "claim_allowed": False,
    }


def verify_receipt_zipraf(path: Path) -> dict[str, Any]:
    with zipfile.ZipFile(path, "r") as zf:
        names = zf.namelist()
        expected = [ZIPRAF_RECEIPT_PATH, ZIPRAF_MANIFEST_PATH, ZIPRAF_SUMS_PATH]
        if names != expected:
            raise ValueError(f"ZIPRAF entry set/order mismatch: {names!r}")
        for name in names:
            p = Path(name)
            if p.is_absolute() or ".." in p.parts:
                raise ValueError("ZIPRAF contains unsafe entry path")
        receipt_bytes = zf.read(ZIPRAF_RECEIPT_PATH)
        manifest_bytes = zf.read(ZIPRAF_MANIFEST_PATH)
        sums_text = zf.read(ZIPRAF_SUMS_PATH).decode("utf-8")

    receipt = json.loads(receipt_bytes)
    manifest = json.loads(manifest_bytes)
    if not isinstance(receipt, dict) or not isinstance(manifest, dict):
        raise ValueError("ZIPRAF JSON entries must be objects")
    _validate_sanitized_receipt_for_zipraf(receipt)
    if manifest.get("schema") != ZIPRAF_RECEIPT_SCHEMA:
        raise ValueError("ZIPRAF manifest schema mismatch")
    if manifest.get("encryption") is not False:
        raise ValueError("ZIPRAF receipt profile must not claim encryption")
    if manifest.get("signature_state") != "TOKEN_VAZIO_NOT_CONFIGURED":
        raise ValueError("ZIPRAF signature state mismatch")
    if manifest.get("claim_allowed") is not False:
        raise ValueError("ZIPRAF claim_allowed must be false")
    receipt_sha = sha256_bytes(canonical_bytes(receipt))
    if manifest.get("receipt_sha256") != receipt_sha:
        raise ValueError("ZIPRAF receipt hash mismatch")
    expected_sums = (
        f"{receipt_sha}  {ZIPRAF_RECEIPT_PATH}\n"
        f"{sha256_bytes(canonical_bytes(manifest))}  {ZIPRAF_MANIFEST_PATH}\n"
    )
    if sums_text != expected_sums:
        raise ValueError("ZIPRAF SHA256SUMS mismatch")
    return {
        "schema": ZIPRAF_RECEIPT_SCHEMA,
        "zipraf_sha256": sha256_file(path),
        "receipt_sha256": receipt_sha,
        "signature_state": manifest["signature_state"],
        "encryption": False,
        "claim_allowed": False,
        "state": "PASS_INTEGRITY_ONLY",
    }


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    sub = ap.add_subparsers(dest="command", required=True)

    p = sub.add_parser("validate-registry")
    p.add_argument("--registry", type=Path, required=True)

    sub.add_parser("probe-isolation")

    p = sub.add_parser("pack-receipt")
    p.add_argument("--receipt", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)

    p = sub.add_parser("verify-receipt")
    p.add_argument("--zipraf", type=Path, required=True)

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
        if args.command == "probe-isolation":
            print(json.dumps(probe_network_isolation(), sort_keys=True))
            return 0
        if args.command == "pack-receipt":
            print(json.dumps(pack_receipt_zipraf(args.receipt, args.output), sort_keys=True))
            return 0
        if args.command == "verify-receipt":
            print(json.dumps(verify_receipt_zipraf(args.zipraf), sort_keys=True))
            return 0

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
