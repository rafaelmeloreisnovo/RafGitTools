#ifndef RAF_PAT_INTEROP_V2_H
#define RAF_PAT_INTEROP_V2_H
/* Pure architecture-neutral planning core. No token, I/O, syscalls or Android API.
 * Caller owns spans and mutable state. 32-bit unsigned required by ELF contract.
 * ABI symbols intentionally named: opaque bit tricks are not an audit substitute.
 */
#if !defined(__SIZEOF_INT__) || __SIZEOF_INT__ != 4
#error "RAFAELIA requires uint32 unsigned int ABI"
#endif
typedef unsigned int raf_u32_v2;
#define RAF_PAT_PROOF_EXACT_TARGET  (1u << 0)
#define RAF_PAT_PROOF_EXACT_SHA     (1u << 1)
#define RAF_PAT_PROOF_READ_SCOPE    (1u << 2)
#define RAF_PAT_PROOF_RUNNER_TRUST  (1u << 3)
#define RAF_PAT_READ_PROOFS_V2 (RAF_PAT_PROOF_EXACT_TARGET | RAF_PAT_PROOF_EXACT_SHA | RAF_PAT_PROOF_READ_SCOPE | RAF_PAT_PROOF_RUNNER_TRUST)
#define RAF_PAT_CAP_ACTIONS_V2 0u
#define RAF_PAT_OP_READ_V2 0u
/* Returns 1 ONLY for validated PAT_ACTIONS read-plan candidate; 0 = HOLD.
 * Does not authenticate or grant permission: provider readback remains mandatory.
 * No fallback or dispatch.
 */
raf_u32_v2 raf_pat_candidate_v2(raf_u32_v2 capability,
                                 raf_u32_v2 operation,
                                 raf_u32_v2 observed_proofs);
/* Compile-time architecture descriptor; bit0 arm32, bit1 arm64,
 * bit2 little endian, bit3 32-bit pointer, bit4 64-bit pointer.
 * Observes compiler target, not the current physical device.
 */
raf_u32_v2 raf_pat_target_abi_v2(void);
#endif
