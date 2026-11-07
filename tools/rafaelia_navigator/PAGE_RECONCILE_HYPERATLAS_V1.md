# HyperAtlas page reconciliation V1 — offline, private metadata

**Producer:** `rafaelmeloreisnovo/RafGitTools`. Existing local module `scope_page_reconcile_v1.py` extends the same Navigator without replacing its archive index. **No provider credentials, no remote calls, no ZIP extraction.**

## Resume in <= 3 reads

1. Canonical Drive `CURRENT_STATE` and `START HERE Ω V2.2`.
2. Private `001_PONTEIRO_RAFGITTOOLS_ATLAS_V1/00_DISPATCH`, then `02_AI_METADATA` (files remain in their original Drive folders).
3. This producer contract + `source_scope_manifest_v1.py` and the tests; pull only the affected provider scope.

The provider adapter exports **private metadata pages** as `page-000.json`, `page-001.json`, etc., each with `schema=rafgittools.hyperatlas.source-scope/v1`, `provider`, `scope_id`, `observed_at`, `complete=false`, `items`, and a `page` block:

```json
{"ordinal":0,"method":"drive.files.list","terminal":false,"provider_incomplete":false,"next_token_sha256":"<64 lowercase hex characters>"}
```

A terminal page has `terminal=true` and `next_token_sha256=null`. Never put raw OAuth tokens, pagination tokens, private file names, Drive IDs, organization-private repo inventories or archive bytes in public GitHub, CI or chat output. The private page JSON exists only under an authorized Drive/private executor. `method` must be one of `drive.files.list`, `drive.folder.fetch`, `github.installation.list`; do not silently merge inconsistent methods.

```sh
cd tools/rafaelia_navigator
python3 -m unittest -v test_source_scope_manifest_v1 test_scope_page_reconcile_v1
python3 scope_page_reconcile_v1.py --input-pages /private/pages --output /private/new-run
```

The output `INDEX.jsonl` and `RECEIPT.json` are private. For any raw page, no current operation is authorized to copy contents, unzip large files or push data to GitHub. `ENUMERATED_UNVERIFIED_SNAPSHOT` means only the observed traversal ended; it is **not** verified totality or a transactional snapshot. No terminal page, duplicate IDs, changed IDs or provider `incompleteSearch` result in `PARTIAL_OR_CONFLICTED` with explicit gaps. A ZIPRAF name does not establish a ZIP64-compatible format; split files require a reconstructible part manifest.

## Observed provider discrepancy (2026-11-07)

A Drive folder fetch of the existing `Zip` folder reported 19 immediate children (18 files + one subfolder), while a filtered Drive search for that same folder returned 2 rows. This is a **provider-method coverage discrepancy**: neither is promoted to verified global truth by comparison alone. A GitHub installation page for `instituto-Rafael` contained 30 repositories; the next offset page returned zero. Treat this as an observed traversal of one organization installation, **not** a consistent historical snapshot or an assertion about other installations.

## Falsification and rollback

12 synthetic unit tests passed locally against the exact predecessor normalizer on 2026-11-07; exact Git blob content was verified after write. No exact-head CI or Android physical E2E claimed. Private source metadata has been kept in Drive, not committed here. No original Drive file or GitHub main branch was modified. Keep PR in DRAFT/HOLD until independent provider pilot, pagination reconciliation and privacy review. Corrections supersede through an append-only receipt; never silently delete historical records.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM; `TOKEN_VAZIO != 0`; `claim_allowed=false`.
