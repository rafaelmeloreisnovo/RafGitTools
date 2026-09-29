#ifndef RAF_KNOWLEDGE_CAMPUS_L0_H
#define RAF_KNOWLEDGE_CAMPUS_L0_H

/* Freestanding Knowledge Campus L0 V1.
 * No libc/system headers, allocator, JNI, Android API or external runtime symbols.
 */
typedef unsigned char raf_kc_u8;
typedef unsigned short raf_kc_u16;
typedef unsigned int raf_kc_u32;
typedef signed int raf_kc_i32;

_Static_assert(sizeof(raf_kc_u8)==1,"u8");
_Static_assert(sizeof(raf_kc_u16)==2,"u16");
_Static_assert(sizeof(raf_kc_u32)==4,"u32");

enum {
    RAF_KC_TIER_HOT=1u,
    RAF_KC_TIER_WARM=2u,
    RAF_KC_TIER_COLD=3u,
    RAF_KC_TIER_ARCHIVE=4u
};

enum {
    RAF_KC_REL_CONTAINS=1u,
    RAF_KC_REL_PART_OF=2u,
    RAF_KC_REL_DEPENDS_ON=3u,
    RAF_KC_REL_DERIVES_FROM=4u,
    RAF_KC_REL_CORRELATES_WITH=5u,
    RAF_KC_REL_ANALOGOUS_TO=6u,
    RAF_KC_REL_CONTRADICTS=7u,
    RAF_KC_REL_EVIDENCED_BY=8u,
    RAF_KC_REL_NEXT=9u,
    RAF_KC_REL_PREVIOUS=10u
};

typedef struct raf_kc_slot_input {
    raf_kc_u32 access_count;
    raf_kc_u32 recency_rank;
    raf_kc_u32 centrality_q16;
    raf_kc_u32 active_project_q16;
    raf_kc_u32 byte_cost_rank;
    raf_kc_u32 remote_hops;
} raf_kc_slot_input;

raf_kc_u32 raf_kc_tag32(const void *data, raf_kc_u32 n, raf_kc_u32 seed);
raf_kc_u32 raf_kc_relation_tag(raf_kc_u32 source_tag, raf_kc_u32 relation_type, raf_kc_u32 target_tag);
raf_kc_i32 raf_kc_slot_score(const raf_kc_slot_input *input);
raf_kc_u32 raf_kc_slot_tier(raf_kc_i32 score);

#endif
