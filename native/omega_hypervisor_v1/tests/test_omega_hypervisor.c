#include "raf_omega_hypervisor.h"

static int step(raf_oh_ctx *ctx, raf_oh_u32 gates, raf_oh_u32 next)
{
    if (raf_oh_add_gates(ctx, gates) != RAF_OH_OK) return 1;
    if (raf_oh_transition(ctx, next) != RAF_OH_OK) return 2;
    return 0;
}

int main(void)
{
    raf_oh_ctx ctx;
    raf_oh_u32 r;

    if (raf_oh_init(&ctx) != RAF_OH_OK) return 1;
    if (ctx.state != RAF_OH_C01_INTENT || ctx.cycle != 0u) return 2;

    r = raf_oh_transition(&ctx, RAF_OH_C02_AUTHORSHIP);
    if (r != RAF_OH_ERR_GATE || ctx.last_missing_mask != RAF_OH_G_INTENT) return 3;

    if (step(&ctx, RAF_OH_G_INTENT, RAF_OH_C02_AUTHORSHIP) != 0) return 4;
    if (step(&ctx, RAF_OH_G_SOURCE_IDENTITY | RAF_OH_G_AUTHORSHIP_BOUNDARY, RAF_OH_C03_GAPS) != 0) return 5;
    if (step(&ctx, RAF_OH_G_GAP_REGISTER | RAF_OH_G_RISK_REVIEW, RAF_OH_C04_MOUNT) != 0) return 6;
    if (step(&ctx, RAF_OH_G_AUTHORITY | RAF_OH_G_MOUNT_POLICY | RAF_OH_G_RELATION_TYPE, RAF_OH_C05_EXECUTE) != 0) return 7;
    if (step(&ctx, RAF_OH_G_EXECUTION_TARGET | RAF_OH_G_ROLLBACK | RAF_OH_G_INPUT_IDENTITY, RAF_OH_C06_VERIFY) != 0) return 8;
    if (step(&ctx, RAF_OH_G_FALSIFIER | RAF_OH_G_GATE_RESULT, RAF_OH_C07_CUSTODY) != 0) return 9;
    if (step(&ctx, RAF_OH_G_RECEIPT | RAF_OH_G_HASH_REF | RAF_OH_G_EVIDENCE_REF, RAF_OH_C08_OMEGA) != 0) return 10;

    if (raf_oh_set_flags(&ctx, RAF_OH_F_PROVIDER_USED | RAF_OH_F_PROMOTION_REQUESTED) != RAF_OH_OK) return 11;
    if (raf_oh_add_gates(&ctx,
            RAF_OH_G_INTEGRITY |
            RAF_OH_G_PROVENANCE |
            RAF_OH_G_SEMANTIC_CONSISTENCY |
            RAF_OH_G_RISK_REVIEW |
            RAF_OH_G_REPOSITORY_HEADS |
            RAF_OH_G_GAP_REGISTER) != RAF_OH_OK) return 12;

    r = raf_oh_transition(&ctx, RAF_OH_C01_INTENT);
    if (r != RAF_OH_ERR_GATE) return 13;
    if ((ctx.last_missing_mask & RAF_OH_G_PROVIDER_REVISION) == 0u) return 14;
    if ((ctx.last_missing_mask & RAF_OH_G_HUMAN_CHECKPOINT) == 0u) return 15;

    if (raf_oh_add_gates(&ctx, RAF_OH_G_PROVIDER_REVISION | RAF_OH_G_HUMAN_CHECKPOINT) != RAF_OH_OK) return 16;
    if (raf_oh_transition(&ctx, RAF_OH_C01_INTENT) != RAF_OH_OK) return 17;

    if (ctx.cycle != 1u) return 18;
    if (ctx.transition_count != 8u) return 19;
    if (ctx.flags != 0u || ctx.gates != 0u || ctx.last_missing_mask != 0u) return 20;

    if (raf_oh_transition(&ctx, RAF_OH_C03_GAPS) != RAF_OH_ERR_TARGET) return 21;

    return 0;
}
