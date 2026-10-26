/* Freestanding deterministic self-test, no hosted I/O or runtime. */
#include "session_queue_core_v1.h"
/* Test-only explicit volatile byte movement prevents compiler-emitted
 * hosted memcpy for ARMv7 aggregate copies; no libc or syscall dependency. */
static void raf_copy_test_intent_v1(raf_queue_intent_v1 *dst, const raf_queue_intent_v1 *src) {
    volatile uint8_t *d = (volatile uint8_t *)dst;
    const volatile uint8_t *s = (const volatile uint8_t *)src;
    uint32_t i;
    for (i=0u; i<(uint32_t)sizeof(*dst); ++i) d[i] = s[i];
}
#define SP(s) (raf_span_v1){ (const uint8_t *)(s), (uint32_t)(sizeof(s)-1u) }
#define VERIFY(x) do{ if(!(x)) ++failed; }while(0)
uint32_t raf_queue_selftest_v1(void) {
    uint32_t failed=0u;
    static const raf_queue_intent_v1 fixture={
        .schema=SP("rafgittools.session-queue-preflight.v1"),
        .envelope_id=SP("11111111111111111111111111111111"),
        .session_id=SP("22222222222222222222222222222222"),
        .target_id=SP("33333333333333333333333333333333"),
        .packet_id=SP("SP08_SESSION_TO_WORK_PACKETS"),
        .mapa_commit=SP("ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09"),
        .mapa_blob=SP("c1d1d8f1692c74957f8b70968fa863f855d93cb0"),
        .operation=SP("PLAN_ONLY"),.state=SP("IN"),
        .privacy_class=SP("UNKNOWN_REVIEW_REQUIRED"),
        .payload_mode=SP("NONE"),
        .idempotency_key=SP("a55180ecbfd5fd0bb948f402d64fdf5457914e43d07993467b4fa7e82734e1bc"),
        .execution_enabled=0,.human_approved=0,.claim_allowed=0,.boolean_types_verified=1
    };
    raf_queue_intent_v1 x;
    raf_copy_test_intent_v1(&x,&fixture);
    raf_queue_intent_v1 y;
    raf_copy_test_intent_v1(&y,&x);
    VERIFY(raf_queue_preflight_v1(&x)==0u);
    x.state=SP("GATES");x.privacy_class=SP("PRIVATE_REQUIRED");
    VERIFY(raf_queue_preflight_v1(&x)==0u);
    raf_copy_test_intent_v1(&x,&y);x.session_id=SP("44444444444444444444444444444444");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_IDEMPOTENCY)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.boolean_types_verified=0;
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PARSER_TYPE)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.execution_enabled=1;
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PROMOTION)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.human_approved=1;
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PROMOTION)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.claim_allowed=1;
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PROMOTION)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.payload_mode=SP("RAW");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PAYLOAD)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.mapa_blob=SP("0000000000000000000000000000000000000000");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PIN)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.state=SP("PROCESSING");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_STATE)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.privacy_class=SP("PUBLIC");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PRIVACY)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.packet_id=SP("SP08 BAD");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PACKET)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.schema=SP("MISSING");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_SCHEMA)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.idempotency_key=SP("0000000000000000000000000000000000000000000000000000000000000000");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_IDEMPOTENCY)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.envelope_id=(raf_span_v1){0,32};
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_BAD_POINTER)!=0u);
    raf_copy_test_intent_v1(&x,&y);x.operation=SP("EXECUTE");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_OPERATION)!=0u);
    VERIFY(raf_queue_transition_v1(RAFQ_IN,RAFQ_GATES));
    VERIFY(raf_queue_transition_v1(RAFQ_IN,RAFQ_HOLD));
    VERIFY(!raf_queue_transition_v1(RAFQ_GATES,RAFQ_IN));
    VERIFY(!raf_queue_transition_v1(RAFQ_GATES,4));
    VERIFY(!raf_queue_transition_v1(RAFQ_HOLD,4));
    VERIFY((raf_queue_preflight_v1(0)&RAFQ_BAD_POINTER)!=0u);
    return failed;
}
