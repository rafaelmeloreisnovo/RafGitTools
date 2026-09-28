#include "raf_energy_cascade_l0.h"

static raf_ec_u64 raf_ec_sat_add_u64(raf_ec_u64 a, raf_ec_u64 b) {
    raf_ec_u64 maxv=(raf_ec_u64)(~(raf_ec_u64)0);
    if (maxv-a < b) return maxv;
    return a+b;
}

raf_ec_u64 raf_ec_apply_efficiency_q16(raf_ec_u64 input_q16, raf_ec_u32 efficiency_q16) {
    raf_ec_u64 hi;
    raf_ec_u64 lo;
    if (efficiency_q16 > 65536u) efficiency_q16=65536u;
    hi=(input_q16 >> 16u) * (raf_ec_u64)efficiency_q16;
    lo=((input_q16 & 65535u) * (raf_ec_u64)efficiency_q16) >> 16u;
    return raf_ec_sat_add_u64(hi,lo);
}

raf_ec_u32 raf_ec_roundtrip_efficiency_q16(raf_ec_u64 input_q16, raf_ec_u64 returned_q16) {
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

raf_ec_u32 raf_ec_ledger_init(raf_ec_ledger *ledger, raf_ec_u64 external_input_q16) {
    if (ledger==(raf_ec_ledger *)0) return RAF_EC_ERR_NULL;
    ledger->external_input_q16=external_input_q16;
    ledger->useful_output_q16=0u;
    ledger->stored_q16=0u;
    ledger->losses_q16=0u;
    ledger->recovered_transfer_q16=0u;
    return RAF_EC_OK;
}

raf_ec_u32 raf_ec_ledger_add_useful(raf_ec_ledger *ledger, raf_ec_u64 value_q16) {
    if (ledger==(raf_ec_ledger *)0) return RAF_EC_ERR_NULL;
    ledger->useful_output_q16=raf_ec_sat_add_u64(ledger->useful_output_q16,value_q16);
    return raf_ec_ledger_validate(ledger);
}

raf_ec_u32 raf_ec_ledger_add_stored(raf_ec_ledger *ledger, raf_ec_u64 value_q16) {
    if (ledger==(raf_ec_ledger *)0) return RAF_EC_ERR_NULL;
    ledger->stored_q16=raf_ec_sat_add_u64(ledger->stored_q16,value_q16);
    return raf_ec_ledger_validate(ledger);
}

raf_ec_u32 raf_ec_ledger_add_loss(raf_ec_ledger *ledger, raf_ec_u64 value_q16) {
    if (ledger==(raf_ec_ledger *)0) return RAF_EC_ERR_NULL;
    ledger->losses_q16=raf_ec_sat_add_u64(ledger->losses_q16,value_q16);
    return raf_ec_ledger_validate(ledger);
}

raf_ec_u32 raf_ec_ledger_note_recovered_transfer(raf_ec_ledger *ledger, raf_ec_u64 value_q16) {
    if (ledger==(raf_ec_ledger *)0) return RAF_EC_ERR_NULL;
    ledger->recovered_transfer_q16=raf_ec_sat_add_u64(ledger->recovered_transfer_q16,value_q16);
    return RAF_EC_OK;
}

raf_ec_u32 raf_ec_ledger_validate(const raf_ec_ledger *ledger) {
    raf_ec_u64 total;
    if (ledger==(const raf_ec_ledger *)0) return RAF_EC_ERR_NULL;
    total=raf_ec_sat_add_u64(ledger->useful_output_q16,ledger->stored_q16);
    total=raf_ec_sat_add_u64(total,ledger->losses_q16);
    return total<=ledger->external_input_q16 ? RAF_EC_OK : RAF_EC_ERR_CONSERVATION;
}

raf_ec_u64 raf_ec_ledger_unaccounted_q16(const raf_ec_ledger *ledger) {
    raf_ec_u64 total;
    if (ledger==(const raf_ec_ledger *)0) return 0u;
    total=raf_ec_sat_add_u64(ledger->useful_output_q16,ledger->stored_q16);
    total=raf_ec_sat_add_u64(total,ledger->losses_q16);
    if (total>=ledger->external_input_q16) return 0u;
    return ledger->external_input_q16-total;
}
