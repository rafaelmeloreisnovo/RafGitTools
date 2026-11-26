"""Bounded offline dependency receipt closure. Structural evidence only.
The expected manifest digest and all lifecycle observations require independent
custody; this module cannot authenticate their producer or authorize reuse.
"""
import hashlib
import json
import re
from datetime import datetime, timezone

LIMIT = 1024
ID = re.compile(r"[A-Za-z0-9_.:/-]{1,128}\Z")
HEX = re.compile(r"[0-9a-f]{64}\Z")


def valid_id(x):
    return isinstance(x, str) and ID.fullmatch(x) is not None and not x.startswith("TOKEN_VAZIO")


def digest(x):
    return hashlib.sha256(json.dumps(x, sort_keys=True, separators=(",", ":"),
                                     ensure_ascii=True, allow_nan=False).encode()).hexdigest()


def utc(x):
    if not isinstance(x, str) or not x.endswith("Z"):
        raise ValueError("UTC_REQUIRED")
    return datetime.fromisoformat(x[:-1] + "+00:00")


def assess(manifest, expected_digest, observations, now):
    """Returns an offline structural candidate or typed blockers, never a skip."""
    def result(reasons, invalid=(), closure=None):
        return dict(schema="rafaelia.dependency-closure.v1",
                    state="RERUN_OR_BLOCK" if reasons else "STRUCTURAL_CANDIDATE_ONLY",
                    reasons=sorted(set(reasons)), invalidated=sorted(set(invalid)),
                    dependency_receipts=closure, producer_authenticated=False,
                    dependency_closure_verified=False, skip_allowed=False,
                    claim_allowed=False, check_status_write_allowed=False)
    if (not isinstance(manifest, dict) or set(manifest) != {"root", "nodes"}
            or not isinstance(observations, dict) or not isinstance(now, datetime)
            or now.tzinfo is None or now.utcoffset() != timezone.utc.utcoffset(now)):
        return result(["INVALID_ENVELOPE"])
    nodes = manifest["nodes"]
    root = manifest["root"]
    if (not isinstance(nodes, dict) or not 1 <= len(nodes) <= LIMIT
            or not valid_id(root) or root not in nodes):
        return result(["INVALID_GRAPH_SCOPE"])
    normalized = {}
    for key, node in nodes.items():
        if (not valid_id(key) or not isinstance(node, dict) or set(node) != {"receipt_sha256", "requires"}
                or not isinstance(node["receipt_sha256"], str) or not HEX.fullmatch(node["receipt_sha256"])
                or not isinstance(node["requires"], list) or len(node["requires"]) > LIMIT
                or any(not valid_id(d) for d in node["requires"]) or len(set(node["requires"])) != len(node["requires"])):
            return result(["INVALID_NODE"])
        normalized[key] = dict(receipt_sha256=node["receipt_sha256"], requires=sorted(node["requires"]))
    if sum(len(n["requires"]) for n in normalized.values()) > LIMIT * 4:
        return result(["EDGE_LIMIT"])
    if not isinstance(expected_digest, str) or not HEX.fullmatch(expected_digest) or digest(dict(root=root, nodes=normalized)) != expected_digest:
        return result(["INDEPENDENT_MANIFEST_MISMATCH"])
    reachable = set()
    stack = [root]
    reverse = {k: [] for k in nodes}
    while stack:
        k = stack.pop()
        if k in reachable:
            continue
        reachable.add(k)
        for dep in normalized[k]["requires"]:
            if dep not in nodes:
                return result(["MISSING_DEPENDENCY:" + dep])
            reverse[dep].append(k)
            stack.append(dep)
    if reachable != set(nodes) or set(observations) - reachable:
        return result(["UNREACHABLE_OR_EXTRA_NODE"])
    pending = {k: len(normalized[k]["requires"]) for k in nodes}
    queue = [k for k, count in pending.items() if count == 0]
    order = []
    while queue:
        k = queue.pop()
        order.append(k)
        for parent in reverse[k]:
            pending[parent] -= 1
            if pending[parent] == 0:
                queue.append(parent)
    if len(order) != len(nodes):
        return result(["DEPENDENCY_CYCLE"])
    reasons, invalid = [], set()
    for key in order:
        obs = observations.get(key)
        if not isinstance(obs, dict):
            reasons.append("TOKEN_VAZIO_OBSERVATION:" + key)
            invalid.add(key)
        else:
            if obs.get("receipt_sha256") != normalized[key]["receipt_sha256"]:
                reasons.append("RECEIPT_MISMATCH:" + key)
                invalid.add(key)
            for flag in ("revoked", "superseded", "permission_invalidated"):
                if obs.get(flag) is not False:
                    reasons.append("LIFECYCLE:" + key + ":" + flag)
                    invalid.add(key)
            if obs.get("conclusion") != "success" or obs.get("artifact_verified") is not True:
                reasons.append("EVIDENCE_UNVERIFIED:" + key)
                invalid.add(key)
            try:
                if not utc(obs.get("issued_at")) <= now < utc(obs.get("expires_at")):
                    raise ValueError("INVALID_WINDOW")
            except (ValueError, TypeError, OverflowError):
                reasons.append("TIME_UNVERIFIED:" + key)
                invalid.add(key)
        if any(dep in invalid for dep in normalized[key]["requires"]):
            reasons.append("TRANSITIVE_INVALIDATION:" + key)
            invalid.add(key)
    return result(reasons, invalid, None if reasons else expected_digest)
