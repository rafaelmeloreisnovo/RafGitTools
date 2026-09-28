# START HERE — TEMPLO_VIVO_ARC V1

State: `IMPLEMENTED_UNTESTED` · `claim_allowed=false`

## Intent
Provide a privacy/provenance control plane for pointer-only intake from `rafaelmeloreisnovo/Rafaelia_Private`, an opaque visual/pixel anchor that performs no OCR, and a deterministic adaptive cadence.

## Current route
```text
Rafaelia_Private/inputs/templo_vivo_arc/queue
→ pointer manifest
→ structural/privacy gate
→ optional visual anchor (hash/fingerprint only)
→ governed action
→ sanitized receipt
→ schedule-state transition
```

## Authority
- private body/classification/authorization/retention: `Rafaelia_Private`
- orchestration/validation/receipt model: `RafGitTools`
- secrets/key bytes: external secret store / GitHub Secrets, never Git
- claim promotion: blocked by default

## Files
- `../config.v1.json` — root cadence and intake policy.
- `../token_vazio.v1.json` — typed missing-state dictionary.
- `../engine.py` — deterministic validator/scheduler/visual anchor.
- `../tests/test_engine.py` — stdlib unit tests.
- `../README.md` — architecture and operational limits.

## Known gaps
- `TOKEN_VAZIO_PRIVATE_RUNNER_BINDING`: no live cross-repo reader is activated here.
- `TOKEN_VAZIO_KEY_ID`: no key material is stored or inferred.
- `TOKEN_VAZIO_HOURLY_PROMOTION_RULE`: the number of successful hourly cycles before 12-hour cadence was not specified.
- `TOKEN_VAZIO_TWELVE_HOURLY_PROMOTION_RULE`: the number of successful 12-hour cycles before daily cadence was not specified.
- 1–4 minute cadence is not representable by GitHub Actions scheduled events; it requires an actually running local loop/service.

## Next
Run the synthetic pointer fixture through `engine.py validate-intake`, then execute the unit suite on the exact branch head and bind the result to a receipt.
