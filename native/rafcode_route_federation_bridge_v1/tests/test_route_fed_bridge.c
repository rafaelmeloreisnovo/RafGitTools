#include "rafcode_route_fed_bridge.h"

static int expect_true(int condition)
{
    return condition ? 0 : 1;
}

static raf_route_request make_route_request(raf_route_u16 trigger)
{
    raf_route_request request;
    request.magic = RAF_ROUTE_REQUEST_MAGIC;
    request.version = RAF_ROUTE_VERSION;
    request.trigger_code = trigger;
    request.flags = RAF_ROUTE_FLAG_SOURCE_BOUND;
    request.source_digest[0] = 0x11111111u;
    request.source_digest[1] = 0x22222222u;
    request.source_digest[2] = 0x44444444u;
    request.source_digest[3] = 0x77777777u;
    request.reserved = 0u;
    return request;
}

static raf_fed_work make_fed_work(void)
{
    raf_fed_work work;
    raf_u32 index;

    work.magic = RAF_FED_WORK_MAGIC;
    work.version = RAF_FED_VERSION;
    work.axis_mask = RAF_FED_AXIS_REQUIRED;
    work.participant_mask = RAF_FED_ROLE_REQUIRED;
    work.transaction_lo = 0x13579bdfu;
    work.transaction_hi = 0x2468ace0u;
    work.action = RAF_FED_ACTION_RESOLVE_MAPA_ROUTE;
    work.state_before = RAF_FED_STATE_UNCERTAIN;
    work.state_after = RAF_FED_STATE_READY_TO_TEST;
    work.flags = RAF_FED_FLAG_RECEIPT_REQUIRED;
    work.input_digest[0] = 0x10203040u;
    work.input_digest[1] = 0x50607080u;
    work.input_digest[2] = 0x90a0b0c0u;
    work.input_digest[3] = 0xd0e0f001u;

    for (index = 0u; index < 4u; index++) {
        work.route_digest[index] = 0u;
    }

    return work;
}

int main(void)
{
    int failed = 0;

    {
        raf_route_request request = make_route_request(RAF_ROUTE_TRIGGER_ATLAS);
        raf_route_receipt route;
        raf_fed_work work = make_fed_work();
        raf_fed_receipt fed;
        raf_u32 bridge_error;

        raf_route_resolve(&request, &route);
        bridge_error = raf_route_fed_digest(&route, work.route_digest);
        raf_fed_validate(&work, &fed, 0u);

        failed += expect_true(route.status == 0u);
        failed += expect_true(bridge_error == 0u);
        failed += expect_true(
            (work.route_digest[0] ^ work.route_digest[1] ^
             work.route_digest[2] ^ work.route_digest[3]) != 0u
        );
        failed += expect_true(fed.status == 0u);
        failed += expect_true(fed.error_mask == 0u);
    }

    {
        raf_route_request request = make_route_request(RAF_ROUTE_TRIGGER_SCIENCE_CLAIMS);
        raf_route_receipt route;
        raf_u32 digest[4];
        raf_u32 bridge_error;

        request.source_digest[3] = request.source_digest[0] ^
                                   request.source_digest[1] ^
                                   request.source_digest[2];
        raf_route_resolve(&request, &route);
        bridge_error = raf_route_fed_digest(&route, digest);

        failed += expect_true(route.status == 0u);
        failed += expect_true(route.source_fold == 0u);
        failed += expect_true(bridge_error == 0u);
        failed += expect_true((digest[0] ^ digest[1] ^ digest[2] ^ digest[3]) != 0u);
    }

    {
        raf_route_receipt route;
        raf_u32 digest[4];
        raf_u32 bridge_error;

        route.magic = RAF_ROUTE_RECEIPT_MAGIC;
        route.version = RAF_ROUTE_VERSION;
        route.status = 1u;
        route.error_mask = RAF_ROUTE_ERROR_AMBIGUOUS;
        route.trigger_code = RAF_ROUTE_TRIGGER_ATLAS;
        route.route_code = 0u;
        route.route_tag = 0u;
        route.safe_flags = 0u;
        route.source_fold = 0u;

        bridge_error = raf_route_fed_digest(&route, digest);

        failed += expect_true(bridge_error != 0u);
        failed += expect_true(digest[0] == 0u);
        failed += expect_true(digest[1] == 0u);
        failed += expect_true(digest[2] == 0u);
        failed += expect_true(digest[3] == 0u);
    }

    return failed;
}
