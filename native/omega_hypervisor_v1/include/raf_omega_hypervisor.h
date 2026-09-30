#ifndef RAF_OMEGA_HYPERVISOR_V1_H
#define RAF_OMEGA_HYPERVISOR_V1_H

/*
 * RAFAELIA Omega Hypervisor V1
 *
 * User-directed clean implementation scaffold.
 * No inherited Vectra/PCR code is imported here.
 *
 * L0 boundary:
 * - freestanding C11;
 * - no system/libc headers;
 * - no heap;
 * - no syscalls/filesystem/network;
 * - no floating point;
 * - no external runtime symbols.
 *
 * This engine implements custody/state transitions only.
 * It does not virtualize hardware and it does not prove authorship,
 * physical runtime, scientific claims, or provider state.
 */

typedef unsigned int raf_oh_u32;

_Static_assert(sizeof(raf_oh_u32) == 4, "raf_oh_u32 width");

enum raf_oh_state {
    RAF_OH_C01_INTENT = 1u,
    RAF_OH_C02_AUTHORSHIP = 2u,
    RAF_OH_C03_GAPS = 3u,
    RAF_OH_C04_MOUNT = 4u,
    RAF_OH_C05_EXECUTE = 5u,
    RAF_OH_C06_VERIFY = 6u,
    RAF_OH_C07_CUSTODY = 7u,
    RAF_OH_C08_OMEGA = 8u
};

enum raf_oh_result {
    RAF_OH_OK = 0u,
    RAF_OH_ERR_NULL = 1u,
    RAF_OH_ERR_STATE = 2u,
    RAF_OH_ERR_TARGET = 3u,
    RAF_OH_ERR_GATE = 4u
};

enum raf_oh_gate {
    RAF_OH_G_INTENT               = 1u << 0,
    RAF_OH_G_SOURCE_IDENTITY      = 1u << 1,
    RAF_OH_G_AUTHORSHIP_BOUNDARY  = 1u << 2,
    RAF_OH_G_GAP_REGISTER         = 1u << 3,
    RAF_OH_G_RISK_REVIEW          = 1u << 4,
    RAF_OH_G_AUTHORITY            = 1u << 5,
    RAF_OH_G_MOUNT_POLICY         = 1u << 6,
    RAF_OH_G_RELATION_TYPE        = 1u << 7,
    RAF_OH_G_EXECUTION_TARGET     = 1u << 8,
    RAF_OH_G_ROLLBACK             = 1u << 9,
    RAF_OH_G_INPUT_IDENTITY       = 1u << 10,
    RAF_OH_G_FALSIFIER            = 1u << 11,
    RAF_OH_G_GATE_RESULT          = 1u << 12,
    RAF_OH_G_RECEIPT              = 1u << 13,
    RAF_OH_G_HASH_REF             = 1u << 14,
    RAF_OH_G_EVIDENCE_REF         = 1u << 15,
    RAF_OH_G_INTEGRITY            = 1u << 16,
    RAF_OH_G_PROVENANCE           = 1u << 17,
    RAF_OH_G_SEMANTIC_CONSISTENCY = 1u << 18,
    RAF_OH_G_REPOSITORY_HEADS     = 1u << 19,
    RAF_OH_G_PROVIDER_REVISION    = 1u << 20,
    RAF_OH_G_HUMAN_CHECKPOINT     = 1u << 21
};

enum raf_oh_context_flag {
    RAF_OH_F_PROVIDER_USED = 1u << 0,
    RAF_OH_F_PROMOTION_REQUESTED = 1u << 1
};

typedef struct raf_oh_ctx {
    raf_oh_u32 state;
    raf_oh_u32 gates;
    raf_oh_u32 flags;
    raf_oh_u32 cycle;
    raf_oh_u32 transition_count;
    raf_oh_u32 last_missing_mask;
} raf_oh_ctx;

raf_oh_u32 raf_oh_init(raf_oh_ctx *ctx);
raf_oh_u32 raf_oh_required_mask(raf_oh_u32 state, raf_oh_u32 flags);
raf_oh_u32 raf_oh_next_state(raf_oh_u32 state);
raf_oh_u32 raf_oh_transition(raf_oh_ctx *ctx, raf_oh_u32 target_state);
raf_oh_u32 raf_oh_add_gates(raf_oh_ctx *ctx, raf_oh_u32 gate_mask);
raf_oh_u32 raf_oh_set_flags(raf_oh_ctx *ctx, raf_oh_u32 flags);

#endif
