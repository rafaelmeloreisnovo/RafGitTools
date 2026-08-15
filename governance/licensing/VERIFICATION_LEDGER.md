# License / README Verification Ledger

Append-only verification cache. Reuse a row only while all relevant evidence fingerprints remain unchanged.

| Verified UTC | Target repo | Target ref | Target LICENSE blob | Target README blob | Upstream | Upstream license evidence | Classification | Obligations / finding | Status | Reverify when |
|---|---|---|---|---|---|---|---|---|---|---|
| 2026-08-15 | `rafaelmeloreisnovo/nanoGPT` | `master` | `329db5e33310fd5d98343e2bca690bfb07317526` | `610c4037b5fc0787a9f600c73449e9398ed9f03e` | `karpathy/nanoGPT` | upstream `LICENSE`, MIT, copyright 2022 Andrej Karpathy; verified from official upstream | `FORK_UPSTREAM` / `THIRD_PARTY_WITH_USER_MODIFICATIONS` | Preserve upstream MIT copyright + permission notice in copies/substantial portions. README already describes local repository as a fork with enhancements. No independent authorship of upstream core inferred. | `VERIFIED_LIMITED` | target LICENSE/README changes; upstream identity/license changes; new third-party structure enters scope; authorship boundary is promoted |

## Human ownership-routing decisions — 2026-08-15

These rows are routing decisions, not substitutes for reading the applicable upstream license. Before an automated correction, resolve the exact upstream/license evidence and append a fingerprinted verification row above/below as appropriate.

| Target/family | Default ownership route | Deep separated-authorial review? | Operational instruction |
|---|---|---:|---|
| `linuxkernel` / Linux upstream body | `UPSTREAM_THIRD_PARTY` | No by default | Preserve upstream authorship, COPYING/SPDX and file-scoped licensing. Correct only evidenced compliance/documentation gaps. |
| `llamaRafaelia` / llama.cpp upstream body | `UPSTREAM_THIRD_PARTY` | **Yes** | Preserve llama.cpp upstream. Review only clearly separated RAFAELIA modules for independent conception/structure/logic/implementation and compatible licensing. |
| `nanoGPT` | `UPSTREAM_THIRD_PARTY` | No by default | Preserve Karpathy/MIT and fork provenance. Integrated enhancements do not need an ownership claim. |
| `termux-app-rafacodephi` / Termux upstream body | `UPSTREAM_THIRD_PARTY` | **Yes** | Preserve Termux and file/module-scoped obligations. Review only separated RafCodePhi components as possible independent authorial units. |
| `Shizuku` | `UPSTREAM_THIRD_PARTY` | No by default | Preserve upstream license, attribution and applicable brand/asset constraints; do not seek ownership of upstream body. |
| `OpenSSL` | `UPSTREAM_THIRD_PARTY` | No by default | Preserve upstream copyright/license history; correct only evidenced compliance/documentation gaps. |
| `gradle` | `UPSTREAM_THIRD_PARTY` | No by default | Preserve Gradle upstream attribution/license. Integrated maintenance does not become an ownership claim. |
| `qemu_rafaelia` / QEMU upstream body | `UPSTREAM_THIRD_PARTY` | No by default | Preserve QEMU upstream and path/file-scoped licenses. User improvements integrated into upstream body need not be claimed. |
| `florisboard` | `UPSTREAM_THIRD_PARTY` | No by default | Treat upstream code/assets as upstream unless contrary evidence establishes a separately independent component. |
| `BLAKE3` upstream body | `UPSTREAM_THIRD_PARTY` | **Yes** | Preserve BLAKE3 upstream/license options. Review the already separated authorial area only; verify boundary, README/LICENSE/headers and provenance. |
| `LuaJIT` | `UPSTREAM_THIRD_PARTY` | No by default | Preserve Mike Pall/upstream MIT attribution; do not infer ownership from ports or ASM changes. |
| `UserLAnd` / `UserLAnd2` upstream body | `UPSTREAM_THIRD_PARTY` | **Yes** | Preserve upstream. Locate only already separated candidate authorial components and verify independent structure/logic/provenance before any authorial label. |

### Immediate-correction rule

When a review finds an obvious compliance/documentation defect whose governing upstream obligation has already been verified, fix the banal defect immediately on the audit/work branch and append the evidence here. Examples: incorrect/missing README upstream link, attribution/NOTICE index omission, or a proven SPDX/header mismatch. Do not automatically change licenses, copyright ownership, material code, or legal interpretation.

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
