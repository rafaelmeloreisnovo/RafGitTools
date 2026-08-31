#!/usr/bin/env python3
"""Evidence-first lexical GitHub Actions workflow graph audit.

This is intentionally not a full YAML semantic parser. It extracts bounded,
auditable workflow topology and applies a non-regression ratchet to changed
GitHub Actions YAML. Success here is not security/compliance certification.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import tempfile
from collections import Counter
from pathlib import Path

SHA40 = re.compile(r"^[0-9a-fA-F]{40}$")
DOCKER_DIGEST = re.compile(r"^docker://.+@sha256:[0-9a-fA-F]{64}$")
USES = re.compile(r"^\s*(?:-\s*)?uses:\s*['\"]?([^'\"\s#]+)")
NAME = re.compile(r"^name:\s*(.+?)\s*$")
JOB = re.compile(r"^  ([A-Za-z0-9_-]+):\s*$")
NEEDS = re.compile(r"^\s{4}needs:\s*(.+?)\s*$")
RUNS_ON = re.compile(r"^\s{4}runs-on:\s*(.+?)\s*$")
TIMEOUT = re.compile(r"^\s{4}timeout-minutes:\s*(.+?)\s*$")
ADDED_USES = re.compile(r"^\+\s*(?:-\s*)?uses:\s*['\"]?([^'\"\s#]+)")
ADDED_WRITE_ALL = re.compile(r"^\+\s*permissions:\s*write-all\s*(?:#.*)?$")
ADDED_PULL_REQUEST_TARGET = re.compile(r"^\+\s*pull_request_target:\s*(?:#.*)?$")
ADDED_SECRETS_INHERIT = re.compile(r"^\+\s*secrets:\s*inherit\s*(?:#.*)?$")

TRIGGERS = (
    "push", "pull_request", "pull_request_target", "workflow_dispatch",
    "workflow_call", "workflow_run", "repository_dispatch", "schedule",
    "issues", "issue_comment", "release",
)


def classify_use(value: str) -> dict:
    if value.startswith("./"):
        return {"value": value, "kind": "local", "immutable": True, "ref": None}
    if value.startswith("docker://"):
        return {
            "value": value,
            "kind": "docker",
            "immutable": bool(DOCKER_DIGEST.fullmatch(value)),
            "ref": value.split("@", 1)[1] if "@" in value else None,
        }
    if "@" not in value:
        return {"value": value, "kind": "unversioned", "immutable": False, "ref": None}
    target, ref = value.rsplit("@", 1)
    kind = "reusable_workflow" if "/.github/workflows/" in target else "remote_action"
    return {"value": value, "kind": kind, "immutable": bool(SHA40.fullmatch(ref)), "ref": ref}


def scalar_or_list(raw: str) -> list[str]:
    raw = raw.strip().strip("'\"")
    if raw.startswith("[") and raw.endswith("]"):
        return [x.strip().strip("'\"") for x in raw[1:-1].split(",") if x.strip()]
    return [raw] if raw else []


def graph_integrity(jobs: dict[str, dict]) -> dict:
    job_ids = set(jobs)
    undefined = sorted(
        {dep for meta in jobs.values() for dep in meta["needs"] if dep not in job_ids}
    )
    self_needs = sorted(job_id for job_id, meta in jobs.items() if job_id in meta["needs"])
    cycles: list[list[str]] = []
    visiting: list[str] = []
    state: dict[str, int] = {job_id: 0 for job_id in jobs}

    def dfs(node: str) -> None:
        state[node] = 1
        visiting.append(node)
        for dep in jobs[node]["needs"]:
            if dep not in jobs:
                continue
            if state[dep] == 0:
                dfs(dep)
            elif state[dep] == 1:
                try:
                    start = visiting.index(dep)
                    cycle = visiting[start:] + [dep]
                except ValueError:
                    cycle = [node, dep, node]
                if cycle not in cycles:
                    cycles.append(cycle)
        visiting.pop()
        state[node] = 2

    for job_id in jobs:
        if state[job_id] == 0:
            dfs(job_id)
    return {"undefined_needs": undefined, "self_needs": self_needs, "cycles": cycles}


def parse_workflow(path: Path, root: Path) -> dict:
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    workflow_name = path.name
    triggers: set[str] = set()
    uses: list[dict] = []
    jobs: dict[str, dict] = {}
    job_declarations: Counter[str] = Counter()
    current_job = None
    in_on = False
    in_jobs = False

    for idx, line in enumerate(lines, 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if line.startswith("name:"):
            m = NAME.match(line)
            if m:
                workflow_name = m.group(1).strip("'\"")
        if line.strip() == "jobs:" and not line.startswith(" "):
            in_jobs = True
            current_job = None
            continue
        if line.startswith("on:") or line.startswith('"on":') or line.startswith("'on':"):
            in_on = True
            inline = line.split(":", 1)[1]
            for trig in TRIGGERS:
                if re.search(rf"\b{re.escape(trig)}\b", inline):
                    triggers.add(trig)
            continue
        if in_on:
            indent = len(line) - len(line.lstrip(" "))
            if indent == 0:
                in_on = False
            else:
                stripped = line.strip()
                for trig in TRIGGERS:
                    if stripped.startswith(trig + ":") or stripped == trig:
                        triggers.add(trig)
        if in_jobs:
            if line and not line.startswith(" "):
                in_jobs = False
                current_job = None
            else:
                m_job = JOB.match(line)
                if m_job:
                    current_job = m_job.group(1)
                    job_declarations[current_job] += 1
                    jobs.setdefault(
                        current_job,
                        {"needs": [], "runs_on": None, "timeout_minutes": None, "uses": []},
                    )
        m_needs = NEEDS.match(line)
        if m_needs and current_job:
            jobs[current_job]["needs"] = scalar_or_list(m_needs.group(1))
        m_ro = RUNS_ON.match(line)
        if m_ro and current_job:
            jobs[current_job]["runs_on"] = m_ro.group(1).strip("'\"")
        m_timeout = TIMEOUT.match(line)
        if m_timeout and current_job:
            jobs[current_job]["timeout_minutes"] = m_timeout.group(1).strip("'\"")
        m_use = USES.match(line)
        if m_use:
            dep = classify_use(m_use.group(1))
            dep["line"] = idx
            uses.append(dep)
            if current_job:
                jobs[current_job]["uses"].append(dep["value"])

    top_permissions = any(line.startswith("permissions:") for line in lines)
    write_all = any(re.match(r"^\s*permissions:\s*write-all\s*$", line) for line in lines)
    secrets_inherit = any(re.match(r"^\s*secrets:\s*inherit\s*$", line) for line in lines)
    concurrency = any(line.startswith("concurrency:") for line in lines)
    edges = [
        {"from": dep, "to": job_id, "type": "needs"}
        for job_id, meta in jobs.items()
        for dep in meta["needs"]
    ]
    integrity = graph_integrity(jobs)
    duplicate_job_ids = sorted(k for k, count in job_declarations.items() if count > 1)

    return {
        "path": path.relative_to(root).as_posix(),
        "name": workflow_name,
        "triggers": sorted(triggers),
        "jobs": jobs,
        "job_edges": edges,
        "uses": uses,
        "top_level_permissions_explicit": top_permissions,
        "write_all_observed": write_all,
        "secrets_inherit_observed": secrets_inherit,
        "concurrency_explicit": concurrency,
        "duplicate_job_ids": duplicate_job_ids,
        "graph_integrity": integrity,
        "risk_markers": {
            "pull_request_target": "pull_request_target" in triggers,
            "mutable_uses_count": sum(1 for u in uses if u["kind"] != "local" and not u["immutable"]),
        },
    }


def run_git(root: Path, args: list[str]) -> str:
    proc = subprocess.run(
        ["git", "-C", str(root), *args], capture_output=True, text=True, check=False
    )
    if proc.returncode != 0:
        raise RuntimeError((proc.stderr or proc.stdout).strip() or "git command failed")
    return proc.stdout


def changed_yaml_metadata(root: Path, base_ref: str) -> tuple[set[str], set[str]]:
    if not base_ref:
        return set(), set()
    out = run_git(
        root,
        ["diff", "--name-status", base_ref, "HEAD", "--", ".github/workflows", ".github/actions"],
    )
    changed: set[str] = set()
    added: set[str] = set()
    for row in out.splitlines():
        parts = row.split("\t")
        if len(parts) < 2:
            continue
        status = parts[0]
        path = parts[-1]
        if not path.lower().endswith((".yml", ".yaml")):
            continue
        changed.add(path)
        if status.startswith("A"):
            added.add(path)
    return changed, added


def diff_policy_findings(root: Path, base_ref: str) -> tuple[list[dict], list[dict]]:
    if not base_ref:
        return [], []
    out = run_git(
        root,
        [
            "diff", "--unified=0", "--no-color", base_ref, "HEAD", "--",
            ".github/workflows", ".github/actions",
        ],
    )
    findings: list[dict] = []
    advisories: list[dict] = []
    current = "TOKEN_VAZIO_PATH"
    for line in out.splitlines():
        if line.startswith("+++ b/"):
            current = line[6:]
            continue
        if not current.lower().endswith((".yml", ".yaml")):
            continue
        m = ADDED_USES.match(line)
        if m:
            dep = classify_use(m.group(1))
            if dep["kind"] != "local" and not dep["immutable"]:
                findings.append(
                    {"path": current, "value": dep["value"], "reason": "NEW_MUTABLE_OR_UNVERSIONED_DEPENDENCY"}
                )
        if ADDED_WRITE_ALL.match(line):
            findings.append({"path": current, "value": "permissions: write-all", "reason": "NEW_WRITE_ALL_PERMISSIONS"})
        if ADDED_PULL_REQUEST_TARGET.match(line):
            advisories.append({"path": current, "value": "pull_request_target", "reason": "NEW_HIGH_RISK_TRIGGER_REQUIRES_REVIEW"})
        if ADDED_SECRETS_INHERIT.match(line):
            advisories.append({"path": current, "value": "secrets: inherit", "reason": "NEW_SECRET_PROPAGATION_REQUIRES_REVIEW"})
    return findings, advisories


def build_report(root: Path, mode: str, base_ref: str) -> dict:
    wf_dir = root / ".github" / "workflows"
    paths = sorted(list(wf_dir.glob("*.yml")) + list(wf_dir.glob("*.yaml"))) if wf_dir.exists() else []
    workflows = [parse_workflow(p, root) for p in paths]
    by_path = {w["path"]: w for w in workflows}
    names: dict[str, list[str]] = {}
    trigger_counts: Counter[str] = Counter()
    dependency_kinds: Counter[str] = Counter()
    for w in workflows:
        names.setdefault(w["name"], []).append(w["path"])
        trigger_counts.update(w["triggers"])
        dependency_kinds.update(u["kind"] for u in w["uses"])
    duplicate_names = {k: v for k, v in names.items() if len(v) > 1}

    strict_findings: list[dict] = []
    advisories: list[dict] = []
    changed_paths: set[str] = set()
    added_paths: set[str] = set()
    if mode == "strict-changed":
        changed_paths, added_paths = changed_yaml_metadata(root, base_ref)
        diff_findings, diff_advisories = diff_policy_findings(root, base_ref)
        strict_findings.extend(diff_findings)
        advisories.extend(diff_advisories)

        for path in sorted(added_paths):
            w = by_path.get(path)
            if w and not w["top_level_permissions_explicit"]:
                strict_findings.append({"path": path, "value": "permissions", "reason": "NEW_WORKFLOW_MISSING_EXPLICIT_PERMISSIONS"})

        for path in sorted(changed_paths):
            w = by_path.get(path)
            if not w:
                continue
            gi = w["graph_integrity"]
            if w["duplicate_job_ids"]:
                strict_findings.append({"path": path, "value": w["duplicate_job_ids"], "reason": "CHANGED_WORKFLOW_DUPLICATE_JOB_ID"})
            if gi["undefined_needs"]:
                strict_findings.append({"path": path, "value": gi["undefined_needs"], "reason": "CHANGED_WORKFLOW_UNDEFINED_NEEDS"})
            if gi["cycles"]:
                strict_findings.append({"path": path, "value": gi["cycles"], "reason": "CHANGED_WORKFLOW_CYCLIC_NEEDS"})
            if any(meta["runs_on"] and meta["timeout_minutes"] is None for meta in w["jobs"].values()):
                advisories.append({"path": path, "value": "timeout-minutes", "reason": "CHANGED_WORKFLOW_JOB_WITHOUT_TIMEOUT"})
            if not w["concurrency_explicit"]:
                advisories.append({"path": path, "value": "concurrency", "reason": "CHANGED_WORKFLOW_WITHOUT_EXPLICIT_CONCURRENCY"})

    runner_jobs = [meta for w in workflows for meta in w["jobs"].values() if meta["runs_on"]]
    mutable_workflows = sorted(w["path"] for w in workflows if w["risk_markers"]["mutable_uses_count"])
    undefined_total = sum(len(w["graph_integrity"]["undefined_needs"]) for w in workflows)
    cyclic_workflows = sorted(w["path"] for w in workflows if w["graph_integrity"]["cycles"])
    duplicate_job_workflows = sorted(w["path"] for w in workflows if w["duplicate_job_ids"])
    immutable_remote = sum(1 for w in workflows for u in w["uses"] if u["kind"] != "local" and u["immutable"])
    mutable_remote = sum(1 for w in workflows for u in w["uses"] if u["kind"] != "local" and not u["immutable"])

    return {
        "schema": "rafgittools.workflow_graph_audit.v1",
        "schema_revision": "1.1.0",
        "analysis_kind": "LEXICAL_STRUCTURAL_INVENTORY_NOT_FULL_YAML_AST",
        "claim_allowed": False,
        "mode": mode,
        "base_ref": base_ref or "TOKEN_VAZIO",
        "summary": {
            "workflow_count": len(workflows),
            "job_count": sum(len(w["jobs"]) for w in workflows),
            "job_edge_count": sum(len(w["job_edges"]) for w in workflows),
            "dependency_count": sum(len(w["uses"]) for w in workflows),
            "dependency_kinds": dict(sorted(dependency_kinds.items())),
            "immutable_remote_dependency_count": immutable_remote,
            "mutable_dependency_count": mutable_remote,
            "mutable_dependency_workflow_count": len(mutable_workflows),
            "pull_request_target_workflow_count": sum(1 for w in workflows if w["risk_markers"]["pull_request_target"]),
            "workflow_call_workflow_count": sum(1 for w in workflows if "workflow_call" in w["triggers"]),
            "explicit_permissions_workflow_count": sum(1 for w in workflows if w["top_level_permissions_explicit"]),
            "workflows_without_explicit_permissions_count": sum(1 for w in workflows if not w["top_level_permissions_explicit"]),
            "write_all_workflow_count": sum(1 for w in workflows if w["write_all_observed"]),
            "secrets_inherit_workflow_count": sum(1 for w in workflows if w["secrets_inherit_observed"]),
            "concurrency_workflow_count": sum(1 for w in workflows if w["concurrency_explicit"]),
            "runner_job_count": len(runner_jobs),
            "runner_jobs_with_timeout_count": sum(1 for meta in runner_jobs if meta["timeout_minutes"] is not None),
            "runner_jobs_without_timeout_count": sum(1 for meta in runner_jobs if meta["timeout_minutes"] is None),
            "undefined_needs_count": undefined_total,
            "cyclic_workflow_count": len(cyclic_workflows),
            "duplicate_job_id_workflow_count": len(duplicate_job_workflows),
            "duplicate_workflow_names": duplicate_names,
            "trigger_counts": dict(sorted(trigger_counts.items())),
            "changed_yaml_count": len(changed_paths),
            "added_yaml_count": len(added_paths),
            "strict_finding_count": len(strict_findings),
            "advisory_count": len(advisories),
        },
        "bounded_lists": {
            "mutable_dependency_workflows": mutable_workflows,
            "cyclic_workflows": cyclic_workflows,
            "duplicate_job_id_workflows": duplicate_job_workflows,
            "workflows_without_explicit_permissions": sorted(w["path"] for w in workflows if not w["top_level_permissions_explicit"]),
        },
        "strict_findings": strict_findings,
        "advisories": advisories,
        "workflows": workflows,
        "invariants": [
            "LEXICAL_PARSE != FULL_YAML_SEMANTICS",
            "CI_SUCCESS != SERVER_SIDE_ENFORCEMENT",
            "MUTABLE_REF != IMMUTABLE_SUPPLY_CHAIN",
            "DOCKER_TAG != DOCKER_DIGEST",
            "TOKEN_VAZIO != PASS",
            "NO_FINDING != NO_RISK",
        ],
    }


def self_test() -> int:
    with tempfile.TemporaryDirectory() as td:
        root = Path(td)
        wf = root / ".github" / "workflows"
        wf.mkdir(parents=True)
        (wf / "x.yml").write_text(
            """name: X\non:\n  pull_request:\npermissions:\n  contents: read\njobs:\n  a:\n    runs-on: ubuntu-latest\n    timeout-minutes: 5\n    steps:\n      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683\n  b:\n    needs: a\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/setup-python@v5\n""",
            encoding="utf-8",
        )
        r = build_report(root, "report", "")
        assert r["summary"]["workflow_count"] == 1
        assert r["summary"]["job_count"] == 2
        assert r["summary"]["job_edge_count"] == 1
        assert r["summary"]["mutable_dependency_count"] == 1
        assert r["summary"]["undefined_needs_count"] == 0
        assert r["summary"]["cyclic_workflow_count"] == 0
        assert r["workflows"][0]["job_edges"] == [{"from": "a", "to": "b", "type": "needs"}]
        assert classify_use("docker://alpine:3.22")["immutable"] is False
        assert classify_use("docker://example/image@sha256:" + "a" * 64)["immutable"] is True
    print("WORKFLOW_GRAPH_SELF_TEST=PASS")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=".")
    ap.add_argument("--mode", choices=("report", "strict-changed"), default="report")
    ap.add_argument("--base-ref", default="")
    ap.add_argument("--output", default="workflow-graph-audit.json")
    ap.add_argument("--summary", action="store_true")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        return self_test()
    root = Path(args.repo).resolve()
    report = build_report(root, args.mode, args.base_ref)
    out = root / args.output
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    if args.summary:
        print("WORKFLOW_GRAPH_AUDIT")
        for k, v in report["summary"].items():
            print(f"{k}={json.dumps(v, sort_keys=True)}")
        for advisory in report["advisories"]:
            print(f"ADVISORY {advisory['path']}: {advisory['value']} -> {advisory['reason']}")
    if report["strict_findings"]:
        for finding in report["strict_findings"]:
            print(f"ERROR {finding['path']}: {finding['value']} -> {finding['reason']}")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
