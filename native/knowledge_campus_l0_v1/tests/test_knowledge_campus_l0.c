#include "raf_knowledge_campus_l0.h"

int main(void) {
    static const char name[]="CRANKSHAFT";
    raf_kc_slot_input hot={1000u,900u,65536u,65536u,1u,0u};
    raf_kc_slot_input cold={0u,0u,0u,0u,100u,20u};
    raf_kc_u32 a=raf_kc_tag32(name,10u,1u);
    raf_kc_u32 b=raf_kc_tag32(name,10u,1u);
    if (a==0u || a!=b) return 1;
    if (raf_kc_relation_tag(a,RAF_KC_REL_PART_OF,b)==0u) return 2;
    if (raf_kc_slot_tier(raf_kc_slot_score(&hot))!=RAF_KC_TIER_HOT) return 3;
    if (raf_kc_slot_tier(raf_kc_slot_score(&cold))!=RAF_KC_TIER_ARCHIVE) return 4;
    return 0;
}
