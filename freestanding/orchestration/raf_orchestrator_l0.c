#include "raf_orchestrator_l0.h"

_Static_assert(sizeof(raf_u32) == 4u, "raf_u32 must be 32-bit");
_Static_assert(sizeof(raf_u64) == 8u, "raf_u64 must be 64-bit");

static struct raf_receipt raf_receipt_from_context(
    const struct raf_context *context,
    raf_u32 state
) {
    struct raf_receipt receipt;
    receipt.state = state;
    receipt.flags = context != (void *)0 ? context->flags : 0u;
    receipt.warning_mask = context != (void *)0 ? context->warning_mask : RAF_WARN_INVALID_PLAN;
    receipt.executed_stages = context != (void *)0 ? context->executed_stages : 0u;
    receipt.last_module_code = context != (void *)0 ? context->last_module_code : 0u;
    receipt.claim_allowed = 0u;
    return receipt;
}

raf_u32 raf_orchestrator_validate(
    const struct raf_stage *stages,
    raf_usize stage_count,
    raf_u32 *warning_mask
) {
    raf_usize index = 0u;
    raf_u64 seen = 0u;
    raf_u32 warnings = 0u;

    if (stages == (const struct raf_stage *)0 || stage_count == 0u) {
        warnings |= RAF_WARN_INVALID_PLAN;
        if (warning_mask != (raf_u32 *)0) {
            *warning_mask |= warnings;
        }
        return RAF_STATE_BLOCKED;
    }

    while (index < stage_count) {
        const struct raf_stage *stage = &stages[index];
        raf_u64 bit;

        if (stage->module_id == 0u || stage->module_id > RAF_MAX_MODULE_ID ||
            stage->execute == (raf_stage_fn)0) {
            warnings |= RAF_WARN_INVALID_PLAN;
            if (warning_mask != (raf_u32 *)0) {
                *warning_mask |= warnings;
            }
            return RAF_STATE_BLOCKED;
        }

        bit = ((raf_u64)1u) << stage->module_id;
        if ((seen & bit) != 0u) {
            warnings |= RAF_WARN_INVALID_PLAN;
            if (warning_mask != (raf_u32 *)0) {
                *warning_mask |= warnings;
            }
            return RAF_STATE_BLOCKED;
        }
        seen |= bit;
        index += 1u;
    }

    if (warning_mask != (raf_u32 *)0) {
        *warning_mask |= warnings;
    }
    return RAF_STATE_COMPLETED;
}

struct raf_receipt raf_orchestrator_run(
    const struct raf_stage *stages,
    raf_usize stage_count,
    struct raf_context *context
) {
    raf_usize index = 0u;
    raf_u32 validation_state;
    raf_u32 terminal_state = RAF_STATE_COMPLETED;

    if (context == (struct raf_context *)0) {
        return raf_receipt_from_context((const struct raf_context *)0, RAF_STATE_BLOCKED);
    }

    validation_state = raf_orchestrator_validate(stages, stage_count, &context->warning_mask);
    if (validation_state != RAF_STATE_COMPLETED) {
        return raf_receipt_from_context(context, RAF_STATE_BLOCKED);
    }

    while (index < stage_count) {
        const struct raf_stage *stage = &stages[index];
        raf_u32 stage_state;

        if ((context->flags & stage->required_flags) != stage->required_flags) {
            context->warning_mask |= RAF_WARN_INCOHERENT_FLAGS;
            terminal_state = RAF_STATE_BLOCKED;
            break;
        }

        context->last_module_code = stage->module_kind;
        stage_state = stage->execute(context, stage->stage_state);
        context->executed_stages += 1u;

        if (stage_state == RAF_STATE_CONTINUE) {
            index += 1u;
            continue;
        }
        if (stage_state == RAF_STATE_TOKEN_VAZIO) {
            context->warning_mask |= RAF_WARN_TOKEN_VAZIO;
            terminal_state = RAF_STATE_TOKEN_VAZIO;
            break;
        }
        if (stage_state == RAF_STATE_BLOCKED ||
            stage_state == RAF_STATE_FAILED ||
            stage_state == RAF_STATE_COMPLETED) {
            terminal_state = stage_state;
            break;
        }

        context->warning_mask |= RAF_WARN_INVALID_STATE;
        terminal_state = RAF_STATE_FAILED;
        break;
    }

    return raf_receipt_from_context(context, terminal_state);
}
