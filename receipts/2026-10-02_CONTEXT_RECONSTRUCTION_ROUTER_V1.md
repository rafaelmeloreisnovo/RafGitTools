# Receipt — Context Reconstruction Router V1 — 2026-10-02

```text
receipt_id=RGT-CONTEXT-RECONSTRUCTION-ROUTER-V1-20261002
repo=rafaelmeloreisnovo/RafGitTools
branch=docs/context-reconstruction-router-v1-20261002
pre_receipt_head=9f31be963231d644469dbf6554d15f6744dd84e5
state=IMPLEMENTED_UNTESTED
claim_allowed=false
```

## Intent

Create a small canonical routing layer so humans and AIs can reconstruct RafGitTools context from references, authorities and evidence boundaries without scanning or copying the full corpus.

## Source / authority

Existing authorities reused rather than replaced:

- `docs/RAFGITTOOLS_CURRENT_STATE.md`
- `docs/INDEX.md`
- `docs/architecture/RAFGITTOOLS_CONTEXT_WORKBENCH_INTEGRATION_AUDIT_V1.md`
- `contracts/context-bundle-v2.schema.json`
- `scripts/context_bundle_v2.py`
- `app/src/main/kotlin/com/rafgittools/workspace/ContextBroker.kt`
- `tools/rafaelia_navigator/README.md`
- `docs/AI_SESSION_DISPATCH_ADAPTER_V1.md`
- `docs/RAFGITFS_GOVERNED_GIT_WRITE_V1.md`

Drive START HERE remains documentary/longitudinal authority; RafGitTools remains producer authority for this implementation.

## Delta

Added:

- `docs/navigation/CONTEXT_RECONSTRUCTION_START_V1.md`
- `configs/context-reconstruction-routes.v1.json`
- `contracts/context-reconstruction-seed-v1.schema.json`
- `examples/context-reconstruction-seed/minimal.example.json`
- `scripts/validate_context_reconstruction_registry.py`
- `tests/test_context_reconstruction_registry.py`

Updated:

- `docs/INDEX.md` with a quick human/AI reconstruction ingress.

## Control properties

- `source_min` constrained to 1..3 refs by registry policy and validator.
- seed is reference-first; raw corpus/prompt/response payload keys are rejected by the structural validator.
- route requires authority, execution target and evidence rule.
- unknown/missing route state preserves `TOKEN_VAZIO` and fails closed.
- `claim_allowed=false` is mandatory in registry and seed.
- no ContextBundleV3, no parallel middleware and no replacement of existing ContextBroker/Navigator/RafGitFS contracts.

## Evidence state

```text
SOURCE=materialized on branch
ARTIFACT=7-file delta plus index update
EXECUTION=NOT_RUN
EVIDENCE=TOKEN_VAZIO_EXACT_HEAD_CI
CLAIM=BLOCKED
```

The validator/test are present but their execution is not claimed by this receipt. Exact-head CI/provider evidence must be observed after a pull request/workflow run.

## Rollback

Branch is additive and isolated. Main is unchanged until an authorized merge. Rollback is branch close/revert; historical authorities remain untouched.

## R3

`F_ok`: compact START router, machine-readable service registry, reference-first seed, structural validator and docs ingress materialized.

`F_gap`: exact-head validator/test/CI result is `TOKEN_VAZIO`; current-head runtime/release state remains outside this documentation/control delta.

`F_next`: open draft PR → observe exact-head workflows → repair only evidence-backed regressions → append successor evidence receipt/longitudinal μWRITE.
