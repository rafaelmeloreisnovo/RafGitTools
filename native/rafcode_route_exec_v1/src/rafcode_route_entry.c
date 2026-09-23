#include "rafcode_route_exec.h"

static __inline__ __attribute__((__always_inline__)) raf_route_u32 raf_route_exec_nz(
    raf_route_u32 value)
{
    return (value | (0u - value)) >> 31;
}

static __inline__ __attribute__((__always_inline__)) void raf_route_zero_request(
    raf_route_request *request)
{
    request->magic = 0u;
    request->version = 0u;
    request->trigger_code = 0u;
    request->flags = 0u;
    request->source_digest[0] = 0u;
    request->source_digest[1] = 0u;
    request->source_digest[2] = 0u;
    request->source_digest[3] = 0u;
    request->reserved = 0u;
}

void raf_route_entry(void)
{
    raf_route_request request;
    raf_route_receipt receipt;
    raf_route_iptr transferred;
    raf_route_u32 read_error;
    raf_route_u32 write_error;
    raf_route_u32 exit_status;

    raf_route_zero_request(&request);

    transferred = raf_route_sys_read(0, &request, sizeof(request));
    read_error = raf_route_exec_nz(
        (raf_route_u32)transferred ^ (raf_route_u32)sizeof(request)
    );

    if (read_error != 0u) {
        request.magic = 0u;
    }

    raf_route_resolve(&request, &receipt);

    transferred = raf_route_sys_write(1, &receipt, sizeof(receipt));
    write_error = raf_route_exec_nz(
        (raf_route_u32)transferred ^ (raf_route_u32)sizeof(receipt)
    );

    exit_status = (write_error * 3u) |
                  ((1u ^ write_error) * (raf_route_u32)receipt.status * 2u);

    raf_route_sys_exit(exit_status);
}
