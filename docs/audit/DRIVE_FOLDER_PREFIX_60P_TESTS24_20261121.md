# RAFAELIA Ω — Scoped private folder capture and local producer proof (2026-11-21)

**Status:** EVIDENCED_SCOPED / NONTERMINAL / B0160 BLOCKED. This public file is a pointer only: no raw provider IDs, page tokens, account identifiers, credentials, or source content.

## Authorities and exact refs

- Canonical route: Drive AGENTS.md → START HERE Ω V2.2 → CURRENT_STATE B0159; Mapa controls federation. RafGitTools owns local code/contracts, Supabase private schema owns retained page manifests.
- Source branch: `rafcode/drive-parent-nullsafe-manifest-20261121-hold`, base `a07efb3fe4def800a61514fd255c66cbb4243e31`, no PR, no merge.
- Metadata source functions: `scripts/drive_debianum_metadata_stream_v1.py` (Git blob `68991e57904ad3ad3f84e601acc8c8a840ba68b0`) and `scripts/drive_debianum_recursive_stream_v2.py` (Git blob `888d74a67cc067456b09c77d7234f7e9e55a69d1`).
- Exact scoped excerpts matched source SHA256: V1 `44042fc33ff1a850a3c6a105573255c002c0fb84d47386648d53cf493d2f69e7`; V2 `4a9add9b9261306fce543ef0d2fc788814708dffad0dcb7c4e76f5bb38734758`.

## Observed evidence — private, no raw IDs here

- Private migrated table `raf_private.drive_folder_capture_pages_v1` and append batch function `raf_private.drive_folder_capture_append_batch_v1`. Exact migrations: `20261121153012` and `20261121153346` respectively. `security_invoker` remains in metadata view V2; page table uses FORCE RLS and explicit public grants revoked.
- Two independent pass IDs each have **60 persisted nonterminal pages, 27,600 positions and 27,600 unique exact provider IDs**, with zero cross-page duplicates, cursor chain breaks and page-hash readback discrepancies for that prefix.
- Direct ID comparison: A-only=0, B-only=0, 27,600 exact matches; selected creation/modification time and parent metadata changed on 0 joined identities. This says nothing about untraversed pages or provider snapshot isolation.
- All 2,310 B0154 historical folder IDs were found in that prefix, so the previous 2,074 "not in metadata projection" cases were **not lost from this provider observation**. Another 25,290 observed folder IDs were outside B0154's historical membership.
- Of 396 folders in the metadata projection, 357 appeared in the saved prefix. The other 39 cannot be characterized as missing while the crawl is nonterminal.
- **Private append-only source receipt:** `MU-INVENTORY-FOLDER-CAPTURE-60P-20261121-S1`, continuity `seq=222`, predecessor `MU-INVENTORY-B0160-TERMINAL-PROBE-20261118-S4`, recorded SHA256 computed server-side with parent-hash binding; readback exact. `claim_allowed=false`.
- Source verification contract: [read-only private SQL verifier](../supabase/verify_drive_folder_capture_scoped_v1.sql). Public markdown does not replicate private manifests.

## Local executable evidence and limits

- `tests/test_drive_identity_partition_reconcile_v1.py`: **17 PASS_LOCAL** (synthetic page/identity falsifiers).
- `tests/test_drive_metadata_fingerprint_contracts_v1.py`: **7 PASS_LOCAL** using SHA256-verified exact *function excerpts* of V1/V2 from original GitHub commit, plus matching constants. **Not the complete producer modules**. Their content fingerprints are metadata canonical JSON hashes, not source-byte equivalence.
- Combined test command `python -m unittest discover -s tests -p 'test_drive_*v1.py' -v`: **24/24 PASS_LOCAL** in a separate Python 3.13 execution environment. GitHub Actions CI=NOT_RUN, Android device=NOT_RUN, byte-content hashes=TOKEN_VAZIO.
- A subsequent multi-tool continuation call was **blocked because the platform could not determine the safety status of the request**. The particular nested tool or cause is TOKEN_VAZIO. Do not attribute it to a specific provider, source item, page, or cursor; do not bypass the block. Readback after the block confirmed no new pages and a released writer lease.

## Next controlled gates

1. Keep retained observations private, typed and immutable. Do not publish page tokens/raw IDs or infer universal coverage from the observed prefix.
2. When tool safety status is resolvable by approved means, finish two *independent complete* provider page chains and store terminal receipts; if a cursor expires, record that explicitly instead of fabricating continuity.
3. Audit both full ID sets, exact membership, metadata timing, duplicate IDs, and page-hash readback, separately reporting provider snapshot isolation as NOT_PROVEN unless there is provider consistency evidence.
4. Do not calculate source-byte SHA256 without separate authorized original-byte custody. Provider metadata fingerprints are not proof of content equivalence.
5. B0160 promotion remains BLOCKED pending integrated writer lease, exact predecessor, immutable partitions, canonical receipt and independent readback. No automatic workflow, PR merge or release.

R3: F_ok=24 scoped local tests + private 60-page prefix with B0154 full membership; F_gap=nonterminal globally incomplete provider capture, unknown safety blocker provenance, historical content hashes; F_next=authorized continuation and global falsifiers only when blocker is removed through normal controls.
