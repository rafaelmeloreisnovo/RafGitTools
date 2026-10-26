#ifndef RAFAELIA_SESSION_QUEUE_CORE_V1_H
#define RAFAELIA_SESSION_QUEUE_CORE_V1_H
#include <stdint.h>

/* Input spans are borrowed, contiguous, readable memory owned by caller.
 * Parsing/trust classification is an OUTER boundary, not performed here.
 * No libc, heap, syscalls, globals with mutable state, or secrets.
 */
typedef struct { const uint8_t *bytes; uint32_t size; } raf_span_v1;
typedef struct {
    raf_span_v1 schema, envelope_id, session_id, target_id, packet_id;
    raf_span_v1 mapa_commit, mapa_blob, operation, state, privacy_class;
    raf_span_v1 payload_mode, idempotency_key;
    uint8_t execution_enabled, human_approved, claim_allowed;
    uint8_t boolean_types_verified; /* authenticated parser asserts true JSON booleans */
} raf_queue_intent_v1;

enum {
    RAFQ_SCHEMA = 1u << 0, RAFQ_ID = 1u << 1,
    RAFQ_PACKET = 1u << 2, RAFQ_PIN = 1u << 3,
    RAFQ_OPERATION = 1u << 4, RAFQ_STATE = 1u << 5,
    RAFQ_PRIVACY = 1u << 6, RAFQ_PAYLOAD = 1u << 7,
    RAFQ_PROMOTION = 1u << 8, RAFQ_IDEMPOTENCY = 1u << 9,
    RAFQ_PARSER_TYPE = 1u << 10, RAFQ_BAD_POINTER = 1u << 11
};
enum { RAFQ_IN = 1, RAFQ_GATES = 2, RAFQ_HOLD = 3 };

/* 0 => structural preflight ONLY, never authorization/dispatch. */
uint32_t raf_queue_preflight_v1(const raf_queue_intent_v1 *input);
int raf_queue_transition_v1(uint32_t before, uint32_t after);
#endif
