#!/usr/bin/env python3
"""
RafGitTools Forensic Git Provenance Mode V1.

Read-only collector and conservative audit engine for local Git repositories.
It never mutates the target repository, never accesses the network, and never
promotes anomaly scores into allegations of fraud, plagiarism, censorship, or
malicious intent.

Python: 3.9+
Dependencies: standard library + local `git` executable for `collect`.
"""

from __future__ import annotations

import argparse
import copy
import datetime as dt
import hashlib
import json
import os
import pathlib
import platform
import re
import subprocess
import sys
from typing import Any, Dict, Iterable, List, Mapping, Optional, Sequence, Tuple

SCHEMA_VERSION = "rafgittools.forensic-git-evidence-run/v1"
REPORT_VERSION = "rafgittools.forensic-git-report/v1"
TOOL_VERSION = "1.0.0"
SHA1_RE = re.compile(r"^[0-9a-f]{40}$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")

CLAIM_PROVADO = "PROVADO"
CLAIM_EVIDENCIADO = "EVIDENCIADO"
CLAIM_HIPOTESE = "HIPOTESE"
CLAIM_REFUTADO = "REFUTADO"
CLAIM_TOKEN_VAZIO = "TOKEN_VAZIO"

DEFAULT_WEIGHTS: Dict[str, float] = {
    "T": 1.0,
    "G": 1.0,
    "A": 1.0,
    "F": 1.0,
    "C": 1.0,
    "X": 1.0,
    "E": 1.0,
}


class ForensicError(RuntimeError):
    """Raised for invalid input or failed read-only collection."""


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def parse_time(value: Optional[str]) -> Optional[dt.datetime]:
    if not value:
        return None
    raw = value.strip()
    if raw.endswith("Z"):
        raw = raw[:-1] + "+00:00"
    try:
        parsed = dt.datetime.fromisoformat(raw)
    except ValueError as exc:
        raise ForensicError(f"invalid ISO-8601 timestamp: {value!r}") from exc
    if parsed.tzinfo is None:
        raise ForensicError(f"timestamp must include timezone: {value!r}")
    return parsed.astimezone(dt.timezone.utc)


