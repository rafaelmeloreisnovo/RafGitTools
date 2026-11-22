# RafGitTools ↔ RafPolimata: Historical Matrix Geometry 8×3×2 V1

**State:** `IMPLEMENTED_UNTESTED`; `claim_allowed=false`; experimental draft review only.

## Routing / authority

1. **Drive NOVOexport:** March–July 2025 original private conversations, user/assistant role-qualified, source immutability. Do not upload raw text to GitHub logs.
2. **RafPolimata:** sole producer of `scripts/historical_matrix_geometry_v1.py`, `compiler/raf_matrix_geometry_v1.h`, the inherited Matrix Compose V1 encoder and its tests. Exact branch/hash are in `configs/historical-matrix-geometry-federation.v1.json`.
3. **RafGitTools:** typed control-plane routing, fail-closed job schema and local-only receipt verifier. It does not execute arbitrary shell commands and cannot replace producer semantics.
4. **RLL:** independent science repository; no likelihood/covariance/H(z)/cosmology change is introduced by this mathematical model.

## Geometry contract

- 8 cube corner positions (3 binary axes) × 3 layers × 2 contextual oppositions = **48 keyed slots**.
- Missing slot is `TOKEN_VAZIO`, **not** an all-zero matrix.
- Optional mirror on bit axis 0/1/2 is a bijective permutation of source positions.
- Central XOR/weighted/sum/majority folds are **projections**, not reversible archive codecs.
- Integer `10³` grid can be materialized within budget; `1000³` is addressable symbolically but its 48-fold full materialization is rejected.
- Q16 `sqrt(3)/2≈56756/65536` models an integer-scale ratio, not a calibrated physical field.
- RLL scientific claims and any quantum/space-time physical interpretation remain `TOKEN_VAZIO`.

## Files and exact commands

`configs/historical-matrix-geometry-federation.v1.json` pins the producer branch and safe execution boundary.

`schemas/rafaelia_historical_matrix_geometry_job.v1.schema.json` requires a typed job and six exact outputs.

`scripts/verify_historical_matrix_geometry_receipt.py` checks:

- six named artifacts exist and parse;
- IFDex, primary receipt and geometry receipt schemas;
- sha256 consistency of logical matrix, packed byte stream, IFDex, geometry and projections;
- actual packed 10-bit parity **and decoded byte equality**;
- 48 unique canonical slots, provenance under mirroring, 8 corners/parities and Q16 scales;
- 65,536 byte aggregate materialization cap;
- no false reversibility, physical claim, or automatic claim promotion.

Run with a **private local producer output folder**:

```sh
python3 -m unittest -v tests.test_matrix_compose_contract tests.test_historical_matrix_geometry_federation
python3 scripts/verify_historical_matrix_geometry_receipt.py --dir /path/to/private/producer-output
```

Do not publish the `matrix.output.json` bytes or `geometry.output.v1.json` when the source was private; any provider-hosted CI uses exclusively synthetic fixtures. A passing verification is scoped integrity/protocol evidence, not trusted signing, authored originality, hardware, or scientific model validation.

## Acceptance / rollback

- Producer: existing Python matrix-compose regressions; 8×3×2 negative tests; C99 hosted and ARM32/ARM64 syntax gates; exact commit receipts.
- Consumer: synthetic positive/negative receipts plus separately generated producer receipts. `IMPLEMENTED_UNTESTED != PASS`.
- Runtime/device/physical gates: separate and unresolved.
- No GitHub automatic merge, branch protection change, PAT use, RLL dataset modification, release or secrets.

Rollback: close the feature PRs or revert additive files only. Historical source, primary compiler path, and RLL scientific pipeline are unchanged.
