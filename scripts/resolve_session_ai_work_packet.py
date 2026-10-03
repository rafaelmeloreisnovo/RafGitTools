#!/usr/bin/env python3
"""Resolve a Mapa session packet against a pinned local checkout, read-only."""
import argparse
import json
import subprocess
import sys
from pathlib import Path

MAPA_REPO = "rafaelmeloreisnovo/Mapa"
MAPA_COMMIT = "ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09"
DISPATCH_PATH = "data/control-plane/SESSION_AI_WORK_DISPATCH_V1.json"
DISPATCH_BLOB = "c1d1d8f1692c74957f8b70968fa863f855d93cb0"
SCHEMA = "RAFAELIA_SESSION_AI_WORK_DISPATCH_V1"


class Blocked(Exception):
    pass


def git(checkout, *args):
    try:
        return subprocess.check_output(
            ["git", "-C", str(checkout), *args], stderr=subprocess.DEVNULL
        ).decode("utf-8").strip()
    except (OSError, subprocess.CalledProcessError) as exc:
        raise Blocked(f"git source verification failed: {exc}") from exc


def resolve(checkout, packet_id=None, agent_id=None, expected_commit=MAPA_COMMIT, expected_blob=DISPATCH_BLOB):
    checkout = Path(checkout)
    head = git(checkout, "rev-parse", "HEAD")
    if head != expected_commit:
        raise Blocked(f"Mapa HEAD mismatch: expected {expected_commit}, got {head}")
    blob = git(checkout, "rev-parse", f"HEAD:{DISPATCH_PATH}")
    if blob != expected_blob:
        raise Blocked(f"dispatch blob mismatch: expected {expected_blob}, got {blob}")
    try:
        data = json.loads((checkout / DISPATCH_PATH).read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        raise Blocked(f"cannot read dispatch JSON: {exc}") from exc
    if data.get("schema") != SCHEMA:
        raise Blocked("unexpected dispatch schema")
    authority = data.get("source_authority") or {}
    if authority.get("federated_router") != MAPA_REPO:
        raise Blocked("Mapa federated router authority mismatch")
    if authority.get("executor") != "rafaelmeloreisnovo/RafGitTools":
        raise Blocked("RafGitTools executor authority mismatch")
    if data.get("claim_allowed") is not False:
        raise Blocked("dispatch claim_allowed must be false")
    invariants = data.get("invariants", [])
    if "AGENT_ASSIGNMENT != AGENT_EXECUTION" not in invariants:
        raise Blocked("assignment/execution invariant missing")

    roles = data.get("agent_roles")
    packets = data.get("session_packets")
    if not isinstance(roles, list) or not isinstance(packets, list):
        raise Blocked("agent_roles and session_packets must be arrays")
    role_by_id = {}
    for role in roles:
        rid = role.get("id")
        if not rid or rid in role_by_id:
            raise Blocked(f"missing or duplicate agent id: {rid}")
        for field in ("locality", "owner", "mission", "outputs", "execution_target", "evidence_rule"):
            value = role.get(field)
            if not value:
                raise Blocked(f"{rid}: invalid {field}")
        sources = role["source_min"]
        if not isinstance(sources, list) or not 1 <= len(sources) <= 3:
            raise Blocked(f"{rid}: source_min must contain 1..3 entries")
        role_by_id[rid] = role
    packet_by_id = {}
    for packet in packets:
        pid = packet.get("id")
        if not pid or pid in packet_by_id:
            raise Blocked(f"missing or duplicate packet id: {pid}")
        for field in ("authority", "source_min", "execution_target", "evidence_rule", "agents"):
            if not packet.get(field):
                raise Blocked(f"{pid}: missing {field}")
        if packet.get("claim_allowed") is not False:
            raise Blocked(f"{pid}: claim_allowed must be false")
        if any(agent not in role_by_id for agent in packet["agents"]):
            raise Blocked(f"{pid}: unknown agent reference")
        packet_by_id[pid] = packet

    if packet_id:
        packet = packet_by_id.get(packet_id)
        if packet is None:
            raise Blocked(f"unknown packet: {packet_id}")
        selected_roles = [role_by_id[rid] for rid in packet["agents"]]
        selection = {"kind": "packet", "id": packet_id}
        packet_summary = {key: packet[key] for key in (
            "id", "alpha_summary", "delta", "authority", "source_min",
            "execution_target", "evidence_rule", "status", "omega_exit", "claim_allowed"
        ) if key in packet}
    else:
        role = role_by_id.get(agent_id)
        if role is None:
            raise Blocked(f"unknown agent: {agent_id}")
        selected_roles = [role]
        selection = {"kind": "agent", "id": agent_id}
        packet_summary = None
    return {
        "state": "ROUTE_RESOLVED",
        "selection": selection,
        "packet": packet_summary,
        "agents": selected_roles,
        "source": {"repo": MAPA_REPO, "commit": head, "path": DISPATCH_PATH, "blob": blob},
        "assignment_is_execution": False,
        "evidence_state": "STRUCTURAL_ROUTE_ONLY",
        "claim_allowed": False,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mapa-checkout", required=True, help="local Mapa git checkout at the pinned commit")
    selection = parser.add_mutually_exclusive_group(required=True)
    selection.add_argument("--packet")
    selection.add_argument("--agent")
    args = parser.parse_args()
    try:
        result = resolve(args.mapa_checkout, packet_id=args.packet, agent_id=args.agent)
    except Blocked as exc:
        print(json.dumps({"state": "ROUTE_STATE_BLOCKED", "reason": str(exc), "claim_allowed": False}, ensure_ascii=False, indent=2))
        return 2
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
