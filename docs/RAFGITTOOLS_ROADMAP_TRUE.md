# RAFGITTOOLS_ROADMAP_TRUE

- Status: **ACTIVE — evidence-first operational roadmap with bounded reconciliation overlay**
- Observed provider head for this overlay: `main@3f63ac845fcc4fed99c62087d52b75148dcc9aa1`
- Historical reconciliation baseline: `main@8af97a580e535d2015e8211000850e282b031763`
- Updated: **2026-10-03**
- Scope of this revision: **documentation truth hotfix; PR #617 + #618 exact evidence; no exhaustive semantic claim across the 642-commit interval**
- Rule: `SOURCE_OBSERVED != TEST_PROVEN != BUILD_PROVEN != RUNTIME_PROVEN != DEVICE_PROVEN != RELEASE_PROVEN`
- Historical 288-feature matrix: planning reference only; it is not the current evidence denominator.

## 2026-10-03 bounded reconciliation lane

Closed with exact bounded evidence:

1. **BR-1 Context Reconstruction Router V1** — PR #617 head `5878176b49f19f297f6573ea528f66218c3a036d`; START #590 / run `37090622351` SUCCESS; merge `f2ab825454a42f07ba55db742b04390092826475`.
2. **BR-2 Deterministic custody replay producer** — PR #618 head `e0e5010c5142d77af9f5a6c6c8e5ba7bd419d978`; START #591 / run `37090667573` SUCCESS; merge/current observed head `3f63ac845fcc4fed99c62087d52b75148dcc9aa1`.
3. **BR-3 Documentation truth boundary** — compare baseline `8af97a...` → current observed head reports 642 commits ahead; therefore the unreconciled interval is explicitly `TOKEN_VAZIO_RECONCILIATION_REQUIRED` instead of being silently treated as current audited state.

Open next lane, without feature expansion:

4. **BR-4 Partition post-baseline delta by evidence-bearing domain** — group existing commits only; do not create new capability.
5. **BR-5 Reconcile highest-authority domain first** — provider/source/evidence readback, one bounded domain at a time.
6. **BR-6 Keep current-state/status/roadmap synchronized** — each closed partition receives exact refs and leaves all unresolved partitions as `TOKEN_VAZIO`.

The context reconstruction seed is a reference packet, not a corpus backup. The deterministic replay hotfix proves repeatable producer bytes for fixed inputs; it does not claim a historical envelope has already been reconstructed.

## 2026-09-28 low-level/toolchain lane

Completed, revision-bound:

1. **RLA-1 RAFANDROID shell** — PR #521 merged; START #241 SUCCESS.
2. **RLA-2 single-root CI integration** — standalone workflow removed; RAFANDROID lives inside canonical START.
3. **RLA-3 toolchain hardening** — no `eval`, bound NDK selection, explicit DEX/APK/signature gates, shadows/tails/friction diagnostics.
4. **RLA-4 Silicon Light L0** — PR #526 merged; host freestanding + NDK ARMv7/AArch64 gates PASS.
5. **RLA-5 generated minimal Android fixture** — canonical L0 copied into scaffold; JNI adapter and APK build/DEX/APK verification PASS.
6. **RLA-6 security/build closure** — final #526 head passed Android unit/instrumentation/lint/assemble plus CodeQL Actions and Java/Kotlin.

Open next lane:

7. **RLA-7 equivalence inventory** — find duplicated deterministic byte/fixed-point/state helpers.
8. **RLA-8 property/fuzz** — prove ranges, overflow, aliasing/non-overlap contract and deterministic equivalence before migration.
9. **RLA-9 physical Android** — same exact APK/hash install/launch/restart receipt.
10. **RLA-10 QEMU/VM producer evidence** — guest boot/readback remains owned by its runtime producer.
11. **RLA-11 reproducibility** — independent environment/toolchain identity + object/artifact digest comparison.

Do not interpret RLA-1..6 as physical-device or release proof.

## Historical 2026-09-18 delivery lane

The delivery map is now gate-first:

1. D0 documentation/source reconciliation;
2. D1 responsive Home/source dashboard;
3. D2 verified Drive/SAF copy gate;
4. D3 explicit GitHub recipient binding;
5. D4 staged-file → RafGitFS workspace promotion;
6. D5 exact-head START evidence;
7. D6 same-artifact physical Android acceptance;
8. D7 signed release.

D1 and D2 are source candidates and remain `IMPLEMENTED_UNTESTED` until provider CI closes.
D3 is deliberately `TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED`; a generic import must not guess repository/ref/path.
D4 reuses RafGitFS instead of creating another Git writer.

Canonical detail:
- `RAFGITTOOLS_DEVELOPMENT_DELIVERY_MAP_V1.md`
- `architecture/RAFGITTOOLS_DRIVE_GITHUB_DELIVERY_ARCHITECTURE_V1.md`
- `RESPONSIVE_LAYOUT_GATE_V1.md`
- `RELEASE_NOTES_NEXT.md`

## State already reached in source

