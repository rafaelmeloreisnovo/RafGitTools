# RAFAELIA — Session Queue Freestanding L0 V1

Status: SOURCE_IMPLEMENTED / LOCAL_SYNTHETIC_TEST_PASS_SCOPED.
PROMOTION=HOLD; claim_allowed=false; RUNNER/DEVICE=NOT_RUN.

## Motivation and exact authority

This additive successor honors the freestanding-only directive, with no new
Python/JVM/Node runtime code. The earlier PR #707 remains an unmerged historical
draft; do NOT merge its Python runtime validator as part of this successor.
This branch starts from unchanged main@93711b96e70d22881991178edff59d84a2dc983f.

Federated authority: Mapa SESSION_AI_WORK_DISPATCH_V1.json,
commit ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09,
Git blob c1d1d8f1692c74957f8b70968fa863f855d93cb0.
Local authority: RafGitTools. Dispatch remains PLAN_ONLY.
Assignment is not execution. No PAT or private source is read.

## Native assets

- src/freestanding/session_queue_preflight_core_v1.c
- tests/native/session_queue_preflight_selftest_v1.c

Core: strict bounded JSON parser and SHA-256 identity checksum implemented in
freestanding C11. Max input size 4096 bytes, exact 15 known fields, no
allocation, syscall, libc, hosted I/O, mutable global storage or runtime
dependencies. Caller owns its input and output buffers. The source contains
only compiler type headers stdint.h / stddef.h.

Public signatures:
  struct rq_result { uint32_t errors; uint32_t state; };
  int rafaelia_queue_preflight(const unsigned char *input,
                              size_t length, struct rq_result *result);
  int rafaelia_queue_transition(unsigned char from, unsigned char to);
  int rafaelia_queue_native_selftest(void);  // test source only

Result state 0=HOLD; 1=PREFLIGHT_ONLY. Neither state enables execution.
Error bitmask: INPUT=1, SCHEMA=2, OPAQUE_ID=4, PACKET=8,
MAPA_PIN=16, STATE=32, PRIVACY=64, PAYLOAD=128,
PROMOTION=256, IDEMPOTENCY=512. Unknown, duplicate or malformed JSON
fields fail closed. Accepted plan identities are 32 lowercase hex, packet
ids use the existing SPnn_ convention, and state is IN/GATES/HOLD.
The immutable eight-field canonical identity is hashed with SHA-256 and
domain-separation prefix RAFAELIA-PREFLIGHT-V1 plus NUL. State and privacy
changes do not change the key; session/target changes do.
SHA-256 is correlation only; it is NOT a signature, MAC, encryption,
origin authentication, authorization, or anti-replay fence.
The parser accepts escaped ASCII JSON strings, not arbitrary Unicode.
Privacy-classified content and source bodies are prohibited by this public
contract.

Allowed state suggestions: IN->GATES/HOLD, GATES->HOLD, HOLD->GATES.
The core does not persist, move, dispatch or lock Drive data.

## Reproducibility / evidence

Compiler flags:
  -std=c11 -ffreestanding -fno-builtin -fno-stack-protector
  -nostdlib -O2 -Wall -Wextra -Wconversion -Werror

Cross-compile:
  clang -target armv7a-linux-gnueabihf [flags] -c source.c -o armv7.o
  clang -target aarch64-linux-gnu [flags] -c source.c -o aarch64.o

Cross-target combined relocatable ELF object:
  clang -target TARGET -fuse-ld=lld -nostdlib -r core.o selftest.o -o combined.o
  readelf -sW combined.o

Local results (scoped):
* 112 deterministic test-harness vectors PASS locally, including canonical
  SHA-256 compatibility, valid reordering/escaped JSON, invalid field/type,
  extra/duplicate field, wrong authority and forbidden transitions.
* x86-64 freestanding shared-object test returned native selftest EXIT=0.
* ARMv7/AArch64 C11 freestanding compilation succeeded.
* ARMv7/AArch64 combined relocatable objects: zero unresolved global symbols.
* ARMv7 initial compiler-introduced memset reference was corrected by
  volatile initialization and rechecked. This was a targeted ABI hotfix.

These claims are local host/synthetic evidence. No physical ARM32/64 run
or scoped native GitHub Actions job is claimed.

## Security and bounded future work

RafGitTools START.yml, branch protection, PAT secrets and private repositories
are unchanged. Drive remains a private documentary source, not a transactional
queue. No broker/AEAD/KMS, secret scope, CI private isolation, replay fence,
lease, data export, provider permission probe, or physical Android execution
is included. None may be inferred from the user's GitHub Actions read/write
setting. The private CI bridge remains IMPLEMENTED_UNTESTED/HOLD.

