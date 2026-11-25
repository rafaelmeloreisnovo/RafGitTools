"""Offline candidate selector; never authorizes skipping, dispatch or promotion."""
import hashlib
import re
from datetime import datetime, timezone

DIGESTS = ("source_closure", "toolchain", "workflow", "environment", "policy",
           "dependency_receipts", "gate_version")
IDENTITY = ("repository", "target_branch", "gate_id")
ATTESTATIONS = ("producer_authenticated", "runner_trusted", "artifact_verified",
                "consumer_readback", "dependency_closure_verified", "authority_current")


def hex_id(value, n):
    return isinstance(value, str) and re.fullmatch(r"[0-9a-f]{%d}" % n, value) is not None


def utc(value):
    if not isinstance(value, str) or not value.endswith("Z"):
        raise ValueError("UTC_REQUIRED")
    return datetime.fromisoformat(value[:-1] + "+00:00")


def evaluate(receipt, current, observed, artifact, now, max_age_seconds):
    """Inputs must come from independent trusted adapters, not self-attested JSON.

    No provider calls, no secrets, no new check status. Even a candidate requires
    a consumer gate. Only reviewed offline structural gates are candidates.
    """
    reasons = set()
    if not all(isinstance(v, dict) for v in (receipt, current, observed)):
        return result({"INVALID_ENVELOPE"})
    for key in IDENTITY:
        value = current.get(key)
        if not isinstance(value, str) or not value.strip() or value.startswith("TOKEN_VAZIO") or receipt.get(key) != value:
            reasons.add("IDENTITY:" + key)
    if current.get("gate_class") != "STRUCTURAL_OFFLINE" or receipt.get("gate_class") != "STRUCTURAL_OFFLINE":
        reasons.add("GATE_CLASS_NOT_ELIGIBLE")
    if current.get("reuse_reviewed") is not True:
        reasons.add("REUSE_POLICY_UNREVIEWED")
    for key in DIGESTS:
        value = current.get(key)
        if not hex_id(value, 64) or receipt.get(key) != value:
            reasons.add("CLOSURE:" + key)
    for key in ("source_sha", "artifact_sha256"):
        if not hex_id(receipt.get(key), 40 if key == "source_sha" else 64):
            reasons.add("RECEIPT_ID:" + key)
    if not hex_id(current.get("source_sha"), 40):
        reasons.add("CURRENT_SHA_UNKNOWN")
    for key in ("run_id", "job_id", "attempt", "artifact_id"):
        value = receipt.get(key)
        if type(value) is not int or value <= 0 or type(observed.get(key)) is not int or observed.get(key) != value:
            reasons.add("PROVIDER_ID:" + key)
    for key in ("repository", "source_sha", "artifact_sha256"):
        if observed.get(key) != receipt.get(key):
            reasons.add("PROVIDER_BINDING:" + key)
    if receipt.get("conclusion") != "success" or observed.get("conclusion") != "success":
        reasons.add("PRODUCER_NOT_SUCCESS")
    for key in ATTESTATIONS:
        if observed.get(key) is not True:
            reasons.add("UNVERIFIED:" + key)
    for key in ("revoked", "superseded", "permission_invalidated"):
        if observed.get(key) is not False:
            reasons.add("LIFECYCLE:" + key)
    if not isinstance(artifact, bytes) or hashlib.sha256(artifact).hexdigest() != receipt.get("artifact_sha256"):
        reasons.add("ARTIFACT_BYTES_MISMATCH")
    try:
        issued, expires = utc(receipt.get("issued_at")), utc(receipt.get("expires_at"))
        if not isinstance(now, datetime) or now.tzinfo is None or now.utcoffset() != timezone.utc.utcoffset(now):
            raise ValueError("NOW_UTC_REQUIRED")
        if type(max_age_seconds) is not int or max_age_seconds <= 0:
            raise ValueError("MAX_AGE_REQUIRED")
        if not issued <= now < expires or (now - issued).total_seconds() > max_age_seconds:
            reasons.add("EXPIRED_OR_FUTURE_RECEIPT")
    except (ValueError, TypeError, OverflowError):
        reasons.add("TIME_UNTRUSTED")
    return result(reasons)


def result(reasons):
    return {"schema": "rafaelia.receipt-reuse-candidate.v1",
            "decision": "RERUN_OR_BLOCK" if reasons else "CANDIDATE_ONLY",
            "reasons": sorted(reasons), "skip_allowed": False,
            "check_status_write_allowed": False, "claim_allowed": False}
