# RafGitTools — Three-Interaction Evidence Hotfix V2

**State:** `IMPLEMENTED_SOURCE_ONLY / DRAFT_NO_PROMOTE / claim_allowed=false`

This successor consumes the three-interaction ChatGPT evidence bundle produced by RafPolimata.

## Hotfix

The previous V1 gate accepted one operational-evidence receipt. V2 closes the structural gap where **multiple consecutive prompt/response observations must be treated as one ordered causal evidence bundle**.

### P0 gates

- scientific false-positive/overfit risk → falsification mesh;
- ChatGPT session misclassification risk → non-training evidence classifier;
- unsafe auto-correction risk → exact target/reproducer/root cause/rollback gate.

`P0` means **block promotion and route evidence now**. It does not mean “patch automatically regardless of evidence”.

## Decision states

- `HOTFIX_HOLD_TARGET_REQUIRED`: evidence is valid but no target-specific correction envelope exists.
- `HOTFIX_ELIGIBLE_BOUNDED`: exact target + SHA + reproducer + root cause + bounded patch + tests + rollback exist.
- `BLOCKED`: evidence contract or safety invariant failed.

## Rollback

V2 is additive. Closing/reverting this draft removes only the bundle-aware successor; V1 and historical receipts remain intact.
