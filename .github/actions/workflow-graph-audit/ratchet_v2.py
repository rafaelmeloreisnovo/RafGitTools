#!/usr/bin/env python3
"""Touched-workflow hardening ratchet for GitHub Actions.

The repository may contain legacy CI debt. This ratchet does not pretend that
legacy debt is already fixed. Instead, every workflow touched by a change must
cross a stricter boundary: fixed runner, immutable remote actions, explicit
permissions, and checkout without persisted credentials.

The full repository is also inventoried so debt remains measurable. A green
result is a non-regression signal, not a security or compliance certification.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

SHA40 = re.compile(r"^[0-9a-fA-F]{40}$")
DOCKER_DIGEST = re.compile(r"^docker://.+@sha256:[0-9a-fA-F]{64}$")
USES = re.compile(r"^(?P<indent>\s*)(?:-\s*)?uses:\s*['\"]?(?P<value>[^'\"\s#]+)")
RUNS_ON = re.compile(r"^\s{4}runs-on:\s*['\"]?(?P<value>[^'\"#]+?)['\"]?\s*(?:#.*)?$")
TIMEOUT = re.compile(r"^\s{4}timeout-minutes:\s*(?P<value>.+?)\s*(?:#.*)?$")
PERSIST_FALSE = re.compile(r"^\s*persist-credentials:\s*false\s*(?:#.*)?$", re.IGNORECASE)
UNTRUSTED_EXPR = re.compile(
    r"\$\{\{\s*(?:inputs\.|github\.head_ref|github\.event\.(?:issue|pull_request|comment|discussion|review)\.)"
)


def run_git(root: Path, args: list[str]) -> str:
    proc = subprocess.run(
        ["git", "-C", str(root), *args],
        capture_output=True,
        text=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError((proc.stderr or proc.stdout).strip() or "git command failed")
    return proc.stdout


def immutable_use(value: str) -> bool:
    if value.startswith("./"):
        return True
    if value.startswith("docker://"):
        return bool(DOCKER_DIGEST.fullmatch(value))
    if "@" not in value:
        return False
    return bool(SHA40.fullmatch(value.rsplit("@", 1)[1]))


def checkout_has_persist_false(lines: list[str], use_index: int) -> bool:
    line = lines[use_index]
    use_indent = len(line) - len(line.lstrip(" "))
    for candidate in lines[use_index + 1 :]:
        if not candidate.strip() or candidate.lstrip().startswith("#"):
            continue
        indent = len(candidate) - len(candidate.lstrip(" "))
        stripped = candidate.lstrip()
        if indent < use_indent:
            break
        if indent == use_indent and stripped.startswith("-"):
            break
        if PERSIST_FALSE.match(candidate):
            return True
    return False


def analyze_workflow(path: Path, root: Path) -> dict:
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    rel = path.relative_to(root).as_posix()
    findings: list[dict] = []
    advisories: list[dict] = []

    top_permissions = any(line.startswith("permissions:") for line in lines)
    concurrency = any(line.startswith("concurrency:") for line in lines)
    if not top_permissions:
        findings.append({"path": rel, "reason": "MISSING_TOP_LEVEL_PERMISSIONS", "value": "permissions"})
    if not concurrency:
        advisories.append({"path": rel, "reason": "MISSING_EXPLICIT_CONCURRENCY", "value": "concurrency"})

    runner_values: list[str] = []
    mutable_uses: list[dict] = []
    checkout_debt: list[dict] = []
    untrusted_expr_lines: list[int] = []
    runner_jobs_without_timeout = 0
    current_runner_seen = False
    current_timeout_seen = False

    for idx, line in enumerate(lines):
        m_run = RUNS_ON.match(line)
        if m_run:
            if current_runner_seen and not current_timeout_seen:
                runner_jobs_without_timeout += 1
            current_runner_seen = True
            current_timeout_seen = False
            runner = m_run.group("value").strip()
            runner_values.append(runner)
            if runner == "ubuntu-latest":
                findings.append({"path": rel, "line": idx + 1, "reason": "FLOATING_UBUNTU_LATEST_RUNNER", "value": runner})

        if current_runner_seen and TIMEOUT.match(line):
            current_timeout_seen = True

        m_use = USES.match(line)
        if m_use:
            value = m_use.group("value")
            if not immutable_use(value):
                item = {"path": rel, "line": idx + 1, "reason": "MUTABLE_OR_UNVERSIONED_REMOTE_DEPENDENCY", "value": value}
                mutable_uses.append(item)
                findings.append(item)
            if value.startswith("actions/checkout@") and not checkout_has_persist_false(lines, idx):
                item = {"path": rel, "line": idx + 1, "reason": "CHECKOUT_PERSISTS_CREDENTIALS_OR_NOT_EXPLICITLY_DISABLED", "value": value}
                checkout_debt.append(item)
                findings.append(item)

        if UNTRUSTED_EXPR.search(line):
            untrusted_expr_lines.append(idx + 1)
            advisories.append({
                "path": rel,
                "line": idx + 1,
                "reason": "UNTRUSTED_CONTEXT_EXPRESSION_REQUIRES_SHELL_BOUNDARY_REVIEW",
                "value": line.strip(),
            })

    if current_runner_seen and not current_timeout_seen:
        runner_jobs_without_timeout += 1
    if runner_jobs_without_timeout:
        advisories.append({
            "path": rel,
            "reason": "RUNNER_JOB_WITHOUT_TIMEOUT_REQUIRES_REVIEW",
            "value": runner_jobs_without_timeout,
        })

    return {
        "path": rel,
        "findings": findings,
        "advisories": advisories,
        "metrics": {
            "runner_count": len(runner_values),
            "floating_ubuntu_latest_count": sum(1 for x in runner_values if x == "ubuntu-latest"),
            "mutable_remote_dependency_count": len(mutable_uses),
            "checkout_persistence_debt_count": len(checkout_debt),
            "top_level_permissions_explicit": top_permissions,
            "concurrency_explicit": concurrency,
            "untrusted_context_expression_count": len(untrusted_expr_lines),
            "runner_jobs_without_timeout_count": runner_jobs_without_timeout,
        },
    }


def changed_workflows(root: Path, base_ref: str) -> list[Path]:
    if not base_ref:
        return []
    out = run_git(root, ["diff", "--name-status", base_ref, "HEAD", "--", ".github/workflows"])
    paths: list[Path] = []
    for row in out.splitlines():
        parts = row.split("\t")
        if len(parts) < 2:
            continue
        status = parts[0]
        rel = parts[-1]
        if status.startswith("D") or not rel.lower().endswith((".yml", ".yaml")):
            continue
        path = root / rel
        if path.is_file():
            paths.append(path)
    return sorted(set(paths))


def all_workflows(root: Path) -> list[Path]:
    wf = root / ".github" / "workflows"
    if not wf.exists():
        return []
    return sorted(list(wf.glob("*.yml")) + list(wf.glob("*.yaml")))


def build_report(root: Path, base_ref: str) -> dict:
    all_reports = [analyze_workflow(path, root) for path in all_workflows(root)]
    changed_paths = changed_workflows(root, base_ref)
    changed_reports = [analyze_workflow(path, root) for path in changed_paths]
    strict_findings = [f for report in changed_reports for f in report["findings"]]
    advisories = [a for report in changed_reports for a in report["advisories"]]

    def debt_workflows(metric: str) -> list[str]:
        return sorted(
            report["path"]
            for report in all_reports
            if report["metrics"].get(metric, 0)
        )

    return {
        "schema": "rafgittools.workflow_hardening_ratchet.v2",
        "schema_revision": "2.0.0",
        "analysis_kind": "TOUCHED_WORKFLOW_NON_REGRESSION_RATCHET",
        "claim_allowed": False,
        "base_ref": base_ref or "TOKEN_VAZIO",
        "strict_scope": "CHANGED_WORKFLOWS_ONLY",
        "summary": {
            "workflow_count": len(all_reports),
            "changed_workflow_count": len(changed_reports),
            "strict_finding_count": len(strict_findings),
            "advisory_count": len(advisories),
            "floating_runner_workflow_count": len(debt_workflows("floating_ubuntu_latest_count")),
            "mutable_dependency_workflow_count": len(debt_workflows("mutable_remote_dependency_count")),
            "checkout_persistence_debt_workflow_count": len(debt_workflows("checkout_persistence_debt_count")),
        },
        "bounded_debt_lists": {
            "floating_runner_workflows": debt_workflows("floating_ubuntu_latest_count"),
            "mutable_dependency_workflows": debt_workflows("mutable_remote_dependency_count"),
            "checkout_persistence_debt_workflows": debt_workflows("checkout_persistence_debt_count"),
        },
        "strict_findings": strict_findings,
        "advisories": advisories,
        "changed_workflows": changed_reports,
        "invariants": [
            "LEGACY_DEBT != NEW_DEBT",
            "CHANGED_WORKFLOW_MUST_CROSS_HARDENING_BOUNDARY",
            "UBUNTU_LATEST != REPRODUCIBLE_RUNNER_LABEL",
            "MUTABLE_ACTION_REF != IMMUTABLE_SUPPLY_CHAIN",
            "CHECKOUT_DEFAULT_CREDENTIAL_PERSISTENCE != LEAST_PRIVILEGE",
            "TOKEN_VAZIO != PASS",
            "RATCHET_PASS != SECURITY_CERTIFICATION",
        ],
    }


def self_test() -> int:
    with tempfile.TemporaryDirectory() as td:
        root = Path(td)
        wf = root / ".github" / "workflows"
        wf.mkdir(parents=True)
        bad = wf / "bad.yml"
        bad.write_text(
            """name: Bad\non:\n  pull_request:\njobs:\n  test:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n""",
            encoding="utf-8",
        )
        bad_report = analyze_workflow(bad, root)
        reasons = {f["reason"] for f in bad_report["findings"]}
        assert "MISSING_TOP_LEVEL_PERMISSIONS" in reasons
        assert "FLOATING_UBUNTU_LATEST_RUNNER" in reasons
        assert "MUTABLE_OR_UNVERSIONED_REMOTE_DEPENDENCY" in reasons
        assert "CHECKOUT_PERSISTS_CREDENTIALS_OR_NOT_EXPLICITLY_DISABLED" in reasons

        good = wf / "good.yml"
        good.write_text(
            """name: Good\non:\n  pull_request:\npermissions:\n  contents: read\nconcurrency:\n  group: good-${{ github.ref }}\njobs:\n  test:\n    runs-on: ubuntu-24.04\n    timeout-minutes: 5\n    steps:\n      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1\n        with:\n          persist-credentials: false\n""",
            encoding="utf-8",
        )
        good_report = analyze_workflow(good, root)
        assert good_report["findings"] == []
    print("WORKFLOW_HARDENING_RATCHET_SELF_TEST=PASS")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=".")
    ap.add_argument("--base-ref", default="")
    ap.add_argument("--output", default="workflow-hardening-ratchet.json")
    ap.add_argument("--summary", action="store_true")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        return self_test()

    root = Path(args.repo).resolve()
    report = build_report(root, args.base_ref)
    out = root / args.output
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    if args.summary:
        print("WORKFLOW_HARDENING_RATCHET")
        for key, value in report["summary"].items():
            print(f"{key}={json.dumps(value, sort_keys=True)}")
        for advisory in report["advisories"]:
            print(f"ADVISORY {advisory['path']}: {advisory['value']} -> {advisory['reason']}")

    if report["strict_findings"]:
        for finding in report["strict_findings"]:
            print(f"ERROR {finding['path']}: {finding['value']} -> {finding['reason']}")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
