#ifndef RAFAELIA_PAT_CONTROLPLANE_GATE_V1_H
#define RAFAELIA_PAT_CONTROLPLANE_GATE_V1_H
#include <stdint.h>
enum raf_pat_id_v1 {
    RAF_PAT_ACTIONS=0, RAF_PAT_AGENTS=1, RAF_PAT_CODESPACES=2,
    RAF_PAT_DEPENDABOT=3, RAF_PAT_ENV=4, RAF_PAT_ENVIRONMENTS=5,
    RAF_PAT_COUNT=6
};
enum raf_pat_operation_v1 {
    RAF_OP_PUBLIC_REPO_METADATA=0, RAF_OP_PRIVATE_READ=1,
    RAF_OP_PULL_REQUEST=2, RAF_OP_BRANCH_PROTECTION=3,
    RAF_OP_VISIBILITY=4, RAF_OP_SECRETS=5,
    RAF_OP_DEPENDABOT_WRITE=6, RAF_OP_ENVIRONMENT_WRITE=7,
    RAF_OP_WORKFLOW_DISPATCH=8, RAF_OP_MERGE=9
};
enum raf_pat_fault_v1 {
    RAF_PAT_BAD_TARGET=1u, RAF_PAT_BAD_SHA=2u,
    RAF_PAT_UNKNOWN_CAP=4u, RAF_PAT_UNVERIFIED_SCOPE=8u,
    RAF_PAT_MUTATION_HOLD=16u, RAF_PAT_NO_AUTO_EXECUTION=32u,
    RAF_PAT_OPERATOR_REVIEW=64u
};
typedef struct {
    const uint8_t *repository;
    uint32_t repository_length;
    const uint8_t *expected_sha;
    uint32_t expected_sha_length;
    uint32_t capability;
    uint32_t operation;
    uint32_t operator_approved;
} raf_pat_plan_request_v1;
typedef struct {
    uint32_t flags;
    uint32_t plan_read_only;
    uint32_t dispatch_allowed;
    uint32_t promotion_allowed;
} raf_pat_plan_result_v1;
/* Pure freestanding planner. NEVER executes, retrieves or validates PATs.
 * Spans must be caller-owned/readable. No I/O; output is intent only.
 */
uint32_t raf_pat_plan_v1(const raf_pat_plan_request_v1 *req,
                         raf_pat_plan_result_v1 *result);
uint32_t raf_pat_selftest_v1(void);
#endif
