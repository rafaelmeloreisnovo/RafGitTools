# START HERE — Aplicações Rafael IA / RafGitTools / Raf Benchmark

**Authority:** RafGitTools producer code; Mapa governs federation; Drive governs document custody; RLL governs science. **State:** CORRECTION_DRAFT / HOLD. This is not a green APK release.

## Why this directory exists
Earlier PR #741 was merged to main on 2026-10-31 despite two workflow failures and the Android build being skipped. This successor is append-only correction with no deletion of that history, no silent promotion. Link the parent PR by exact SHA before accepting evidence.

## Navigation
- Android launcher: `app/src/main/kotlin/com/rafgittools/RafBenchmarkActivity.kt`
- Execution kernel: `app/src/main/kotlin/com/rafgittools/benchmark/RafBenchmarkCore.kt`
- Reference catalog: `RafApplicationsCatalog.kt` and `RafApplicationsCatalogView.kt` (K001..K090, A..H, CATALOG_ONLY)
- Declarative Drive exchange: `RafBenchmarkExchange.kt` (V1/V2 proposals only, untrusted)
- Local custody: `RafBenchmarkReceiptStore.kt` (hash chain, no external hardware attestation)
- Screen-specific credits: `ContextualCredits.kt`
- Producer routes: `../areas/{rafgittools,mapa,rll,termux,drive}/` (authority pointers, no code duplication)
- Operator contract: `../00_CONTRACTS/WORKFLOW_AND_CREDITS_V2.json`.

## Minimal gate
SOURCE -> CLASSIFY -> PIN_DECLARED -> HUMAN_APPROVE -> EXECUTE_LOCAL -> VERIFY_LOCAL -> EXPORT_SAF_IF_SELECTED -> PROVIDER_READBACK_IF_AVAILABLE -> INDEX -> R3.

Do not use Github Actions K-Secrets / PAT to bypass per-repository gates or grant agents unlimited scope.
An externally supplied SHA is a declaration, not authentication. Unknown source authority, external data, third-party licensing or provider rights blocks cross-repo actions.
No 90 unrelated kernels are claimed; only the bounded local compute test is implemented. CPU frequency, L3/DRAM and physical core scaling remain unmeasured.

## Drive (private; never publish provider folder IDs or raw receipts in this public repo)
Official account, user-visible nesting: `Aplicações Rafael IA/RafGitTools/Raf Benchmark/`.
Subfolders: `001_PONTEIRO`, `10_PROPOSTAS`, `20_AUTORIZACOES`, `30_RECEIPTS_RESTRITOS`, `40_GATES`, `50_TOKEN_VAZIO`, `60_ATLAS_90`, `70_CREDITOS`.
No public sharing or contents uploads are authorized by the existence of these folders.

## Safety and rollback
- PR starts DRAFT/HOLD. Do not auto-merge/release.
- Android compilation, ABI/device runtime, full dependency license audit, independent reviewer and protected-branch checks require exact-head receipts.
- Preserve original commits. If unsafe, revert only this feature on a new branch; never erase historical ledger/receipts.
- SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM. TOKEN_VAZIO ≠ 0.
