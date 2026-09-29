#include "raf_energy_cascade_l0.h"

#define Q16(x) ((raf_ec_u64)(x) << 16u)

int main(void) {
    raf_ec_ledger l;
    if (raf_ec_ledger_init(&l,Q16(100u))!=RAF_EC_OK) return 1;
    if (raf_ec_ledger_add_useful(&l,Q16(30u))!=RAF_EC_OK) return 2;
    if (raf_ec_ledger_note_recovered_transfer(&l,Q16(20u))!=RAF_EC_OK) return 3;
    if (raf_ec_ledger_add_stored(&l,Q16(10u))!=RAF_EC_OK) return 4;
    if (raf_ec_ledger_add_loss(&l,Q16(50u))!=RAF_EC_OK) return 5;
    if (raf_ec_ledger_unaccounted_q16(&l)!=Q16(10u)) return 6;
    if (raf_ec_apply_efficiency_q16(Q16(100u),32768u)!=Q16(50u)) return 7;
    if (raf_ec_roundtrip_efficiency_q16(Q16(100u),Q16(60u))>65536u) return 8;
    if (raf_ec_ledger_add_loss(&l,Q16(11u))!=RAF_EC_ERR_CONSERVATION) return 9;
    return 0;
}
