# RAFAELIA Ω — Private Drive metadata identity projection

**Route:** `START HERE Ω V2.2 → CURRENT_STATE → source authority → Supabase private schema → readback → receipt → R3`.

**Owner/authority:** RafGitTools is a source adapter and governance route; the Supabase `raf_private` schema owns execution; Drive owns originals and inventory; Mapa governs federation. This public pointer holds **no private metadata or credentials**.

## Scope and identity

- Private relation: `raf_private.drive_metadata_identity_projection_v1`, populated on query from the immutable `drive_metadata_index_v1` observations.
- Primary comparison key: `(account_alias, source_file_id)`. The same provider ID denotes **provider identity**, not identical bytes.
- `MULTI_ROOT_OBSERVATION` denotes one identity visible under multiple scanned roots; `REOBSERVED_SAME_ROOT` denotes repeated metadata observation.
- Deterministic candidate selection: provider modification time, observation time and UUID tie-breaker. It selects only a **representative metadata observation**, never deletes or rewrites the rest.
- `content_equivalence='TOKEN_VAZIO'`, `claim_allowed=false`. Filename, document title, semantic similarity, root path, MIME or provider version fingerprint are never sufficient proof of content equality.

## Reconstruct without duplicating data

- [Private SQL source](../docs/supabase/drive_metadata_identity_projection_v1.sql) — exact deployed additive view DDL; already applied through Supabase migration `rafaelia_private_drive_identity_projection_v1_20261111`. **Do not replay automatically.**
- [Read-only falsifiers](../docs/supabase/verify_drive_metadata_identity_projection_v1.sql) — original rows retained, group-count invariant, zero unsafe promotions, `security_invoker` and private access boundary.
- [Metadata producer contract](../docs/copilot/TASK_DEBIANUM_DRIVE_METADATA_STREAM_V1.md) — source admission and explicit `NOT_READ` byte evidence.
- Canonical search begins in Drive START HERE Ω V2.2; the private receipt and exact observed counts remain in the authorized Supabase ledger, **not here**.

## PIRC-2 / DMAIC gates

1. Define: deduplicate observation *identity* without changing source data.
2. Measure: count original rows, projected provider IDs, repeated groups and variant scan roots privately.
3. Analyze: distinguish same ID, same version and same source bytes; compare true content only with trusted hashes + source-byte readback.
4. Improve: additive `security_invoker=true` view in non-exposed schema.
5. Control: six adversarial classifier checks, zero promoted claims, original-count conservation and permission readback; Supabase record SHA verified separately.
6. Rollback/supersession: no DELETE and no DROP of source data; if the view contract fails, supersede by reviewed version and append correction receipt. Do not auto-merge or publish a data dump.

## Boundaries: compiler and CI

This metadata projection is PostgreSQL SQL, **not** ARM32/ARM64 machine code. Hosted shell/YAML/Clang/LLVM/SQL are toolchain or orchestration surfaces; only explicitly built native runtime objects can be `-ffreestanding`. No JNI, NDK, SDK, JDK, Gradle, runtime C source or existing CI workflow is changed by this documentation branch. The independent RAFYML L0 multi-ABI gate remains authoritative for that scope.

**Status:** `DATABASE_PASS_SCOPED` / `GITHUB_DRAFT_HOLD` / `CONTENT_EQUIVALENCE=TOKEN_VAZIO` / `claim_allowed=false`.

**R3:** `F_ok` private identity projection + readback; `F_gap` cross-shard, cross-provider and byte-equality proof; `F_next` metadata convergence tests, then source-byte custody only when authorized.
