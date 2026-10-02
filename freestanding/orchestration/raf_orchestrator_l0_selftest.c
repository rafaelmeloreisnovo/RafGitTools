#include "raf_orchestrator_l0.h"

struct test_state {
    raf_u32 counter;
};

static raf_u32 stage_continue(struct raf_context *context, void *opaque) {
    struct test_state *state = (struct test_state *)opaque;
    state->counter += 1u;
    context->warning_mask |= 0u;
    return RAF_STATE_CONTINUE;
}

static raf_u32 stage_token_vazio(struct raf_context *context, void *opaque) {
    struct test_state *state = (struct test_state *)opaque;
    state->counter += 1u;
    context->warning_mask |= RAF_WARN_EVIDENCE_MISSING;
    return RAF_STATE_TOKEN_VAZIO;
}

static raf_u32 stage_must_not_run(struct raf_context *context, void *opaque) {
    struct test_state *state = (struct test_state *)opaque;
    state->counter += 100u;
    context->warning_mask |= RAF_WARN_MODULE_UNAVAILABLE;
    return RAF_STATE_FAILED;
}

int main(void) {
    unsigned char payload[8] = {0u};
    struct test_state state = {0u};
    struct raf_context context;
    struct raf_receipt receipt;
    const raf_u32 strict_flags =
        RAF_FLAG_SOURCE_BOUND |
        RAF_FLAG_AUTHORITY_BOUND |
        RAF_FLAG_FAIL_CLOSED |
        RAF_FLAG_SHADOW_GUARD;

    const struct raf_stage stages[] = {
        {1u, RAF_MODULE_ROUTE, RAF_BOUNDARY_FREESTANDING_CORE, strict_flags, stage_continue, &state},
        {2u, RAF_MODULE_CRYPTO_RMR, RAF_BOUNDARY_FREESTANDING_CORE, strict_flags, stage_token_vazio, &state},
        {3u, RAF_MODULE_VM_KERNEL, RAF_BOUNDARY_FREESTANDING_CORE, strict_flags, stage_must_not_run, &state}
    };

    context.payload = payload;
    context.payload_size = (raf_usize)sizeof(payload);
    context.flags = strict_flags;
    context.warning_mask = 0u;
    context.executed_stages = 0u;
    context.last_module_code = 0u;
    context.caller_state = (void *)0;

    receipt = raf_orchestrator_run(stages, 3u, &context);

    if (receipt.state != RAF_STATE_TOKEN_VAZIO) return 1;
    if (receipt.executed_stages != 2u) return 2;
    if (state.counter != 2u) return 3;
    if ((receipt.warning_mask & RAF_WARN_TOKEN_VAZIO) == 0u) return 4;
    if ((receipt.warning_mask & RAF_WARN_EVIDENCE_MISSING) == 0u) return 5;
    if (receipt.claim_allowed != 0u) return 6;

    return 0;
}
