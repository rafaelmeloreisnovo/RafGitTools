#!/usr/bin/env python3
"""RafGitTools host adapter: exact-byte Google Drive custody, RMR/pai BLAKE3.

No Google token is read here. A separately authorized Drive transport is
responsible for uploads and raw readback. This verifier never uploads, deletes,
mutates Git refs, approves PRs or treats provider IDs as cryptographic hashes.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

HEX64 = re.compile(r"^[0-9a-f]{64}$")
HEX40 = re.compile(r"^[0-9a-f]{40}$")
BLAKE3_KAT = {
    b"": "af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262",
    b"abc": "6437b3ac38465133ffb63b75273a8db548c558465d79db03fd359c6cd5bd9d85",
}
CHAIN_TAG = b"RAFAELIA_DRIVE_CUSTODY_CHAIN_V1\x00"


class CustodyError(ValueError):
    pass


def sha256_file(path):
    digest = hashlib.sha256()
    size = 0
    with open(path, "rb") as stream:
        while True:
            block = stream.read(64 * 1024)
            if not block:
                break
            size += len(block)
            digest.update(block)
    return digest.hexdigest(), size


def rmr_pai_blake3(pai, path):
    if not Path(pai).is_file():
        raise CustodyError("RMR_PAI_EXECUTABLE_NOT_FOUND")
    completed = subprocess.run(
        [str(pai), "hash", "--file", str(path), "--algo", "blake3"],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
        timeout=60, check=False, shell=False,
    )
    if completed.returncode != 0:
        raise CustodyError("RMR_PAI_HASH_FAILED")
    lines = completed.stdout.strip().splitlines()
    if len(lines) != 1:
        raise CustodyError("RMR_PAI_AMBIGUOUS_OUTPUT")
    match = re.fullmatch(r"([0-9a-f]{64})  (.+)", lines[0])
    if not match or match.group(2) != str(path):
        raise CustodyError("RMR_PAI_BAD_DIGEST_OR_PATH")
    return match.group(1)


def rmr_pai_preflight(pai):
    with tempfile.TemporaryDirectory(prefix="raf-drive-kat-") as td:
        for index, (content, expected) in enumerate(BLAKE3_KAT.items()):
            path = Path(td) / ("kat_" + str(index))
            path.write_bytes(content)
            if rmr_pai_blake3(pai, path) != expected:
                raise CustodyError("RMR_PAI_BLAKE3_KAT_MISMATCH")


def canonical_bytes(data):
    return json.dumps(
        data, sort_keys=True, ensure_ascii=True,
        separators=(",", ":"), allow_nan=False,
    ).encode("utf-8")


def chain_digest(previous_digest, receipt_body):
    if not isinstance(previous_digest, str) or not HEX64.fullmatch(previous_digest):
        raise CustodyError("INVALID_PREDECESSOR_DIGEST")
    return hashlib.sha256(
        CHAIN_TAG + bytes.fromhex(previous_digest) + canonical_bytes(receipt_body)
    ).hexdigest()


def verify(
    source, drive_readback, provider_sha256, source_commit,
    source_repo, workflow_run_id, artifact_id, drive_file_id,
    previous_digest, pai,
):
    if not HEX64.fullmatch(provider_sha256):
        raise CustodyError("INVALID_PROVIDER_DIGEST")
    if not HEX40.fullmatch(source_commit):
        raise CustodyError("INVALID_SOURCE_COMMIT")
    if (not re.fullmatch(r"rafaelmeloreisnovo/[A-Za-z0-9_.-]+", source_repo)
            or not str(drive_file_id).strip()
            or int(workflow_run_id) <= 0 or int(artifact_id) <= 0):
        raise CustodyError("INVALID_PROVIDER_LOCATORS")
    # Verify all source and readback bytes, before making any authority claim.
    initial_sha, source_size = sha256_file(source)
    drive_sha, drive_size = sha256_file(drive_readback)
    if source_size != drive_size or source_size < 1:
        raise CustodyError("TRANSFER_BYTE_COUNT_MISMATCH")
    if initial_sha != provider_sha256:
        raise CustodyError("PROVIDER_SOURCE_SHA256_MISMATCH")
    if drive_sha != initial_sha:
        raise CustodyError("DRIVE_READBACK_SHA256_MISMATCH")
    # Do not pretend SHA-256 is BLAKE3: require actual RMR/pai with two KATs.
    rmr_pai_preflight(pai)
    source_b3 = rmr_pai_blake3(pai, source)
    drive_b3 = rmr_pai_blake3(pai, drive_readback)
    if source_b3 != drive_b3:
        raise CustodyError("DRIVE_READBACK_BLAKE3_MISMATCH")
    body = {
        "schema": "rafaelia.drive-ci-custody.v1",
        "producer": "rafaelmeloreisnovo/RafGitTools",
        "algorithm": "RMR_PAI_BLAKE3_256_PLUS_SHA256",
        "hash_engine": "rafaelmeloreisnovo/BLAKE3/rmr/core/hash_blake3.c",
        "privacy_class": "PRIVATE_DRIVE_CUSTODY",
        "claim_allowed": False,
        "promotion_allowed": False,
        "content_scope": "EXACT_ZIP_BYTES_ONLY",
        "source_repo": source_repo,
        "source_commit": source_commit,
        "workflow_run_id": int(workflow_run_id),
        "github_artifact_id": int(artifact_id),
        "github_reported_sha256": provider_sha256,
        "drive_file_id": drive_file_id,
        "content_sha256": initial_sha,
        "content_blake3_256": source_b3,
        "byte_count": source_size,
        "readback_result": "BYTES_MATCH_DUAL_HASH",
        "provider_identity_status": "CALLER_SUPPLIED_CHECK_PROVIDER_READBACK_SEPARATELY",
        "previous_receipt_sha256": previous_digest,
    }
    return {**body, "receipt_chain_sha256": chain_digest(previous_digest, body)}


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--source", required=True)
    ap.add_argument("--drive-readback", required=True)
    ap.add_argument("--provider-sha256", required=True)
    ap.add_argument("--source-commit", required=True)
    ap.add_argument("--source-repo", required=True)
    ap.add_argument("--workflow-run-id", required=True, type=int)
    ap.add_argument("--artifact-id", required=True, type=int)
    ap.add_argument("--drive-file-id", required=True)
    ap.add_argument("--previous-receipt-sha256", required=True)
    ap.add_argument("--pai", required=True, help="Exact RMR pai executable")
    ap.add_argument("--output", required=True)
    args = ap.parse_args(argv)
    try:
        receipt = verify(
            args.source, args.drive_readback, args.provider_sha256,
            args.source_commit, args.source_repo, args.workflow_run_id,
            args.artifact_id, args.drive_file_id,
            args.previous_receipt_sha256, args.pai,
        )
        # Exclusive creation: no silent overwrite or deletion of receipts.
        out = Path(args.output)
        with out.open("x", encoding="utf-8") as stream:
            stream.write(json.dumps(receipt, sort_keys=True, indent=2) + "\n")
            stream.flush()
            os.fsync(stream.fileno())
        print(json.dumps({
            "state": "DUAL_HASH_READBACK_MATCH",
            "receipt": str(out),
            "receipt_chain_sha256": receipt["receipt_chain_sha256"],
            "claim_allowed": False,
        }, sort_keys=True))
        return 0
    except (CustodyError, OSError, subprocess.TimeoutExpired, ValueError) as exc:
        print(json.dumps({
            "state": "BLOCKED",
            "reason": str(exc),
            "claim_allowed": False,
        }, sort_keys=True), file=sys.stderr)
        return 3


if __name__ == "__main__":
    raise SystemExit(main())
