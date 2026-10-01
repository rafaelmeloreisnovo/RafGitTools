#!/usr/bin/env python3
"""RAFAELIA Execution Convoy V1.

A fail-closed control-plane executor for bounded, typed workgroups.
It intentionally does not execute arbitrary shell supplied by a plan.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import time
from pathlib import Path
from typing import Any

SCHEMA = "rafaelia.execution-convoy.v1"
RECEIPT_SCHEMA = "rafaelia.execution-convoy-receipt.v1"
CHECKPOINT_SCHEMA = "rafaelia.execution-convoy-checkpoint.v1"
FINAL_SCHEMA = "rafaelia.execution-convoy-result.v1"
ALLOWED_MUTATION = {"READ_ONLY", "TEST"}
ALLOWED_OPS = {"ASSERT_FILE", "ASSERT_JSON", "HASH_FILE", "UNITTEST_DISCOVERY"}
PASS_STATES = {"PASS", "SKIPPED_RESUME"}

class ConvoyError(RuntimeError):
    pass

class IntegrityError(ConvoyError):
    pass


def canonical_bytes(obj: Any) -> bytes:
    return json.dumps(obj, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def file_digests(path: Path) -> dict[str, str]:
    data = path.read_bytes()
    out = {"sha256": sha256_bytes(data)}
    try:
        import blake3  # type: ignore
    except Exception:
        out["blake3"] = "TOKEN_VAZIO_UNAVAILABLE"
    else:
        out["blake3"] = blake3.blake3(data).hexdigest()
    return out


def safe_repo_path(root: Path, raw: str) -> Path:
    if not raw or os.path.isabs(raw):
        raise ConvoyError(f"unsafe path: {raw!r}")
    root = root.resolve()
    target = (root / raw).resolve()
    if target != root and root not in target.parents:
        raise ConvoyError(f"path escapes repository root: {raw!r}")
    return target


def leverage(stage: dict[str, Any]) -> float:
    h = stage.get("heuristic", {})
    vals = [h.get(k) for k in ("U", "R", "D", "A", "E", "C")]
    if any(not isinstance(v, int) for v in vals):
        return -1.0
    U, R, D, A, E, C = vals
    if not all(0 <= v <= 5 for v in (U, R, D, A, E)) or not 1 <= C <= 5:
        return -1.0
    return ((U + R + D + A) * E) / C


def validate_plan(plan: dict[str, Any]) -> None:
    if plan.get("schema") != SCHEMA:
        raise ConvoyError("unsupported plan schema")
    if plan.get("claim_allowed") is not False:
        raise ConvoyError("claim_allowed must be false in V1")
    stages = plan.get("stages")
    if not isinstance(stages, list) or not stages:
        raise ConvoyError("stages must be a non-empty list")
    ids: list[str] = []
    for s in stages:
        if not isinstance(s, dict):
            raise ConvoyError("stage must be object")
        sid = s.get("id")
        if not isinstance(sid, str) or not sid or sid in ids:
            raise ConvoyError("stage ids must be unique non-empty strings")
        ids.append(sid)
        if s.get("mutation_level") not in ALLOWED_MUTATION:
            raise ConvoyError(f"stage {sid}: mutation level not allowed in V1")
        if s.get("operation") not in ALLOWED_OPS:
            raise ConvoyError(f"stage {sid}: unsupported operation")
        needs = s.get("needs", [])
        if not isinstance(needs, list) or any(not isinstance(x, str) for x in needs):
            raise ConvoyError(f"stage {sid}: needs must be string list")
        if leverage(s) < 0:
            raise ConvoyError(f"stage {sid}: heuristic is ungrounded or invalid")
    idset = set(ids)
    for s in stages:
        for dep in s.get("needs", []):
            if dep not in idset:
                raise ConvoyError(f"stage {s['id']}: unknown dependency {dep}")
    # Cycle detection.
    temp: set[str] = set()
    perm: set[str] = set()
    by_id = {s["id"]: s for s in stages}
    def visit(node: str) -> None:
        if node in perm:
            return
        if node in temp:
            raise ConvoyError("dependency cycle detected")
        temp.add(node)
        for dep in by_id[node].get("needs", []):
            visit(dep)
        temp.remove(node)
        perm.add(node)
    for sid in ids:
        visit(sid)


def stage_order(plan: dict[str, Any]) -> list[str]:
    validate_plan(plan)
    remaining = {s["id"]: s for s in plan["stages"]}
    done: set[str] = set()
    order: list[str] = []
    while remaining:
        ready = [s for s in remaining.values() if set(s.get("needs", [])) <= done]
        if not ready:
            raise ConvoyError("no ready stage")
        ready.sort(key=lambda s: (-leverage(s), s["id"]))
        chosen = ready[0]
        order.append(chosen["id"])
        done.add(chosen["id"])
        del remaining[chosen["id"]]
    return order


def _emit(event: str, **fields: Any) -> None:
    payload = {"event": event, **fields}
    print("CONVOY " + json.dumps(payload, sort_keys=True, separators=(",", ":")), flush=True)


def _execute_stage(root: Path, stage: dict[str, Any]) -> tuple[str, dict[str, Any], list[str]]:
    op = stage["operation"]
    cfg = stage.get("config", {})
    artifacts: dict[str, Any] = {}
    evidence: list[str] = []
    if op in {"ASSERT_FILE", "ASSERT_JSON", "HASH_FILE"}:
        path = safe_repo_path(root, str(cfg.get("path", "")))
        if not path.is_file():
            return "FAIL", {"path": str(cfg.get("path", "")), "reason": "FILE_MISSING"}, evidence
        dig = file_digests(path)
        artifacts = {"path": str(path.relative_to(root.resolve())), "bytes": path.stat().st_size, "digests": dig}
        if op == "ASSERT_JSON":
            try:
                json.loads(path.read_text(encoding="utf-8"))
            except Exception as exc:
                return "FAIL", {**artifacts, "reason": "INVALID_JSON", "detail": type(exc).__name__}, evidence
            evidence.append("JSON_PARSE_PASS")
        elif op == "ASSERT_FILE":
            evidence.append("FILE_PRESENT")
        else:
            evidence.append("DIGEST_COMPUTED")
        return "PASS", artifacts, evidence
    if op == "UNITTEST_DISCOVERY":
        start_dir_raw = str(cfg.get("start_dir", "tests"))
        pattern = str(cfg.get("pattern", "test_*.py"))
        start_dir = safe_repo_path(root, start_dir_raw)
        if not start_dir.is_dir() or "/" in pattern or "\\" in pattern:
            return "FAIL", {"reason": "UNSAFE_OR_MISSING_TEST_TARGET"}, evidence
        argv = [sys.executable, "-m", "unittest", "discover", "-s", str(start_dir), "-p", pattern, "-v"]
        cp = subprocess.run(argv, cwd=root, shell=False, text=True, capture_output=True, timeout=int(cfg.get("timeout_seconds", 120)))
        combined = (cp.stdout or "") + (cp.stderr or "")
        artifacts = {"returncode": cp.returncode, "stdout_sha256": sha256_bytes(combined.encode("utf-8")), "argv": ["python", "-m", "unittest", "discover", "-s", start_dir_raw, "-p", pattern, "-v"]}
        evidence.append("UNITTEST_PROCESS_OBSERVED")
        return ("PASS" if cp.returncode == 0 else "FAIL"), artifacts, evidence
    raise ConvoyError(f"unreachable operation: {op}")


def _receipt_payload(receipt: dict[str, Any]) -> dict[str, Any]:
    return {k: v for k, v in receipt.items() if k != "receipt_sha256"}


def verify_receipt_chain(receipt_dir: Path) -> tuple[list[dict[str, Any]], str]:
    receipts: list[dict[str, Any]] = []
    previous = "GENESIS"
    for path in sorted(receipt_dir.glob("*.receipt.json")):
        obj = json.loads(path.read_text(encoding="utf-8"))
        expected = sha256_bytes(canonical_bytes(_receipt_payload(obj)))
        if obj.get("receipt_sha256") != expected:
            raise IntegrityError(f"receipt digest mismatch: {path.name}")
        if obj.get("previous_receipt_sha256") != previous:
            raise IntegrityError(f"receipt predecessor mismatch: {path.name}")
        previous = expected
        receipts.append(obj)
    return receipts, previous


def execute_plan(plan_path: Path, root: Path, output_dir: Path, source_sha: str, *, resume: bool = False, retry_failed: bool = False) -> dict[str, Any]:
    root = root.resolve()
    plan = json.loads(plan_path.read_text(encoding="utf-8"))
    validate_plan(plan)
    plan_sha = sha256_bytes(canonical_bytes(plan))
    output_dir.mkdir(parents=True, exist_ok=True)
    receipt_dir = output_dir / "receipts"
    receipt_dir.mkdir(parents=True, exist_ok=True)
    checkpoint_path = output_dir / "checkpoint.json"
    existing: dict[str, str] = {}
    previous = "GENESIS"
    seq = 0
    if resume:
        receipts, previous = verify_receipt_chain(receipt_dir)
        seq = len(receipts)
        for r in receipts:
            if r.get("plan_sha256") != plan_sha or r.get("source_sha") != source_sha:
                raise IntegrityError("resume source/plan identity mismatch")
            existing[r["stage_id"]] = r["state"]
        # Recovery is append-only: prior FAIL/BLOCKED receipts remain in the chain,
        # but their latest states are reopened for one bounded retry pass.
        if retry_failed:
            for sid in [sid for sid, state in existing.items() if state in {"FAIL", "BLOCKED"}]:
                del existing[sid]
    elif any(receipt_dir.glob("*.receipt.json")):
        raise ConvoyError("receipt directory is not empty; use --resume or a fresh output directory")

    stages = {s["id"]: s for s in plan["stages"]}
    status: dict[str, str] = dict(existing)
    executed: list[str] = []

    while len(status) < len(stages) or (retry_failed and any(v == "FAIL" for v in status.values())):
        candidates: list[dict[str, Any]] = []
        progressed = False
        for sid, stage in stages.items():
            current = status.get(sid)
            if current in {"PASS", "BLOCKED"}:
                continue
            if current == "FAIL" and not retry_failed:
                continue
            deps = stage.get("needs", [])
            dep_states = [status.get(dep) for dep in deps]
            if any(ds in {"FAIL", "BLOCKED"} for ds in dep_states):
                status[sid] = "BLOCKED"
                progressed = True
                seq += 1
                receipt = {
                    "schema": RECEIPT_SCHEMA, "seq": seq, "stage_id": sid, "state": "BLOCKED",
                    "reason": "DEPENDENCY_NOT_PASS", "previous_receipt_sha256": previous,
                    "plan_sha256": plan_sha, "source_sha": source_sha, "operation": stage["operation"],
                    "claim_allowed": False, "artifacts": {}, "evidence": []
                }
                digest = sha256_bytes(canonical_bytes(receipt)); receipt["receipt_sha256"] = digest
                (receipt_dir / f"{seq:04d}-{sid}.receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True)+"\n", encoding="utf-8")
                previous = digest
                _emit("stage", seq=seq, stage=sid, state="BLOCKED", receipt_sha256=digest)
                continue
            if all(ds == "PASS" for ds in dep_states):
                candidates.append(stage)
        if not candidates:
            if progressed:
                continue
            unresolved = [sid for sid in stages if sid not in status]
            if unresolved:
                raise ConvoyError(f"unresolved stages: {unresolved}")
            break
        candidates.sort(key=lambda s: (-leverage(s), s["id"]))
        stage = candidates[0]
        sid = stage["id"]
        _emit("stage_start", stage=sid, score=leverage(stage), operation=stage["operation"])
        t0 = time.monotonic_ns()
        try:
            state, artifacts, evidence = _execute_stage(root, stage)
        except (ConvoyError, OSError, subprocess.SubprocessError) as exc:
            state = "FAIL"
            artifacts = {"reason": type(exc).__name__, "detail": str(exc)}
            evidence = []
        elapsed = time.monotonic_ns() - t0
        status[sid] = state
        executed.append(sid)
        seq += 1
        receipt = {
            "schema": RECEIPT_SCHEMA, "seq": seq, "stage_id": sid, "state": state,
            "previous_receipt_sha256": previous, "plan_sha256": plan_sha, "source_sha": source_sha,
            "operation": stage["operation"], "mutation_level": stage["mutation_level"],
            "frontier_leverage_score": leverage(stage), "elapsed_monotonic_ns": elapsed,
            "claim_allowed": False, "artifacts": artifacts, "evidence": evidence,
        }
        digest = sha256_bytes(canonical_bytes(receipt)); receipt["receipt_sha256"] = digest
        (receipt_dir / f"{seq:04d}-{sid}.receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True)+"\n", encoding="utf-8")
        previous = digest
        _emit("stage", seq=seq, stage=sid, state=state, receipt_sha256=digest)
        checkpoint = {
            "schema": CHECKPOINT_SCHEMA, "plan_sha256": plan_sha, "source_sha": source_sha,
            "last_receipt_sha256": previous, "states": status, "claim_allowed": False
        }
        checkpoint_path.write_text(json.dumps(checkpoint, indent=2, sort_keys=True)+"\n", encoding="utf-8")
        if state == "FAIL":
            retry_failed = False

    final_state = "PASS_LIMITED" if all(v == "PASS" for v in status.values()) else "FAIL"
    result = {
        "schema": FINAL_SCHEMA,
        "workgroup_id": plan.get("workgroup_id"),
        "state": final_state,
        "source_sha": source_sha,
        "plan_sha256": plan_sha,
        "last_receipt_sha256": previous,
        "states": status,
        "executed_this_run": executed,
        "claim_allowed": False,
        "F_ok": [sid for sid, st in status.items() if st == "PASS"],
        "F_gap": [sid for sid, st in status.items() if st != "PASS"],
        "F_next": ["retry_failed_or_resolve_blocked" if any(st != "PASS" for st in status.values()) else "promote_only_with_scope_specific_evidence"]
    }
    (output_dir / "result.json").write_text(json.dumps(result, indent=2, sort_keys=True)+"\n", encoding="utf-8")
    _emit("result", state=final_state, last_receipt_sha256=previous, claim_allowed=False)
    return result


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--plan", required=True, type=Path)
    ap.add_argument("--root", type=Path, default=Path.cwd())
    ap.add_argument("--output", required=True, type=Path)
    ap.add_argument("--source-sha", required=True)
    ap.add_argument("--resume", action="store_true")
    ap.add_argument("--retry-failed", action="store_true")
    ns = ap.parse_args()
    try:
        result = execute_plan(ns.plan, ns.root, ns.output, ns.source_sha, resume=ns.resume, retry_failed=ns.retry_failed)
    except (ConvoyError, IntegrityError, json.JSONDecodeError, OSError, subprocess.SubprocessError) as exc:
        _emit("fatal", state="FAIL_CLOSED", error=type(exc).__name__, detail=str(exc))
        return 2
    return 0 if result["state"] == "PASS_LIMITED" else 1

if __name__ == "__main__":
    raise SystemExit(main())
