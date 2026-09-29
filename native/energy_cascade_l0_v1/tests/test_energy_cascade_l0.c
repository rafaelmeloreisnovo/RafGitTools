#include "raf_energy_cascade_l0.h"

#define Q16(x) ((raf_ec_u64)(x) << 16u)

static raf_ec_u32 reference_roundtrip(raf_ec_u64 input_q16, raf_ec_u64 returned_q16) {
    raf_ec_u64 whole;
    raf_ec_u64 rem;
    raf_ec_u64 frac;
    if (input_q16==0u) return 0u;
    if (returned_q16>=input_q16) return 65536u;
    whole=returned_q16/input_q16;
    rem=returned_q16%input_q16;
    frac=(rem<<16u)/input_q16;
    return (raf_ec_u32)((whole<<16u)+frac);
}

int main(void) {
    raf_ec_ledger l;
    static const raf_ec_u64 inputs[] = {
        1u,
        2u,
        3u,
        65536u,
        Q16(100u),
        0x0000ffffffffffffULL,
        0x7fffffffffffffffULL,
        0xffffffffffffffffULL
    };
    static const raf_ec_u64 returned[] = {
        0u,
        1u,
        2u,
        32768u,
        Q16(60u),
        0x000000ffffffffffULL,
        0x123456789abcdef0ULL,
        0xfffffffffffffffeULL
    };
    unsigned int i;

    if (raf_ec_ledger_init(&l,Q16(100u))!=RAF_EC_OK) return 1;
    if (raf_ec_ledger_add_useful(&l,Q16(30u))!=RAF_EC_OK) return 2;
    if (raf_ec_ledger_note_recovered_transfer(&l,Q16(20u))!=RAF_EC_OK) return 3;
    if (raf_ec_ledger_add_stored(&l,Q16(10u))!=RAF_EC_OK) return 4;
    if (raf_ec_ledger_add_loss(&l,Q16(50u))!=RAF_EC_OK) return 5;
    if (raf_ec_ledger_unaccounted_q16(&l)!=Q16(10u)) return 6;
    if (raf_ec_apply_efficiency_q16(Q16(100u),32768u)!=Q16(50u)) return 7;
    if (raf_ec_roundtrip_efficiency_q16(Q16(100u),Q16(60u))!=39321u) return 8;
    if (raf_ec_roundtrip_efficiency_q16(0u,1u)!=0u) return 9;
    if (raf_ec_roundtrip_efficiency_q16(1u,1u)!=65536u) return 10;

    for (i=0u;i<(unsigned int)(sizeof(inputs)/sizeof(inputs[0]));++i) {
        if (raf_ec_roundtrip_efficiency_q16(inputs[i],returned[i])!=
            reference_roundtrip(inputs[i],returned[i])) return 20+(int)i;
    }

    if (raf_ec_ledger_add_loss(&l,Q16(11u))!=RAF_EC_ERR_CONSERVATION) return 40;
    return 0;
}
