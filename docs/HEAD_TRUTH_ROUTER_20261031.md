# RafGitTools — HEAD-bound documentation truth router (2026-10-31)

> **State:** DOCUMENTATION_CANDIDATE / DRAFT_HOLD / NOT_A_BUILD_RECEIPT  
> **Source:** `rafaelmeloreisnovo/RafGitTools` `main@79d3cabba55fca3c3ed4eeb15126155d24289395`, observed on 2026-10-31.  
> **Scope:** root documentation, build configuration, newly merged workflow and current-state navigation. This is **not** a claim that every document, nested directory, device or CI job was audited.  
> **Supersedes for current navigation only:** 2026-09-28 README toolchain summary and 2026-10-03 current-state header; historical receipts and audits stay intact.

## Read the source first

| Contract | Source at observed main | What can be said | What cannot be inferred |
| --- | --- | --- | --- |
| Gradle plugin | [root build.gradle](../build.gradle), blob `048b0119aab8bf6bc9bcc9510b3e77b518a94a35` | AGP `9.1.1` is configured as a candidate; Kotlin `2.4.21`, KSP `2.3.10`, Hilt `2.60.1` are declared | Successful dependency resolution, Kotlin/Compose/KSP interoperability or official release |
| Android API and ABIs | [app/build.gradle](../app/build.gradle), blob `ab8e8f6561239c802fe1a4da85e8c764cdc110a0` | `compileSdk=37`, `targetSdk=34`, `minSdk=24`; ABI filters `armeabi-v7a` and `arm64-v8a` | Build, install, launch, physical-device tests or availability of toolchain on Termux |
| Gradle distribution | [wrapper properties](../gradle/wrapper/gradle-wrapper.properties), blob `3271eae37c4b10ee892f68bf251552867b1f5eed` | Wrapper URL points to Gradle `9.5.1` | Compatibility or verified download |
| Atlas APK consumer workflow | [.github/workflows/ci-android-atlas-consumer.yml](../.github/workflows/ci-android-atlas-consumer.yml), blob `dd1d5654a9fd867f40f4fc0cbc424218efbea6cd` | Workflow is present in the observed merged main, references pinned external source `rafaelmeloreisnovo/actions@2daa8d40d401aab20f765fed9c3f229d90c8ff4c` | That it ran successfully on current main, authenticated to Drive, or passed handset/SAF validation |

## Evidence and risk at observation

- Main is currently **private** and the branch-protection flag returned `protected=false` in the GitHub branch read. An inaccessible detailed protection/ruleset endpoint is **not** proof of absence of all provider controls.
- Drive CURRENT_STATE HOTSTATE V4.5 reports RafGitTools merge PR#739 as merged into main at the observed SHA, and associated START run `41204064625` as **FAILURE**; Android Gradle run `41204064626` as **FAILURE**. These are historical observation pointers, **not** independently rerun by this documentation change; causes remain `TOKEN_VAZIO`.
- Prior 2026-09-28 / 2026-10-03 tests and merges in existing documents are revision-bound **historical** evidence, not current HEAD PASS.
- **No** production source, API credential, workflow, release signature, provider protection or device state is changed by this documentation branch.
- `claim_allowed=false`, `release_allowed=false`, `PROMOTION=HOLD`.

## Documentation refactoring contract

The 2026-10-31 bounded enumeration found **176 direct children** in `docs/` (files **and** subdirectories). This is **not** a complete recursive file census. Work on the following surfaces in order, without overwriting historical evidence:

1. Current entry: `README.md` → **this router** → `docs/RAFGITTOOLS_CURRENT_STATE.md` → `docs/INDEX.md`.
2. Build: `docs/BUILD.md` must cite exact Gradle sources. Candidate configuration is not guaranteed compatible.
3. Technical maps: `docs/CODE_TO_DOC_MAP.md`, `docs/RAFGITTOOLS_CODE_REALITY_MATRIX.md`, `docs/STATUS_REPORT.md`, roadmap; reconcile each against the producer code at the new exact HEAD.
4. Security, legal, privacy, child safeguarding, authentication and attribution: require individual authority and negative tests before editing semantics. No global license rewrite.
5. Named historical/dated files and receipts: keep immutable; use successor + `supersedes` reference, not silent replacement.
6. Automated audit candidate: source-driven links, missing targets, dated/current classification, stale pinned revision claims, command existence and code→doc coverage. Until a full recursive report and gates are executed: `TOKEN_VAZIO_DOC_COVERAGE`.

## Gates / R3

- **F_ok:** exact code/config and workflow files were read, a bounded documentation correction was authored and routed by reference. This is `DOC_SOURCE_RECONCILED_SCOPED` only.
- **F_gap:** full recursive docs inventory, all referenced commands, CI on this proposed HEAD, Android runtime, real release/provider governance and review are `TOKEN_VAZIO` or `NOT_RUN`.
- **F_next:** run documentation link/command checks at exact PR head and classify remaining documents from source paths; independent review before any merge.
- **Rollback:** close draft PR without merge; historical files are preserved.
