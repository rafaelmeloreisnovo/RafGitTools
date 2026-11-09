# RAFAELIA Ω · RAFYML L0 graph guard — scoped receipt V1

Status: LOCAL_SCOPED_PASS / PR_DRAFT_HOLD / claim_allowed=false.
Parent: PR #807 (https://github.com/rafaelmeloreisnovo/RafGitTools/pull/807).
Source producer: rafaelmeloreisnovo/RafGitTools. Original main validator unchanged.

## Exact source identity (Git blob SHA, readback verified)
- include/rafyml_l0_graph.h: `0ef9c74f8ce72b62efa0b23e18f3be23089bcccc`
- src/rafyml_l0_graph.c: `ee7e9bbfbaaec44d4798a3e12c19d3206736001b`
- tests/rafyml_l0_graph_test.c: `30c682f149228c0fa31e1a663d540fbab1352d83`
- L0 C source SHA-256 of observed bytes: `cae941a11f3d9dba945a2893e5dc1db66a3cb4f2f2b662a489ed9801e33d5604`.

## Local execution evidence (hosted test harness is NOT freestanding)
- Compiler: Clang 17.0.0, target host x86_64-unknown-linux-gnu.
- `clang -std=c11 -Wall -Wextra -Werror -fsanitize=address,undefined -g -Iinclude -c src/rafyml_l0_graph.c` (in isolated scratch).
- Hosted adversarial test executable: `checks=16 failures=0`, ASan/UBSan, exit 0.
- Published test source Git blob independently matched the tested local source blob.
- Cross-compiled **objects only** with `-target armv7a-none-eabi` and `-target aarch64-none-elf`, `-ffreestanding -fno-builtin -fno-stack-protector -Wall -Wextra -Werror`.
- `readelf -h`: ARM ELF32 little-endian; AArch64 ELF64 little-endian.
- `nm -u`: no undefined external symbols reported for the two objects.
- No link of executable, no Android device run, no CI dispatch, no performance measurement.

## Interface and limitation
Function `rafyml_l0_graph_check(doc, scratch, capacity)` checks bounded graph structure, root/parent relations, child counts, cycles and orphan nodes. Caller supplies scratch buffer of at least `node_count` bytes, and valid readable node-array memory. It does **not** validate raw pointer allocation extents, UTF-8, source YAML, string pool, canonicalization, device timing, ELF loader, or ABI compatibility of a persisted C struct. The original `rafyml_validate_document` is not replaced and is **not** proven memory-safe by these tests.

## DMAIC / PIRC-2
- Define: potential out-of-range sibling walk in existing graph validation; preserve old producer.
- Measure: 16 local vectors (15 ordinary cases + restore), two target objects, no undefined externals.
- Analyze: candidate separate guard enforces range before indexing any sibling.
- Improve: additive C11 guard/header/test on review branch only.
- Control: no main merge; CI exact-head and physical ARM E2E not run. N0 observed sources/tests, N2 rationale, N5 physical behavior.
- Falsifier: malicious pointers whose memory is not readable cannot be made safe merely through `node_count`; require buffer-length provenance and external memory boundary.
- Rollback: close draft PR #807; main remains unchanged.
- F_next: add focused CI on exact HEAD, independent fuzz/differential vectors against V1 emitter, source license decision, and ARM physical execution.
