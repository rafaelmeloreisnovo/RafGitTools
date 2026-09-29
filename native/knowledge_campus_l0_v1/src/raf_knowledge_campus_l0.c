#include "raf_knowledge_campus_l0.h"

static raf_kc_u32 raf_kc_mix32(raf_kc_u32 x) {
    x ^= x >> 16u;
    x *= 0x7FEB352Du;
    x ^= x >> 15u;
    x *= 0x846CA68Bu;
    x ^= x >> 16u;
    return x;
}

static raf_kc_i32 raf_kc_sat_add(raf_kc_i32 a, raf_kc_i32 b) {
    if (b > 0 && a > (raf_kc_i32)2147483647 - b) return (raf_kc_i32)2147483647;
    if (b < 0 && a < (raf_kc_i32)(-2147483647 - 1) - b) return (raf_kc_i32)(-2147483647 - 1);
    return a + b;
}

raf_kc_u32 raf_kc_tag32(const void *data, raf_kc_u32 n, raf_kc_u32 seed) {
    const raf_kc_u8 *p=(const raf_kc_u8 *)data;
    raf_kc_u32 i;
    raf_kc_u32 h=seed ^ 0x811C9DC5u;
    if (p==(const raf_kc_u8 *)0) return raf_kc_mix32(h ^ n);
    for (i=0u;i<n;++i) {
        h ^= (raf_kc_u32)p[i];
        h *= 0x01000193u;
    }
    return raf_kc_mix32(h ^ n);
}

raf_kc_u32 raf_kc_relation_tag(raf_kc_u32 source_tag, raf_kc_u32 relation_type, raf_kc_u32 target_tag) {
    raf_kc_u32 x=source_tag ^ (relation_type * 0x9E3779B9u);
    x=raf_kc_mix32(x ^ target_tag);
    return x;
}

raf_kc_i32 raf_kc_slot_score(const raf_kc_slot_input *input) {
    raf_kc_i32 score=0;
    if (input==(const raf_kc_slot_input *)0) return (raf_kc_i32)(-2147483647 - 1);
    score=raf_kc_sat_add(score,(raf_kc_i32)(input->access_count > 65535u ? 65535u : input->access_count) * 8);
    score=raf_kc_sat_add(score,(raf_kc_i32)(input->recency_rank > 65535u ? 65535u : input->recency_rank) * 6);
    score=raf_kc_sat_add(score,(raf_kc_i32)(input->centrality_q16 >> 8u));
    score=raf_kc_sat_add(score,(raf_kc_i32)(input->active_project_q16 >> 8u));
    score=raf_kc_sat_add(score,-((raf_kc_i32)(input->byte_cost_rank > 65535u ? 65535u : input->byte_cost_rank) * 3));
    score=raf_kc_sat_add(score,-((raf_kc_i32)(input->remote_hops > 65535u ? 65535u : input->remote_hops) * 16));
    return score;
}

raf_kc_u32 raf_kc_slot_tier(raf_kc_i32 score) {
    if (score >= 8192) return RAF_KC_TIER_HOT;
    if (score >= 2048) return RAF_KC_TIER_WARM;
    if (score >= 0) return RAF_KC_TIER_COLD;
    return RAF_KC_TIER_ARCHIVE;
}
