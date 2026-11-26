"""Offline START receipt byte readback. Provider binding must be independently obtained.
Never authenticates a provider, skips a gate or writes a check status.
"""
import hashlib
import io
import json
import re
import zipfile
import zlib

MAX_BYTES = 65536


def unique_object(pairs):
    out = {}
    for k, v in pairs:
        if k in out:
            raise ValueError("duplicate JSON key")
        out[k] = v
    return out


def reject_non_json_constant(value):
    raise ValueError("non-JSON numeric constant: " + value)


def verify(archive, binding):
    """binding: repo, execution/provider SHA, run/job/attempt/artifact IDs,
    provider conclusions and ZIP digest. Obtain from authenticated provider,
    not the ZIP or a user-supplied assertion. This is only a byte crosscheck.
    """
    def result(state, reason):
        return dict(state=state, reason=reason, claim_allowed=False,
                    skip_allowed=False, check_status_write_allowed=False)

    if not isinstance(binding, dict) or not isinstance(archive, bytes) or not 0 < len(archive) <= MAX_BYTES:
        return result("REJECTED", "INVALID_INPUT")
    for k in ("run_id", "job_id", "run_attempt", "artifact_id"):
        if type(binding.get(k)) is not int or binding[k] <= 0:
            return result("REJECTED", "INVALID_PROVIDER_ID")
    for k in ("execution_sha", "provider_sha", "zip_sha256"):
        n = 64 if k == "zip_sha256" else 40
        if not isinstance(binding.get(k), str) or not re.fullmatch(r"[0-9a-f]{%d}" % n, binding[k]):
            return result("REJECTED", "INVALID_DIGEST")
    if not isinstance(binding.get("repository"), str) or not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", binding["repository"]):
        return result("REJECTED", "INVALID_REPOSITORY")
    if hashlib.sha256(archive).hexdigest() != binding["zip_sha256"]:
        return result("REJECTED", "ZIP_DIGEST_MISMATCH")
    try:
        with zipfile.ZipFile(io.BytesIO(archive)) as z:
            entries = z.infolist()
            if len(entries) != 1 or entries[0].filename != "START_RECEIPT.json" or not 0 < entries[0].file_size <= MAX_BYTES:
                return result("REJECTED", "INVALID_ARCHIVE_LAYOUT")
            raw = z.read(entries[0])
        receipt = json.loads(raw, object_pairs_hook=unique_object,
                             parse_constant=reject_non_json_constant)
    except (ValueError, OSError, RuntimeError, zipfile.BadZipFile, NotImplementedError, zlib.error):
        return result("REJECTED", "INVALID_ARCHIVE_OR_JSON")
    if not isinstance(receipt, dict) or receipt.get("schema") != "RAFAELIA_START_V1":
        return result("REJECTED", "INVALID_SCHEMA")
    for k in ("repository", "execution_sha", "provider_sha"):
        if receipt.get(k) != binding[k]:
            return result("REJECTED", "PROVIDER_BINDING_MISMATCH")
    if (receipt.get("sha") != binding["execution_sha"] or receipt.get("run_id") != str(binding["run_id"])
            or receipt.get("run_attempt") != str(binding["run_attempt"])):
        return result("REJECTED", "ATTEMPT_OR_SHA_MISMATCH")
    if receipt.get("claim_allowed") is not False or receipt.get("automatic_promotion") is not False:
        return result("REJECTED", "CLAIM_BOUNDARY")
    if binding.get("run_conclusion") != "success" or binding.get("job_conclusion") != "success":
        return result("REJECTED", "PROVIDER_NOT_SUCCESS")
    if receipt.get("state") != "PASS_WITH_TYPED_SKIPS" or receipt.get("missing_required_gates") != []:
        return result("REJECTED", "RECEIPT_NOT_SUCCESS")
    required, stages = receipt.get("required_gates"), receipt.get("stages")
    if not isinstance(required, dict) or not required or not isinstance(stages, dict):
        return result("REJECTED", "GATE_MAP_MISSING")
    if any(type(v) is not bool or (v and stages.get(k) != "SUCCESS") for k, v in required.items()):
        return result("REJECTED", "REQUIRED_GATE_MISSING")
    if any(v not in ("SUCCESS", "TOKEN_VAZIO_NOT_SELECTED") for v in stages.values()):
        return result("REJECTED", "NONTERMINAL_OR_FAILED_STAGE")
    out = result("BYTE_READBACK_ONLY", "EXACT_BINDING_MATCH")
    out.update(artifact_id=binding["artifact_id"], job_id=binding["job_id"],
               run_attempt=binding["run_attempt"], json_sha256=hashlib.sha256(raw).hexdigest())
    return out
