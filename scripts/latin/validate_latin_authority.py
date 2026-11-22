#!/usr/bin/env python3
"""LATIN policy structure preflight, NOT provider/admin authorization."""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
POLICY = ROOT / "contracts/latin/latin-authority.v1.json"
OUT = ROOT / "dist/latin/authority-receipt.v1.json"
REQUIRED = {"SOURCE_LICENSE_AND_AUTHORSHIP", "GITHUB_HUMAN_PR_APPROVAL", "PROTECTED_RULESET_READBACK", "NEGATIVE_REJECTION_PROOF", "EXACT_HEAD_CI", "ARTIFACT_SHA256", "LOWFALA_IR_ABI_PARITY", "PRIVACY_REVIEW", "ROLLBACK_REPLAY"}

def validate(p):
    if p.get("schema") != "rafaelia.latin.authority.v1" or p.get("owner") != "rafaelmeloreisnovo/RafGitTools":
        raise ValueError("invalid source authority")
    if p.get("producer") != "rafaelmeloreisnovo/RafPolimata" or p.get("consumer") != "instituto-Rafael/relativity-living-light":
        raise ValueError("unexpected producer or consumer")
    if p.get("github_authorization") != "TOKEN_VAZIO_PROVIDER_AND_HUMAN" or p.get("claim_allowed") is not False:
        raise ValueError("unverified promotion")
    caps = p.get("capabilities", {})
    if caps.get("K_SECRETS") != "TOKEN_VAZIO_NO_VERIFIED_GITHUB_PATCH_OR_PROVIDER_BINDING":
        raise ValueError("K-Secrets cannot be presumed available")
    if caps.get("PAT_ENV") != "MANUAL_ADMINISTRATION_ROUTE_NOT_GRANTED_TO_THIS_WORKFLOW":
        raise ValueError("admin permission silently promoted")
    if caps.get("PAT_ENVIRONMENTS") != "DISTINCT_UNBOUND_IDENTITY":
        raise ValueError("two distinct secret identities collapsed")
    s = p.get("security", {})
    for k in ("allow_secret_fallback", "log_secret_values", "private_to_public_export", "network_admin_calls", "code_mutation", "training_executed", "auto_merge", "auto_approve", "auto_dispatch", "claim_allowed", "automatic_adoption"):
        if s.get(k) is not False:
            raise ValueError("forbidden capability: " + k)
    if not REQUIRED.issubset(set(p.get("required_gates", []))):
        raise ValueError("required gate missing")
    m = p.get("memory", {})
    if m.get("raw_text_upload") is not False or m.get("append_only") is not True or m.get("scope") != "PRIVATE_POINTER_HASH_RECEIPT_ONLY":
        raise ValueError("unsafe memory policy")
    if not p.get("normative_mapping"):
        raise ValueError("normative applicability mapping missing")
    return {"schema":"rafaelia.latin.gate-receipt.v1","result":"PASS_POLICY_STRUCTURE_ONLY","provider_enforcement":"TOKEN_VAZIO","human_approval":"TOKEN_VAZIO","runtime":"NOT_RUN","claim_allowed":False}

def main():
    data = POLICY.read_bytes()
    receipt = validate(json.loads(data))
    receipt["policy_sha256"] = hashlib.sha256(data).hexdigest()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(receipt, sort_keys=True, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))

if __name__ == "__main__":
    main()
