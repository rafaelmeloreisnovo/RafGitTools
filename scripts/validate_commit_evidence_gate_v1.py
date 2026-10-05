#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artifacts" / "commit-evidence-gate-v1-source-receipt.json"

FILES = {
    "envelope": ROOT / "app/src/main/kotlin/com/rafgittools/rafgitfs/assurance/CommitEvidenceEnvelope.kt",
    "screen": ROOT / "app/src/main/kotlin/com/rafgittools/ui/screens/rafgitfs/CommitEvidenceGateScreen.kt",
    "tests": ROOT / "app/src/test/kotlin/com/rafgittools/rafgitfs/assurance/CommitEvidenceEnvelopeTest.kt",
    "docs": ROOT / "docs/architecture/COMMIT_EVIDENCE_GATE_V1.md",
    "human_safeguard_docs": ROOT / "docs/architecture/HUMAN_DIGNITY_SAFEGUARD_V1.md",
}

REQUIRED_ENVELOPE = [
    "RAFGITTOOLS_COMMIT_EVIDENCE_ENVELOPE_V1",
    "TOKEN_VAZIO",
    "HUMAN_DIGNITY",
    "CHILD_SAFETY",
    "INCLUSION_NONDISCRIMINATION",
    "ACCESSIBILITY_INCLUSION",
    "SAFE_HEALTHY_WORK",
    "RiskSeverity",
    "UrgencyClass",
    "humanFirstRequired",
    "ENV-HUMAN-AUTHORITY-",
    "ENV-HUMAN-NONCOMPENSATORY-",
    "EXACT_HEAD_CI",
    "SERVER_ENFORCEMENT",
    "TRUSTED_TIME",
    "TRANSPARENCY_LOG",
    "INDEPENDENT_REVIEW",
    "PUBLICATION_ANCHOR",
    "AI_AGENT",
    "ENV-CI-HEAD-MISMATCH",
    "ENV-APPROVAL-PLAN-MISMATCH",
    "ENV-TIME-SUBJECT-MISMATCH",
    "ENV-LOG-SUBJECT-MISMATCH",
    "ENV-CORRELATED-TIME-LOG",
    "ENV-SELF-REVIEW",
    "ENV-PRIVACY-007",
    "1748365262L",
    "claimAllowed: Boolean = false",
]

REQUIRED_TESTS = [
    "missing human dignity blocks draft ready and merge",
    "child safety fail is non compensatory",
    "human safeguard cannot bypass with not applicable",
    "AI cannot certify human dignity pass",
    "human safeguard pass without risk typing is unresolved",
    "wrong head CI never allows ready or merge",
    "approval for different plan never allows draft",
    "token vazio trusted time blocks merge",
    "timestamp for different artifact is rejected",
    "correlated trusted time and transparency witnesses are rejected",
    "same producer cannot author source and satisfy independent review",
    "private payload disclosure is fail closed",
    "scientific profile requires typed publication date",
    "AI generation is not verification by identity",
]

REQUIRED_DOCS = [
    "UNKNOWN != PASS",
    "TOKEN_VAZIO != 0",
    "SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM",
    "PUBLIC_PROOF != PUBLIC_PAYLOAD",
    "AI_GENERATED != VERIFIED",
]

REQUIRED_HUMAN_DOCS = [
    "HUMAN_DIGNITY > DELIVERY_SPEED",
    "CHILD_SAFETY > FEATURE_COMPLETION",
    "UNKNOWN_HUMAN_IMPACT != SAFE",
    "AI_ASSESSMENT != HUMAN_AUTHORITY",
    "HUMAN_DIGNITY",
    "CHILD_SAFETY",
    "INCLUSION_NONDISCRIMINATION",
    "ACCESSIBILITY_INCLUSION",
    "SAFE_HEALTHY_WORK",
    "legal_compliance_certified: false",
    "claim_allowed: false",
]


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    errors: list[str] = []
    for name, path in FILES.items():
        if not path.is_file():
            errors.append(f"MISSING:{name}:{path.relative_to(ROOT)}")

    if errors:
        emit(errors)
        return 2

    envelope = FILES["envelope"].read_text(encoding="utf-8")
    tests = FILES["tests"].read_text(encoding="utf-8")
    docs = FILES["docs"].read_text(encoding="utf-8")
    human_docs = FILES["human_safeguard_docs"].read_text(encoding="utf-8")
    screen = FILES["screen"].read_text(encoding="utf-8")

    for needle in REQUIRED_ENVELOPE:
        if needle not in envelope:
            errors.append(f"ENVELOPE_MISSING:{needle}")
    for needle in REQUIRED_TESTS:
        if needle not in tests:
            errors.append(f"TEST_MISSING:{needle}")
    for needle in REQUIRED_DOCS:
        if needle not in docs:
            errors.append(f"DOC_MISSING:{needle}")
    for needle in REQUIRED_HUMAN_DOCS:
        if needle not in human_docs:
            errors.append(f"HUMAN_DOC_MISSING:{needle}")

    if "mergeAllowed" not in screen or "Commit Evidence Gate" not in screen:
        errors.append("SCREEN_GATE_NOT_EXPOSED")
    if "HUMAN-FIRST" not in screen or "HUMAN_DIGNITY > DELIVERY_SPEED" not in screen:
        errors.append("SCREEN_HUMAN_FIRST_NOT_EXPOSED")
    if "risk=" not in screen or "urgency=" not in screen:
        errors.append("SCREEN_HUMAN_RISK_URGENCY_NOT_EXPOSED")
    if "privatePayloadIncluded: Boolean = false" not in envelope:
        errors.append("PRIVACY_DEFAULT_NOT_FAIL_CLOSED")
    if "artifactDigest: String?" not in envelope:
        errors.append("CANONICAL_ARTIFACT_DIGEST_MISSING")
    if "class UnboundEvidenceAdapter" not in envelope or "EvidenceState.TOKEN_VAZIO" not in envelope:
        errors.append("UNBOUND_ADAPTER_MUST_RETURN_TOKEN_VAZIO")
    if "claimAllowed: Boolean = true" in envelope:
        errors.append("CLAIM_DEFAULT_TRUE_FORBIDDEN")

    emit(errors)
    return 0 if not errors else 2


def emit(errors: list[str]) -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "schema": "RAFGITTOOLS_COMMIT_EVIDENCE_SOURCE_GATE_V1",
        "state": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "reproducibility_seed": 1748365262,
        "errors": errors,
        "files": {
            name: {
                "path": str(path.relative_to(ROOT)),
                "sha256": sha256(path) if path.is_file() else None,
            }
            for name, path in FILES.items()
        },
        "invariants": [
            "HUMAN_DIGNITY>DELIVERY_SPEED",
            "CHILD_SAFETY>FEATURE_COMPLETION",
            "UNKNOWN_HUMAN_IMPACT!=SAFE",
            "AI_ASSESSMENT!=HUMAN_AUTHORITY",
            "UNKNOWN!=PASS",
            "TOKEN_VAZIO!=0",
            "AI_GENERATED!=VERIFIED",
            "PUBLIC_PROOF!=PUBLIC_PAYLOAD",
            "SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM",
        ],
    }
    OUT.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(payload, indent=2, sort_keys=True))


if __name__ == "__main__":
    raise SystemExit(main())
