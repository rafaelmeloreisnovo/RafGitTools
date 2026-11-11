#!/usr/bin/env python3
"""Verify a signed APK and emit a minimal, deterministic provenance receipt."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
from datetime import datetime, timezone
from pathlib import Path

HEX_64 = re.compile(r"^[0-9a-fA-F]{64}$")


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--apk", required=True, type=Path)
    p.add_argument("--apksigner", default="apksigner")
    p.add_argument("--output-dir", required=True, type=Path)
    p.add_argument("--repository", required=True)
    p.add_argument("--git-commit", required=True)
    p.add_argument("--run-id", required=True)
    return p.parse_args()


def main() -> int:
    args = parse_args()
    if not args.apk.is_file():
        raise SystemExit(f"APK_NOT_FOUND: {args.apk}")

    args.output_dir.mkdir(parents=True, exist_ok=True)
    proc = subprocess.run(
        [args.apksigner, "verify", "--verbose", "--print-certs", str(args.apk)],
        check=False,
        capture_output=True,
        text=True,
    )
    transcript = proc.stdout + proc.stderr
    (args.output_dir / "apksigner.txt").write_text(transcript, encoding="utf-8")
    if proc.returncode != 0:
        raise SystemExit(f"APKSIGNER_VERIFY_FAILED: exit={proc.returncode}")

    cert = {}
    schemes = {}
    for line in transcript.splitlines():
        line = line.strip()
        if "Signer #1 certificate DN:" in line:
            cert["dn"] = line.split(":", 1)[1].strip()
        elif "Signer #1 certificate SHA-256 digest:" in line:
            value = line.split(":", 1)[1].strip().lower()
            if HEX_64.fullmatch(value):
                cert["sha256"] = value
        elif line.startswith("Verified using v") and "scheme" in line:
            label, value = line.split(":", 1)
            schemes[label.removeprefix("Verified using ").strip()] = value.strip().lower() == "true"

    artifact_sha256 = hashlib.sha256(args.apk.read_bytes()).hexdigest()
    receipt = {
        "schema": "rafaelia.apk-signature-receipt.v1",
        "state": "VERIFIED_BY_APKSIGNER",
        "artifact": args.apk.name,
        "artifact_sha256": artifact_sha256,
        "certificate": cert,
        "signature_schemes": schemes,
        "source": {
            "repository": args.repository,
            "git_commit": args.git_commit,
            "github_run_id": args.run_id,
        },
        "observed_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "claim_scope": "artifact_integrity_and_signer_certificate_only",
        "claim_allowed": False,
    }
    (args.output_dir / "receipt.json").write_text(
        json.dumps(receipt, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
