# Custody Chain Taxonomy V1

Status: `IMPLEMENTED_UNTESTED` for the v1.0.1 actor-boundary successor until exact-head CI executes. The v1.0.0 predecessor was provider-tested separately.

## Purpose

This layer does not replace existing RafGitTools custody implementations. It classifies them so Drive, GitHub, CI, physical-device observations, human decisions and assistant tool actions cannot be silently conflated.

Core invariant: `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.

## The three orthogonal questions

Every custody event must answer:

1. **What is in custody?** source identity, document, code, bytes, execution, receipt, authorization, transfer, human observation or assistant tool action.
2. **Who has authority?** documentary, implementation, execution, evidence or decision authority.
3. **Which provider can actually attest the observation?** Drive, GitHub, GitHub Actions, physical device, connected tool or an explicit cross-provider bridge.

## Custody classes

| ID | Custody object | Primary boundary |
|---|---|---|
| C01 | Source identity | locator + exact revision; identity only |
| C02 | Documentary custody | Google Drive file/revision/index lineage |
| C03 | Code custody | GitHub repository/path/commit/blob/PR lineage |
| C04 | Artifact-byte custody | byte length + cryptographic digest + artifact locator |
| C05 | Execution custody | run/device/environment/command observation |
| C06 | Receipt custody | append-only evidence event + predecessor/supersession |
| C07 | Authorization custody | explicit human/provider decision within scope |
| C08 | Transfer-bridge custody | cross-provider identity relation without authority laundering |
| C09 | Human-observation custody | explicit observation, still unpromoted without independent gate |
| C10 | Assistant-tool-action custody | connected-tool action/result; provider readback required for mutation claims |

## Google Drive versus GitHub

**Drive** is the documentary/reconstruction authority: file ID, revision lineage, index placement, receipt custody and longitudinal memory.

**GitHub producer repository** is the implementation authority: repository, path, commit/tree/blob, PR, workflow and code-level contracts/tests.

When the same logical object appears in both systems, do not duplicate the corpus merely to prove linkage. Bridge it with stable identities: `Drive file_id + revision_ref <-> repository + commit/blob/PR ref`, plus digest when byte identity matters.

## Human author versus assistant tool operator

**HUMAN_AUTHOR** may provide intent, authorization, decisions and physical observations. Authorization is not execution evidence, and a human statement about provider state remains an observation until independently read back when that distinction matters.

**ASSISTANT_TOOL_OPERATOR** may route, read, compare, request provider actions and record provider results only inside explicit user scope. It does **not** execute the external provider mutation itself. The connected provider/API applies the mutation and returns the provider result. A tool call therefore proves only the bounded request/result that is actually read back; it does not create human authorization, physical execution or claim validity.

For an external mutation, the custody path is:

`HUMAN_AUTHOR authorization -> ASSISTANT_TOOL_OPERATOR orchestration -> PROVIDER execution -> provider readback/ref -> receipt`

If result/readback is absent, the mutation state remains `PENDING`, `TOKEN_VAZIO` or another typed unresolved state; it must not be called `PASS`.

## Federated authority and actor mapping

The cross-surface control plane is `rafaelmeloreisnovo/Mapa:data/control-plane/CUSTODY_CHAIN_TYPE_REGISTRY.v1.json`. RafGitTools remains a producer-specific implementation authority for its own schema, validator, tests and code.

Local-to-federated actor mapping:

- `HUMAN_AUTHOR -> HUMAN_AUTHORITY`
- `ASSISTANT_TOOL_OPERATOR -> ASSISTANT_ORCHESTRATOR`
- `PROVIDER -> CONNECTOR_PROVIDER`
- `PHYSICAL_DEVICE -> RUNTIME_EXECUTOR`

`INDEPENDENT_REVIEWER` and `TOKEN_VAZIO_ACTOR` remain federated-only roles until a local event explicitly needs them. This avoids inventing local actors while preserving the global distinction.

Core execution invariant: `assistant_orchestration != connector_provider_execution`.

## Existing RafGitTools implementations retained

- `RepositoryGovernanceReceiptStore`: local append-only governance receipts; provider acceptance stays separate.
- `FORENSIC_GIT_PROVENANCE_MODE_V1`: Git provenance and hash-chain events.
- `drive-github-staging-receipt-v1.schema.json`: SAF/Drive staging toward GitHub; now classifiable as C04 + C08 rather than replaced.
- Android/build/runtime receipts: classifiable as C04/C05/C06 depending the proof boundary.

## Fail-closed bridge rules

- Drive documentation cannot substitute for GitHub implementation or runtime evidence.
- GitHub commits cannot substitute for physical-device execution.
- Assistant tool output cannot substitute for human authorization.
- Human authorization cannot substitute for test/run evidence.
- A receipt proves that an event was recorded; it proves external provider acceptance only when provider evidence is bound.
- Cross-repository promotion requires producer and consumer evidence for the claimed boundary.

## Machine-readable artifacts

- `contracts/custody-taxonomy-v1.json` — authority/classes/bridge rules.
- `contracts/custody-event-v1.schema.json` — generic typed custody event envelope.
- `scripts/validate_custody_taxonomy.py` — dependency-free fail-closed validator.
- `tests/test_custody_taxonomy.py` — anti-regression tests.

## Gate

`claim_allowed=false` is the default. Structural validation of this taxonomy does not promote runtime, security, legal, scientific or physical claims.
