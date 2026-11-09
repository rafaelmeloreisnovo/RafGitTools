# PIRC-2 L0 — adversarial successor 2026-11-09

BASE: RafGitTools main@b8bdba9a5e3a75c9aa5c621d670913524364f770; PR#805 merged, existing C core unchanged.
DELTA: append-only hosted adversarial test and opt-in Make target, no new GitHub workflow (workflow topology guard remains unchanged).
FALSIFIERS: 256 one-byte cases, every continuation octet for 30 two-byte leads, overlong/surrogate/high invalid forms, NUL/ASCII zero/literal NULL/four spaces, valid 2/3/4 byte sequences, exact 4096/4097 byte boundary, UTF-8 error after NUL; no lexical disambiguation of "molho" (N5).
LOCAL_HOST: Clang 17, AddressSanitizer/UBSan, 7953 assertions PASS; full core source hash matches Git blob 54a1fe1b047d138eb94e3e3a599eae2588989d38.
CROSS_OBJECT: armv7a-none-eabi and aarch64-none-elf compiled with -ffreestanding/-fno-builtin; nm -u EMPTY. This does not prove a working Android/ARM runtime.
COMMAND: make -C native/pirc2_token_l0 CC=clang CLANG=clang adversarial armv7 aarch64.
PIRC2: N0 observed gates; N5 physical device/consumer JNI/standalone binary, semantics, independent review.
SCOPE: no claim of full RafGitTools app freestanding; third-party licenses retained, source unaffected.
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM. claim_allowed=false for physical/semantic results.
ROLLBACK: close unmerged draft PR; if merged, revert scoped Makefile target and test with successor receipt.
