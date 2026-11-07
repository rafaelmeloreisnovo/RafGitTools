# RafGitTools HyperAtlas — source-scope admission V1

Producer: rafaelmeloreisnovo/RafGitTools
Federated routing: rafaelmeloreisnovo/Mapa
Private custody: authorized Google Drive
Owner types: GitHub User and Organization must stay separate.

This additive module is an **offline metadata-only** companion to archive_line_index_v1.py. It consumes a previously authorized small JSON provider snapshot and emits a private read-only index. It never calls GitHub or Drive APIs; never downloads, extracts, backs up, or alters archive/repo contents. Public GitHub stores code and synthetic tests ONLY.

## Re-entry / low-friction route

1. Read current Drive CURRENT_STATE and START HERE Ω V2.2 before history.
2. Reuse existing private Drive 001_PONTEIRO_RAFGITTOOLS_ATLAS_V1 with 00_DISPATCH and 02_AI_METADATA; no competing Atlas.
3. Treat GitHub rafaelmeloreisnovo (User) and instituto-Rafael (Organization) as separate installations and ACL boundaries. Include private organization repositories only under authenticated permission.
4. Locate archives already in Google Drive by original file ID, retaining parent ID, MIME, size or null, and observation time. ZIPRAF is only a format candidate; chunked .zip.001 is a split part, not a standalone ZIP.
5. Ingest a bounded PRIVATE metadata snapshot with explicit complete=false unless provider pagination was independently verified. Keep index and receipt inside a private destination; never in CI logs or a public checkout.
6. Gate deep scan, range reads, GitHub-to-Drive mirror, and claims on independent authorization, exact-source evidence and readback.

## Input contract (synthetic only)

{"schema":"rafgittools.hyperatlas.source-scope/v1","provider":"google_drive","scope_id":"test_folder","observed_at":"2026-11-07T10:00:00Z","complete":false,"items":[{"id":"example","name":"large.zipraf","mime_type":"application/x-zip","size":"4000000000","parents":["test_folder"]}]}

For GitHub: provider=github_installation, account={"login":"ExampleOrg","type":"Organization"}, items with id, full_name, visibility, default_branch. Never infer complete=true from a partial page.

## Bounded local commands

    cd tools/rafaelia_navigator
    python3 -m unittest -v test_source_scope_manifest_v1
    python3 source_scope_manifest_v1.py --input /private/snapshot.json --output /private/new_run_directory

The output directory MUST be new and PRIVATE. The tool creates 0700/0600 permissions, writes INDEX.jsonl and RECEIPT.json with an index SHA-256, and refuses a preexisting destination. Input is bounded to 8 MiB and 10,000 items. It is not a whole-Drive scanner.

## Evidence boundaries / rollback

- SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
- No private filenames, Drive IDs, zip contents, OAuth credentials or organization-private inventory committed to public GitHub.
- Remote ZIP64 range reads, Drive pagination, provider OAuth, relay, GitHub backup, independent archive verification and Android E2E remain NOT_IMPLEMENTED here.
- Synthetic unit tests demonstrate only the offline parser; org and Drive coverage stay partial until checked.
- Rollback is the unmerged draft branch/PR; do not delete or change source archives or canonical Drive indices.
- claim_allowed=false; TOKEN_VAZIO is not zero, PASS or permission.

R3=<F_ok:offline admission, F_gap:provider and E2E unknown, F_next:private paginated provider adapter and exact-head CI>.
