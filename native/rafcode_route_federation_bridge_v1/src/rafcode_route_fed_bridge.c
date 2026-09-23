#include "rafcode_route_fed_bridge.h"

static __inline__ __attribute__((__always_inline__)) raf_u32 raf_bridge_nz(raf_u32 value)
{
    return (value | (0u - value)) >> 31;
}

raf_u32 raf_route_fed_digest(const raf_route_receipt *route, raf_u32 digest[4])
{
    const raf_u32 route_code = route->route_code;
    const raf_u32 trigger_code = route->trigger_code;
    const raf_u32 error_mask =
        (raf_bridge_nz(route->magic ^ RAF_ROUTE_RECEIPT_MAGIC) *
         RAF_ROUTE_FED_ERROR_ROUTE_MAGIC) |
        (raf_bridge_nz((raf_u32)route->version ^ RAF_ROUTE_VERSION) *
         RAF_ROUTE_FED_ERROR_ROUTE_VERSION) |
        (raf_bridge_nz((raf_u32)route->status) *
         RAF_ROUTE_FED_ERROR_ROUTE_STATUS) |
        (((raf_u32)(route_code < RAF_ROUTE_ID_GENERAL) |
          (raf_u32)(route_code > RAF_ROUTE_ID_LEARN)) *
         RAF_ROUTE_FED_ERROR_ROUTE_CODE) |
        (raf_bridge_nz(route_code ^ trigger_code) *
         RAF_ROUTE_FED_ERROR_ROUTE_MAPPING) |
        ((1u ^ raf_bridge_nz(route->route_tag)) *
         RAF_ROUTE_FED_ERROR_ROUTE_TAG);

    if (error_mask != 0u) {
        digest[0] = 0u;
        digest[1] = 0u;
        digest[2] = 0u;
        digest[3] = 0u;
        return error_mask;
    }

    digest[0] = route->route_tag;
    digest[1] = 0x524f0000u ^ route_code;
    digest[2] = 0x46454431u ^ route->source_fold;
    digest[3] = 0x4d500000u ^
                ((trigger_code & 0xffffu) << 8) ^
                (raf_u32)route->version;

    if ((digest[0] ^ digest[1] ^ digest[2] ^ digest[3]) == 0u) {
        digest[3] ^= 1u;
    }

    return 0u;
}
