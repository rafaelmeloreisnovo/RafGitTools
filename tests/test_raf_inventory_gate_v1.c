/* Hosted test harness ONLY; the evaluated header remains freestanding. */
#include "../include/raf_inventory_gate_v1.h"
#include <stdio.h>

static unsigned tests;
static unsigned failed;
static void expect_mask(unsigned actual, unsigned expected, const char *label) {
    ++tests;
    if (actual != expected) {
        ++failed;
        (void)fprintf(stderr, "%s: got=0x%x want=0x%x\n", label, actual, expected);
    }
}
static raf_inv_preflight_v1 fixture(void) {
    raf_inv_preflight_v1 x = {0};
    x.parent_page = 40ull;
    x.candidate_page = 41ull;
    x.parent_nodes_committed = 1000ull;
    x.parent_edges_committed = 1000ull;
    x.candidate_nodes_committed = 1000ull;
    x.candidate_edges_committed = 1000ull;
    x.observed_nodes = x.partition_nodes = 15u;
    x.observed_edges = x.partition_edges = 15u;
    x.predecessor_id_verified = 1u;
    x.receipt_verified = 1u;
    x.partition_readback_verified = 1u;
    x.cursor_bound_to_parent = 1u;
    return x;
}
int main(void) {
    raf_inv_preflight_v1 x = fixture();
    unsigned base = RAF_INV_GAP_SINGLE_WRITER | RAF_INV_GAP_IDENTITY |
                    RAF_INV_GAP_CROSS_DEDUP | RAF_INV_GAP_CONTENT_SHA;
    expect_mask(raf_inv_preflight_check_v1(x), base, "typed gaps synthetic");
    x.candidate_nodes_committed = 1015ull;
    expect_mask(raf_inv_preflight_check_v1(x), base | RAF_INV_GAP_TOTALS,
                "fabricated +15 unique rejected");
    x = fixture(); x.partition_edges = 14u;
    expect_mask(raf_inv_preflight_check_v1(x), base | RAF_INV_GAP_PARTITIONS,
                "partition mismatch");
    x = fixture(); x.receipt_verified = 0u;
    expect_mask(raf_inv_preflight_check_v1(x), base | RAF_INV_GAP_RECEIPT,
                "receipt absent");
    x = fixture(); x.partition_readback_verified = 0u;
    expect_mask(raf_inv_preflight_check_v1(x), base | RAF_INV_GAP_READBACK,
                "readback absent");
    x = fixture(); x.cursor_bound_to_parent = 0u;
    expect_mask(raf_inv_preflight_check_v1(x), base | RAF_INV_GAP_CURSOR,
                "stale cursor");
    x = fixture(); x.candidate_page = 42ull;
    expect_mask(raf_inv_preflight_check_v1(x), base | RAF_INV_GAP_PAGE_SEQUENCE,
                "skipped checkpoint");
    x = fixture(); x.claim_requested = 1u;
    expect_mask(raf_inv_preflight_check_v1(x), base | RAF_INV_GAP_CLAIM,
                "global claim denied");
    x = fixture(); x.parent_page = ~0ull; x.candidate_page = 0ull;
    expect_mask(raf_inv_preflight_check_v1(x), base | RAF_INV_GAP_PAGE_SEQUENCE,
                "overflow denied");
    x = fixture(); x.single_writer_enforced_and_verified = 1u;
    x.batch_identity_verified = 1u; x.unique_deltas_verified = 1u;
    x.cross_shard_dedup_verified = 1u; x.content_sha256_verified = 1u;
    x.unique_nodes_delta = 0ull; x.unique_edges_delta = 0ull;
    expect_mask(raf_inv_preflight_check_v1(x), 0u,
                "synthetic fully attested input (not provider proof)");
    x.unique_nodes_delta = 15ull; x.unique_edges_delta = 15ull;
    expect_mask(raf_inv_preflight_check_v1(x), RAF_INV_GAP_TOTALS,
                "asserted unique delta without counter advance");
    x.candidate_nodes_committed = x.parent_nodes_committed + 15ull;
    x.candidate_edges_committed = x.parent_edges_committed + 15ull;
    expect_mask(raf_inv_preflight_check_v1(x), 0u,
                "synthetic consistent unique delta");
    x.unique_nodes_delta = 16ull;
    expect_mask(raf_inv_preflight_check_v1(x), RAF_INV_GAP_TOTALS,
                "delta exceeds observed rows");
    (void)printf("tests=%u failures=%u\n", tests, failed);
    return failed ? 1 : 0;
}
