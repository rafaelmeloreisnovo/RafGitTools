/* RAFAELIA P0: source-only multi-PAT planning gate. No libc, heap or syscalls.
 * No PAT value, network, GitHub endpoint or administrative execution exists.
 */
#include "pat_controlplane_gate_v1.h"

static uint32_t good_sha(const uint8_t *p, uint32_t n) {
    uint32_t i;
    if (!p || n != 40u) return 0u;
    for (i=0u; i<40u; ++i) {
        const uint8_t c=p[i];
        if (!((c>='0'&&c<='9')||(c>='a'&&c<='f'))) return 0u;
    }
    return 1u;
}
static uint32_t good_repo(const uint8_t *p, uint32_t n) {
    uint32_t i, sep=0u;
    if (!p || n<3u || n>100u) return 0u;
    if (p[0]=='/' || p[n-1u]=='/') return 0u;
    for (i=0u; i<n; ++i) {
        uint8_t c=p[i];
        if (c=='/') { ++sep; continue; }
        if (!((c>='A'&&c<='Z')||(c>='a'&&c<='z')||
              (c>='0'&&c<='9')||c=='.'||c=='_'||c=='-')) return 0u;
    }
    return sep==1u;
}
uint32_t raf_pat_plan_v1(const raf_pat_plan_request_v1 *req,
                         raf_pat_plan_result_v1 *result) {
    uint32_t flags=RAF_PAT_NO_AUTO_EXECUTION;
    if (!result) return RAF_PAT_BAD_TARGET | RAF_PAT_NO_AUTO_EXECUTION;
    result->plan_read_only=0u;
    result->dispatch_allowed=0u;
    result->promotion_allowed=0u;
    result->flags=RAF_PAT_BAD_TARGET | RAF_PAT_NO_AUTO_EXECUTION;
    if (!req) return result->flags;
    if (!good_repo(req->repository, req->repository_length)) flags|=RAF_PAT_BAD_TARGET;
    if (!good_sha(req->expected_sha, req->expected_sha_length)) flags|=RAF_PAT_BAD_SHA;
    if (req->capability>=RAF_PAT_COUNT) flags|=RAF_PAT_UNKNOWN_CAP;
    /* Operator intent is never administrative authorization.
     * Privileged or private operations always fail closed, even if approved.
     */
    if (req->operation!=RAF_OP_PUBLIC_REPO_METADATA) flags|=RAF_PAT_MUTATION_HOLD;
    if (req->capability!=RAF_PAT_ACTIONS) flags|=RAF_PAT_UNVERIFIED_SCOPE;
    if (req->operator_approved!=0u) flags|=RAF_PAT_OPERATOR_REVIEW;
    if ((flags & (RAF_PAT_BAD_TARGET|RAF_PAT_BAD_SHA|RAF_PAT_UNKNOWN_CAP|
                  RAF_PAT_UNVERIFIED_SCOPE|RAF_PAT_MUTATION_HOLD|
                  RAF_PAT_OPERATOR_REVIEW))==0u) {
        result->plan_read_only=1u; /* public proposal only; no provider call */
    }
    result->flags=flags;
    return flags;
}
