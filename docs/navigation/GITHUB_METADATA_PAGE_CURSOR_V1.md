# RAFAELIA Ω — GitHub metadata page cursor adapter V1

Authority: RafGitTools control plane (hosted Python stdlib, GPL-3.0-or-later project). Status: DRAFT / HOLD. No source files from private Drive are published by this addition.

This is not a second Atlas, provider crawler, API client, freestanding kernel, or Supabase migration. It is a bounded page-to-checkpoint adapter used by the existing context reconstruction router. Provider-sourced page bytes must be acquired by an explicitly authorized host adapter, including pagination and branch GET. The adapter does NOT authenticate that source itself.

One deliberate page, up to 100 repository references, is accepted per invocation. The JSON input contract is:

- schema = rafaelia.github_metadata_page.v1, provider = github, source_owner = exact owner
- page_index (zero based); cursor_in / cursor_out: opaque strings or null
- repositories[]: id, full_name, default_branch, head_sha, head_source_url
- unknown head_sha must equal typed TOKEN_VAZIO and head_source_url must be null
- a known 40-hex head_sha must pair with the syntactically matching GitHub branch endpoint; a URL alone is NOT provider authenticity

Example invocation on an explicitly staged non-secret page:

    python3 -I -B tools/github_metadata_page_cursor_v1.py --page-json /private/input/page0.json --ledger /private/receipts/github-census.jsonl --expected-owner rafaelmeloreisnovo

Replay same page yields ALREADY_COMMITTED without rewriting. A next page must match cursor_out, have exactly the next page_index and use new provider IDs. Missing, changed, duplicated, tampered or untyped inputs stop without promoting claims. Per-page receipt contains normalized metadata refs, raw input SHA-256, predecessor SHA-256, and an append-only hash chain, with fsync and exclusive POSIX file lock. SHA chain provides tamper evidence only when its checkpoint is independently anchored; a full malicious ledger replacement remains possible otherwise.

Source endpoint must be authenticated independently and stored with immutable source/response evidence before promotion to Supabase. To reconstruct the baseline 153 repositories, use page cursors, then separately fetch default-branch HEAD for each matching repository: a repository-list page does not prove its branch SHA. Do not fill the 153 missing heads by inference.

No SQL/DML, secret, external network call, Android/ART/DEX/ELF runtime, hardware watchdog or physical device claim. Host Python and OS syscalls are *outside* freestanding L0; L0 C/ASM policy remains in its producer. SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM. claim_allowed=false; release_allowed=false.

Tests: synthetic offline negative fixtures in tools/test_github_metadata_page_cursor_v1.py, executed through the existing Debianum metadata-only gate (no extra workflow). Repeated cursor-out tokens and cross-page repository name collisions are also rejected. Rollback before merge: close PR; after merge: reviewed revert/successor, never truncate receipts.
