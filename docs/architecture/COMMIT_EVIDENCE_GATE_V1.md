# RafGitTools — Commit Evidence Gate V1

Status: `IMPLEMENTED_SOURCE / CI_NOT_OBSERVED / CLAIM_ALLOWED=false`

## Objective

Reduce uncertainty before promoting a governed Git change. The gate does not claim absolute truth or absolute failure-proofing. It constrains promotion so that unknown or missing evidence remains explicit and blocks higher-risk transitions.

```text
UNKNOWN != PASS
TOKEN_VAZIO != 0
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
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
- append-only transparency-log inclusion proof.

Scientific/publication provenance may additionally bind DOI/publication metadata when applicable.

## Promotion lattice

```text
DRAFT_ALLOWED
  requires PRE + ACT

READY_ALLOWED
  requires DRAFT_ALLOWED + observed execution receipt + exact-head CI

MERGE_ALLOWED
  requires READY_ALLOWED + server enforcement + trusted time + transparency proof
```

V1 intentionally leaves provider CI, enforcement, trusted-time and transparency adapters as `TOKEN_VAZIO` until real evidence sources are connected. Therefore the screen is fail-closed for merge by construction.

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

No single clock, registry, chain or publication is promoted to absolute authority. Independent anchors reduce correlated failure and tampering risk.

## Privacy boundary

Public anchoring MUST minimize disclosure. External services should receive only the digest/attestation material needed for verification; private source payloads, secrets, local paths and unnecessary metadata must not be published.

```text
PUBLIC_PROOF != PUBLIC_PAYLOAD
```

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
claim_allowed: false
```

## Next falsifiers

1. mutate `planHash` after approval and verify draft becomes blocked;
2. introduce unresolved three-way conflict and verify draft becomes blocked;
3. attach CI evidence for a non-head SHA and verify ready/merge remain blocked;
4. provide transparency proof without trusted-time proof and verify merge remains blocked;
5. attempt to coerce `TOKEN_VAZIO` to PASS and fail validation;
6. verify external anchoring never transmits workspace payload content.
