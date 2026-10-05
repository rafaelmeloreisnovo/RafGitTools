# RafGitTools — Commit Evidence Gate V1

Status: `IMPLEMENTED_SOURCE / PRE_ARTIFACT_SOURCE_GATE_WIRED / EXACT_HEAD_CI_PENDING / CLAIM_ALLOWED=false`

## Objective

Reduce uncertainty before promoting a governed Git change. The gate does not claim absolute truth or absolute failure-proofing. It constrains promotion so that unknown or missing evidence remains explicit and blocks higher-risk transitions.

```text
UNKNOWN != PASS
TOKEN_VAZIO != 0
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
PUBLIC_PROOF != PUBLIC_PAYLOAD
AI_GENERATED != VERIFIED
```

## Placement

The gate is instantiated inside the existing `WorkspaceEditorScreen` so it reads the exact same `WorkspaceEditorUiState`. It does not create a second workspace and does not execute GitHub writes itself.

Authorized execution remains in the governed RafGitFS flow:

```text
private workspace
→ staged files
→ base SHA / three-way comparison
→ canonical plan
→ DRY_RUN
→ exact approval bound to planHash
→ rafgitfs/* branch
→ atomic commit
→ non-force push
→ draft PR
→ append-only receipt
```

## Canonical EvidenceEnvelope V1

`CommitEvidenceEnvelope` binds the decision to exact identities and typed evidence:

- repository + ref;
- base commit SHA;
- current plan hash;
- exact PR/head SHA;
- evidence records with producer, independence domain, source reference and proof reference;
- optional scientific publication anchors with typed dates;
- `privatePayloadIncluded=false`;
- `claimAllowed=false`.

The reproducibility test root remains the already established RAFAELIA seed:

```text
1748365262
RAFAELIA_SEED_1748365262_a6a1f608-b889-4803-8f59-d57ce00dba1b
```

The seed is for deterministic falsifier/reconstruction behavior. It is not a cryptographic secret, security random source, timestamp, or scientific replication proof.

## AI / multi-agent boundary

Human, AI-agent, automation, provider and external registry are represented as producers with explicit independence domains.

```text
AI_GENERATED != VERIFIED
PRODUCER_IDENTITY != REVIEW_AUTHORITY
MANY_AGENTS != MANY_INDEPENDENT_WITNESSES
```

The same producer cannot satisfy source authorship and independent review for a critical merge decision. Trusted-time and transparency witnesses must also not collapse to the same independence domain.

## Three phases

### PRE — anteriority and boundary

Required before a draft may be published:

- repository + ref identity;
- private workspace identity;
- observed base commit;
- unresolved three-way conflict count = 0.

### ACT — exact authorized transition

Required before execution:

- canonical `planHash` exists;
- exact human approval matches the current planHash;
- no direct main write, force-push or remote delete is permitted by the RafGitFS contract.

Any material plan change invalidates the approval.

### POST — evidence before stronger promotion

Required before promotion beyond draft:

- execution receipt observed;
- CI bound to exact PR head;
- server-side enforcement read back from provider;
- independent trusted-time evidence;
- append-only transparency-log inclusion proof;
- independent review.

Scientific/publication provenance additionally requires at least one typed publication witness when the `SCIENTIFIC_PUBLICATION` profile is selected.

## Promotion lattice

```text
DRAFT_ALLOWED
  requires PRE + ACT

READY_ALLOWED
  requires DRAFT_ALLOWED + observed execution receipt + exact-head CI

MERGE_ALLOWED
  requires READY_ALLOWED
  + server enforcement
  + trusted time
  + transparency proof
  + independent review
  + typed publication anchor when profile=SCIENTIFIC_PUBLICATION
```

No score can compensate for a missing critical requirement.

```text
100 local PASS != one critical TOKEN_VAZIO
1000 copies != 1000 independent witnesses
```

## Time and anteriority

Git commit timestamps are useful metadata but are not treated as sufficient real-world time evidence.

The target model is multi-anchor rather than a private blockchain:

```text
artifact digest
+ Git blob/tree/commit identities
+ signed TSA timestamp (RFC 3161 class)
+ transparency-log inclusion/consistency proof
+ optional DOI/publication metadata
= stronger anteriority evidence
```

No single clock, registry, chain, AI, provider or publication is promoted to absolute authority. Independent anchors reduce correlated failure and tampering risk.

Publication dates are typed. `registered`, `created`, `issued`, `published-online`, `accepted` and similar meanings must not be collapsed into one unqualified date.

## Privacy boundary

Public anchoring MUST minimize disclosure. External services should receive only the digest/attestation material needed for verification; private source payloads, secrets, local paths and unnecessary metadata must not be published.

```text
PUBLIC_PROOF != PUBLIC_PAYLOAD
absolute_privacy = false
privacy_minimization = measurable objective
```

## Pre-artifact source validation

`scripts/validate_commit_evidence_gate_v1.py` is invoked inside the local `workflow-graph-audit` composite action before `actions/upload-artifact`.

This deliberately separates:

```text
SOURCE_GATE_RESULT != ARTIFACT_UPLOAD_RESULT
```

If GitHub artifact quota fails after the validator ran, source evidence remains observable in the job log and the generated local receipt, while downstream tests remain `NOT_RUN`/`TOKEN_VAZIO` rather than being promoted to PASS.

The validator checks presence and hashes of the envelope, screen, tests and this document, plus fail-closed invariants and falsifier names.

## Standards mapping

This architecture is compatible in intent with:

- ISO 8000 provenance/data-quality concepts;
- ISO 9001 process, risk and continual-improvement discipline;
- ISO/IEC 27001 information-security risk management;
- NIST CSF Govern/Identify/Protect/Detect/Respond/Recover;
- NIST SSDF secure-development practices;
- SLSA provenance/verification concepts;
- in-toto signed step/layout evidence;
- RFC 3161 trusted timestamping;
- append-only Merkle transparency logs such as the model used by Sigstore Rekor.

This document does not claim certification or compliance with those standards.

## Explicit non-claims

```yaml
absolute_privacy: false
absolute_antifailure: false
absolute_truth: false
blockchain_required: false
doi_required_for_code_commit: false
provider_ci_connected: false
provider_enforcement_connected: false
trusted_time_connected: false
transparency_log_connected: false
publication_registry_connected: false
independent_review_observed: false
claim_allowed: false
```

## Falsifiers implemented in source tests

1. exact-head CI with the wrong head SHA must block Ready/Merge;
2. `TRUSTED_TIME=TOKEN_VAZIO` must block Merge;
3. trusted-time and transparency proofs from the same independence domain must be rejected;
4. source producer cannot self-satisfy independent review;
5. `privatePayloadIncluded=true` must fail closed;
6. scientific profile requires typed publication date;
7. AI generation and AI verification with the same producer identity must be rejected as independent review.

## Remaining falsifiers

1. mutate `planHash` after approval and verify draft becomes blocked in the UI/ViewModel integration path;
2. introduce unresolved three-way conflict and verify draft becomes blocked;
3. connect real provider evidence and reject stale/non-head CI;
4. connect RFC 3161-compatible evidence and reject invalid signature/time binding;
5. connect transparency evidence and reject missing inclusion/consistency proof;
6. verify external anchoring transmits only minimized digest/attestation material;
7. execute Android/JVM tests on the exact PR head after provider artifact capacity is restored or a non-weakening evidence sink is available.
