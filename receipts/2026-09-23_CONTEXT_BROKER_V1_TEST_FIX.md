# Receipt — ContextBroker merged-test regression correction

Date: 2026-09-23
Parent: merged PR #482 / head `d6ef451fd358fe91a335d1083323deddc9ed8796`
Supersedes evidence claim: `receipts/2026-09-23_CONTEXT_BROKER_V1.md` test status for the merged head
Kind: `REGRESSION_CORRECTION / APPEND_ONLY`
claim_allowed: `false`

## Observed failure

Canonical START run `35819309370` reached Android unit tests and failed before lint/APK. The merged test source contained over-escaped Kotlin string literals in `json_uses_contract_snake_case_fields`, so the exact merged head cannot be promoted to TEST_PROVEN/BUILD_PROVEN.

## Correction

Only the JSON assertion string literals are corrected. ContextBroker runtime semantics are unchanged.

```text
PR #482 merged source = IMPLEMENTED
PR #482 Android unit test gate = FAIL
PR #482 lint/APK = NOT_RUN / skipped downstream
corrected branch exact CI = TOKEN_VAZIO until successor run completes
device = TOKEN_VAZIO
```

R3=<F_ok: failure localized to test-source literal escaping; F_gap: successor exact-head CI; F_next: run full START gate and promote only if the corrected head passes>.
