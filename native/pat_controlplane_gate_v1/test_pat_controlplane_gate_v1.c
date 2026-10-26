/* Pure in-memory regression/falsifier; host runner is external/test-only. */
#include "pat_controlplane_gate_v1.h"
#define CHECK(x) do { if (!(x)) ++failed; } while (0)
uint32_t raf_pat_selftest_v1(void) {
    static const uint8_t repo[]="rafaelmeloreisnovo/RafGitTools";
    static const uint8_t sha[]="c6a66a1ea681e62a6b16962e8925d6e9abe08ebe";
    raf_pat_plan_request_v1 q={repo,sizeof(repo)-1u,sha,sizeof(sha)-1u,
                                 RAF_PAT_ACTIONS,RAF_OP_PUBLIC_REPO_METADATA,0u};
    raf_pat_plan_result_v1 out;
    uint32_t failed=0u,id,op;
    CHECK((raf_pat_plan_v1(&q,&out)&RAF_PAT_NO_AUTO_EXECUTION)!=0u);
    CHECK(out.plan_read_only==1u && out.dispatch_allowed==0u && out.promotion_allowed==0u);
    for (id=0u; id<RAF_PAT_COUNT; ++id) {
        for (op=0u; op<=RAF_OP_MERGE; ++op) {
            q.capability=id;q.operation=op;q.operator_approved=0u;
            raf_pat_plan_v1(&q,&out);
            CHECK(out.dispatch_allowed==0u && out.promotion_allowed==0u);
            CHECK(out.plan_read_only==(id==RAF_PAT_ACTIONS && op==RAF_OP_PUBLIC_REPO_METADATA));
            if (op!=RAF_OP_PUBLIC_REPO_METADATA) CHECK((out.flags&RAF_PAT_MUTATION_HOLD)!=0u);
            if (id!=RAF_PAT_ACTIONS) CHECK((out.flags&RAF_PAT_UNVERIFIED_SCOPE)!=0u);
        }
    }
    q.capability=RAF_PAT_ACTIONS;q.operation=RAF_OP_VISIBILITY;q.operator_approved=1u;
    raf_pat_plan_v1(&q,&out);
    CHECK(out.plan_read_only==0u && (out.flags&RAF_PAT_OPERATOR_REVIEW)!=0u);
    q.operation=RAF_OP_PUBLIC_REPO_METADATA;q.operator_approved=0u;
    q.expected_sha=(const uint8_t *)"bad";q.expected_sha_length=3u;
    raf_pat_plan_v1(&q,&out);CHECK(out.plan_read_only==0u && (out.flags&RAF_PAT_BAD_SHA)!=0u);
    q.expected_sha=sha;q.expected_sha_length=sizeof(sha)-1u;
    q.repository=(const uint8_t *)"a//b";q.repository_length=4u;
    raf_pat_plan_v1(&q,&out);CHECK(out.plan_read_only==0u && (out.flags&RAF_PAT_BAD_TARGET)!=0u);
    CHECK((raf_pat_plan_v1((const raf_pat_plan_request_v1 *)0,&out)&RAF_PAT_BAD_TARGET)!=0u);
    CHECK((raf_pat_plan_v1(&q,(raf_pat_plan_result_v1 *)0)&RAF_PAT_BAD_TARGET)!=0u);
    return failed;
}
