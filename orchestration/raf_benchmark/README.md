# Raf Benchmark · authorial application module (HOLD)

**Producer:** RafGitTools (same Android APK; one additional launcher icon).  
**Federated authority:** Mapa. **Scientific producer:** RLL only when an experiment actually routes there.  
**State:** SOURCE_IMPLEMENTED_UNTESTED_ANDROID / DRAFT_HOLD / claim_allowed=false.

## Per-repository directory routing

- [rafgittools/](areas/rafgittools/README.md): Android screen, local benchmark engine, receipts, contextual credits, build gates.
- [mapa/](areas/mapa/README.md): source/identity/provenance and totality-invariant pointers, never independent runtime claim.
- [rll/](areas/rll/README.md): scientific falsifier routes only, not auto-promotion.
- [termux/](areas/termux/README.md): authorial ARM32/ARM64 source/binary lineage, not imported third-party packages.
- [drive/](areas/drive/README.md): future user-visible 'Aplicações Rafael IA / RafGitTools / Raf Benchmark' documentary folder, **not created**.

Never copy corpus, credentials, private files, repo secrets, third-party source or unverified licenses into this public directory. Each area is a stable navigational pointer with its own producer authority. Source producer owns concrete implementation.

## Current execution envelope

The single launcher is Raf Benchmark inside the *existing* com.rafgittools APK.
The Android app already has network permission globally; this feature does not invoke network APIs, **but it is not a separate UID or security sandbox**. A standalone offline APK with separate UID would be a different security boundary and needs another scoped decision.

- Human presses 'Autorizar e executar ESTE teste'; no implicit agent-triggered execution.
- Synthetic data; buffers: 256 KiB, 1 MiB, 4 MiB, 16 MiB; threads: 1, 2, 4, 8; 4 rounds.
- Bounded battery/power/memory preflight; throughput is local compute+write **not** L3/DRAM benchmark.
- App-private SHA-256 linked local receipts. Not signed or externally anchored; file-level chain is tamper-evident only relative to separately preserved anchor.
- No remote sync, arbitrary command, native loader, GitHub workflow dispatch, token import or user data processing.
- Optional file exchange: the user opens a strict proposal via Android Storage Access Framework (SAF) and must click run separately. The proposal cannot contain executable code, secrets or permissions. [Example](areas/drive/PROPOSAL_EXAMPLE.json).
- After a run, the user may choose a SAF destination (including the Google Drive documents provider if configured) to save a receipt. There is NO automatic ChatGPT-to-device channel or remote synchronization; written files require manual sharing/connector readback.
- App-local SHA256 receipt includes the proposal digest when imported; source SHA in proposal is a declared pointer, NOT independently provider-verified by the device.
- Android app-wide INTERNET remains; this screen does no direct network I/O. The SAF provider may synchronize externally under its own permissions.
- B7 integration requires separate source validation and ABI/runtime gates; historical B7 tests are not this feature's tests.
- AndroidX Compose and Kotlin are referenced with contextual credits; source authorship and dependency attribution must not be conflated.
- Default deny: no blanket reuse of GitHub/K-Secrets across providers, repos or agents. Secret *names* do not demonstrate presence, scope or authorization. Each new capability requires an explicit provider/identity/scope/expiry/receipt preflight.

## Source boundary and falsifiers

`app/src/main/kotlin/com/rafgittools/RafBenchmarkActivity.kt` (launcher)
`app/src/main/kotlin/com/rafgittools/benchmark/` (kernel, credits, local receipts)
`app/src/test/java/com/rafgittools/benchmark/` (unit tests)
`app/src/main/AndroidManifest.xml` (only a new launcher Activity, no new permissions)

Acceptance:
1. Existing launchers still present; exactly one Raf Benchmark launcher entry.
2. Compile `testDevDebugUnitTest` and `assembleDevDebug` at exact PR head.
3. Validate app-private receipt chain; prove tampering fails closed.
4. Device ARM32 and ARM64: observed ABI, run duration, checksum/digest, peak memory, power/battery, app exit and screenshot.
5. Verify in-screen ⓘ credits and full applicable license notices, including transitive dependency license inventory.
6. Device security: no auto-exec via intent, no network traffic for feature path, explicit consent, no leaks; resolve signing and release gates separately.
7. No merge, release or claim promotion until independent review and exact-head provider readback.

**Rollback:** close PR unmerged. If later merged, revert this bounded feature commit set; never delete published historical receipts. New data/migrations require separately governed rollback.

The existing `THIRD_PARTY_LICENSES.md` and `docs/LICENSE_INFO.md` are historical inventories; completeness and legal obligations require per-version dependency SBOM and legal review.
