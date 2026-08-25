#include "rafcode_federation.h"

static __inline__ __attribute__((__always_inline__)) raf_u32 raf_nz(raf_u32 value)
{
    return (value | (0u - value)) >> 31;
}

static __inline__ __attribute__((__always_inline__)) raf_u32 raf_mix(raf_u32 value)
{
    value ^= value >> 16;
    value *= 0x7feb352du;
    value ^= value >> 15;
    value *= 0x846ca68bu;
    return value ^ (value >> 16);
}

void raf_fed_validate(const void *work_pointer, void *receipt_pointer, raf_u32 io_error)
{
    const raf_fed_work *const work = (const raf_fed_work *)work_pointer;
    raf_fed_receipt *const receipt = (raf_fed_receipt *)receipt_pointer;
    const raf_u32 input_fold = work->input_digest[0] ^ work->input_digest[1] ^
                               work->input_digest[2] ^ work->input_digest[3];
    const raf_u32 route_fold = work->route_digest[0] ^ work->route_digest[1] ^
                               work->route_digest[2] ^ work->route_digest[3];
    const raf_u32 mutating = (work->flags >> 2) & 1u;
    const raf_u32 rollback = (work->flags >> 3) & 1u;
    const raf_u32 receipt_required = (work->flags >> 4) & 1u;
    const raf_u32 evidence_bound = (work->flags >> 5) & 1u;
    const raf_u32 evidence_needed =
        (raf_u32)(work->state_after >= RAF_FED_STATE_EVIDENCED_SCOPED);
    const raf_u32 error_mask =
        (raf_nz(io_error) * RAF_FED_ERROR_IO) |
        (raf_nz(work->magic ^ RAF_FED_WORK_MAGIC) * RAF_FED_ERROR_MAGIC) |
        (raf_nz((raf_u32)work->version ^ RAF_FED_VERSION) * RAF_FED_ERROR_VERSION) |
        (raf_nz((raf_u32)work->axis_mask ^ RAF_FED_AXIS_REQUIRED) * RAF_FED_ERROR_AXES) |
        (raf_nz((raf_u32)work->participant_mask ^ RAF_FED_ROLE_REQUIRED) * RAF_FED_ERROR_ROLES) |
        ((1u ^ raf_nz(work->transaction_lo | work->transaction_hi)) * RAF_FED_ERROR_TRANSACTION) |
        (((raf_u32)(work->action < RAF_FED_ACTION_DISCOVER) |
          (raf_u32)(work->action > RAF_FED_ACTION_MAX)) * RAF_FED_ERROR_ACTION) |
        (((raf_u32)(work->state_before > RAF_FED_STATE_MAX) |
          (raf_u32)(work->state_after > RAF_FED_STATE_MAX)) * RAF_FED_ERROR_STATE) |
        (raf_nz(work->flags & RAF_FED_FLAG_CLAIM_ALLOWED) * RAF_FED_ERROR_CLAIM) |
        (raf_nz(work->flags & RAF_FED_FLAG_PRIVATE_PATHS) * RAF_FED_ERROR_PRIVACY) |
        ((mutating & (1u ^ rollback)) * RAF_FED_ERROR_ROLLBACK) |
        ((1u ^ receipt_required) * RAF_FED_ERROR_RECEIPT) |
        ((evidence_needed & (1u ^ evidence_bound)) * RAF_FED_ERROR_EVIDENCE) |
        (raf_nz(work->flags & (0u - (RAF_FED_FLAG_KNOWN + 1u))) * RAF_FED_ERROR_FLAGS) |
        ((1u ^ raf_nz(input_fold)) * RAF_FED_ERROR_INPUT_ID) |
        ((1u ^ raf_nz(route_fold)) * RAF_FED_ERROR_ROUTE_ID);
    const raf_u32 status = raf_nz(error_mask);
    const raf_u32 seed = input_fold ^ route_fold ^ work->transaction_lo ^
                         work->transaction_hi ^ error_mask;

    receipt->magic = RAF_FED_RECEIPT_MAGIC;
    receipt->version = RAF_FED_VERSION;
    receipt->status = (raf_u16)status;
    receipt->error_mask = error_mask;
    receipt->transaction_lo = work->transaction_lo;
    receipt->transaction_hi = work->transaction_hi;
    receipt->axis_role_action = (raf_u32)work->axis_mask |
                                ((raf_u32)work->participant_mask << 8) |
                                ((work->action & 0xffffu) << 16);
    receipt->state_before = work->state_before;
    receipt->state_after = work->state_after;
    receipt->safe_flags = work->flags & (RAF_FED_FLAG_MUTATING |
                                         RAF_FED_FLAG_ROLLBACK_BOUND |
                                         RAF_FED_FLAG_RECEIPT_REQUIRED |
                                         RAF_FED_FLAG_EVIDENCE_BOUND);
    receipt->input_fold = input_fold;
    receipt->trace_tag[0] = raf_mix(seed ^ 0x9e3779b9u);
    receipt->trace_tag[1] = raf_mix(seed ^ 0x243f6a88u);
    receipt->trace_tag[2] = raf_mix(seed ^ 0xb7e15162u);
    receipt->trace_tag[3] = raf_mix(seed ^ work->state_before);
    receipt->trace_tag[4] = raf_mix(seed ^ work->state_after);
    receipt->trace_tag[5] = raf_mix(seed ^ receipt->axis_role_action);
}
