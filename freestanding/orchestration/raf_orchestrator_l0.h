#ifndef RAF_ORCHESTRATOR_L0_H
#define RAF_ORCHESTRATOR_L0_H

/*
 * RAFAELIA L0 orchestration contract.
 *
 * Runtime dependencies: NONE.
 * No libc, allocator, syscall, JNI, JVM, NDK, POSIX, filesystem, threads,
 * atomics, exceptions, RTTI, or external library is required by this core.
 * All state and memory are caller-owned.
 */

typedef unsigned int raf_u32;
typedef unsigned long long raf_u64;
typedef unsigned long raf_usize;

#define RAF_STATE_CONTINUE      0u
#define RAF_STATE_COMPLETED     1u
#define RAF_STATE_TOKEN_VAZIO   2u
#define RAF_STATE_BLOCKED       3u
#define RAF_STATE_FAILED        4u

#define RAF_FLAG_SOURCE_BOUND      (1u << 0)
#define RAF_FLAG_AUTHORITY_BOUND   (1u << 1)
#define RAF_FLAG_FAIL_CLOSED       (1u << 2)
#define RAF_FLAG_LOW_ALLOCATION    (1u << 3)
#define RAF_FLAG_SHADOW_GUARD      (1u << 4)

#define RAF_WARN_TOKEN_VAZIO       (1u << 0)
#define RAF_WARN_MODULE_UNAVAILABLE (1u << 1)
#define RAF_WARN_AUTHORITY_MISSING (1u << 2)
#define RAF_WARN_EVIDENCE_MISSING  (1u << 3)
#define RAF_WARN_ABI_MISMATCH      (1u << 4)
#define RAF_WARN_HOSTED_BOUNDARY   (1u << 5)
#define RAF_WARN_INCOHERENT_FLAGS  (1u << 6)
#define RAF_WARN_INVALID_PLAN      (1u << 7)
#define RAF_WARN_INVALID_STATE     (1u << 8)

#define RAF_MODULE_ROUTE            1u
#define RAF_MODULE_POLIMATA_L0      2u
#define RAF_MODULE_AUDIO_DSP        3u
#define RAF_MODULE_CRYPTO_RMR       4u
#define RAF_MODULE_VM_KERNEL        5u
#define RAF_MODULE_PLATFORM_RUNTIME 6u

#define RAF_BOUNDARY_FREESTANDING_CORE 1u
#define RAF_BOUNDARY_HOSTED_ADAPTER    2u
#define RAF_BOUNDARY_PLATFORM_GATE     3u

#define RAF_MAX_MODULE_ID 63u

struct raf_context {
    unsigned char *payload;
    raf_usize payload_size;
    raf_u32 flags;
    raf_u32 warning_mask;
    raf_u32 executed_stages;
    raf_u32 last_module_code;
    void *caller_state;
};

typedef raf_u32 (*raf_stage_fn)(struct raf_context *context, void *stage_state);

struct raf_stage {
    raf_u32 module_id;
    raf_u32 module_kind;
    raf_u32 boundary;
    raf_u32 required_flags;
    raf_stage_fn execute;
    void *stage_state;
};

struct raf_receipt {
    raf_u32 state;
    raf_u32 flags;
    raf_u32 warning_mask;
    raf_u32 executed_stages;
    raf_u32 last_module_code;
    raf_u32 claim_allowed;
};

/* Returns RAF_STATE_COMPLETED for a valid plan, RAF_STATE_BLOCKED otherwise. */
raf_u32 raf_orchestrator_validate(
    const struct raf_stage *stages,
    raf_usize stage_count,
    raf_u32 *warning_mask
);

/* Iterative, allocation-free, fail-closed orchestration. */
struct raf_receipt raf_orchestrator_run(
    const struct raf_stage *stages,
    raf_usize stage_count,
    struct raf_context *context
);

#endif
