# RAFAELIA Human–AI Bivalent Privacy Middleware v1

**Operation:** `RAFAELIA-HAM-V1-20260722`  
**State:** `IMPLEMENTED_POC / EXACT_CHECKOUT_RUNTIME_TOKEN_VAZIO / CLAIM_ALLOWED_FALSE`

## Invariant

```text
AI -> identify risk, minimize data, explain limits, preserve provenance, abstain
Human -> define intent, approve scope, revoke, appeal, make final decision
```

```text
AI assistance != transfer of human authority
privacy controls != promise of zero risk
file presence != runtime execution
semantic equivalence != byte identity
94% usefulness target != measured usefulness
```

## Purpose

The middleware reduces friction by turning repeated uncertain loops into a finite state machine:

```text
REQUEST
-> RISK_AND_RIGHTS_CLASSIFICATION
-> DATA_MINIMIZATION
-> HUMAN_DECISION
-> DRY_RUN
-> BOUNDED_EXECUTION
-> IMMUTABLE_RECEIPT
-> F_ok / F_gap / F_next
-> STOP_OR_NEXT_EVIDENCE
```

`TOKEN_VAZIO` is accepted only when it contains a reason, a discriminating next step and a loop budget. When no new evidence appears, the system stops instead of generating another low-utility loop.

## People and rights

The contract models:

- data subject;
- affected person or bystander;
- operator;
- domain reviewer;
- privacy reviewer;
- security reviewer;
- rights reviewer;
- auditor.

Health, biometric, location, voice, contact, call-log, SMS and sensor data trigger stronger review. Minor-related data cannot be routed to a public destination and requires privacy plus rights review.

## Privacy and governance

- deny by default;
- raw credentials forbidden in jobs, logs and receipts;
- private/sensitive/secret data cannot be published;
- minimize fields before model access;
- raw sensitive export blocked;
- retention is finite;
- every write requires approval, dry-run and rollback;
- critical risk is blocked in-band;
- irreversible action requires two-step human approval;
- AI cannot execute, finalize or expand scope;
- claim promotion requires verified evidence references.

## Federated roles

| Repository | Role |
|---|---|
| RafGitTools | control plane, consent, authorization, UI, receipt registry |
| RafPolimata | deterministic parser, validator and bounded structurer |
| GAIA_phi | deterministic sensor, manifest and delta layer |
| ZIPRAF_OMEGA_FULL | content-addressed archive and integrity envelope |
| llamaRafaelia | semantic interpreter over bounded/redacted segments |
| Rafaelia_Private | private policy, retention and consent vault |
| termux-app-rafacodephi | local runtime broker and allowlisted executor |
| termux-api_rafcodephi | Android capability adapter with per-capability consent |

## Proof of concept

```bash
python3 -m unittest -v tests/test_human_ai_middleware.py
python3 scripts/validate_human_ai_middleware.py \
  examples/human-ai-middleware/request.safe.json \
  --adapters configs/human-ai-middleware/adapters.v1.json \
  --report artifacts/human-ai-middleware-local-validation.json
```

The validator uses only the Python standard library. It rejects secret material, authority inflation, public exposure of private data, missing consent, missing rights review, unbounded loops, unsupported writes, absent rollback and adapter capability violations.

The observed proof was executed against a local equivalent source bundle before the GitHub branch was fully materialized:

```yaml
local_equivalent_source_tests: 16/16_PASS
local_equivalent_request_validation: PASS
local_equivalent_adapter_count: 8
local_equivalent_findings: 0
exact_git_checkout_execution: TOKEN_VAZIO
source_byte_identity: TOKEN_VAZIO_UNTIL_EXACT_CHECKOUT_RUN
```

This proves the bounded mechanism at PoC scope. It does not claim that the final Git commit bytes have already executed.

## State boundary

```yaml
canonical_contract: IMPLEMENTED
semantic_validator: IMPLEMENTED
adversarial_tests: IMPLEMENTED
local_equivalent_source_validation: VERIFIED
exact_git_checkout_validation: TOKEN_VAZIO
federated_adapter_runtime: TOKEN_VAZIO
Android_device_receipt: TOKEN_VAZIO
measured_usefulness_0_94: TOKEN_VAZIO
claim_allowed: false
```
