# Private Archive & Line Atlas V1 — DRAFT/HOLD

Adapter for RafGitTools/tools/rafaelia_navigator, not a new repo or competing navigator.

## Authority and boundary

RafGitTools: code and local index producer. Mapa: federated route authority. Google Drive: private source/custody. Source files remain read-only. This module has NO Drive OAuth, provider pagination, cloud file download or automatic synchronization. It must not publish private payload to the public GitHub repository or to public CI artifacts.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM. TOKEN_VAZIO != 0. claim_allowed=false.

## Private output structure

    PRIVATE_ATLAS_RUN_001/
      START_HERE.md
      ATLAS.sqlite3
      RECEIPT.json

The SQLite entries table identifies file and ZIP member paths, parent node, size, SHA-256, observed status and depth. Nested ZIP paths use outer.zip!nested.zip!path. The lines table identifies logical line number, fragment number, byte offset, length, SHA-256 and heuristic code/command category. Default does not store plaintext. Explicit --private-text enables local plaintext and must only be used with an access-controlled destination.

ZIP path traversal, symbolic links, encrypted files, high compression ratios, limits and read failures remain gaps; they are not zeros or PASS. No extra dependencies beyond Python stdlib.

## Local synthetic test

    cd tools/rafaelia_navigator
    python3 -m unittest -v test_archive_line_index_v1

## Manual bounded run against a previously authorized LOCAL mirror

    python3 tools/rafaelia_navigator/archive_line_index_v1.py \
      --source "$HOME/storage/shared/RAFAELIA_LOCAL_MIRROR" \
      --output "$HOME/rafaelia-private-atlas-run-001" \
      --max-files 100 --max-bytes 67108864 --max-entry-bytes 8388608 \
      --max-depth 2

This directory is an example, NOT proof of a mounted Drive. Output must not already exist and must be disjoint from source. No full Drive crawl occurs. Existing files, ZIPs and source trees are never overwritten. Do not put the generated index under a public repository clone.

## Manual locator inspection (private database)

    python3 - <<'PY'
    import sqlite3
    db=sqlite3.connect('PRIVATE_ATLAS_RUN_001/ATLAS.sqlite3')
    for loc,st in db.execute('select locator,status from entries order by locator limit 30'):
        print(loc,st)
    db.close()
    PY

## Remote Drive work that is NOT YET IMPLEMENTED

1. A private OAuth/account-bound provider metadata adapter for ID, parent, MIME, size-or-TOKEN_VAZIO, timestamp, version/etag and paginated cursor provenance. All private source metadata stays private.
2. A separately authorized private content adapter to stage selected files, verify bytes and link Drive file ID/revision to the local file. Native Google Docs export semantics differ from original bytes.
3. Incremental resumable indexing, archive member count cap, integrity/CRC tests, change and deletion tombstones, pagination replay, private receipt readback.
4. Private human/AI search route through the current 001_PONTEIRO and existing Navigator/Meio de Campo. Avoid parallel corpus copies.

## Stop, falsifier and rollback

Local synthetic PASS does not establish Drive E2E. Terminal pilot gate: selected small fixture verifies original source unchanged, correct line offsets, ZIP paths, hashes, partial states and bounded execution. Stop on source authority, privacy, provider cursor or transport gap. Roll back an unmerged draft by closing it, or discard only newly generated local output with user approval; do not delete originals.

R3: F_ok=isolated local draft and synthetic falsifiers; F_gap=provider Drive and real-source E2E TOKEN_VAZIO; F_next=private provider adapter after explicit authority and pagination gates.
