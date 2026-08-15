# License / README Verification Ledger

Append-only verification cache. Reuse a row only while all relevant evidence fingerprints remain unchanged.

| Verified UTC | Target repo | Target ref | Target LICENSE blob | Target README blob | Upstream | Upstream license evidence | Classification | Obligations / finding | Status | Reverify when |
|---|---|---|---|---|---|---|---|---|---|---|
| 2026-08-15 | `rafaelmeloreisnovo/nanoGPT` | `master` | `329db5e33310fd5d98343e2bca690bfb07317526` | `610c4037b5fc0787a9f600c73449e9398ed9f03e` | `karpathy/nanoGPT` | upstream `LICENSE`, MIT, copyright 2022 Andrej Karpathy; verified from official upstream | `FORK_UPSTREAM` / `THIRD_PARTY_WITH_USER_MODIFICATIONS` | Preserve upstream MIT copyright + permission notice in copies/substantial portions. README already describes local repository as a fork with enhancements. No independent authorship of upstream core inferred. | `VERIFIED_LIMITED` | target LICENSE/README changes; upstream identity/license changes; new third-party structure enters scope; authorship boundary is promoted |

## Ledger semantics

- `VERIFIED`: exact scope and obligations sufficiently evidenced for the recorded decision.
- `VERIFIED_LIMITED`: license/README facts verified, but broader file-by-file structural provenance remains incomplete.
- `TOKEN_VAZIO`: evidence insufficient.
- `LEGAL_REVIEW_REQUIRED`: factual provenance may be known, but legal consequence needs qualified review.

## Cache algorithm

Before repeating a completed review:

1. Resolve current target LICENSE and README blob SHAs.
2. Resolve upstream identity and current license evidence/ref.
3. Compare against the latest ledger row for that target/scope.
4. If fingerprints match and no new third-party scope is present, reuse the result and record no duplicate audit.
5. If any relevant fingerprint differs, create a new append-only row after re-verification. Never overwrite the previous row.

Date is provenance metadata, not the freshness gate. Content/ref change is the freshness gate.
