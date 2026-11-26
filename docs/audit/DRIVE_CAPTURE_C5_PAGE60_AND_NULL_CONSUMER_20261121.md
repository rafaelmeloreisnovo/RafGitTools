# 𓂀ΔΦΩ — RAFAELIA C5 | page-60 custody + NULL-aware consumer

Date: 2026-11-21. **Classification: public-safe scoped receipt only**. Private Drive object IDs, account numbers, filenames, page cursors, and provider metadata were NOT replicated into this public repository.

## Authority and predecessor
- GitHub producer: `rafaelmeloreisnovo/RafGitTools` branch `rafcode/drive-parent-nullsafe-manifest-20261121-hold`, subject to `AGENTS.md`.
- Canonical routing: official private Drive AGENTS.md → START HERE Ω V2.2 → CURRENT_STATE B0159; original B0159 receipt and partitions unchanged.
- Predecessor public scoped receipt: `docs/audit/DRIVE_CAPTURE_C4_SCOPED_FALSIFICATION_READBACK_20261121.md`; C4 corrective successor `docs/audit/DRIVE_CAPTURE_C4_PARENT_STATE_CORRIGENDUM_20261121.md`.
- Private source: `raf_private.drive_folder_capture_pages_v1`; verified consumer `raf_private.drive_folder_capture_scoped_quality_v1`. Scope: one Google Drive folder-only listing account/query, two separately paginated captures, not global files of all kinds.

## One append-only capture batch (C5)
- Reused private, provider-owned continuation tokens from page 59 of each nonterminal capture, without exposing them publicly.
- Fetched new provider page 60 for each capture, 460 folder IDs each. Each page had 460 unique IDs; no mismatch in the 460-ID sets or selected metadata across A/B.
- Independently checked new page IDs against the previous 60-page private prefix before writing; zero cross-prefix collisions observed for the A capture. B matches the previously identical A prefix.
- Appended **two pages/920 positions in one private batch** using the existing `drive_folder_capture_append_batch_v1` function, append-only triggers, source account/query chain verification, SHA-256 page-manifest digests and writer lease fencing token **48**. SQL result: `pages_added=2,positions_added=920`; lease released.
- Independent SQL readback: page 60 of each capture has 460 positions and 460 unique IDs; the stored request cursor matches predecessor page 59 and page digest recomputes correctly.
- Consumer view readback: each capture now has **61 consecutive pages, 28,060 positions and 28,060 distinct IDs**, zero duplicate positions, zero digest mismatches, zero cursor-chain faults, 6 explicit JSON-null parent rows, and status `CONSISTENT_PREFIX_NONTERMINAL`. The actual A/B intersection is 28,060 IDs; metadata differences inside captured prefixes = 0.
- Historical B0154 join: all 2,310 folder IDs fall within the stored prefix. This is not full provider coverage.
- Both page-60 continuations are nonterminal. `claim_allowed=false`, provider snapshot isolation `NOT_PROVEN`, source-file-byte SHA-256 `TOKEN_VAZIO`.

## Surgical consumer hotfix (isolated branch)
- Problem: earlier offline `drive_identity_partition_reconcile_v1.py` raised `PARENT_IDS_INVALID` on real provider `parent_ids=null`, even though six folder IDs per capture were returned with explicit null and six corresponding current Drive metadata reads provided no parent list.
- Rule after hotfix: explicit null is typed distinctly from a missing field and an empty array; an unrepresentable sentinel outside the valid ID alphabet marks explicit null in metadata-only SHA-256 receipts. Existing array-parent hash behavior is preserved.
- Script predecessor Git blob: `9886951af4d92b1a6da9dba51d3df16bb37183ac`.
- Script successor Git blob: `5e0499af6c18548ca6eedacb91265ac90703fb80`; commit `6567a844c5914c560506fa295a16aef345fc9f0d`. Exact content readback passed.
- New test file blob: `939d51825079c8b510583388f7ccf639d3047fd0`; commit `4a43f465cdf144e5b6e2eecb58cd74923b4b5923`. Exact content readback passed.
- **Test evidence:** reconstructed exact pre-hotfix Git source by verifying its Git blob ID locally, then ran **17 original + 9 adversarial synthetic tests = 26/26 PASS_LOCAL** against the patched full consumer source. Includes null vs empty, null vs parent, field absent vs null, nonterminal and provider completeness failures. This does not establish an end-to-end full-provider consumer run or a GitHub Actions CI pass.
- Code write under fencing token **49**, released. No `main` change, PR, CI dispatch, permissions change or merge.

## Current evidence boundaries and rollback
- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.
- C5 source pages have an append-only storage receipt; neither original pages nor checkpoints were overwritten.
- These two captures do **not** constitute independent transactionally consistent Drive snapshots; agreeing data does not prove global temporal stability.
- The offline consumer's **complete capture** format still intentionally rejects nonterminal final pages; validation of current stored prefixes is scoped to the read-only SQL auditor until a terminal manifest exists. Never fabricate a provider `incomplete_search=false` response.
- All content-byte hashes and global source claims remain `TOKEN_VAZIO`.
- For a failure, create an append-only corrective successor with predecessor reference; do not erase the page rows or replace B0159 without separate authority, acceptance and readback.

## R3
`F_ok`: one scoped fenced 920-position batch + cursor/digest readback + patched consumer under 26 local tests.
`F_gap`: remaining terminal pages, global 60,687-position identity proof, independent snapshot/change token, content-byte evidence, exact-head producer CI.
`F_next`: advance another bounded nonterminal page batch in a future manufacturing cycle, or verify a provider changes-token baseline if connector supports it, then obtain terminal status and external CI evidence. Keep B0160 blocked.

`CURRENT_STATE=B0159_COMMITTED` · `B0160=BLOCKED` · `CLAIM_ALLOWED=false`.
