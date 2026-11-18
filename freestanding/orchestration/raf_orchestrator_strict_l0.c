#include "raf_orchestrator_strict_l0.h"

static struct raf_receipt raf_strict_block(struct raf_context *context, raf_u32 warning) {
    struct raf_receipt receipt;
    if (context != (struct raf_context *)0) context->warning_mask |= warning;
    receipt.state = RAF_STATE_BLOCKED;
    receipt.flags = context != (struct raf_context *)0 ? context->flags : 0u;
    receipt.warning_mask = context != (struct raf_context *)0 ? context->warning_mask : warning;
    receipt.executed_stages = context != (struct raf_context *)0 ? context->executed_stages : 0u;
    receipt.last_module_code = context != (struct raf_context *)0 ? context->last_module_code : 0u;
    receipt.claim_allowed = 0u;
    return receipt;
}

struct raf_receipt raf_orchestrator_run_strict_l0(
    const struct raf_stage *stages, raf_usize stage_count, struct raf_context *context
) {
    raf_usize i;
    if (context == (struct raf_context *)0)
        return raf_strict_block(context, RAF_WARN_INVALID_PLAN);
    if ((context->flags & RAF_STRICT_MIN_FLAGS) != RAF_STRICT_MIN_FLAGS)
        return raf_strict_block(context, RAF_WARN_AUTHORITY_MISSING);
    if ((context->payload == (unsigned char *)0 && context->payload_size != 0u) ||
        stages == (const struct raf_stage *)0 ||
        stage_count == 0u || stage_count > (raf_usize)RAF_MAX_MODULE_ID ||
        stage_count > (raf_usize)((raf_u32)~0u - context->executed_stages))
        return raf_strict_block(context, RAF_WARN_INVALID_PLAN);
    for (i = 0u; i < stage_count; ++i) {
        if (stages[i].boundary != RAF_BOUNDARY_FREESTANDING_CORE)
            return raf_strict_block(context, RAF_WARN_HOSTED_BOUNDARY);
    }
    return raf_orchestrator_run(stages, stage_count, context);
}
