#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import struct
from pathlib import Path
from typing import Any, Dict, Tuple

TOKEN_VAZIO = "TOKEN_VAZIO"
OUTCOMES_ADVANCE = {"PASS"}
OUTCOMES_HOLD = {"FAIL", "BLOCKED", "PENDING", "AUDIT", TOKEN_VAZIO, "IMPLEMENTED_UNTESTED"}


class ContractError(ValueError):
    pass


def load_json(path: str | Path) -> Dict[str, Any]:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def dump_json(obj: Dict[str, Any], out: str | Path | None = None) -> None:
    text = json.dumps(obj, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if out:
        Path(out).write_text(text, encoding="utf-8")
    else:
        print(text, end="")


def _phase_map(config: Dict[str, Any]) -> Dict[str, Dict[str, Any]]:
    return {p["id"]: p for p in config["scheduler"]["phases"]}


def _desired_interval(phase: Dict[str, Any], state: Dict[str, Any]) -> int:
    seq = phase.get("sequence_minutes")
    if seq:
        idx = int(state.get("phase_index", 0))
        if idx < 0 or idx >= len(seq):
            raise ContractError("phase_index outside sequence")
        return int(seq[idx])
    return int(phase["interval_minutes"])


def backend_gate(config: Dict[str, Any], backend_id: str, desired_minutes: int) -> Dict[str, Any]:
    backends = config["scheduler"]["backends"]
    if backend_id not in backends:
        return {
            "state": "BLOCKED",
            "reason": "TOKEN_VAZIO_EXECUTION_TARGET",
            "desired_minutes": desired_minutes,
            "effective_minutes": TOKEN_VAZIO,
        }
    b = backends[backend_id]
    minimum = int(b["minimum_interval_minutes"])
    if desired_minutes < minimum:
        return {
            "state": "BLOCKED",
            "reason": "BACKEND_MIN_INTERVAL",
            "desired_minutes": desired_minutes,
            "effective_minutes": TOKEN_VAZIO,
            "minimum_interval_minutes": minimum,
            "best_effort": bool(b.get("best_effort", False)),
        }
    return {
        "state": "PASS",
        "reason": "WITHIN_BACKEND_CONTRACT",
        "desired_minutes": desired_minutes,
        "effective_minutes": desired_minutes,
        "minimum_interval_minutes": minimum,
        "best_effort": bool(b.get("best_effort", False)),
    }


def advance_schedule(config: Dict[str, Any], state: Dict[str, Any], outcome: str) -> Dict[str, Any]:
    pmap = _phase_map(config)
    phase_id = state.get("phase", TOKEN_VAZIO)
    if phase_id not in pmap:
        raise ContractError("unknown schedule phase")
    if outcome not in OUTCOMES_ADVANCE | OUTCOMES_HOLD:
        raise ContractError("unsupported outcome")

    current_phase = pmap[phase_id]
    next_state = dict(state)
    next_state["last_outcome"] = outcome

    if outcome not in OUTCOMES_ADVANCE:
        desired = _desired_interval(current_phase, state)
        next_state["current_interval_minutes"] = desired
        next_state["advance_state"] = "HELD_NO_PROMOTION"
        return next_state

    seq = current_phase.get("sequence_minutes")
    if seq:
        idx = int(state.get("phase_index", 0))
        if idx + 1 < len(seq):
            idx += 1
            next_state["phase_index"] = idx
            next_state["current_interval_minutes"] = int(seq[idx])
            next_state["advance_state"] = "ADVANCED_WITHIN_PHASE"
            return next_state
        nxt = current_phase.get("next_phase", TOKEN_VAZIO)
        if nxt == TOKEN_VAZIO or nxt not in pmap:
            next_state["advance_state"] = "BLOCKED_NEXT_PHASE_TOKEN_VAZIO"
            return next_state
        np = pmap[nxt]
        next_state.update({
            "phase": nxt,
            "phase_index": 0,
            "phase_success_count": 0,
            "current_interval_minutes": _desired_interval(np, {"phase_index": 0}),
            "advance_state": "PROMOTED_PHASE",
        })
        return next_state

    success_count = int(state.get("phase_success_count", 0)) + 1
    next_state["phase_success_count"] = success_count
    promote_after = current_phase.get("promotion_after_successes", TOKEN_VAZIO)
    if promote_after == TOKEN_VAZIO:
        next_state["current_interval_minutes"] = int(current_phase["interval_minutes"])
        next_state["advance_state"] = "HELD_PROMOTION_RULE_TOKEN_VAZIO"
        return next_state
    if success_count < int(promote_after):
        next_state["current_interval_minutes"] = int(current_phase["interval_minutes"])
        next_state["advance_state"] = "ADVANCED_SUCCESS_COUNTER"
        return next_state
    nxt = current_phase.get("next_phase", TOKEN_VAZIO)
    if nxt == TOKEN_VAZIO or nxt not in pmap:
        next_state["current_interval_minutes"] = int(current_phase["interval_minutes"])
        next_state["advance_state"] = "TERMINAL_OR_BLOCKED_NEXT_PHASE"
        return next_state
    np = pmap[nxt]
    next_state.update({
        "phase": nxt,
        "phase_index": 0,
        "phase_success_count": 0,
        "current_interval_minutes": _desired_interval(np, {"phase_index": 0}),
        "advance_state": "PROMOTED_PHASE",
    })
    return next_state


def validate_intake(pointer: Dict[str, Any], config: Dict[str, Any]) -> Dict[str, Any]:
    required = config["intake_contract"]["required_fields"]
    prohibited = set(config["intake_contract"]["prohibited_fields"])
    missing = [k for k in required if k not in pointer or pointer[k] in (None, "")]
    found_prohibited = sorted(k for k in prohibited if k in pointer)
    token_vazio = sorted(
        k for k in required
        if pointer.get(k) == TOKEN_VAZIO or str(pointer.get(k, "")).startswith("TOKEN_VAZIO_")
    )

    if pointer.get("claim_allowed") is not False:
        return {"state": "FAIL", "reason": "CLAIM_BOUNDARY", "missing": missing, "prohibited": found_prohibited, "token_vazio": token_vazio}
    if found_prohibited:
        return {"state": "FAIL", "reason": "RAW_OR_SECRET_FIELD_PROHIBITED", "missing": missing, "prohibited": found_prohibited, "token_vazio": token_vazio}
    if missing:
        return {"state": "BLOCKED", "reason": "MISSING_REQUIRED_FIELDS", "missing": missing, "prohibited": found_prohibited, "token_vazio": token_vazio}
    if token_vazio:
        return {"state": "BLOCKED", "reason": "REQUIRED_TOKEN_VAZIO", "missing": missing, "prohibited": found_prohibited, "token_vazio": token_vazio}
    return {"state": "PASS", "reason": "STRUCTURAL_POINTER_ONLY", "missing": [], "prohibited": [], "token_vazio": []}


def png_dimensions(data: bytes) -> Tuple[int, int] | None:
    sig = b"\x89PNG\r\n\x1a\n"
    if len(data) >= 24 and data[:8] == sig and data[12:16] == b"IHDR":
        return struct.unpack(">II", data[16:24])
    return None


def visual_anchor(path: str | Path) -> Dict[str, Any]:
    p = Path(path)
    data = p.read_bytes()
    sha256 = hashlib.sha256(data).digest()
    blake2b = hashlib.blake2b(data, digest_size=32).hexdigest()
    dims = png_dimensions(data)
    vec = [int.from_bytes(sha256[i:i + 4], "big") for i in range(0, 32, 4)]
    return {
        "schema": "rafaelia.templo-vivo.visual-anchor.v1",
        "artifact_name": p.name,
        "size_bytes": len(data),
        "sha256": sha256.hex(),
        "blake2b_256": blake2b,
        "width": dims[0] if dims else TOKEN_VAZIO,
        "height": dims[1] if dims else TOKEN_VAZIO,
        "digest_vector_u32": vec,
        "vector_semantics": "OPAQUE_CONTENT_FINGERPRINT_NOT_EMBEDDING",
        "ocr_performed": False,
        "text_extracted": False,
        "human_legible_assume_machine_readable": True,
        "privacy_guarantee": False,
        "claim_allowed": False,
    }


def cmd_schedule(args: argparse.Namespace) -> int:
    cfg = load_json(args.config)
    state = load_json(args.state)
    new_state = advance_schedule(cfg, state, args.outcome)
    gate = backend_gate(cfg, args.backend, int(new_state["current_interval_minutes"]))
    result = {"schedule_state": new_state, "backend_gate": gate, "claim_allowed": False}
    dump_json(result, args.out)
    return 0 if gate["state"] == "PASS" else 2


def cmd_validate(args: argparse.Namespace) -> int:
    cfg = load_json(args.config)
    pointer = load_json(args.input)
    result = validate_intake(pointer, cfg)
    dump_json(result, args.out)
    return 0 if result["state"] == "PASS" else 2


def cmd_anchor(args: argparse.Namespace) -> int:
    result = visual_anchor(args.image)
    dump_json(result, args.out)
    return 0


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="TEMPLO_VIVO_ARC deterministic privacy/provenance control-plane model")
    sub = p.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("schedule-next")
    s.add_argument("--config", required=True)
    s.add_argument("--state", required=True)
    s.add_argument("--outcome", required=True, choices=sorted(OUTCOMES_ADVANCE | OUTCOMES_HOLD))
    s.add_argument("--backend", required=True)
    s.add_argument("--out")
    s.set_defaults(fn=cmd_schedule)

    v = sub.add_parser("validate-intake")
    v.add_argument("--config", required=True)
    v.add_argument("--input", required=True)
    v.add_argument("--out")
    v.set_defaults(fn=cmd_validate)

    a = sub.add_parser("visual-anchor")
    a.add_argument("--image", required=True)
    a.add_argument("--out")
    a.set_defaults(fn=cmd_anchor)
    return p


def main() -> int:
    args = build_parser().parse_args()
    try:
        return int(args.fn(args))
    except (OSError, json.JSONDecodeError, ContractError, KeyError, TypeError, ValueError) as exc:
        print(json.dumps({"state": "FAIL", "error": str(exc), "claim_allowed": False}, ensure_ascii=False))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
