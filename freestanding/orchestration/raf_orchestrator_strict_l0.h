#ifndef RAF_ORCHESTRATOR_STRICT_L0_H
#define RAF_ORCHESTRATOR_STRICT_L0_H
#include "raf_orchestrator_l0.h"
/* Pure C contract: bits are caller assertions, not independently verified authority. */
#define RAF_STRICT_MIN_FLAGS (RAF_FLAG_SOURCE_BOUND | RAF_FLAG_AUTHORITY_BOUND | RAF_FLAG_FAIL_CLOSED | RAF_FLAG_SHADOW_GUARD)
/* Additive API; original generic orchestrator ABI and behavior are preserved. */
struct raf_receipt raf_orchestrator_run_strict_l0(
    const struct raf_stage *stages, raf_usize stage_count, struct raf_context *context
);
#endif
