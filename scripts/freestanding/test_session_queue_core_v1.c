/* Host test only; never linked into the freestanding core. */
#include "session_queue_core_v1.h"
#include <stdio.h>
#include <string.h>
#define SP(s) (raf_span_v1){ (const uint8_t *)(s), (uint32_t)(sizeof(s)-1u) }
#define VERIFY(x) do{ ++count; if(!(x)){ ++failed; printf("FAILED:%d\n",__LINE__); } }while(0)
int main(void) {
    unsigned count=0,failed=0;
    raf_queue_intent_v1 x={
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
    raf_queue_intent_v1 y=x;
    VERIFY(raf_queue_preflight_v1(&x)==0u);
    x.state=SP("GATES");x.privacy_class=SP("PRIVATE_REQUIRED");
    VERIFY(raf_queue_preflight_v1(&x)==0u);
    x=y;x.session_id=SP("44444444444444444444444444444444");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_IDEMPOTENCY)!=0u);
    x=y;x.boolean_types_verified=0;
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PARSER_TYPE)!=0u);
    x=y;x.execution_enabled=1;
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PROMOTION)!=0u);
    x=y;x.human_approved=1;
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PROMOTION)!=0u);
    x=y;x.claim_allowed=1;
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PROMOTION)!=0u);
    x=y;x.payload_mode=SP("RAW");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PAYLOAD)!=0u);
    x=y;x.mapa_blob=SP("0000000000000000000000000000000000000000");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PIN)!=0u);
    x=y;x.state=SP("PROCESSING");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_STATE)!=0u);
    x=y;x.privacy_class=SP("PUBLIC");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PRIVACY)!=0u);
    x=y;x.packet_id=SP("SP08 BAD");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_PACKET)!=0u);
    x=y;x.schema=SP("MISSING");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_SCHEMA)!=0u);
    x=y;x.idempotency_key=SP("0000000000000000000000000000000000000000000000000000000000000000");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_IDEMPOTENCY)!=0u);
    x=y;x.envelope_id=(raf_span_v1){0,32};
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_BAD_POINTER)!=0u);
    x=y;x.operation=SP("EXECUTE");
    VERIFY((raf_queue_preflight_v1(&x)&RAFQ_OPERATION)!=0u);
    VERIFY(raf_queue_transition_v1(RAFQ_IN,RAFQ_GATES));
    VERIFY(raf_queue_transition_v1(RAFQ_IN,RAFQ_HOLD));
    VERIFY(!raf_queue_transition_v1(RAFQ_GATES,RAFQ_IN));
    VERIFY(!raf_queue_transition_v1(RAFQ_GATES,4));
    VERIFY(!raf_queue_transition_v1(RAFQ_HOLD,4));
    VERIFY((raf_queue_preflight_v1(0)&RAFQ_BAD_POINTER)!=0u);
    printf("freestanding_core: %u checks %u failed\n",count,failed);
    return failed ? 1 : 0;
}
