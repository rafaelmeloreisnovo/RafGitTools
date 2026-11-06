# Codex task — Debianum Drive metadata streaming, gated Supabase admission V1

State: `REVIEW_BRANCH / IMPLEMENTED_UNTESTED / claim_allowed=false`.
Producer: `rafaelmeloreisnovo/RafGitTools`.
Federation: Mapa START HERE V2.2 / CURRENT_STATE; Drive official = source/custody; Supabase Rafcodephi `raf_private` = private index, **not binary archive**.

## Reuse, no competing ingestion engine

- Reuse `LAB_RAPIDTOOLS_OMEGA/driver_map/README.md` for typed driver states and UI.
- Existing `scripts/drive_custody_rmr_pair_v1.py` handles *exact-byte custody*, not the metadata-only pass; do not run it on huge Debianum images.
- New `scripts/drive_debianum_metadata_stream_v1.py` streams **Drive API metadata pages only** to JSONL stdout, with no file downloads, no script or `ps` execution, and no direct database mutation. No third-party Python packages.
- `tests/test_drive_debianum_metadata_stream_v1.py` is the offline negative-fixture gate. No tests were executed by this documentation commit.
- Root folder ID, OAuth bearer token, account selection, privileged Supabase executor and any session data stay in **private authorized configuration**, never committed here.

## Source admission by file attributes

For every Drive `files.list` item, require exact ID, parent, name, MIME, modifiedTime, valid size on stored files; use `nextPageToken` until exhausted or fail closed as `PAGE_LIMIT_PARTIAL`.
Provider creation timestamp `1970-01-01` is an invalid placeholder: store `TOKEN_VAZIO_CREATION_TIME`, not claimed historical date.
Classify hidden names (`.ssh`, `.bash_history`, `.pid_ls`, `.config`, etc.), shell scripts, archives and opaque binaries as quarantined **by path/type**; never read their bytes or send their bodies to Supabase. They remain source-preserved in Drive.
Large text blobs may enter a **private metadata index only**; size in bytes is descriptive, not evidence of equality, content hashing, live execution or archival restoration. No app storage download.
Provider `md5Checksum` or `sha256Checksum`, if available, is provider-reported and **not** a verified readback digest.
An historical `ps`/PID snapshot cannot establish which processes currently run in Debian; physical process evidence requires a separate authorized runtime readback.

## Route to Supabase — only after gate

`DRIVE_SOURCE@ID → metadata_page → typed_admission → immutable_fingerprint → approved_privileged_executor → raf_private.drive_metadata_index_v1 → idempotent readback → append-only receipt`.

- Use existing `raf_private.drive_metadata_index_v1`, `reconstruction_batches_v1`, RLS FORCE and no `anon`/`authenticated` SELECT. Do **not** expose the private schema to public Data API or put service-role secrets in Android, web code or CI logs.
- Do not grant the blocked `google_drive_official` graph driver READY merely because metadata is readable; its separate execution-authority gate remains independent.
- Batch only accepted metadata; keep `content_evidence=NOT_READ`, `source_content_sha256=NULL`, `original_preserved=true`, `claim_allowed=false`.
- No direct Supabase sink is implemented in this review branch. The authorized MCP SQL write was unavailable in the initiating session: `REMOTE_WRITE_BLOCKED`. Do not work around that block.
- If the privileged executor is unavailable, emit a typed, complete-or-partial receipt with `TOKEN_VAZIO_EXECUTOR`, preserve the JSONL stream privately only if safe, and stop.

## Falsifiers and acceptance

1. Offline tests reject hidden PID, shell, opaque, bad parent, bad size and invalid manifest JSON; valid giant-file metadata never triggers body transfer.
2. Host interpreter can import/compile the adapter and run `python -m unittest discover -s tests -p 'test_drive_debianum_metadata_stream_v1.py'` (not yet run).
3. Provider pagination exhausts or reports `PAGE_LIMIT_PARTIAL`; do not turn first 100 entries into a global count.
4. Private Supabase insert/readback checks exact ID/version, deduplication and zero unexpected modifications to prior Mapa/Drive records.
5. CI/Android/process `ps` runtime = `NOT_RUN` until independently receipted; no auto-merge to main.

Rollback: abandon review branch or append a superseding receipt; never mutate/delete Drive originals or previous evidence. `SOURCE≠ARTIFACT≠EXECUTION≠EVIDENCE≠CLAIM`.

## Verify a private structural derivative

For a V1 recovered-shard feature artifact, use
`scripts/verify_structural_vectors_v1.py` after obtaining private source and
artifact readbacks through an authorized transport. Supply both byte digests
from a trusted receipt.

```sh
python3 scripts/verify_structural_vectors_v1.py \
  --source "$SOURCE_JSON" --artifact "$FEATURES_JSON" \
  --source-sha256 "$SOURCE_SHA256" --artifact-sha256 "$FEATURES_SHA256"
python3 -m unittest discover -s tests -p 'test_verify_structural_vectors_v1.py'
```

The verifier checks byte identity, source-order ordinals, the seven declared
integer counts and their aggregates. The JSON receipt reports
`structural_derivation=VERIFIED` only after all checks succeed. Overall state
is `VERIFIED_LIMITED` because the V1 artifact leaves the recipe for
`conversation_ref_sha256` unspecified. That field receives format validation
and retains `TOKEN_VAZIO_DERIVATION_RECIPE`.

Inputs are bounded to 64 MiB for the source and 2 MiB for the artifact.
Duplicate keys and nonfinite JSON literals fail closed. Failures exit 3
with a typed error. Receipts contain hashes and aggregate counts.
The synthetic suite runs in the START coherence job alongside the existing
NOVOexport genealogy tests, including draft pull requests. Corpus bytes, real identifiers and private receipts stay outside Git.
This hosted Python standard-library utility runs on explicitly supplied
readbacks. Provider location and permissions require their own evidence.