- Android/Kotlin/Compose/Hilt/Room foundation is present.
- Advanced local Git/JGit and GitHub API surfaces are present.
- Authentication, offline/recovery, multi-provider and native/JNI surfaces are present at different evidence depths.
- Repository Governance is present with provider observation/audit, mutation planning, UI and local receipt custody.
- Governance mutation planning has fail-closed states for unknown pre-state, non-reversible mutations and provider drift.
- Local governance receipts use an append-only SHA-256 chain while keeping provider acceptance/re-probe as separate evidence.
- FNEXT cross-repository receipt validation is present and fail-closed; structural validity does not promote physical/scientific claims.
- Recent CI/security/compile fixes are integrated in the main lineage.
- Context Reconstruction Router V1 is present and exact-head CI-bound at PR #617.
- Custody deterministic replay support is present and exact-head CI-bound at PR #618.

## Gate P0 — exact-current-head truth

**State:** `OPEN / PARTIALLY_OBSERVED`.

Required:

1. resolve exact promoted SHA;
2. enumerate required workflows for that SHA;
3. bind each workflow result to run/job IDs;
4. bind build artifacts and hashes to the same SHA;
5. do not inherit PASS from predecessor commits.

The historical 2026-09-06 audit directly observed one workflow success on `56f4ce...` (Human Impact Cross-Repo Gate V1 run `34031951218`). PR #617 and #618 have exact candidate-head START success, but that evidence is bounded to those heads and does not automatically establish complete current-main runtime/device/release truth.

## Gate P0D — documentation reconciliation truth

**State:** `OPEN / TOKEN_VAZIO_RECONCILIATION_REQUIRED`.

Observed fact: the 2026-09-28 documentation baseline is 642 commits behind the current provider head used by this overlay. Closure requires partitioning that interval and reconciling each evidence-bearing domain without inheriting claims across commits. Until then, the triad may record bounded verified deltas, but must not call the whole interval audited.

## Gate P1 — physical Android device

**State:** `TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED`.

A valid closure requires the same chain:

```text
commit
→ CI/build artifact
→ artifact SHA-256
→ physical install
→ launch
→ package/ABI/device identity
→ runtime/logcat receipt
→ artifact hash revalidation
```

No historical APK closes a current-head device gate.

## Gate P2 — provider governance safety

**State:** `SOURCE_READY / PROVIDER_EVIDENCE_GATED`.

The source already supports a conservative transaction model. Operational closure requires:

1. authoritative observed pre-state;
2. observed authority;
3. desired-state plan;
4. deterministic dry-run/fingerprint;
5. only losslessly reversible mutations;
6. provider write;
7. authoritative re-probe;
8. append-only receipt;
9. rollback only if no provider drift occurred.

`TOKEN_VAZIO` pre-state, lossy rollback and drift must remain blockers.

## Gate P3 — real core fixtures

Close independently:

- PAT/OAuth/SSH/gh/GPG authentication paths with disposable authorized fixtures;
- clone/fetch/pull/push/rebase/force-with-lease/conflict fixtures;
- worktree/bisect/LFS external runtime fixtures;
- offline enqueue → process death/restart/network transition → recovery;
- GitLab, Bitbucket, Gitea/Forgejo and Azure DevOps provider-real paths.

Shared interfaces do not establish provider parity.

## Gate P4 — bounded terminal and external local AI runtimes

- Keep current terminal classified as `BOUNDED_EXECUTOR` until PTY/VT100 lifecycle, resize, signal and escape-sequence evidence exists.
- Keep LLaMA/local-model paths external-runtime-gated until dependencies, model hashes, native library identity and device execution are bound.

## Gate P5 — release

**State:** `BLOCKED_BY_EVIDENCE`.

Before `release_allowed=true`:

- exact candidate SHA;
- complete required CI/build gate;
- signed release artifact;
- signing identity/digest provenance;
- exact-artifact physical smoke;
- critical-flow regression receipts;
- distribution policy;
- explicit release decision.

Release is never inferred from source presence or a debug build.

## Documentation gate — permanent

Every source change that changes user-visible behavior, evidence level, build/runtime gate, security boundary or release state must update or explicitly reconcile:

```text
docs/RAFGITTOOLS_CURRENT_STATE.md
docs/STATUS_REPORT.md
docs/RAFGITTOOLS_ROADMAP_TRUE.md
docs/CODE_TO_DOC_MAP.md (when routing changes)
docs/INDEX.md (when canonical navigation changes)
```

If this cannot be done in the same revision, emit `TOKEN_VAZIO_DOC_DRIFT` with an owner and next verifiable step.

Machine-generated/state artifacts are not manually edited merely to match prose. If stale, label them `TOKEN_VAZIO_REGEN_REQUIRED` until their proper generator executes.

## Priority order

```text
P0D documentation reconciliation truth
→ P0 exact-head evidence coherence
→ P1 physical device
→ P2 provider-governance readback/safe apply
→ P3 Git/Auth/Offline/provider fixtures
→ P4 PTY/external runtimes
→ P5 release
→ P6 expansion/optimization
```

The former D0/D1/D2 delivery sequence remains historical/parallel planning; no expansion outranks the truth gates above.

## Historical lineage

The 2026-08-14 BUILD anchor and PR #346/#347 sequence remain evidence/genealogy only. They no longer define the active roadmap head.

## R3

- **F_ok:** recent verified deltas #617/#618 are bound to exact candidate heads and runs; the documentation now exposes rather than hides the post-baseline drift.
- **F_gap:** the 642-commit post-baseline interval, full exact-current-head evidence inventory, physical device, provider-real fixtures and release remain open.
- **F_next:** close P0D by bounded domain partitions, then continue evidence order; no feature expansion while an unclassified documentation interval remains.