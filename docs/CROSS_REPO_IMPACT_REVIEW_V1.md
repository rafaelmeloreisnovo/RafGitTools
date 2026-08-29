# CROSS_REPO_IMPACT_REVIEW V1

Status: **IMPLEMENTED_LOCAL / LIVE_INTEROP_TOKEN_VAZIO**  
Authority: `RafGitTools` governs transport/provenance checks; producer repositories remain authoritative for their own artifacts.

## Purpose

A repository-local PASS does not imply cross-repository compatibility.

```text
LOCAL_PASS != CROSS_REPO_COMPATIBLE
```

This contract converts cross-repository impact review from prose into a deterministic, fail-closed gate. It is intentionally narrower than scientific validation, runtime proof, provider enforcement, security review, or release authorization.

## Epistemic boundary

```text
schema-valid != semantically-compatible
fixture-pass != live producer→consumer execution
hash-match != authorship
hash-match != scientific truth
documentation != runtime evidence
CI-pass != provider-side promotion authorization
TOKEN_VAZIO != false != 0 != null
```

Synthetic fixtures exist to falsify the validator. They are not live interoperability evidence.

## Contract

- Normative structure: `schemas/cross_repo_impact_review.v1.schema.json`
- Executable semantics: `tools/validate_cross_repo_impact_review.py`
- Fixture suite: `fixtures/cross_repo_impact/suite.v1.json`

## Change classes

`SCHEMA`, `STATE_SEMANTICS`, `RELATION_SEMANTICS`, `DIGEST_CANONICALIZATION`, `BINARY_ABI`, `RUNTIME_PREFIX`, `CLI_PROTOCOL`, `AUTH_POLICY`, `PRIVACY_RETENTION`, `CLAIM_PROMOTION`, `ROLLBACK`, `METRIC_IDENTITY`.

Purely internal changes that do not alter an exported contract are outside this gate.

## Fail-closed falsifiers

| Code | Falsifier |
|---|---|
| `STRUCT_REQUIRED_FIELD_MISSING` | required contract field absent |
| `SCHEMA_VERSION_UNSUPPORTED` | consumer receives unknown contract version |
| `GOLDEN_DIGEST_MISMATCH` | observed golden digest differs from expected |
| `GOLDEN_FILE_DIGEST_MISMATCH` | declared digest differs from referenced fixture bytes |
| `TOKEN_VAZIO_COERCED` | epistemic unknown replaced by false/0/null/default |
| `PATH_IDENTITY_COLLISION` | one revision identity hides divergent content |
| `PROVIDER_CONTEXT_UNBOUND` | required status context renamed/unobserved/not enforced |
| `APPROVAL_SHA_STALE` | approval is bound to a different HEAD |
| `DEVICE_RECEIPT_ROLE_REUSE` | one physical receipt reused for ARM32 and ARM64 |
| `LEGACY_RUNTIME_PREFIX_PRESENT` | promoted RAFCODEΦ surface embeds legacy Termux prefix |
| `METRIC_SERIES_HETEROGENEOUS` | one statistical pool mixes identity dimensions |
| `CLAIM_WITHOUT_CROSS_REPO_RECEIPT` | claim attempted without cross-repo execution |
| `INCOMPATIBLE_WITHOUT_MIGRATION` | incompatible change lacks migration contract |
| `INCOMPATIBLE_WITHOUT_ROLLBACK` | incompatible change lacks concrete rollback anchor |

## Run

```bash
python3 tools/validate_cross_repo_impact_review.py \
  --suite fixtures/cross_repo_impact/suite.v1.json
```

Use `--json` for machine-readable results.

## First real interoperability pair

Producer: `rafaelmeloreisnovo/RafGitTools#357`  
Consumer: `rafaelmeloreisnovo/RafPolimata#298`

The same `artifact_ref` fixture must be emitted and consumed without digest drift. The consumer must preserve source facts separately from derived relations.

Until exact producer and consumer revisions execute together:

```text
cross_repo_execution_receipt = TOKEN_VAZIO
claim_allowed_from_interop = false
```

A synthetic fixture MUST NOT be substituted for this receipt.

## Independent external gates

This validator cannot synthesize ARM32/ARM64 device execution, real RAFCODEΦ package-stack evidence, production signing, provider-side rulesets, independent approval, external scientific replication, or real repeated-series benchmark data. Missing evidence remains typed `TOKEN_VAZIO` with a falsifiable next step.

## Zombie / stub / placeholder rule

Textual appearance alone is not deletion evidence. Every candidate is classified first:

```text
REQUIRED
COMPATIBILITY
EXPERIMENTAL
DEAD_PROVEN
UNKNOWN
```

Deletion is authorized only for `DEAD_PROVEN` after reference/consumer search, build/test impact, rollback anchor, deletion provenance and a negative test showing absence is safe. `UNKNOWN` remains a gap.

## Anti-regression

A breaking producer contract must preserve backward compatibility or increment/version the contract and provide migration + rollback. The producer freezes the contract before the consumer freezes implementation.

## Definition of Done for #387

```text
schema validates
AND positive fixture passes
AND every negative fixture fails for its expected typed reason
AND one real producer→consumer pair executes
AND receipt binds both exact commits
AND rollback/migration behavior is represented
AND unresolved external/runtime gates remain TOKEN_VAZIO
```

The fixture conditions are executable here. The live producer→consumer receipt remains deliberately unpromoted until observed.
