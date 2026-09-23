#include "rafcode_route.h"

static int expect_true(int condition)
{
    return condition ? 0 : 1;
}

static raf_route_request valid_request(raf_route_u16 trigger)
{
    raf_route_request request;
    request.magic = RAF_ROUTE_REQUEST_MAGIC;
    request.version = RAF_ROUTE_VERSION;
    request.trigger_code = trigger;
    request.flags = RAF_ROUTE_FLAG_SOURCE_BOUND;
    request.source_digest[0] = 0x11111111u;
    request.source_digest[1] = 0x22222222u;
    request.source_digest[2] = 0x44444444u;
    request.source_digest[3] = 0x88888888u;
    request.reserved = 0u;
    return request;
}

int main(void)
{
    int failed = 0;
    raf_route_u16 trigger;

    for (trigger = RAF_ROUTE_TRIGGER_GENERAL;
         trigger <= RAF_ROUTE_TRIGGER_MAX;
         trigger = (raf_route_u16)(trigger + 1u)) {
        raf_route_request request = valid_request(trigger);
        raf_route_receipt receipt;
        raf_route_resolve(&request, &receipt);
        failed += expect_true(receipt.status == 0u);
        failed += expect_true(receipt.error_mask == 0u);
        failed += expect_true(receipt.route_code == (raf_route_u32)trigger);
        failed += expect_true(receipt.route_tag != 0u);
        failed += expect_true(receipt.source_fold != 0u);
    }

    {
        raf_route_request request = valid_request(0u);
        raf_route_receipt receipt;
        raf_route_resolve(&request, &receipt);
        failed += expect_true(receipt.status == 1u);
        failed += expect_true((receipt.error_mask & RAF_ROUTE_ERROR_TRIGGER) != 0u);
        failed += expect_true(receipt.route_code == 0u);
        failed += expect_true(receipt.route_tag == 0u);
    }

    {
        raf_route_request request = valid_request(RAF_ROUTE_TRIGGER_ATLAS);
        raf_route_receipt receipt;
        request.flags |= RAF_ROUTE_FLAG_AMBIGUOUS;
        raf_route_resolve(&request, &receipt);
        failed += expect_true(receipt.status == 1u);
        failed += expect_true((receipt.error_mask & RAF_ROUTE_ERROR_AMBIGUOUS) != 0u);
        failed += expect_true(receipt.route_code == 0u);
    }

    {
        raf_route_request request = valid_request(RAF_ROUTE_TRIGGER_CODE_RUNTIME);
        raf_route_receipt receipt;
        request.flags = 0u;
        raf_route_resolve(&request, &receipt);
        failed += expect_true(receipt.status == 1u);
        failed += expect_true((receipt.error_mask & RAF_ROUTE_ERROR_SOURCE_BOUND) != 0u);
    }

    {
        raf_route_request request = valid_request(RAF_ROUTE_TRIGGER_SCIENCE_CLAIMS);
        raf_route_receipt receipt;
        request.source_digest[0] = 0u;
        request.source_digest[1] = 0u;
        request.source_digest[2] = 0u;
        request.source_digest[3] = 0u;
        raf_route_resolve(&request, &receipt);
        failed += expect_true(receipt.status == 1u);
        failed += expect_true((receipt.error_mask & RAF_ROUTE_ERROR_SOURCE_ID) != 0u);
    }

    {
        raf_route_request request = valid_request(RAF_ROUTE_TRIGGER_SCIENCE_CLAIMS);
        raf_route_receipt receipt;
        request.source_digest[3] = request.source_digest[0] ^
                                   request.source_digest[1] ^
                                   request.source_digest[2];
        raf_route_resolve(&request, &receipt);
        failed += expect_true(receipt.status == 0u);
        failed += expect_true(receipt.source_fold == 0u);
        failed += expect_true(receipt.route_code == RAF_ROUTE_ID_SCIENCE_CLAIMS);
    }

    {
        raf_route_request request = valid_request(RAF_ROUTE_TRIGGER_LEARN);
        raf_route_receipt receipt;
        request.reserved = 1u;
        raf_route_resolve(&request, &receipt);
        failed += expect_true(receipt.status == 1u);
        failed += expect_true((receipt.error_mask & RAF_ROUTE_ERROR_RESERVED) != 0u);
    }

    {
        raf_route_request request = valid_request(RAF_ROUTE_TRIGGER_GAP);
        raf_route_receipt receipt;
        request.flags |= 0x80000000u;
        raf_route_resolve(&request, &receipt);
        failed += expect_true(receipt.status == 1u);
        failed += expect_true((receipt.error_mask & RAF_ROUTE_ERROR_FLAGS) != 0u);
    }

    return failed;
}
