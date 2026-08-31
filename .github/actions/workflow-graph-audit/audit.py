#!/usr/bin/env python3
"""Lexical GitHub Actions workflow graph audit.

This is intentionally not a full YAML semantic parser. It extracts auditable
workflow topology using a bounded grammar and never promotes lexical success to
security/compliance certification.
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
USES = re.compile(r"^\s*(?:-\s*)?uses:\s*['\"]?([^'\"\s#]+)")
NAME = re.compile(r"^name:\s*(.+?)\s*$")
JOB = re.compile(r"^  ([A-Za-z0-9_-]+):\s*$")
NEEDS = re.compile(r"^\s{4}needs:\s*(.+?)\s*$")
RUNS_ON = re.compile(r"^\s{4}runs-on:\s*(.+?)\s*$")
ADDED_USES = re.compile(r"^\+\s*(?:-\s*)?uses:\s*['\"]?([^'\"\s#]+)")

TRIGGERS = (
    "push", "pull_request", "pull_request_target", "workflow_dispatch",
    "workflow_call", "workflow_run", "repository_dispatch", "schedule",
    "issues", "issue_comment", "release",
)


def classify_use(value: str) -> dict:
    if value.startswith("./"):
        return {"value": value, "kind": "local", "immutable": True, "ref": None}
    if value.startswith("docker://"):
        return {"value": value, "kind": "docker", "immutable": False, "ref": None}
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


def parse_workflow(path: Path, root: Path) -> dict:
    text = path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    workflow_name = path.name
    triggers: set[str] = set()
    uses: list[dict] = []
    jobs: dict[str, dict] = {}
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
                    jobs.setdefault(current_job, {"needs": [], "runs_on": None, "uses": []})
        m_needs = NEEDS.match(line)
        if m_needs and current_job:
            jobs[current_job]["needs"] = scalar_or_list(m_needs.group(1))
        m_ro = RUNS_ON.match(line)
        if m_ro and current_job:
            jobs[current_job]["runs_on"] = m_ro.group(1).strip("'\"")
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
    timeout_jobs = sum(1 for line in lines if re.match(r"^\s{4}timeout-minutes:", line))

    edges = []
    for job_id, meta in jobs.items():
        for dep in meta["needs"]:
            edges.append({"from": dep, "to": job_id, "type": "needs"})

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
        "jobs_with_timeout_observed": timeout_jobs,
        "risk_markers": {
            "pull_request_target": "pull_request_target" in triggers,
            "mutable_uses_count": sum(1 for u in uses if u["kind"] in {"remote_action", "reusable_workflow", "unversioned"} and not u["immutable"]),
        },
    }


def changed_mutable_uses(root: Path, base_ref: str) -> list[dict]:
    if not base_ref:
        return []
    proc = subprocess.run(
        ["git", "-C", str(root), "diff", "--unified=0", "--no-color", base_ref, "HEAD", "--", ".github/workflows", ".github/actions"],
        capture_output=True, text=True, check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError((proc.stderr or proc.stdout).strip() or "git diff failed")
    findings = []
    current = "TOKEN_VAZIO_PATH"
    for line in proc.stdout.splitlines():
        if line.startswith("+++ b/"):
            current = line[6:]
            continue
        # Ratchet only YAML-bearing GitHub Actions definitions. The diff scope
        # intentionally includes .github/actions so composite action.yml files
        # are checked, while implementation files such as audit.py cannot be
        # misclassified by coincidental source text like `uses: list[dict]`.
        if not current.lower().endswith((".yml", ".yaml")):
            continue
        m = ADDED_USES.match(line)
        if not m:
            continue
        dep = classify_use(m.group(1))
        if dep["kind"] in {"local", "docker"}:
            continue
        if not dep["immutable"]:
            findings.append({"path": current, "uses": dep["value"], "reason": "NEW_MUTABLE_OR_UNVERSIONED_DEPENDENCY"})
    return findings


def build_report(root: Path, mode: str, base_ref: str) -> dict:
    wf_dir = root / ".github" / "workflows"
    paths = sorted(list(wf_dir.glob("*.yml")) + list(wf_dir.glob("*.yaml"))) if wf_dir.exists() else []
    workflows = [parse_workflow(p, root) for p in paths]
    names = {}
    for w in workflows:
        names.setdefault(w["name"], []).append(w["path"])
    duplicate_names = {k: v for k, v in names.items() if len(v) > 1}
    changed = changed_mutable_uses(root, base_ref) if mode == "strict-changed" else []
    return {
        "schema": "rafgittools.workflow_graph_audit.v1",
        "analysis_kind": "LEXICAL_STRUCTURAL_INVENTORY_NOT_FULL_YAML_AST",
        "claim_allowed": False,
        "mode": mode,
        "base_ref": base_ref or "TOKEN_VAZIO",
        "summary": {
            "workflow_count": len(workflows),
            "job_count": sum(len(w["jobs"]) for w in workflows),
            "dependency_count": sum(len(w["uses"]) for w in workflows),
            "mutable_dependency_count": sum(w["risk_markers"]["mutable_uses_count"] for w in workflows),
            "pull_request_target_workflow_count": sum(1 for w in workflows if w["risk_markers"]["pull_request_target"]),
            "explicit_permissions_workflow_count": sum(1 for w in workflows if w["top_level_permissions_explicit"]),
            "concurrency_workflow_count": sum(1 for w in workflows if w["concurrency_explicit"]),
            "duplicate_workflow_names": duplicate_names,
            "new_mutable_dependency_findings": len(changed),
        },
        "changed_dependency_findings": changed,
        "workflows": workflows,
        "invariants": [
            "LEXICAL_PARSE != FULL_YAML_SEMANTICS",
            "CI_SUCCESS != SERVER_SIDE_ENFORCEMENT",
            "MUTABLE_REF != IMMUTABLE_SUPPLY_CHAIN",
            "TOKEN_VAZIO != PASS",
            "NO_FINDING != NO_RISK",
        ],
    }


def self_test() -> int:
    with tempfile.TemporaryDirectory() as td:
        root = Path(td)
        wf = root / ".github" / "workflows"
        wf.mkdir(parents=True)
        (wf / "x.yml").write_text("""name: X\non:\n  pull_request:\npermissions:\n  contents: read\njobs:\n  a:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683\n  b:\n    needs: a\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/setup-python@v5\n""", encoding="utf-8")
        r = build_report(root, "report", "")
        assert r["summary"]["workflow_count"] == 1
        assert r["summary"]["job_count"] == 2
        assert r["summary"]["mutable_dependency_count"] == 1
        assert r["workflows"][0]["job_edges"] == [{"from": "a", "to": "b", "type": "needs"}]
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
        s = report["summary"]
        print("WORKFLOW_GRAPH_AUDIT")
        for k, v in s.items():
            print(f"{k}={json.dumps(v, sort_keys=True)}")
    if report["changed_dependency_findings"]:
        for f in report["changed_dependency_findings"]:
            print(f"ERROR {f['path']}: {f['uses']} -> {f['reason']}")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
