# AGENTS.md — RAFAELIA / RafGitTools

## 0. Federation entry — read this first

This repository is the RAFAELIA **control-plane executor/tool-router**. `Mapa` is the federated authority for routing and state. `AGENTS.md` is a repository-local entry adapter; it does not replace evidence, contracts, source authority or receipts.

Canonical executor contract: `configs/agent-entry-kernel.v1.json`.  
Federated authority contract: `github:rafaelmeloreisnovo/Mapa/data/control-plane/RAFAELIA_FEDERATED_WORK_SERVICE_CONTRACT.v1.json`.

Before acting, answer these twelve questions with exact pointers or typed `TOKEN_VAZIO`:

1. **Quem sou?** — agent role + repository role.
2. **Qual repo/ref/path/hash estou lendo?** — bind exact repository/ref/commit/path/object identity.
3. **Qual minha autoridade?** — local authority, Mapa/federated authority and write scope.
4. **Qual minha fronteira?** — allowed claim scope, forbidden promotions and `claim_allowed`.
5. **Quais índices locais devo abrir?** — minimum relevant indices and why.
6. **Qual rota do Mapa corresponde ao objetivo?** — route/anchors or typed `TOKEN_VAZIO` plus stop condition.
7. **Que lacunas já existem?** — gap IDs, TOKEN_VAZIO, uncertainties and dependencies.
8. **Qual evidência é atual?** — exact commit/artifact/protocol/device/receipt scope and staleness.
9. **Qual gate posso executar?** — gate, falsifier, exit criterion and rollback when mutating.
10. **Quando devo parar?** — no-marginal-gain, dependency block, authority boundary or observed exit.
11. **Onde registro o delta?** — local receipt, Mapa transition and Drive reconstruction when material.
12. **Quais regras de governança, dados, privacidade e segurança governam esta unidade?**

Mandatory service dimensions are **epistemic, operational, provenance, governance, data, privacy, security and reconstruction**. Governance/data/privacy/security are non-compensatory: a critical blocker remains a blocker even when other axes are green.

Core invariants:

- `TOKEN_VAZIO` is valid and never means zero/false/PASS.
- Urgency orders execution; it does not increase truth.
- `READY_TO_TEST != RESOLVED`.
- Evidence is bound to exact commit/path/artifact/protocol/environment/device.
- Deferred or ignored-with-reason work remains indexed; it is not silently deleted.
- Local repository authority governs local internals; Mapa governs federated routing/state; federation contracts govern edges.
- Documentation is not runtime evidence; hash is not scientific validation.
- Cross-repository success requires producer + consumer evidence for the claimed boundary.
- Historical observations are append-only; successors supersede instead of rewriting.
- Do not copy private/sensitive payload when a typed reference is enough.
- Security/provenance success must derive from terminal verifier evidence; never hardcode it.
- High/critical mutation requires concrete rollback before execution.

Canonical federation reference: `docs/AGENT_FEDERATION_ENTRY_V1.md`.

## 1. Local role

RafGitTools owns deterministic routing, service classification, ledgers, gates, transition receipts, cross-repository contracts and control-plane validation. It must not silently promote runtime/scientific claims of repositories it indexes.

Minimum local entry set:

```text
configs/agent-entry-kernel.v1.json
configs/workflow-master-index.json
configs/gap-closure-execution.v1.json
data/evidence/github/cross-repo-gap-closure-20260819.v1.json
AGENTS.md
```

Do not broaden the crawl until these indices cannot reconstruct the requested goal.

### Practice fast lane

For bounded human/AI contribution routing, use:

```text
docs/AI_HUMAN_FAST_ROUTE_V1.md
configs/practice-router.v1.json
scripts/validate_practice_router.py
```

This is a local adapter to the federated Practice ATLAS in `Mapa`; it does not replace Mapa authority.
Prefer one named route and <=3 source files before broad repository search.

## 2. Build

- Primary dev build: `./scripts/gradlew_with_java17.sh assembleDevDebug`
- Hermetic native fallback: `sh scripts/termux/build_apkc_hermetic.sh --abi both` (NativeActivity only; it does not build the full Compose app).
- Install dev build: `./scripts/gradlew_with_java17.sh installDevDebug`
- Internal unsigned release validation: `ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleProductionRelease`

## 3. Test & validation

- Local setup: `./scripts/prepare_local_properties.sh`
- Unit tests: `./scripts/gradlew_with_java17.sh testDevDebugUnitTest`
- Lint: `./scripts/gradlew_with_java17.sh lintDevDebug`
- Canonical governance gate: `sh scripts/validate_rafaelia_workflow.sh`
- Agent kernel: `python3 scripts/check_agent_entry_kernel.py`
- One work envelope: `python3 scripts/validate_federated_work_item.py <work-item.json>`

