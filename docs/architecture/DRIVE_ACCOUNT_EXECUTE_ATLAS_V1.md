# RAFAELIA Ω — Drive Account Select → Execute → Catalog V1

State: **REVIEW_BRANCH / CODE_WRITTEN / HOSTED_CI_NOT_RUN / PROVIDER_E2E_NOT_RUN / claim_allowed=false**.

## Current authority and routes
- Bootstrap: Drive START HERE Ω V2.2 → CURRENT_STATE → LEADER(coordination) → Mapa(authority) → Atlas(routes).
- Source/custody: selected Google Drive account + user-selected folder.
- Producer: RafGitTools/scripts/drive_debianum_recursive_stream_v2.py (already implemented).
- New wrapper: scripts/drive_account_catalog_v1.py.
- Private index: existing Supabase project Rafcodephi, raf_private.drive_metadata_index_v1, raf_private.drive_metadata_scan_receipts_v1, drive_cloud_selection_intents_v1.
- RMRCTI measurement producer: llamaRafaelia/rmrCti (rafa_cti_scan.c → triad_cti_couple.py → OMEGA); never interpret ΔP as physical attractor evidence without RMRCTI's falsifiers.

## Intended human UX (NOT YET WIRED)
1. Select Google account via provider OAuth chooser, *not* a hard-coded public account list.
2. Select the folder/root within that exact account.
3. Press **Executar** and authorize a bounded, metadata-only scan.
4. Host executor verifies OAuth principal matches selected account; scans provider pages recursively.
5. Private catalog folder receives START_HERE.md, CATALOG.jsonl, RECEIPT.json only on complete scoped success.
6. Privileged gate may later ingest *approved metadata only* into raf_private tables, with readback.
7. UI displays current run, gaps and indexed tree. A scan receipt is NOT proof of source file bytes.

The isolated wrapper implements steps 4–5 for a separately supplied valid OAuth token or offline fixture. It does NOT provide a mobile chooser, persistent worker, Google Drive destination upload, archive-member expansion, or Supabase writer. No private payload belongs in public GitHub/CI.

## No-friction private output
The executor must use an existing private directory with mode 0700 and a **new** child directory. Example (placeholders only):

    mkdir -m 700 -p /private/rafaelia-atlas
    GOOGLE_DRIVE_OAUTH_ACCESS_TOKEN=<supplied-by-private-broker> \
      python3 scripts/drive_account_catalog_v1.py \
      --account-alias selected01 \
      --expected-email selected@example.invalid \
      --root-id <selected-real-folder-id> \
      --output /private/rafaelia-atlas/SCAN_0001

Never hard-code tokens in a shell script, repo, command line, workflow artifact or log. The bearer must be injected by a private authorized OAuth broker, not by this module. Account binding uses the provider's Drive about(user.emailAddress), not an untrusted label.

## Invariants and checkpoints
- Metadata-only, standard-library host runtime, no download or decompression of Google Drive file bytes.
- Admit supported MIME types, reject sensitive names, verify parent, UUID/Drive ID and times in producer's existing scanner; unknown/rejected entries are counted and not presented as indexed.
- Atomic private output with exclusive lock; no existing destination replaced.
- SHA256 of *metadata JSONL* represents metadata record stream identity. It is **not** original file content SHA256.
- A complete in-scope scan is not a global Drive crawl or proof of all accounts. If pagination fails, publish no partially labeled complete catalog.
- No automatic migration, trigger, merge, publish, cross-account copy, or training. RMRCTI/ΔP/stability analyses are separate, opt-in and evidence-gated.
- Structure: account → root → folder → file → [archive member: TOKEN_VAZIO] → [line: TOKEN_VAZIO] → [token: TOKEN_VAZIO].
- Relations: parent, same-source-version, historical-successor, inferred-semantic (candidate only), invalid/superseded. No cartesian product of all nodes.

## Contract tests
    python3 -m unittest -v tests/test_drive_account_catalog_v1.py

Checks: atomic private output, no overwrite, error atomicity, invalid slot, OAuth principal mismatch, private directory enforcement. Synthetic wrapper tests do not establish Google provider E2E.

## Next gates
G1: exact hosted-head Python tests & independent review.
G2: private authorized OAuth account-selection adapter + real pagination pilot.
G3: selected-root readback + private Drive folder destination upload and digest check.
G4: privileged Supabase metadata ingestion (existing raf_private tables) + negative RLS checks + readback.
G5: optional ZIP/TAR/RMRCTI content analysis separately authorized by source/revision.

**STOP:** Without SOURCE, AUTHORITY, EXECUTION_TARGET and EVIDENCE_RULE, write TOKEN_VAZIO and route BLOCKED. SOURCE≠ARTIFACT≠EXECUTION≠EVIDENCE≠CLAIM.
