#include "raf_pat_interop_v2.h"

/* Arithmetic Boolean expression with no C branch; compiler output must be
 * independently inspected before any claim of machine-level branchlessness. */
raf_u32_v2 raf_pat_candidate_v2(raf_u32_v2 capability,
                                 raf_u32_v2 operation,
                                 raf_u32_v2 observed_proofs) {
    const raf_u32_v2 all = RAF_PAT_READ_PROOFS_V2;
    return (raf_u32_v2)((capability == RAF_PAT_CAP_ACTIONS_V2) &
                        (operation == RAF_PAT_OP_READ_V2) &
                        ((observed_proofs & all) == all));
}

raf_u32_v2 raf_pat_target_abi_v2(void) {
    raf_u32_v2 bits = 0u;
#if defined(__arm__) && !defined(__aarch64__)
    bits |= 1u;
#elif defined(__aarch64__)
    bits |= 2u;
#endif
#if defined(__BYTE_ORDER__) && (__BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__)
    bits |= 4u;
#endif
#if defined(__SIZEOF_POINTER__) && (__SIZEOF_POINTER__ == 4)
    bits |= 8u;
#elif defined(__SIZEOF_POINTER__) && (__SIZEOF_POINTER__ == 8)
    bits |= 16u;
#endif
    return bits;
}