def canonical_json_bytes(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def sha256_hex(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def redact_email(email: str) -> str:
    """Preserve a stable fingerprint without exporting the mailbox."""
    if not email:
        return ""
    return f"sha256:{sha256_hex(email.strip().lower().encode('utf-8'))}"


def _run_git(repo: pathlib.Path, args: Sequence[str], *, stdin: Optional[bytes] = None, timeout: int = 60) -> bytes:
    env = os.environ.copy()
    env["GIT_NO_REPLACE_OBJECTS"] = "1"
    env["LC_ALL"] = "C"
    cmd = ["git", "-C", str(repo), *args]
    try:
        completed = subprocess.run(
            cmd,
            input=stdin,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
            timeout=timeout,
            env=env,
        )
    except FileNotFoundError as exc:
        raise ForensicError("git executable not found") from exc
    except subprocess.TimeoutExpired as exc:
        raise ForensicError(f"git command timed out: {' '.join(cmd)}") from exc
    if completed.returncode != 0:
        stderr = completed.stderr.decode("utf-8", "replace").strip()
        raise ForensicError(f"git command failed ({completed.returncode}): {' '.join(cmd)}: {stderr}")
    return completed.stdout


def _git_text(repo: pathlib.Path, args: Sequence[str], *, timeout: int = 60) -> str:
    return _run_git(repo, args, timeout=timeout).decode("utf-8", "replace").strip()


def _validate_repository(repo: pathlib.Path) -> None:
    if not repo.exists() or not repo.is_dir():
        raise ForensicError(f"repository path is not a directory: {repo}")
    if _git_text(repo, ["rev-parse", "--is-inside-work-tree"]) != "true":
        raise ForensicError(f"not a Git work tree: {repo}")


def _resolve_commits(repo: pathlib.Path, revision: str, max_commits: int) -> List[str]:
    if max_commits < 1:
        raise ForensicError("max_commits must be >= 1")
    output = _git_text(repo, ["rev-list", "--reverse", "--topo-order", f"--max-count={max_commits}", revision])
    commits = [line for line in output.splitlines() if line]
    if not commits:
        raise ForensicError(f"revision resolved to no commits: {revision}")
    return commits


def _parse_commit_metadata(repo: pathlib.Path, sha: str) -> Dict[str, Any]:
    fmt = (
        "%H%x00%P%x00%T%x00%an%x00%ae%x00%aI%x00"
        "%cn%x00%ce%x00%cI%x00%G?%x00%GS%x00%GK%x00%B"
    )
    raw = _run_git(repo, ["show", "-s", f"--format={fmt}", sha])
    parts = raw.split(b"\x00", 12)
    if len(parts) != 13:
        raise ForensicError(f"unexpected git show metadata field count for {sha}: {len(parts)}")
    decoded = [p.decode("utf-8", "replace") for p in parts]
    (
        commit_sha,
        parents_text,
        tree_sha,
        author_name,
        author_email,
        author_date,
        committer_name,
        committer_email,
        committer_date,
        signature_status,
        signer,
        signing_key,
        message,
    ) = decoded
    parents = [p for p in parents_text.split() if p]
    parent_trees = [_git_text(repo, ["rev-parse", f"{parent}^{{tree}}"] ) for parent in parents]
    refs_text = _git_text(repo, ["for-each-ref", "--contains", sha, "--format=%(refname)"])
    refs = sorted({line for line in refs_text.splitlines() if line})

    files_text = _git_text(
        repo,
        ["diff-tree", "--root", "--no-commit-id", "--name-only", "-r", "--first-parent", sha],
    )
    files = sorted({line for line in files_text.splitlines() if line})
    empty_against_first_parent = bool(parents and tree_sha == parent_trees[0])
    empty_root = not parents and not files

    diff_bytes = _run_git(repo, ["show", "--format=", "--binary", "--first-parent", sha], timeout=120)
    patch_id: Optional[str] = None
    if diff_bytes.strip():
        patch_out = _run_git(repo, ["patch-id", "--stable"], stdin=diff_bytes)
        patch_line = patch_out.decode("ascii", "replace").strip().split()
        if patch_line and SHA1_RE.fullmatch(patch_line[0]):
            patch_id = patch_line[0]

    return {
        "sha": commit_sha,
        "parents": parents,
        "tree": tree_sha,
        "parent_trees": parent_trees,
        "author": {
            "name": author_name,
            "email_fingerprint": redact_email(author_email),
            "date": author_date,
        },
        "committer": {
            "name": committer_name,
            "email_fingerprint": redact_email(committer_email),
            "date": committer_date,
        },
        "signature": {
            "git_status": signature_status or "N",
            "signer": signer or None,
            "key_id": signing_key or None,
        },
        "message": message.rstrip("\n"),
        "refs_containing": refs,
        "diff": {
            "files_changed": len(files),
            "files": files,
            "patch_id_stable": patch_id,
            "empty_against_first_parent": empty_against_first_parent,
            "empty_root": empty_root,
        },
        "platform": None,
        "external_receipt": None,
        "agent": None,
    }


def collect_repository(
    repository_path: str,
    revision: str = "HEAD",
    max_commits: int = 500,
    *,
    include_path: bool = False,
    generated_at: Optional[str] = None,
) -> Dict[str, Any]:
    """Collect read-only local Git evidence with replacement refs disabled."""
    repo = pathlib.Path(repository_path).expanduser().resolve()
    _validate_repository(repo)
    commits = _resolve_commits(repo, revision, max_commits)
    git_version = _git_text(repo, ["--version"])
    top = pathlib.Path(_git_text(repo, ["rev-parse", "--show-toplevel"])).resolve()
    common_dir = _git_text(repo, ["rev-parse", "--git-common-dir"])
    repo_identity: Dict[str, Any] = {
        "repository_name": top.name,
        "repository_path_sha256": sha256_hex(str(top).encode("utf-8")),
        "git_common_dir_sha256": sha256_hex(str((top / common_dir).resolve()).encode("utf-8")),
    }
    if include_path:
        repo_identity["repository_path"] = str(top)

    evidence = {
        "schema_version": SCHEMA_VERSION,
        "case_id": f"local-{top.name}-{commits[-1][:12]}",
        "generated_at": generated_at or utc_now(),
        "collection_mode": "LOCAL_READ_ONLY_NO_NETWORK",
        "claim_allowed": False,
        "repository": repo_identity,
        "collector": {
            "tool": "RafGitTools Forensic Git Provenance Mode",
            "tool_version": TOOL_VERSION,
            "python": platform.python_version(),
            "platform": platform.platform(),
            "git": git_version,
            "replacement_refs_disabled": True,
            "working_tree_mutated": False,
        },
        "revision": revision,
        "commits": [_parse_commit_metadata(repo, sha) for sha in commits],
        "external_events": [],
        "limitations": [
            "LOCAL_GIT_HAS_NO_AUTHORITATIVE_PUSH_EVENT",
            "LOCAL_REFLOGS_ARE_MACHINE_LOCAL_AND_NOT_COLLECTED_BY_DEFAULT",
            "PLATFORM_COMMENT_EDIT_HISTORY_NOT_AVAILABLE",
            "GITHUB_ACTIONS_LOGS_NOT_AVAILABLE",
            "AI_TASK_RECEIPTS_NOT_AVAILABLE_UNLESS_ENRICHED",
            "NO_INTENT_OR_LEGAL_CONCLUSION_INFERRED",
        ],
    }
    evidence["custody"] = {
        "canonical_input_sha256": sha256_hex(canonical_json_bytes({k: v for k, v in evidence.items() if k != "custody"}))
    }
    return evidence


def validate_evidence(evidence: Mapping[str, Any]) -> None:
    if evidence.get("schema_version") != SCHEMA_VERSION:
        raise ForensicError(f"unsupported schema_version: {evidence.get('schema_version')!r}")
    if evidence.get("claim_allowed") is not False:
        raise ForensicError("claim_allowed must be false")
    commits = evidence.get("commits")
    if not isinstance(commits, list) or not commits:
        raise ForensicError("commits must be a non-empty array")
    seen: set[str] = set()
    for index, commit in enumerate(commits):
        if not isinstance(commit, Mapping):
            raise ForensicError(f"commits[{index}] must be an object")
        sha = commit.get("sha")
        if not isinstance(sha, str) or not SHA1_RE.fullmatch(sha):
            raise ForensicError(f"commits[{index}].sha must be a 40-char lowercase SHA-1")
        if sha in seen:
            raise ForensicError(f"duplicate commit SHA: {sha}")
        seen.add(sha)
        parents = commit.get("parents", [])
        if not isinstance(parents, list) or any(not isinstance(p, str) or not SHA1_RE.fullmatch(p) for p in parents):
            raise ForensicError(f"commits[{index}].parents invalid")
        for person_key in ("author", "committer"):
            person = commit.get(person_key)
            if not isinstance(person, Mapping):
                raise ForensicError(f"commits[{index}].{person_key} missing")
            parse_time(person.get("date"))
        for optional_time in (
            ((commit.get("platform") or {}).get("first_seen_at") if isinstance(commit.get("platform"), Mapping) else None),
            ((commit.get("external_receipt") or {}).get("observed_at") if isinstance(commit.get("external_receipt"), Mapping) else None),
        ):
            parse_time(optional_time)


def _seconds(a: Optional[dt.datetime], b: Optional[dt.datetime]) -> Optional[int]:
    if a is None or b is None:
        return None
    return int((a - b).total_seconds())


def _finding(
    code: str,
    state: str,
    statement: str,
    *,
    vector: str,
    severity: int,
    evidence_refs: Sequence[str],
    falsifier: str,
    limitations: Sequence[str] = (),
) -> Dict[str, Any]:
    return {
        "code": code,
        "state": state,
        "statement": statement,
        "vector": vector,
        "severity": severity,
        "evidence_refs": list(evidence_refs),
        "falsifier": falsifier,
        "limitations": list(limitations),
    }


def _looks_like_agent(identity: str) -> bool:
    lowered = identity.lower()
    tokens = ("copilot", "codex", "claude", "bot", "agent", "dependabot", "github-actions")
    return any(token in lowered for token in tokens)


def analyze_commit(commit: Mapping[str, Any], known_dates: Mapping[str, dt.datetime]) -> Tuple[Dict[str, Any], List[str]]:
    sha = str(commit["sha"])
    ref = f"commit:{sha}"
    author = commit["author"]
    committer = commit["committer"]
    ta = parse_time(author.get("date"))
    tc = parse_time(committer.get("date"))
    platform_obj = commit.get("platform") if isinstance(commit.get("platform"), Mapping) else {}
    receipt_obj = commit.get("external_receipt") if isinstance(commit.get("external_receipt"), Mapping) else {}
    tg = parse_time(platform_obj.get("first_seen_at")) if platform_obj else None
    tr = parse_time(receipt_obj.get("observed_at")) if receipt_obj else None

    deltas = {
        "author_to_committer_seconds": _seconds(tc, ta),
        "committer_to_platform_seconds": _seconds(tg, tc),
        "platform_to_receipt_seconds": _seconds(tr, tg),
    }
    findings: List[Dict[str, Any]] = []
    blind_spots: List[str] = []

    diff = commit.get("diff") if isinstance(commit.get("diff"), Mapping) else {}
    empty = bool(diff.get("empty_against_first_parent") or diff.get("empty_root"))
    if empty:
        findings.append(_finding(
            "EMPTY_COMMIT",
            CLAIM_PROVADO,
            "The commit preserves the first-parent tree (or is an empty root commit).",
            vector="G",
            severity=2,
            evidence_refs=[ref, f"tree:{commit.get('tree')}"],
            falsifier="Show a parent-relative tree change or a file delta omitted by the collector.",
            limitations=["EMPTY_COMMIT_DOES_NOT_ESTABLISH_PURPOSE_OR_INTENT"],
        ))

    author_name = str(author.get("name") or "")
    committer_name = str(committer.get("name") or "")
    agent_obj = commit.get("agent") if isinstance(commit.get("agent"), Mapping) else {}
    agent_named = _looks_like_agent(author_name) or _looks_like_agent(committer_name) or bool(agent_obj.get("producer"))
    if agent_named:
        findings.append(_finding(
            "AGENT_PROVENANCE_INDICATOR",
            CLAIM_EVIDENCIADO,
            "Commit identity or enriched metadata indicates an automated/AI agent.",
            vector="A",
            severity=2,
            evidence_refs=[ref],
            falsifier="Provide authoritative identity metadata showing that the matched agent label is unrelated to production.",
            limitations=["IDENTITY_LABEL_DOES_NOT_REVEAL_PROMPT_CONTEXT_OR_HUMAN_CONTROL"],
        ))
        if not agent_obj.get("task_receipt"):
            findings.append(_finding(
                "AGENT_TASK_RECEIPT_MISSING",
                CLAIM_TOKEN_VAZIO,
                "The exact task, prompt context, model version, and human intervention are not evidenced.",
                vector="E",
                severity=4,
                evidence_refs=[ref],
                falsifier="Attach a task receipt cryptographically or operationally bound to this commit SHA.",
            ))
            blind_spots.append("AI_TASK_RECEIPT")

    if ta and tc and tc < ta:
        findings.append(_finding(
            "COMMITTER_BEFORE_AUTHOR",
            CLAIM_EVIDENCIADO,
            "Committer time precedes author time.",
            vector="T",
            severity=3,
            evidence_refs=[ref],
            falsifier="Demonstrate timezone/parser error or authoritative clock normalization that removes the inversion.",
            limitations=["TIMESTAMP_INVERSION_IS_NOT_PROOF_OF_FRAUD"],
        ))
    delta_ac = deltas["author_to_committer_seconds"]
    if delta_ac is not None and abs(delta_ac) >= 86400:
        findings.append(_finding(
            "AUTHOR_COMMITTER_DELTA_LARGE",
            CLAIM_EVIDENCIADO,
            f"Author and committer clocks differ by {delta_ac} seconds.",
            vector="T",
            severity=2,
            evidence_refs=[ref],
            falsifier="Show a documented import, rebase, patch application, or clock correction explaining the delta.",
            limitations=["LARGE_DELTA_IS_AN_INVESTIGATION_SIGNAL_ONLY"],
        ))

    for parent in commit.get("parents", []):
        parent_date = known_dates.get(parent)
        if parent_date and tc and tc < parent_date:
            findings.append(_finding(
                "CHILD_COMMITTER_TIME_BEFORE_PARENT",
                CLAIM_EVIDENCIADO,
                "Child committer time precedes a collected parent's committer time.",
                vector="T",
                severity=3,
                evidence_refs=[ref, f"commit:{parent}"],
                falsifier="Show that either timestamp is inaccurate or that history was imported with declared clock discontinuity.",
                limitations=["NON_MONOTONIC_TIME_DOES_NOT_REWRITE_DAG_PARENTAGE"],
            ))

    if tg is None:
        blind_spots.append("PLATFORM_FIRST_SEEN_CLOCK")
    elif tc and tg < tc:
        findings.append(_finding(
            "PLATFORM_BEFORE_COMMITTER",
            CLAIM_EVIDENCIADO,
            "Platform first-seen time precedes the declared committer time.",
            vector="T",
            severity=4,
            evidence_refs=[ref],
            falsifier="Provide corrected platform event ordering or commit metadata.",
            limitations=["MAY_INDICATE_CLOCK_OR_IMPORT_EFFECT; NOT AUTOMATICALLY FALSIFICATION"],
        ))

    if tr is None:
        blind_spots.append("INDEPENDENT_RECEIPT_CLOCK")
    elif tg and tr < tg:
        findings.append(_finding(
            "RECEIPT_BEFORE_PLATFORM",
            CLAIM_EVIDENCIADO,
            "Independent receipt time precedes platform first-seen time.",
            vector="T",
            severity=2,
            evidence_refs=[ref],
            falsifier="Show receipt timestamp or platform timestamp is wrong.",
            limitations=["CAN_BE_EXPECTED_WHEN_RECEIPT_PRECEDES_PUSH"],
        ))

    pusher = platform_obj.get("pusher") if platform_obj else None
    if not pusher:
        blind_spots.append("PUSHER_IDENTITY")

    signature = commit.get("signature") if isinstance(commit.get("signature"), Mapping) else {}
    sig_status = signature.get("git_status")
    if sig_status in (None, "", "N"):
        findings.append(_finding(
            "COMMIT_SIGNATURE_ABSENT",
            CLAIM_EVIDENCIADO,
            "No good cryptographic commit signature is present in the collected Git metadata.",
            vector="A",
            severity=1,
            evidence_refs=[ref],
            falsifier="Provide a verifiable signed commit object or platform verification payload for this SHA.",
            limitations=["UNSIGNED_DOES_NOT_MEAN_UNAUTHENTIC"],
        ))
    elif sig_status not in ("G", "U", "Y", "R"):
        findings.append(_finding(
            "COMMIT_SIGNATURE_NOT_GOOD",
            CLAIM_EVIDENCIADO,
            f"Git signature status is {sig_status!r}, not a good/valid status.",
            vector="A",
            severity=3,
            evidence_refs=[ref],
            falsifier="Verify the commit with the correct keyring and record the verification payload.",
        ))

    if diff.get("patch_id_stable") is None and not empty:
        blind_spots.append("PATCH_ID_STABLE")

    findings.extend([
        _finding(
            "DELIBERATE_DATE_SHIFT",
            CLAIM_TOKEN_VAZIO,
            "Deliberate timestamp manipulation is not demonstrated by Git clocks alone.",
            vector="E",
            severity=0,
            evidence_refs=[ref],
            falsifier="Provide causally linked records showing intentional timestamp alteration and purpose.",
        ),
        _finding(
            "HISTORY_FALSIFICATION",
            CLAIM_TOKEN_VAZIO,
            "History falsification is not demonstrated by the collected local object metadata.",
            vector="E",
            severity=0,
            evidence_refs=[ref],
            falsifier="Provide a prior authoritative object/ref snapshot and a verified contradictory rewritten state.",
        ),
        _finding(
            "MALICIOUS_COORDINATION",
            CLAIM_TOKEN_VAZIO,
            "Coordination, intent, and benefit are not established.",
            vector="E",
            severity=0,
            evidence_refs=[ref],
            falsifier="Provide independent communications, access records, or other causal evidence.",
        ),
    ])

    unique_blind_spots = sorted(set(blind_spots))
    return {
        "sha": sha,
        "clocks": {
            "author": author.get("date"),
            "committer": committer.get("date"),
            "platform_first_seen": platform_obj.get("first_seen_at") if platform_obj else None,
            "external_receipt": receipt_obj.get("observed_at") if receipt_obj else None,
            "deltas": deltas,
        },
        "findings": findings,
        "blind_spots": unique_blind_spots,
    }, unique_blind_spots


def _score(findings: Iterable[Mapping[str, Any]], weights: Mapping[str, float]) -> Dict[str, Any]:
    by_vector = {key: 0.0 for key in DEFAULT_WEIGHTS}
    counted_codes: List[str] = []
    for finding in findings:
        state = finding.get("state")
        severity = int(finding.get("severity", 0))
        vector = str(finding.get("vector", "E"))
        if state == CLAIM_TOKEN_VAZIO or severity <= 0:
            continue
        if vector not in by_vector:
            vector = "E"
        by_vector[vector] += severity * float(weights.get(vector, 1.0))
        counted_codes.append(str(finding.get("code")))
    return {
        "purpose": "PRIORITIZATION_ONLY",
        "not_for": ["GUILT", "LEGAL_ATTRIBUTION", "RETALIATION", "AUTOMATIC_SANCTION"],
        "by_vector": by_vector,
        "total": round(sum(by_vector.values()), 3),
        "counted_findings": counted_codes,
    }


def audit_evidence(
    evidence: Mapping[str, Any],
    *,
    previous_event_hash: Optional[str] = None,
    generated_at: Optional[str] = None,
    weights: Optional[Mapping[str, float]] = None,
) -> Dict[str, Any]:
    validate_evidence(evidence)
    evidence_copy = copy.deepcopy(dict(evidence))
    input_hash = sha256_hex(canonical_json_bytes(evidence_copy))
    known_dates: Dict[str, dt.datetime] = {}
    for commit in evidence_copy["commits"]:
        tc = parse_time(commit["committer"].get("date"))
        if tc:
            known_dates[commit["sha"]] = tc

    commit_reports: List[Dict[str, Any]] = []
    all_findings: List[Dict[str, Any]] = []
    blind_spots: set[str] = set()
    for commit in evidence_copy["commits"]:
        commit_report, commit_blind = analyze_commit(commit, known_dates)
        commit_reports.append(commit_report)
        all_findings.extend(commit_report["findings"])
        blind_spots.update(commit_blind)

    for limitation in evidence_copy.get("limitations", []):
        blind_spots.add(str(limitation))

    active_weights = dict(DEFAULT_WEIGHTS)
    if weights:
        for key, value in weights.items():
            if key not in active_weights:
                raise ForensicError(f"unknown anomaly vector weight: {key}")
            active_weights[key] = float(value)

    score = _score(all_findings, active_weights)
    generated = generated_at or utc_now()
    parse_time(generated)
    event_preimage: Dict[str, Any] = {
        "report_version": REPORT_VERSION,
        "tool_version": TOOL_VERSION,
        "case_id": evidence_copy.get("case_id"),
        "generated_at": generated,
        "claim_allowed": False,
        "automatic_attribution": False,
        "automatic_retaliation": False,
        "input_sha256": input_hash,
        "previous_event_hash": previous_event_hash,
        "repository": evidence_copy.get("repository"),
        "commit_reports": commit_reports,
        "blind_spots": sorted(blind_spots),
        "anomaly_score": score,
        "verdict": {
            "status": "PASS_LIMITED_CONSERVATIVE_AUDIT",
            "anomaly_is_not_intent": True,
            "git_date_is_not_physical_receipt": True,
            "current_refs_are_not_complete_history": True,
            "claim_boundary": "NO_FRAUD_CENSORSHIP_PLAGIARISM_OR_MALICE_CLAIM_FROM_SCORE",
        },
        "f_ok": [
            "LOCAL_GIT_OBJECTS_NORMALIZED",
            "FOUR_CLOCK_MODEL_APPLIED_WHERE_DATA_EXISTS",
            "EMPTY_COMMITS_AND_PROVENANCE_GAPS_CLASSIFIED",
            "HASH_CHAIN_EVENT_CREATED",
        ],
        "f_gap": sorted(blind_spots),
        "f_next": [
            "ENRICH_WITH_PLATFORM_PUSH_PR_REVIEW_AND_WORKFLOW_EVENTS",
            "PRESERVE_REFS_PULL_RELEASE_ASSETS_AND_EXTERNAL_RECEIPTS",
            "COMPARE_PATCH_ID_RANGE_DIFF_AND_FORK_GRAPH",
            "BIND_AI_TASK_RECEIPTS_TO_EXACT_COMMIT_SHA",
        ],
    }
    event_hash = sha256_hex(canonical_json_bytes(event_preimage))
    report = dict(event_preimage)
    report["event_hash"] = event_hash
    report["custody"] = {
        "algorithm": "SHA-256",
        "input_sha256": input_hash,
        "previous_event_hash": previous_event_hash,
        "event_hash": event_hash,
        "canonicalization": "UTF-8 JSON sorted keys compact separators",
    }
    return report


def _read_json(path: pathlib.Path) -> Dict[str, Any]:
    try:
        with path.open("r", encoding="utf-8") as handle:
            value = json.load(handle)
    except (OSError, json.JSONDecodeError) as exc:
        raise ForensicError(f"cannot read JSON {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ForensicError(f"top-level JSON must be an object: {path}")
    return value


def _write_json(path: pathlib.Path, value: Mapping[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    try:
        path.write_text(payload, encoding="utf-8")
    except OSError as exc:
        raise ForensicError(f"cannot write JSON {path}: {exc}") from exc


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="forensic_git.py",
        description="Read-only Git provenance collection and conservative forensic audit.",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    collect = sub.add_parser("collect", help="Collect local Git evidence without network or repository mutation.")
    collect.add_argument("--repo", required=True, help="Local Git work tree.")
    collect.add_argument("--revision", default="HEAD", help="Revision/range accepted by git rev-list.")
    collect.add_argument("--max-commits", type=int, default=500)
    collect.add_argument("--output", required=True)
    collect.add_argument("--include-path", action="store_true", help="Include raw local path; default stores only SHA-256.")
    collect.add_argument("--generated-at", help="Deterministic ISO-8601 timestamp for reproducible fixtures.")

    audit = sub.add_parser("audit", help="Audit a collected/enriched evidence JSON.")
    audit.add_argument("--input", required=True)
    audit.add_argument("--output", required=True)
    audit.add_argument("--previous-event-hash")
    audit.add_argument("--generated-at", help="Deterministic ISO-8601 timestamp for reproducible receipts.")

    verify = sub.add_parser("verify", help="Recompute and verify report event hash.")
    verify.add_argument("--input", required=True)

    return parser


def verify_report(report: Mapping[str, Any]) -> bool:
    expected = report.get("event_hash")
    if not isinstance(expected, str) or not SHA256_RE.fullmatch(expected):
        raise ForensicError("report event_hash missing or invalid")
    preimage = dict(report)
    preimage.pop("event_hash", None)
    preimage.pop("custody", None)
    actual = sha256_hex(canonical_json_bytes(preimage))
    return actual == expected


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    try:
        if args.command == "collect":
            evidence = collect_repository(
                args.repo,
                args.revision,
                args.max_commits,
                include_path=args.include_path,
                generated_at=args.generated_at,
            )
            _write_json(pathlib.Path(args.output), evidence)
            print(json.dumps({
                "status": "PASS_LOCAL_READ_ONLY",
                "commits": len(evidence["commits"]),
                "output": args.output,
                "claim_allowed": False,
            }, sort_keys=True))
            return 0
        if args.command == "audit":
            evidence = _read_json(pathlib.Path(args.input))
            report = audit_evidence(
                evidence,
                previous_event_hash=args.previous_event_hash,
                generated_at=args.generated_at,
            )
            _write_json(pathlib.Path(args.output), report)
            print(json.dumps({
                "status": report["verdict"]["status"],
                "event_hash": report["event_hash"],
                "output": args.output,
                "claim_allowed": False,
            }, sort_keys=True))
            return 0
        if args.command == "verify":
            report = _read_json(pathlib.Path(args.input))
            ok = verify_report(report)
            print(json.dumps({"status": "PASS" if ok else "FAIL", "event_hash_valid": ok}, sort_keys=True))
            return 0 if ok else 2
        parser.error("unknown command")
        return 2
    except ForensicError as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc), "claim_allowed": False}, ensure_ascii=False), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
