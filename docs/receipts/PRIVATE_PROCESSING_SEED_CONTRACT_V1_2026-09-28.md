# Receipt — Private Processing Seed Contract V1 — 2026-09-28

State: SOURCE_MATERIALIZED / BRANCH_READBACK_PASS / LOCAL_TEST_OBSERVED_UNPROMOTED / REMOTE_CI_NOT_RUN / DEVICE_E2E_NOT_RUN
claim_allowed: false

## Scope

Branch: `feat/private-processing-seed-contract-v1`

Parent materialization head before this receipt:

`c3b890d5586aa14b46102a7b4a71c6d02b7a2799`

This receipt records only the semantic-seed contract delta. It does not assert a physical Android run, private GitHub publication, Drive source hash, or end-to-end evidence.

## Materialized artifacts

| Path | Blob SHA |
|---|---|
| `contracts/private-processing-seed-v1.schema.json` | `83c2ded58308c0ec109d462a908ed5c17d2971db` |
| `examples/private-processing-seed.example.json` | `2aab60c22eefb47a32c820f29d2e84c98fadb640` |
| `scripts/validate_private_processing_seed_v1.py` | `caa31c4ffe2ba3d344f6201849f9a6b4520473f7` |
| `tests/test_private_processing_seed_v1.py` | `460425eefeeff37116955a772060cd9be6fe9f06` |
| `configs/private-processing-capability.v1.json` | `33551e885a043c856bf1f70e6c5f9a49b835e27d` |
| `docs/PRIVATE_PROCESSING_CAPABILITY_V1.md` | `7b514aeb959c7ff3c1d6bfe902affa32d0f09614` |

All six paths were read back from the integration branch after the writes.

## Semantic delta

The contract now preserves four separate states:

```text
architecture = DETERMINED
execution    = NOT_RUN
evidence     = TOKEN_VAZIO
claim        = BLOCKED
```

`TOKEN_VAZIO` is defined as an explicit unresolved sentinel for an expected field. It is not interchangeable with null, empty string, zero, false, NOT_RUN, FAIL or PASS.

## Local bounded validation observation

Before repository writes, the exact validator/test content was exercised in an ephemeral Python environment:

```text
seed validator: PASS structural/state coherence only
unit tests: 6 passed
```

Evidence state for this observation: `OBSERVED_UNPROMOTED`.

This does not promote repository CI, physical device execution, or end-to-end evidence.

## Gates

```text
SOURCE_MATERIALIZATION      = PASS
BRANCH_READBACK             = PASS
LOCAL_BOUNDED_TEST          = OBSERVED_UNPROMOTED
REMOTE_REPOSITORY_CI        = NOT_RUN
PHYSICAL_DEVICE_END_TO_END  = NOT_RUN
DRIVE_SELECTED_SOURCE_SHA   = TOKEN_VAZIO
ACTIVITY_RECEIPT_SHA        = TOKEN_VAZIO
CLAIM_ALLOWED               = false
```

## R3

F_ok = semantic seed schema + example + dependency-free validator + six invariant tests + capability/doc integration are materialized and branch-read back.

F_gap = remote CI is not observed; no physical Android execution exists for this delta; selected Drive source/output/receipt hashes remain TOKEN_VAZIO.

F_next = open the integration PR, observe checks without promoting absent evidence, then run one explicit Android SAF source selection only after the code delta is accepted.
