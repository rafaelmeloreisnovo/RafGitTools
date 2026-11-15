# RAFAELIA Ω — HyperAtlas V4 → Mapa MANIFOLD incremental (V1, REVIEW/HOLD)

Owner: `RafGitTools` for the V4 metadata adapter; `Mapa` for the routing DAG. No production entrypoint modified.

## Boundary and data model

`HyperAtlasV4Incremental` checkpoints and hashed row batches are existing app-private source records. The bridge operates on **two distinct, COMPLETE, immutable captures** of the **same SAF selected-tree scope**, verifies checkpoint/record counts, and fingerprints *only metadata rows* grouped by pseudonymous directory key. The output schema matches the input of the `Mapa` branch `review/rafaelia-incremental-manifold-router-v1-20261115`, tool `tools/incremental_manifold_router_v1.py`. No Mapa routing algorithm is copied into RafGitTools.

> IMPORTANT: The current V4 walker returns immediately on `COMPLETE` and does not produce a new capture on refresh. Thus **real Android capture refresh and provenance/locking are NOT IMPLEMENTED**. Call this bridge only with separately captured and readback-verified snapshots. An assertion by a caller does not prove that a snapshot came from Drive. The bridge deliberately emits `change_detection=TOKEN_VAZIO_PROVIDER_REFRESH_NOT_VERIFIED`. The Mapa router **must reject** this candidate until a separately authorized adapter has proved actual separate provider captures, immutable readback, scoped completeness, and freshness. Internal checkpoint `COMPLETE` is not equivalent to verified provider freshness.

Inputs to `preview`: `Capture(captureRef, Checkpoint, List<Row>)` ×2 plus locally expected/observed LEADER revision values. `captureRef` is a local immutable capture identifier, **not** a provider revision or cryptographic receipt. SHA-256 fingerprints of field-length-prefixed metadata distinguish `NULL` from `0` and avoid path ambiguity; they do not authenticate providers or hash file contents.

Output: Mapa-compatible candidate `manifest.schema=RAFAELIA_INCREMENTAL_MANIFOLD_ROUTER_V1`, `leader`, `shards`, `changed_shards`, `tasks=[]`; the Mapa router currently rejects its intentionally blocked `change_detection`. Directory units are intentionally independent; cross-project and parent-child dependencies remain owned by Mapa / the producer's own verified graph, not inferred from SAF path text. Tombstones retain deleted directory addresses without pretending data still exists. Raw provider IDs, names, paths, source bytes and tokens are absent from the output.

## Local gates and falsifiers

- Reject incomplete checkpoint, mismatched provider-tree scope, count drift, orphan or duplicate row identity, invalid metadata and excessive snapshots (max 8,192 rows and 4,096 visited directories).
- Reject reused `captureRef` with changed metadata; exact replay allowed; rows sorted before hashing for idempotence.
- Reject caller-supplied LEADER revision divergence. **This is NOT an external atomic CAS.** A real writer must acquire explicit exclusivity, do provider compare-and-swap, verify source revision/receipt and read back the successor. Without provider-side atomicity, return `BLOCKED` and never advance the global pointer.
- Test `NULL` versus `0`, missing/changed directories, captured snapshot sensitivity, and deterministic replay.

## Not executed

- Drive SAF provider refresh; Android physical E2E and multi-ABI APK build; provider change feed; real `CAS`/single-writer lease; encrypted export; content SHA-256; file-byte equivalence; end-to-end Notion/Supabase receipt; measurements of RAM, CPU, time, or compression.
- The bridge does not alter V3, V4, main, Android UI, RLL, EstudioAudio, data permissions, filesystem or the canonical START HERE.

## Local test

Compile `HyperAtlasV4Incremental.kt` and `HyperAtlasV4ManifoldBridgeV1.kt` and run the accompanying JUnit tests. A host test with a *temporary* minimal JUnit harness validates pure Kotlin logic only; it does not replace Gradle or device CI.

## Rollback

No promotion/merge, no deletion. Keep isolated review branch. If failed, correct with successor commit or leave branch unmerged; preserve all preceding receipts. `claim_allowed=false`.
