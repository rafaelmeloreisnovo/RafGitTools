#include "raf_omega_hypervisor.h"

raf_oh_u32 raf_oh_init(raf_oh_ctx *ctx)
{
    if (ctx == (raf_oh_ctx *)0) return RAF_OH_ERR_NULL;
    ctx->state = RAF_OH_C01_INTENT;
    ctx->gates = 0u;
    ctx->flags = 0u;
    ctx->cycle = 0u;
    ctx->transition_count = 0u;
    ctx->last_missing_mask = 0u;
    return RAF_OH_OK;
}

raf_oh_u32 raf_oh_next_state(raf_oh_u32 state)
{
    if (state >= RAF_OH_C01_INTENT && state < RAF_OH_C08_OMEGA) return state + 1u;
    if (state == RAF_OH_C08_OMEGA) return RAF_OH_C01_INTENT;
    return 0u;
}

raf_oh_u32 raf_oh_required_mask(raf_oh_u32 state, raf_oh_u32 flags)
{
    raf_oh_u32 mask = 0u;

    switch (state) {
    case RAF_OH_C01_INTENT:
        mask = RAF_OH_G_INTENT;
        break;
    case RAF_OH_C02_AUTHORSHIP:
        mask = RAF_OH_G_SOURCE_IDENTITY | RAF_OH_G_AUTHORSHIP_BOUNDARY;
        break;
    case RAF_OH_C03_GAPS:
        mask = RAF_OH_G_GAP_REGISTER | RAF_OH_G_RISK_REVIEW;
        break;
    case RAF_OH_C04_MOUNT:
        mask = RAF_OH_G_AUTHORITY | RAF_OH_G_MOUNT_POLICY | RAF_OH_G_RELATION_TYPE;
        break;
    case RAF_OH_C05_EXECUTE:
        mask = RAF_OH_G_EXECUTION_TARGET | RAF_OH_G_ROLLBACK | RAF_OH_G_INPUT_IDENTITY;
        break;
    case RAF_OH_C06_VERIFY:
        mask = RAF_OH_G_FALSIFIER | RAF_OH_G_GATE_RESULT;
        break;
    case RAF_OH_C07_CUSTODY:
        mask = RAF_OH_G_RECEIPT | RAF_OH_G_HASH_REF | RAF_OH_G_EVIDENCE_REF;
        break;
    case RAF_OH_C08_OMEGA:
        mask = RAF_OH_G_INTEGRITY |
               RAF_OH_G_PROVENANCE |
               RAF_OH_G_SEMANTIC_CONSISTENCY |
               RAF_OH_G_RISK_REVIEW |
               RAF_OH_G_REPOSITORY_HEADS |
               RAF_OH_G_GAP_REGISTER;
        if ((flags & RAF_OH_F_PROVIDER_USED) != 0u) {
            mask |= RAF_OH_G_PROVIDER_REVISION;
        }
        if ((flags & RAF_OH_F_PROMOTION_REQUESTED) != 0u) {
            mask |= RAF_OH_G_HUMAN_CHECKPOINT;
        }
        break;
    default:
        break;
    }

    return mask;
}

raf_oh_u32 raf_oh_add_gates(raf_oh_ctx *ctx, raf_oh_u32 gate_mask)
{
    if (ctx == (raf_oh_ctx *)0) return RAF_OH_ERR_NULL;
    if (ctx->state < RAF_OH_C01_INTENT || ctx->state > RAF_OH_C08_OMEGA) return RAF_OH_ERR_STATE;
    ctx->gates |= gate_mask;
    return RAF_OH_OK;
}

raf_oh_u32 raf_oh_set_flags(raf_oh_ctx *ctx, raf_oh_u32 flags)
{
    if (ctx == (raf_oh_ctx *)0) return RAF_OH_ERR_NULL;
    if (ctx->state < RAF_OH_C01_INTENT || ctx->state > RAF_OH_C08_OMEGA) return RAF_OH_ERR_STATE;
    ctx->flags = flags & (RAF_OH_F_PROVIDER_USED | RAF_OH_F_PROMOTION_REQUESTED);
    return RAF_OH_OK;
}

raf_oh_u32 raf_oh_transition(raf_oh_ctx *ctx, raf_oh_u32 target_state)
{
    raf_oh_u32 expected;
    raf_oh_u32 required;
    raf_oh_u32 missing;

    if (ctx == (raf_oh_ctx *)0) return RAF_OH_ERR_NULL;
    if (ctx->state < RAF_OH_C01_INTENT || ctx->state > RAF_OH_C08_OMEGA) return RAF_OH_ERR_STATE;

    expected = raf_oh_next_state(ctx->state);
    if (target_state != expected) return RAF_OH_ERR_TARGET;

    required = raf_oh_required_mask(ctx->state, ctx->flags);
    missing = required & ~ctx->gates;
    ctx->last_missing_mask = missing;
    if (missing != 0u) return RAF_OH_ERR_GATE;

    if (ctx->state == RAF_OH_C08_OMEGA) {
        ctx->cycle += 1u;
        ctx->flags = 0u;
    }

    ctx->state = target_state;
    ctx->gates = 0u;
    ctx->last_missing_mask = 0u;
    ctx->transition_count += 1u;
    return RAF_OH_OK;
}
