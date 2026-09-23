#include "rafcode_route.h"

static __inline__ __attribute__((__always_inline__)) raf_route_u32 raf_route_nz(raf_route_u32 value)
{
    return (value | (0u - value)) >> 31;
}

static __inline__ __attribute__((__always_inline__)) raf_route_u32 raf_route_mix(raf_route_u32 value)
{
    value ^= value >> 16;
    value *= 0x7feb352du;
    value ^= value >> 15;
    value *= 0x846ca68bu;
    return value ^ (value >> 16);
}

/*
 * ROUTES_OMEGA_V1 binds trigger classes 1..10 one-to-one to R0001..R0010.
 * V1 therefore resolves route_code = trigger_code after fail-closed validation.
 * A future non-identity mapping requires a new contract version.
 */
void raf_route_resolve(const void *request_pointer, void *receipt_pointer)
{
    const raf_route_request *const request = (const raf_route_request *)request_pointer;
    raf_route_receipt *const receipt = (raf_route_receipt *)receipt_pointer;
    const raf_route_u32 trigger = (raf_route_u32)request->trigger_code;
    const raf_route_u32 flags = request->flags;
    const raf_route_u32 source_fold = request->source_digest[0] ^
                                      request->source_digest[1] ^
                                      request->source_digest[2] ^
                                      request->source_digest[3];
    const raf_route_u32 source_present = request->source_digest[0] |
                                         request->source_digest[1] |
                                         request->source_digest[2] |
                                         request->source_digest[3];
    const raf_route_u32 source_bound = flags & RAF_ROUTE_FLAG_SOURCE_BOUND;
    const raf_route_u32 ambiguous = flags & RAF_ROUTE_FLAG_AMBIGUOUS;
    const raf_route_u32 trigger_error =
        (raf_route_u32)(trigger < RAF_ROUTE_TRIGGER_GENERAL) |
        (raf_route_u32)(trigger > RAF_ROUTE_TRIGGER_MAX);
    const raf_route_u32 error_mask =
        (raf_route_nz(request->magic ^ RAF_ROUTE_REQUEST_MAGIC) * RAF_ROUTE_ERROR_MAGIC) |
        (raf_route_nz((raf_route_u32)request->version ^ RAF_ROUTE_VERSION) * RAF_ROUTE_ERROR_VERSION) |
        (trigger_error * RAF_ROUTE_ERROR_TRIGGER) |
        ((1u ^ raf_route_nz(source_bound)) * RAF_ROUTE_ERROR_SOURCE_BOUND) |
        (raf_route_nz(ambiguous) * RAF_ROUTE_ERROR_AMBIGUOUS) |
        (raf_route_nz(flags & (0u - (RAF_ROUTE_FLAG_KNOWN + 1u))) * RAF_ROUTE_ERROR_FLAGS) |
        ((1u ^ raf_route_nz(source_present)) * RAF_ROUTE_ERROR_SOURCE_ID) |
        (raf_route_nz(request->reserved) * RAF_ROUTE_ERROR_RESERVED);
    const raf_route_u32 status = raf_route_nz(error_mask);
    const raf_route_u32 route_code = trigger * (1u ^ status);
    const raf_route_u32 route_tag = raf_route_mix(
        source_fold ^ route_code ^ (trigger << 16) ^ 0x9e3779b9u
    );

    receipt->magic = RAF_ROUTE_RECEIPT_MAGIC;
    receipt->version = RAF_ROUTE_VERSION;
    receipt->status = (raf_route_u16)status;
    receipt->error_mask = error_mask;
    receipt->trigger_code = trigger;
    receipt->route_code = route_code;
    receipt->route_tag = route_tag * (1u ^ status);
    receipt->safe_flags = flags & (RAF_ROUTE_FLAG_SOURCE_BOUND |
                                   RAF_ROUTE_FLAG_EXPANSION_REQUIRED);
    receipt->source_fold = source_fold;
}