A structural `PASS` from these validators is not runtime/device/scientific/privacy-totality proof.

## 4. Stack contract

- Android app: Kotlin + Gradle + Jetpack Compose + JGit + Retrofit/OkHttp + Room.
- Target Android API: compileSdk/targetSdk 34, minSdk 24.
- Supported native ABIs: `armeabi-v7a` and `arm64-v8a`.

## 5. Safety, data and governance rules

- Do not claim stubs (`GPG`, `LFS`, `worktree`, `webhook`) as production-ready.
- Do not alter release signing behavior for public distribution without explicit intent.
- Keep CI and local commands aligned with `docs/BUILD.md`.
- Do not copy a different repository's local AGENTS specialization into this repository as authority.
- Classify data/privacy/security before mutating a surface that can expose or transfer data.
- Use minimum necessary data; preserve redaction and access boundaries.
- Unknown governance/privacy/security classification blocks mutation instead of defaulting to permissive.
- A receipt from RafGitTools cannot promote another repository's runtime state by itself.

## 6. ARM32 / Termux invariant

Do not bootstrap Android SDK command-line tools inside Android/Termux ARM32 unless `ANDROID_SDK_ROOT` or `ANDROID_HOME` already points to a valid compatible SDK.

Termux ARM32 is a runtime/toolchain validation environment by default, not the canonical APK build host.

Desktop/CI validation:

```bash
./scripts/gradlew_with_java17.sh assembleDevDebug assembleProductionDebug
ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleDevRelease assembleProductionRelease
./scripts/native/verify_apks.sh
```

Termux validation:

```bash
./scripts/termux_arm32_runtime_check.sh
```

## 7. Do not break

- Keep `armeabi-v7a`.
- Keep `arm64-v8a`.
- Keep JDK 17 unless Gradle/AGP/Kotlin/KSP are upgraded together.
- Do not replace ARM32 ASM with ARM64-only code.
- Do not claim full APK build support inside Termux ARM32 without proof.
- Do not convert an unresolved governance/data/privacy/security state into implicit success.


### Federated session-agent dispatch

For session-to-work routing, use `configs/session-agent-dispatch.v1.json` and
`scripts/resolve_session_ai_work_packet.py`. Mapa remains the catalog and routing
authority. The resolver is read-only, requires the pinned Mapa commit and Git blob,
and fails closed on any mismatch. A resolved assignment is not agent execution or
runtime evidence; preserve `claim_allowed=false` and `TOKEN_VAZIO` for unobserved
results. Do not copy the Mapa role/packet catalog into RafGitTools.

<!-- RAFAELIA_FREESTANDING_POLICY_V1 -->
## Freestanding / dependency-minimization gate

Default engineering direction: move implementation toward the smallest **authorial, low-level, deterministic and freestanding** core that the target can truthfully support. This is a direction plus gate, not permission to relabel hosted code as freestanding.

Rules:

- Prefer repository-owned primitives over a new external dependency when the local implementation can be smaller, auditable, testable and maintainable.
- Keep pure/freestanding cores free of network access, package managers, dynamic downloads, hosted runtimes, GC, heap, libc, syscalls and platform APIs unless the owning scoped contract explicitly permits that item.
- Keep unavoidable OS/Android/JVM/JNI/POSIX/toolchain integrations at an explicit hosted/platform boundary. A build-time tool is not automatically a runtime dependency, and neither may be hidden.
- Any new or retained external dependency must state: purpose, owner/source, license, version or immutable identity when available, build-time/runtime scope, reason it cannot yet be replaced, tests/evidence, rollback and the smallest credible authorial replacement path. Unknown fields remain `TOKEN_VAZIO`.
- Never copy/vendor third-party code and call it authorial. Provenance and license survive transformation.
- Do not add a dependency merely to shorten code. Optimize for reconstructibility, bounded complexity and falsifiable behavior, not dependency-count theater.
- Node.js 20 is EOL and MUST NOT be introduced or retained as an active CI/tooling baseline. If Node is actually required, use a maintained LTS line; the repository migration baseline on 2026-10-07 is Node 24. Do not add Node to a repository that does not need it.
- For low-level changes, inspect emitted artifacts/ABI where applicable; source resemblance alone is not freestanding evidence.
- Preserve `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`, `TOKEN_VAZIO != 0`, and `IMPLEMENTED_UNTESTED != PASS`.
- Before merging a dependency-reduction change, prove that required behavior did not silently disappear. Removal without equivalent behavior or an explicit scope reduction is a regression, not a freestanding win.

For every material dependency delta record:

`boundary -> previous dependency -> replacement/retention reason -> execution -> evidence -> rollback -> remaining TOKEN_VAZIO`.

<!-- /RAFAELIA_FREESTANDING_POLICY_V1 -->

