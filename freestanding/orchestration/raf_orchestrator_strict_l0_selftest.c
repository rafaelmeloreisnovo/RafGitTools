#include "raf_orchestrator_strict_l0.h"
#define CHECK(x) do { if (!(x)) return __LINE__; } while (0)
static raf_u32 count;
static raf_u32 stage_continue(struct raf_context *context, void *opaque) {
    (void)context; (void)opaque;
    ++count;
    return RAF_STATE_CONTINUE;
}
int main(void) {
    unsigned char payload = 0u;
    struct raf_stage stage = {1u, RAF_MODULE_ROUTE, RAF_BOUNDARY_FREESTANDING_CORE, 0u, stage_continue, (void *)0};
    struct raf_context ctx = {&payload, 1u, RAF_STRICT_MIN_FLAGS, 0u, 0u, 0u, (void *)0};
    struct raf_receipt r;
    r = raf_orchestrator_run_strict_l0(&stage, 1u, &ctx);
    CHECK(r.state == RAF_STATE_COMPLETED && r.executed_stages == 1u && count == 1u && !r.claim_allowed);
    ctx.flags = 0u; ctx.warning_mask = 0u;
    r = raf_orchestrator_run_strict_l0(&stage, 1u, &ctx);
    CHECK(r.state == RAF_STATE_BLOCKED && (r.warning_mask & RAF_WARN_AUTHORITY_MISSING) && count == 1u);
    ctx.flags = RAF_STRICT_MIN_FLAGS; ctx.warning_mask = 0u;
    stage.boundary = RAF_BOUNDARY_HOSTED_ADAPTER;
    r = raf_orchestrator_run_strict_l0(&stage, 1u, &ctx);
    CHECK(r.state == RAF_STATE_BLOCKED && (r.warning_mask & RAF_WARN_HOSTED_BOUNDARY) && count == 1u);
    stage.boundary = RAF_BOUNDARY_FREESTANDING_CORE; ctx.warning_mask = 0u;
    ctx.payload = (unsigned char *)0;
    r = raf_orchestrator_run_strict_l0(&stage, 1u, &ctx);
    CHECK(r.state == RAF_STATE_BLOCKED && (r.warning_mask & RAF_WARN_INVALID_PLAN) && count == 1u);
    ctx.payload = &payload; ctx.warning_mask = 0u;
    ctx.executed_stages = (raf_u32)~0u;
    r = raf_orchestrator_run_strict_l0(&stage, 1u, &ctx);
    CHECK(r.state == RAF_STATE_BLOCKED && (r.warning_mask & RAF_WARN_INVALID_PLAN) && count == 1u);
    ctx.executed_stages = 0u; ctx.warning_mask = 0u;
    r = raf_orchestrator_run_strict_l0(&stage, 0u, &ctx);
    CHECK(r.state == RAF_STATE_BLOCKED && (r.warning_mask & RAF_WARN_INVALID_PLAN) && count == 1u);
    r = raf_orchestrator_run_strict_l0(&stage, 1u, (struct raf_context *)0);
    CHECK(r.state == RAF_STATE_BLOCKED && (r.warning_mask & RAF_WARN_INVALID_PLAN) && count == 1u);
    /* Backwards-compatibility: the original generic API remains independent. */
    ctx.flags = 0u; ctx.warning_mask = 0u;
    r = raf_orchestrator_run(&stage, 1u, &ctx);
    CHECK(r.state == RAF_STATE_COMPLETED && count == 2u && !r.claim_allowed);
    return 0;
}
