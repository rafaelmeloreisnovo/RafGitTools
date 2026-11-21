# RAFAELIA Ω — scoped producer size guard / provenance successor (2026-11-21 C3)

**Status:** CODE_WRITTEN_READBACK_SCOPED; `claim_allowed=false`. No inventory checkpoint promotion. No PR, merge, publish action, workflow_dispatch, or deployment. Public metadata summary only; private file IDs/names/account credentials excluded.

## Source/authority/target
- Production code authority: `rafaelmeloreisnovo/RafGitTools`, private-governed workflow but public repository. Branch: `rafcode/drive-parent-nullsafe-manifest-20261121-hold`, isolated from `main`.
- Source predecessor blobs (Git objects): V1 `68991e57904ad3ad3f84e601acc8c8a840ba68b0`, V2 `888d74a67cc067456b09c77d7234f7e9e55a69d1`.
- Updated code blobs read back: V1 `2a6de89a5946004efff739e1d414c2cdaa8cd468`, V2 `384447021c804008443fbb7aecf5bb4fd86b8712`.
- Updated synthetic contract test blob: `6cf57bb832c5585831a3daf7e4fd48b3e5e51f81`; source path `tests/test_drive_metadata_fingerprint_contracts_v1.py`.
- Write lease: global resource RAFAELIA:INVENTORY:DRIVE_GITHUB; fencing token 42 validated immediately before each code mutation, released with `released=true`.
- Change: both producer functions reject boolean and non-(int,str) byte sizes before `int()`; valid folder/native-doc and integer handling remain code-compatible. No historic data overwrite.
- Commit chain (GitHub observed): `728295d6cf3f44867735ee2e7080a0777f7eab6d` → `6fd6833a2af4e29d787ac5212467fea16c1414ac` → `87f9111e8de0a3916ad61a4780419fceb2521cc1`.
- Readback: all three updated content strings compared byte-for-byte against retrieved contents; reported Git blob SHAs match updates.

## Tests and falsifiers
- **Local test execution:** 8/8 synthetic source-extracted producer size tests PASSED in an isolated Python harness. Tests include bool True/False, float truncation, valid decimal string/int, negative/overflow, folder NULL size and native Doc absence of size.
- **Source of 16 test definitions:** 7 previous fingerprint tests + 9 size tests in GitHub branch, confirmed as file content, but **not executed on exact full-module branch HEAD**. `IMPLEMENTED_UNTESTED` for this 16-test suite, not CI PASS.
- **CI:** combined status / workflow runs for last code commit empty; `NOT_PROVEN`.
- **Preservation:** Supabase private view `drive_metadata_identity_nullsafe_v2` still has 11,810 indexed identities, 134 NULL-parent undercount cases, and zero allowed global claims.
- **Provider read-only scope:** 12 nonterminal pages yielded 5,520 distinct folder IDs, no same-page or cross-page duplicates within that scope; a later unbounded extension failed on connector call limit and yielded no complete manifest.
- **Reconciled closed partitions:** 3 additional parent-folder scopes of 19, 17 and 16 items matched their earlier metadata index sets in two terminal observations each; a fourth scope has 29 current items against 15 historical, with 14 current non-folder items each created after 2026-11-05, absent from consulted historical index/B0154. This is new coverage, NOT a proven loss or duplicate.
- **Privacy:** new individual provider IDs and filenames are not present in this public file; no source bytes were read.

## Next acceptance gates
1. Execute the 16 full-module test definitions in a controlled runner, publish scoped CI receipt only on actual terminal result, no automatic deployment.
2. Reconstruct complete per-page identity manifests in private custody with scope, UTC timestamps, cursor references kept secret, stable provider+account+file key, and immutable append-only partitions; audit cross-page duplication.
3. Independently repeat the same query scope and compare actual identity sets, metadata changes and source timestamps. Provider snapshot isolation is still NOT_PROVEN even if two passes agree.
4. Admit the 14 new folder-child metadata observations only through the authorized private producer admission contract and a fenced writer; do not infer byte hashes from metadata.
5. Keep `CURRENT_STATE=B0159_COMMITTED`, `B0160=BLOCKED` until predecessor/partition/receipt/checkpoint readback and B0160 acceptance are proved.
6. Rollback code by append-only successor/reversal commit on isolated branch only if regressions observed; preserve original blobs and receipts.

R3: F_ok = code write/readback and provider scoped identity checks; F_gap = exact-head executable CI, global ID manifest, cross-provider dedup/content bytes, provider snapshot isolation; F_next = validate full-module producer tests in trusted runner and persist next single private, complete scoped partition under fresh lease.
