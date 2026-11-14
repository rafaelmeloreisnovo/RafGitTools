#ifndef RAF_INVENTORY_GATE_V1_H
#define RAF_INVENTORY_GATE_V1_H
/*
 * RAFAELIA Omega / RafGitTools - inventory checkpoint preflight v1.
 * ISO C11 freestanding; zero libc, heap, syscalls, JNI, SDK or NDK calls.
 * Input is a typed, caller-verified projection, NEVER raw JSON or proof.
 * The returned bitmask is a set of blockers; zero is only a scoped
 * structural decision for the supplied values, NOT provider enforcement.
 * This header does not acquire a lock, fetch content, or compute SHA-256.
 * See: START HERE V2.2 -> CURRENT_STATE -> Mapa (federated authority).
 */

enum raf_inv_gap_v1 {
    RAF_INV_GAP_PARENT         = 1u << 0,
    RAF_INV_GAP_PAGE_SEQUENCE  = 1u << 1,
    RAF_INV_GAP_PARTITIONS     = 1u << 2,
    RAF_INV_GAP_RECEIPT       = 1u << 3,
    RAF_INV_GAP_READBACK      = 1u << 4,
    RAF_INV_GAP_SINGLE_WRITER = 1u << 5,
    RAF_INV_GAP_IDENTITY      = 1u << 6,
    RAF_INV_GAP_TOTALS        = 1u << 7,
    RAF_INV_GAP_CURSOR        = 1u << 8,
    RAF_INV_GAP_CROSS_DEDUP   = 1u << 9,
    RAF_INV_GAP_CONTENT_SHA   = 1u << 10,
    RAF_INV_GAP_CLAIM         = 1u << 11
};

typedef struct raf_inv_preflight_v1 {
    unsigned long long parent_page;
    unsigned long long candidate_page;
    unsigned long long parent_nodes_committed;
    unsigned long long parent_edges_committed;
    unsigned long long candidate_nodes_committed;
    unsigned long long candidate_edges_committed;
    unsigned long long unique_nodes_delta;
    unsigned long long unique_edges_delta;
    unsigned int observed_nodes;
    unsigned int observed_edges;
    unsigned int partition_nodes;
    unsigned int partition_edges;
    unsigned char predecessor_id_verified;
    unsigned char receipt_verified;
    unsigned char partition_readback_verified;
    unsigned char single_writer_enforced_and_verified;
    unsigned char batch_identity_verified;
    unsigned char unique_deltas_verified;
    unsigned char cursor_bound_to_parent;
    unsigned char cross_shard_dedup_verified;
    unsigned char content_sha256_verified;
    unsigned char claim_requested;
} raf_inv_preflight_v1;

static unsigned int raf_inv_preflight_check_v1(raf_inv_preflight_v1 x)
{
    unsigned int gap = 0u;
    if (!x.predecessor_id_verified) gap |= RAF_INV_GAP_PARENT;
    if (x.parent_page == ~0ull || x.candidate_page != x.parent_page + 1ull)
        gap |= RAF_INV_GAP_PAGE_SEQUENCE;
    if (x.observed_nodes != x.partition_nodes ||
        x.observed_edges != x.partition_edges ||
        x.partition_nodes == 0u || x.partition_edges == 0u)
        gap |= RAF_INV_GAP_PARTITIONS;
    if (!x.receipt_verified) gap |= RAF_INV_GAP_RECEIPT;
    if (!x.partition_readback_verified) gap |= RAF_INV_GAP_READBACK;
    if (!x.single_writer_enforced_and_verified)
        gap |= RAF_INV_GAP_SINGLE_WRITER;
    if (!x.cursor_bound_to_parent) gap |= RAF_INV_GAP_CURSOR;
    if (!x.batch_identity_verified || !x.unique_deltas_verified)
        gap |= RAF_INV_GAP_IDENTITY;
    if (x.candidate_nodes_committed < x.parent_nodes_committed ||
        x.candidate_edges_committed < x.parent_edges_committed)
        gap |= RAF_INV_GAP_TOTALS;
    if (x.unique_deltas_verified) {
        /* Subtraction avoids addition overflow and fabricated totals. */
        if (x.candidate_nodes_committed < x.parent_nodes_committed ||
            x.candidate_edges_committed < x.parent_edges_committed ||
            x.candidate_nodes_committed - x.parent_nodes_committed != x.unique_nodes_delta ||
            x.candidate_edges_committed - x.parent_edges_committed != x.unique_edges_delta ||
            x.unique_nodes_delta > (unsigned long long)x.observed_nodes ||
            x.unique_edges_delta > (unsigned long long)x.observed_edges)
            gap |= RAF_INV_GAP_TOTALS;
    } else if (x.candidate_nodes_committed != x.parent_nodes_committed ||
               x.candidate_edges_committed != x.parent_edges_committed) {
        gap |= RAF_INV_GAP_TOTALS;
    }
    if (!x.cross_shard_dedup_verified) gap |= RAF_INV_GAP_CROSS_DEDUP;
    if (!x.content_sha256_verified) gap |= RAF_INV_GAP_CONTENT_SHA;
    /* Only externally attested completion may authorize a claim; this
     * structural checker deliberately cannot attest provider completion. */
    if (x.claim_requested) gap |= RAF_INV_GAP_CLAIM;
    return gap;
}

#endif /* RAF_INVENTORY_GATE_V1_H */
