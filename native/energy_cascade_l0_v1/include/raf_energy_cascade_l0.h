#ifndef RAF_ENERGY_CASCADE_L0_H
#define RAF_ENERGY_CASCADE_L0_H

/* Freestanding Energy Cascade L0 V1.
 * Accounting only: no I/O, no chemistry control, no pressure/ignition setpoints.
 */
typedef unsigned int raf_ec_u32;
typedef signed int raf_ec_i32;
typedef unsigned long long raf_ec_u64;

_Static_assert(sizeof(raf_ec_u32)==4,"u32");
_Static_assert(sizeof(raf_ec_u64)==8,"u64");

enum {
    RAF_EC_OK=0u,
    RAF_EC_ERR_NULL=1u,
    RAF_EC_ERR_RANGE=2u,
    RAF_EC_ERR_CONSERVATION=3u
};

typedef struct raf_ec_ledger {
    raf_ec_u64 external_input_q16;
    raf_ec_u64 useful_output_q16;
    raf_ec_u64 stored_q16;
    raf_ec_u64 losses_q16;
    raf_ec_u64 recovered_transfer_q16;
} raf_ec_ledger;

raf_ec_u64 raf_ec_apply_efficiency_q16(raf_ec_u64 input_q16, raf_ec_u32 efficiency_q16);
raf_ec_u32 raf_ec_roundtrip_efficiency_q16(raf_ec_u64 input_q16, raf_ec_u64 returned_q16);
raf_ec_u32 raf_ec_ledger_init(raf_ec_ledger *ledger, raf_ec_u64 external_input_q16);
raf_ec_u32 raf_ec_ledger_add_useful(raf_ec_ledger *ledger, raf_ec_u64 value_q16);
raf_ec_u32 raf_ec_ledger_add_stored(raf_ec_ledger *ledger, raf_ec_u64 value_q16);
raf_ec_u32 raf_ec_ledger_add_loss(raf_ec_ledger *ledger, raf_ec_u64 value_q16);
raf_ec_u32 raf_ec_ledger_note_recovered_transfer(raf_ec_ledger *ledger, raf_ec_u64 value_q16);
raf_ec_u32 raf_ec_ledger_validate(const raf_ec_ledger *ledger);
raf_ec_u64 raf_ec_ledger_unaccounted_q16(const raf_ec_ledger *ledger);

#endif
