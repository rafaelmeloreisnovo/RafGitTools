# RAFAELIA Omega — RAFYML L0 scoped multi-ABI CI hotfix (PIRC-2 / 6Sigma)

State: REVIEW_ONLY / PR_DRAFT_HOLD / claim_allowed=false.
Predecessor: receipts/2026-11-09_RAFYML_L0_GRAPH_SCOPED_V1.md.
Producer authority: rafaelmeloreisnovo/RafGitTools PR #807.
Cross-repo boundary: rafaelmeloreisnovo/actions remains a separate upstream Gradle fork (MIT, Gradle Inc.); **no files changed there**.
RafGitTools repository governing license: GPL-3.0-or-later; changes in this repository stay under its license boundary.

## Delta — new files, no original replaced
- scripts/ci/rafyml_l0_multiabi_gate.sh — Git blob `c941cdbd3fb8ba227fb3502da73c2729c06b79ef`.
- .github/workflows/rafyml-l0-multiabi.yml — Git blob `580e7eba1420db6cfe140d85a085f1a9ff859f64`.
- Script initial commit: `f2b434c309074f9be2d81cda6719b991d6cfc400`.
- Workflow commit: `fc33804afb61db7c791817e844059464258a088b`.
- GitHub connector readback of both new file contents and blob IDs: successful.
- Existing graph guard, header, test and V1 parser were **not changed**.

## Exact source provenance for local probe
Copied the 4 existing PR sources from connector reads into isolated local scratch; independently recomputed Git blob SHA-1 identities:
- src/rafyml_l0_graph.c `ee7e9bbfbaaec44d4798a3e12c19d3206736001b`
- tests/rafyml_l0_graph_test.c `30c682f149228c0fa31e1a663d540fbab1352d83`
- include/rafyml_l0_graph.h `0ef9c74f8ce72b62efa0b23e18f3be23089bcccc`
- include/rafyml_runtime.h `502e50cfd9fae16eeb994f3cc4f761f6206c140f`
All four matched the GitHub connector's blob identities at tested source scope. A Git blob hash is **not** a signed provenance or a device attestation.

## N0 — measured host-local evidence
- POSIX `sh -n` script parse: exit 0.
- Default `--dry-run`: printed preconditions/flags/risks and performed no compilation.
- Local manual `--run`: three stage approvals acknowledged; Clang 17.0.0 x86_64 Linux host.
- Sanitized hosted test: `checks=16 failures=0` (ASan + UBSan), exit 0. **These are graph-structure tests**, not the PIRC-2 Omega00..Omega15 semantic language corpus.
- `-target armv7a-none-eabi`: ELF32 ARM relocatable object, no `nm -u` undefined symbols; locally observed object SHA-256 `ef1ceb9e9f5d07b1114ae9f5f22f6d464ddd6c1729561c20734b617e1f72e586`.
- `-target aarch64-none-elf`: ELF64 AArch64 relocatable object, no `nm -u` undefined symbols; locally observed object SHA-256 `4a106b009f5e7339dae79fd8c04e3bdea773421ea5535cccd50e448d69f6ee3d`.
- These artifact hashes are **local objects**, not uploaded CI artifacts or independently reproduced target-device binaries.

## CI boundary and falsifiers
- Workflow event: `pull_request`, only PR #807 with same-repository source and narrowed path filters; no external `uses` actions; permissions `contents: read`, timeout 8 min, no release/deploy/signing.
- Fetches `refs/pull/807/head` and verifies its SHA against the event head; invokes script `--ci-run` under scoped GitHub event environment.
- Current exact-head GitHub Actions evidence: first-page lookup returned zero runs; state `NOT_OBSERVED`, never `PASS`.
- Falsifiers: wrong PR head, missing tool, compiler diagnostic, wrong ELF class/ISA/REL type, undefined symbol, failing host vector => **FAIL CLOSED**.
- N2: LLVM host compiler and GitHub runner remain external; freestanding applies to target L0 object, not a YAML runner.
- N5/TOKEN_VAZIO: actual CI run, ELF linking/loading, ABI execution on ARM devices, APK, zero-overhead benchmark, constant-time security, PIRC-2 lexical semantics, license scope of third-party imports, full graph and corpus proof.

## Roles and rollback
Architect: bound a single freestanding graph checker; do not replace the Python-hosted YAML converter.
Operator: script default DRY-RUN; manual `--run` requests YES for each stage; authorized PR CI uses `--ci-run`.
Auditor: compare source blob IDs, output diagnostics and exact-head CI; retain negative results.
Recorder/data engineer: append-only successor; source != artifact != execution != evidence != claim.
Rollback: close this Draft PR or revert only new commits/files; no main or upstream repository mutation.
F_next: inspect first exact-head workflow result and GitHub billing availability; add byte-level parser differential tests, physical ARM ABI/loader E2E and semantic Omega00..Omega15 tests only after scope-approved evidence.
