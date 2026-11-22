# RAFAELIA Ω · RAFYML L0 multi-ABI focused CI — successor receipt S1

- Source authority: `rafaelmeloreisnovo/RafGitTools`, base `main@10c7e1fdd3df3a1b1e8046f729d722f3e818c015`.
- Parent: `receipts/2026-11-09_RAFYML_L0_GRAPH_SCOPED_V1.md` (historical DRAFT status superseded by PR #807 merged; no prior bytes modified).
- Change scope: additive hosted gate `scripts/ci/verify_rafyml_l0_multiabi.sh` + one scoped step in existing `.github/workflows/START.yml`. No tenth workflow, no runtime/parser/YAML grammar/license/Android changes.
- Source bindings: `src/rafyml_l0_graph.c` Git blob `ee7e9bbfbaaec44d4798a3e12c19d3206736001b`; `tests/rafyml_l0_graph_test.c` `30c682f149228c0fa31e1a663d540fbab1352d83`; `include/rafyml_l0_graph.h` `0ef9c74f8ce72b62efa0b23e18f3be23089bcccc`; `include/rafyml_runtime.h` `502e50cfd9fae16eeb994f3cc4f761f6206c140f`.
- New gate blob readback: `72119c96bb9e11566dd35d279df91c7012cba2c6`. START.yml successor blob: `16e184facf9ebdf24489ab3b980d9bd69e156b9f`.
- Local test environment: Clang 17.0.0, x86_64 host, `bash`, `readelf`, `nm`, ASan+UBSan; existing source reconstructed and all four Git blob SHA identities matched.
- Local N0 observations: `checks=16 failures=0`; ARMv7 target `armv7a-none-eabi` ELF32/ARM/little-endian; AArch64 `aarch64-none-elf` ELF64/AArch64/little-endian; two object compilations per target matched byte-for-byte; both `nm -u` outputs empty.
- Local object SHA-256: ARMv7 `ef1ceb9e9f5d07b1114ae9f5f22f6d464ddd6c1729561c20734b617e1f72e586`; AArch64 `4a106b009f5e7339dae79fd8c04e3bdea773421ea5535cccd50e448d69f6ee3d`. These hashes bind local objects, not yet CI artifacts or physical devices.
- CI exact-head: `NOT_VERIFIED` at receipt creation. GitHub Actions is hosted orchestration (L1), never a freestanding executable.
- PIRC-2 N2: pinning a hosted regression gate to START helps detect invalid graph/ELF object changes. N3: whether target linking/runtime can be made fully independent remains testable future work. N5: ARM physical behavior, final link/loader, syscall/NDK independence of the *entire* pipeline, ABI-stable disk serialization, Android APK signing, security review, provider protection.
- Urgency: HIGH for false PASS prevention; change risk LOW-MEDIUM (existing START CI gate changes, isolated draft), with possible extra CI time.
- Falsifiers: any nonzero hosted harness, sanitizer report, compiler warning, wrong ELF class/machine/data, undefined symbol, or differing same-target object bytes => CI FAIL; no fail suppression.
- Rollback before merge: close draft PR / abandon branch; after merge if specifically approved, additive revert with successor receipt only (not history deletion).
- Provenance: SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM; TOKEN_VAZIO != 0; IMPLEMENTED_UNTESTED != PASS; claim_allowed=false.
- L9 routes: L=PR807→successor; O=host/ARM; T=CI/ELF; P=Git blob; C=RafGitTools producer; R=START→script; I=001_PONTEIRO; E=local 16 and 2 cross-objects; A=fail-closed gate.
- R3: F_ok=local scoped gate PASS, GitHub write/readback; F_gap=exact-head CI, linking, real device; F_next=inspect PR exact-head CI and negative-case failure/rollback without merging.
