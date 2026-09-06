# RafGitTools — Documentation Audit Receipt — 2026-09-06

**Receipt class:** `DOCS_ONLY_PROVENANCE`  
**Repository:** `rafaelmeloreisnovo/RafGitTools`  
**Base revision:** `56f4ce95158e6b8a1dbfa4fd8c029937aea20224`  
**Working branch:** `docs/documentation-audit-2026-09-06`  
**Policy:** `SOURCE_OBSERVED → DOC_ALIGNED → CROSSCHECKED → RECEIPTED`  
**Code mutation authorized:** `false`  
**Claim promotion authorized:** `false`  
**Release promotion authorized:** `false`

## Purpose

Reconcile the active documentation with the current source tree and evidence boundaries without modifying application code, scripts, workflows, schemas, configs, tests, assets or machine-state JSON.

## Source observations used

### Current repository identity

- current base `main@56f4ce95158e6b8a1dbfa4fd8c029937aea20224`;
- current base merged PR #414 (`ci: fix TruffleHog event commit range`) on 2026-09-06.

### Repository Governance source anchors

Observed at the exact base revision:

- `app/src/main/kotlin/com/rafgittools/data/github/RepositoryGovernanceApiService.kt`;
- `app/src/main/kotlin/com/rafgittools/data/github/RepositoryGovernanceAudit.kt`;
- `app/src/main/kotlin/com/rafgittools/data/github/RepositoryGovernanceReceiptStore.kt`;
- `app/src/main/kotlin/com/rafgittools/ui/screens/settings/RepositoryGovernanceMutationPlan.kt`;
- `app/src/main/kotlin/com/rafgittools/ui/screens/settings/RepositoryGovernanceViewModel.kt`;
- `app/src/main/kotlin/com/rafgittools/ui/screens/settings/RepositoryGovernanceScreen.kt`;
- corresponding governance tests.

The mutation-plan source explicitly blocks unknown provider pre-state, non-reversible mutation and provider drift. Its plan is executable only with proven authority, non-archived repository, no blockers and reversible items.

The receipt-store source explicitly maintains a V2 local append-only SHA-256 chain and distinguishes local receipt recording from provider acceptance/re-probe.

### FNEXT receipt validator

Observed:

- `scripts/rafaelia_receipt_validator.py`;
- schema `rafaelia.cross-repo-receipt.v1`;
- evidence levels from `TOKEN_VAZIO` through `INDEPENDENTLY_REPRODUCED`;
- event-specific maximum promotion;
- fail-closed rule against `claim_allowed=true` while unresolved `token_vazio` exists.

Structural validity is intentionally narrower than runtime/device/scientific proof.

### Historical execution anchor preserved

The 2026-08-14 build receipt remains historical and commit-bound:

- commit `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`;
- Android Client Build run `31821491676`;
- APK SHA-256 `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`;
- dual ABI observed;
- device physical proof remained `TOKEN_VAZIO`.

It was **not** promoted to current-main BUILD evidence.

### Current-main workflow observation

For exact current base `56f4ce...`, Human Impact Cross-Repo Gate V1 run `34031951218` was observed with conclusion `success`.

The complete set of current-head workflows was not individually re-read to terminal results in this documentation transaction, therefore:

```text
current_head_complete_workflow_inventory = TOKEN_VAZIO
current_head_build_receipt                = TOKEN_VAZIO
```

## Documentation findings

1. Active current-state text still framed PR #346/#347 and the 2026-08-14 branch as present state.
2. `STATUS_REPORT.md` still centered on the 2026-08-14 BUILD checkpoint.
3. `RAFGITTOOLS_ROADMAP_TRUE.md` still used the PR #347 successor gate as active roadmap head.
4. `README.md` mixed current source descriptions with July feature/file percentages and counts that were not revision-bound.
5. `CODE_TO_DOC_MAP.md` did not route the current Repository Governance surfaces and still labeled GPG as a stub.
6. `ECOSYSTEM_RUNTIME_STATE.json` remains a 2026-08-14 machine-state snapshot. It is outside the docs-only mutation scope and was therefore not rewritten.
7. Historical canonical evidence under dated paths is valid provenance and must not be overwritten to simulate freshness.

## Documentation changes

Changed only documentation:

- `README.md` — rewritten as compact evidence-aware current router;
- `docs/INDEX.md` — current order of truth, historical/machine-state distinction;
- `docs/RAFGITTOOLS_CURRENT_STATE.md` — current-main source/evidence cut;
- `docs/STATUS_REPORT.md` — current classification and open gates;
- `docs/RAFGITTOOLS_ROADMAP_TRUE.md` — evidence-ordered operational roadmap;
- `docs/CODE_TO_DOC_MAP.md` — current code→docs routing;
- this receipt.

Preserved unchanged:

- source code;
- tests;
- workflows;
- configs/schemas;
- `ECOSYSTEM_RUNTIME_STATE.json`;
- dated canonical receipts/evidence.

## Change commits before this receipt

```text
505d776a461c905149d55f1278ac55ccbbf64039  current state
6d4218926953e0a6022c47456cd4d0d78c8a78f1  status report
13880a5704920c76932267472f4df16d1fdb7110  operational roadmap
6bba49f65bb064041b89b8ff6bd4db003a3e0b5d  documentation index
c7cb08b50e95606f4af3f5a8d577123d9613235f  code-to-doc map
511c1ce34ecdfdfb9b46e40758c5b8553f43c97d  README router
```

The receipt creation commit necessarily follows these entries and is not self-hashed into its own body.

## Non-promotions / TOKEN_VAZIO

```text
current main complete CI/build inventory = TOKEN_VAZIO
current machine-state regeneration       = TOKEN_VAZIO_REGEN_REQUIRED
physical device execution                = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
provider-real fixture matrix             = TOKEN_VAZIO_RUNTIME
PTY/VT100                                 = TOKEN_VAZIO_PTY
external LLaMA/model runtime             = TOKEN_VAZIO_RUNTIME
signed release provenance                = TOKEN_VAZIO_RELEASE
exact current feature/file counts        = TOKEN_VAZIO_RECOUNT_REQUIRED
claim_allowed                            = false
release_allowed                          = false
```

## Acceptance criteria for this documentation transaction

- all changed paths are documentation paths or root `README.md`;
- no source/test/workflow/config/schema/machine-state change;
- current mutable docs no longer present PR #346/#347 as present operational state;
- historical receipts retain original revision meaning;
- unsupported current counts/claims use `TOKEN_VAZIO`;
- generated/machine state is not manually falsified into freshness;
- final branch-vs-base diff is inspected before delivery.

## R3

- **F_ok:** source-to-document drift on the active routing layer is corrected with explicit provenance and no claim promotion.
- **F_gap:** generated/machine state and runtime/device/release gates require their own executable processes.
- **F_next:** verify the final changed-path set, open a reviewable docs-only PR and keep every non-document delta at zero.