# RAFGITTOOLS_ROADMAP_TRUE

- Status: **ACTIVE — operational roadmap**
- Observed base: `main@56f4ce95158e6b8a1dbfa4fd8c029937aea20224`
- Updated: **2026-09-06**
- Scope of this revision: **documentation only**
- Rule: `SOURCE_OBSERVED != TEST_PROVEN != BUILD_PROVEN != RUNTIME_PROVEN != DEVICE_PROVEN != RELEASE_PROVEN`
- Historical 288-feature matrix: planning reference only; it is not the current evidence denominator.

## State already reached in source

- Android/Kotlin/Compose/Hilt/Room foundation is present.
- Advanced local Git/JGit and GitHub API surfaces are present.
- Authentication, offline/recovery, multi-provider and native/JNI surfaces are present at different evidence depths.
- Repository Governance is present with provider observation/audit, mutation planning, UI and local receipt custody.
- Governance mutation planning has fail-closed states for unknown pre-state, non-reversible mutations and provider drift.
- Local governance receipts use an append-only SHA-256 chain while keeping provider acceptance/re-probe as separate evidence.
- FNEXT cross-repository receipt validation is present and fail-closed; structural validity does not promote physical/scientific claims.
- Recent CI/security/compile fixes are integrated in the main lineage.

## Gate P0 — exact-current-head truth

**State:** `OPEN / PARTIALLY_OBSERVED`.

Required:

1. resolve exact promoted SHA;
2. enumerate required workflows for that SHA;
3. bind each workflow result to run/job IDs;
4. bind build artifacts and hashes to the same SHA;
5. do not inherit PASS from predecessor commits.

Current documentation audit directly observed one workflow success on `56f4ce...` (Human Impact Cross-Repo Gate V1 run `34031951218`). The remaining exact-head workflow/build inventory stays `TOKEN_VAZIO` until individually read back.

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
P0 exact-head evidence coherence
→ P1 physical device
→ P2 provider-governance readback/safe apply
→ P3 Git/Auth/Offline/provider fixtures
→ P4 PTY/external runtimes
→ P5 release
→ P6 expansion/optimization
```

## Historical lineage

The 2026-08-14 BUILD anchor and PR #346/#347 sequence remain evidence/genealogy only. They no longer define the active roadmap head.

## R3

- **F_ok:** roadmap is now source/evidence ordered instead of feature-count ordered; governance and receipt validation are represented at their real source maturity.
- **F_gap:** full exact-head evidence inventory, physical device, provider-real fixtures and release remain open.
- **F_next:** close gates in evidence order; expansion must not outrun P0/P1 without an explicit reason and receipt.