Acceptance gates still required: independent review, exact-HEAD CI
compilation/testing of this native core on a separate gated lane within the
single existing START workflow, negative fuzz/ASan/UBSan, ARMv7/AArch64
device execution and binary receipt hashes, privacy/rights/child-protection
checks, and approved private broker protocol before any execution feature.

F_ok: two freestanding C assets compile and test locally.
F_gap: above provider/physical/E2E gates remain TOKEN_VAZIO.
F_next: verify provider source readback + Draft PR, then add minimal native
CI gate in an independently reviewed patch, and record exact-head run receipts.
Rollback: close Draft PR without merge; if ever merged, revert only these
additions with an approved successor commit; preserve every historical object.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
TOKEN_VAZIO != 0. FREESTANDING_CORE != HOSTED_ADAPTER.
claim_allowed=false; no default-branch mutation; no secret disclosures.


## Additive single-root CI hotfix (Draft branch only)

An additional exact-HEAD native-only *gate* was inserted into the existing
START.yml coherence job on this PR branch. This is not a second workflow,
scheduler, PAT lane, checkout of a private repository, or release operation.
It compiles and executes the in-memory selftest in an **explicit hosted CI
test-entry boundary**. The freestanding L0 core itself remains unchanged,
unlinked to libc and independently inspected for undefined symbols on both
ARMv7 and AArch64. The test adapter uses a C main entry point only in CI,
never in the freestanding runtime artifact.

The gate fails closed when only one native asset exists, C11 compilation,
cross-target object linking, undefined-symbol scan or native test fails.
Existing branches with neither source file report
NATIVE_L0=TOKEN_VAZIO_SOURCE_NOT_PRESENT rather than implying a PASS.
Original plan, checkout, coherence tests, policy and release rules are
preserved. This workflow change is limited to this Draft PR until reviewed.

Verification of *GitHub runner* execution and exact HEAD is separately
required before stating a provider PASS; selftest locally returning 0 is
not enough. The rollback is a reviewed revert of this one inserted START
step and the three new files. All original workflow lines are preserved.

## P0 successor — binary proof anti-false-PASS

P0 observation: the previous START native audit scanned only undefined symbols
with binding GLOBAL. A synthetic freestanding object with a WEAK undefined
reference `optional_external` passed that old audit: old query returned zero,
while a binding-independent readelf predicate found one. This is a real
false-PASS mode, not evidence of a production compromise.

P0 remediation (Draft branch, not main):
1. Require both native source and native selftest at exact checkout; missing
   source now fails closed instead of printing SOURCE_NOT_PRESENT with exit 0.
2. Check every named undefined ELF symbol, regardless of GLOBAL/WEAK binding,
   for x86_64, ARMv7 and AArch64 combined relocatable native objects.
3. Compile an ephemeral freestanding WEAK-negative-control object for both
   ARMv7/AArch64; verify the same parser recognizes its external reference.
4. Add an exhaustive 1-bit-flip test over the canonical native fixture
   in the freestanding selftest; every flipped fixture must return HOLD.
5. Run an instrumented copy of C core + native test under ASan and UBSan
   only as a hosted CI *test* binary. Neither sanitizer nor libc becomes a
   dependency of any produced core/ARM object.
6. Keep one existing START workflow; no new trigger, workflow, PAT scope or
   secret. Preserve revert path and provider readback.

Before this P0 delta, `main` had advanced from
`93711b96e70d22881991178edff59d84a2dc983f` to
`a8a0cb1efa82bcec73c5ce0f63cec300b9e395d0`. The observed main-side
commit differences touched three credentials-contract/documentation paths;
they did not include START.yml. This is **not** integration acceptance
and does not imply that any future, moving main remains non-conflicting.

The selftest passing locally in an earlier commit cannot be inherited across
this new P0 HEAD; require direct exact-commit Github Actions readback for the
new exhaustive fixture and sanitizer gate. Negative controls must pass and no
native source may be absent. Run-state TOKEN_VAZIO until terminal evidence.

F_ok P0 before hosted readback: false-PASS observed and bounded hotfix source
written to Draft PR.
F_gap P0: exact-HEAD hosted test and sanitizer completion, physical ARM32/64,
independent security review and privileged bridge proof.
F_next P0: check terminal CI and individual native step output; retain
claim_allowed=false and PROMOTION=HOLD until non-compensatory gates are proven.
