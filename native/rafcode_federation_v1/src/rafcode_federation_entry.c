#include "rafcode_federation.h"

static inline raf_u32 raf_nz(raf_u32 value)
{
    return (value | (0u - value)) >> 31;
}

static inline void raf_zero_work(raf_fed_work *work)
{
    work->magic = 0u;
    work->version = 0u;
    work->axis_mask = 0u;
    work->participant_mask = 0u;
    work->transaction_lo = 0u;
    work->transaction_hi = 0u;
    work->action = 0u;
    work->state_before = 0u;
    work->state_after = 0u;
    work->flags = 0u;
    work->input_digest[0] = 0u;
    work->input_digest[1] = 0u;
    work->input_digest[2] = 0u;
    work->input_digest[3] = 0u;
    work->route_digest[0] = 0u;
    work->route_digest[1] = 0u;
    work->route_digest[2] = 0u;
    work->route_digest[3] = 0u;
}

void raf_entry(void)
{
    raf_fed_work work;
    raf_fed_receipt receipt;
    raf_iptr transferred;
    raf_u32 read_error;
    raf_u32 write_error;
    raf_u32 exit_status;

    raf_zero_work(&work);
    transferred = raf_sys_read(0, &work, sizeof(work));
    read_error = raf_nz((raf_u32)transferred ^ (raf_u32)sizeof(work));
    raf_fed_validate(&work, &receipt, read_error);
    transferred = raf_sys_write(1, &receipt, sizeof(receipt));
    write_error = raf_nz((raf_u32)transferred ^ (raf_u32)sizeof(receipt));
    exit_status = (write_error * 3u) |
                  ((1u ^ write_error) * (raf_u32)receipt.status * 2u);
    raf_sys_exit(exit_status);
}
