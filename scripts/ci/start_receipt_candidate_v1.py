"""Compose the exact START ZIP reader and the lifecycle gate, offline only.
Inputs other than archive bytes require independently trusted producer adapters.
A final summary is not evidence for the underlying Android/security jobs.
"""
import io
import json
import zipfile

from receipt_reuse_gate_v1 import evaluate, result, DIGESTS
from verify_start_receipt_artifact_v1 import verify, unique_object

GATE_ID = "START_FINAL_RECEIPT_STRUCTURE"
FINAL_JOB = "09 · Final receipt / fechamento"


def assess(archive, binding, receipt, current, observed, now, max_age_seconds):
    """No network, mutation, status transfer or implicit attestation.

    binding and observed must come from independent authenticated adapters.
    Producer closure manifests are required independently of START ZIP contents.
    Even a positive result is a candidate for review of this structural gate only.
    """
    if not all(isinstance(x, dict) for x in (binding, receipt, current, observed)):
        return result({"INVALID_ENVELOPE"})
    if receipt.get("gate_id") != GATE_ID or current.get("gate_id") != GATE_ID:
        return result({"FINAL_SUMMARY_NOT_UNDERLYING_GATE_EVIDENCE"})
    if (binding.get("job_name") != FINAL_JOB or binding.get("artifact_expired") is not False
            or type(binding.get("artifact_run_id")) is not int
            or binding.get("artifact_run_id") != binding.get("run_id")):
        return result({"PROVIDER_ARTIFACT_CONTEXT_UNVERIFIED"})
    byte_result = verify(archive, binding)
    if byte_result["state"] != "BYTE_READBACK_ONLY":
        return result({"BYTE_READBACK:" + byte_result["reason"]})
    pairs = {"repository": "repository", "source_sha": "execution_sha", "run_id": "run_id",
             "job_id": "job_id", "attempt": "run_attempt", "artifact_id": "artifact_id",
             "artifact_sha256": "zip_sha256", "conclusion": "run_conclusion"}
    if any(receipt.get(k) != binding.get(v) or observed.get(k) != binding.get(v)
           for k, v in pairs.items()):
        return result({"LIFECYCLE_PROVIDER_BINDING_MISMATCH"})
    # The archive has already passed the strict one-entry size/layout/JSON check.
    with zipfile.ZipFile(io.BytesIO(archive)) as z:
        summary = json.loads(z.read("START_RECEIPT.json"), object_pairs_hook=unique_object)
    if (summary.get("base_ref") != receipt.get("target_branch")
            or binding.get("target_branch") != summary.get("base_ref")
            or observed.get("target_branch") != summary.get("base_ref")):
        return result({"TARGET_BRANCH_MISMATCH"})
    # Independent producer closure is NOT inferred from the final summary or current manifest.
    if any(observed.get(k) != receipt.get(k) for k in DIGESTS):
        return result({"INDEPENDENT_PRODUCER_CLOSURE_MISSING_OR_MISMATCHED"})
    # These two booleans are derived here, not accepted from caller.
    derived = dict(observed, artifact_verified=True, consumer_readback=True)
    out = evaluate(receipt, current, derived, archive, now, max_age_seconds)
    out.update(byte_readback=byte_result, scope=GATE_ID)
    return out
