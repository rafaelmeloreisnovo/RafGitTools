# RafGitTools × Google Drive — Custody CI with RMR/pai V1

**2026-10-15** · Owner: RafGitTools · authority: GitHub code and CI, private Google Drive custody, existing RMR/pai hosted hash adapter · NO DELETE · NO AUTO MERGE.

## Existing program

This is a narrow successor of docs/federation/DISTRIBUTED_CUSTODY_FABRIC_V1.md and docs/architecture/RAFGITTOOLS_BIBLIOTHECA_DRIVE_CONTROL_PLANE_V1.md. Preserve ONE canonical .github/workflows/START.yml; its source-bound validators are extended in scripts/validate_rafaelia_workflow.sh. No additional active workflow entrypoint. The phone is a bounded orchestrator/dispatcher, not the permanent corpus or heavy build runner. Google Drive is storage and receipt custody, NOT a compute runner. Computation belongs to an authorized cloud execution provider or a separately evidenced runtime. No second storage provider is introduced now.

## Working artifact path

GitHub Actions exact-run artifact -> authenticated downloaded ZIP -> private Drive raw upload -> provider returns fileId -> Drive raw readback -> verify equal byte size and SHA-256 -> private append-only documentary receipt. For stronger dual-hash verification, the existing RMR/pai BLAKE3 adapter validates both origin and downloaded bytes and chains the successor.

One real source-backed transfer was completed 2026-10-15: RafGitTools workflow run 38711297966, head 4f3425a3930b03621b3a0a82165429ef2cf5170f, artifact ID 11961380420, 741 bytes. GitHub reported SHA-256 ad7cd3c711d7e4b73ecf559a17fa0f115385ddbdcbfd4ef7df71a30301ea50cf. The same SHA-256 was computed over the complete raw Drive readback. The private Google Doc receipt has the actual Drive fileId, owner/ACL observation and parent ID; no private IDs, personal bytes, conversation shards or tokens are copied here.

RMR/pai BLAKE3 was NOT EXECUTED for that real transfer. Its evidence is typed TOKEN_VAZIO_BLAKE3_RMR_PAI_NOT_EXECUTED, not a claim of dual-hash closure.

## Executable host adapter

scripts/drive_custody_rmr_pair_v1.py verifies:
- identical byte length and SHA-256 for GitHub-origin artifact and Drive raw readback;
- SHA-256 matches independently observed GitHub Actions artifact digest;
- real RMR/pai binary: pai run --mode cli hash --file FILE --algo blake3;
- two BLAKE3 known-answer tests (empty, abc), source/readback BLAKE3 equivalence;
- canonical JSON domain-separated receipt chain SHA-256 and create-only output file.

If pai does not exist or KAT fails, the verifier BLOCKS. It never calls a different algorithm while claiming BLAKE3. It never authenticates to Drive, deletes data, writes main, promotes PRs or replaces previous receipts. The caller must separately prove that the Drive file ID and GitHub metadata were obtained from the real providers.

The authorial RMR/pai implementation is under rafaelmeloreisnovo/BLAKE3, rmr/core/hash_blake3.c and rmr/core/cli.c. The previously observed exact RMR source commit is 584a686ed68d3fb0189797a661884678a78df01b. The primitive and C implementation are upstream BLAKE3; RMR contributes the hosted adapter, tooling and provenance. A matching digest does not prove hardware identity or a cryptographic signature.

## Source test and end-to-end validation scopes

Unit falsifiers are in tests/test_drive_custody_rmr_pair_v1.py. They mock the BLAKE3 backend solely to exercise negative control-flow, so they are NOT cryptographic KATs. The canonical pipeline runs those tests via scripts/validate_rafaelia_workflow.sh. The real pai executable must be built from the pinned source on an authorized cloud runner and run against actual source and Drive readback bytes. That executable stage is not an Android freestanding binary and not yet proven in this PR.

Public source does not hold Drive account ID, file IDs, secrets, private shards or a recoverable restricted fragment. The Drive side stores the actual locators, receipt and ACL readbacks under a dedicated user-owned custody subtree. Additional providers are out of scope until first Drive custody is robust.

## R3

F_ok: one Google Drive real ZIP upload, provider metadata and raw readback, exact SHA-256 equality with GitHub reported artifact digest, private documentary receipt; separate hosted script/test/START gate source added.

F_gap: actual BLAKE3 check over this real transferred artifact; pinning proof for built pai binary; large-scale retries and cross-provider file age/eTag control; signed independent timestamp; external reviewer/provider enforcement.

F_next: run exact pinned RMR/pai gate on that same ZIP source+Drive bytes and append successor receipt; avoid reopening prior object. Never promote source/test results as runtime/review/security claims.
