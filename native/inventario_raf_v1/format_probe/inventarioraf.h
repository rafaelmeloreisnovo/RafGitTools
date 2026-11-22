/* SPDX-License-Identifier: GPL-3.0-or-later
 * inventarioRAF L0 — newly authored scoped adapter in RafGitTools.
 * No runtime libc, heap, syscalls, JNI, JVM, Android headers or external globals.
 * The caller owns memory, transport, clocks, scheduling and ACL.
 */
#ifndef INVENTARIORAF_L0_H
#define INVENTARIORAF_L0_H

typedef __UINT8_TYPE__ ir_u8;
typedef __UINT16_TYPE__ ir_u16;
typedef __UINT32_TYPE__ ir_u32;
typedef __UINT64_TYPE__ ir_u64;
typedef __SIZE_TYPE__ ir_size;

#define IR_FORMAT_UNKNOWN 0u
#define IR_FORMAT_ELF     1u
#define IR_FORMAT_DEX     2u
#define IR_FORMAT_ZIP     3u
#define IR_FORMAT_ART     4u

#define IR_OK             0u
#define IR_BLOCKED        1u
#define IR_TOKEN_VAZIO    2u
#define IR_UNSUPPORTED    3u
#define IR_BUDGET         4u

#define IR_BACKEND_AUTO   0u
#define IR_BACKEND_SCALAR 1u
#define IR_BACKEND_NEON   2u
#define IR_BACKEND_GPU    3u
#define IR_BACKEND_NUMA   4u

#define IR_CAP_SCALAR (1u << 0)
#define IR_CAP_NEON   (1u << 1)
#define IR_CAP_SIMD   (1u << 2)
#define IR_CAP_GPU    (1u << 3)
#define IR_CAP_MULTI  (1u << 4)
#define IR_CAP_NUMA   (1u << 5)

#define IR_WARN_FALLBACK       (1u << 0)
#define IR_WARN_PARTIAL_FORMAT (1u << 1)
#define IR_WARN_HOST_REQUIRED  (1u << 2)
#define IR_WARN_UNKNOWN_ARCH   (1u << 3)

#define IR_MAX_BYTES (1u << 20)
#define IR_MAX_MEMBERS 128u

struct ir_request {
    const ir_u8 *data;
    ir_size size;
    ir_u32 authorized_source; /* 1 = caller declares access allowed */
    ir_u32 commit;            /* 1 = advance checkpoint, 0 = dry run */
    ir_u32 backend;           /* caller request; only SCALAR is implemented */
    ir_u32 advertised_caps;   /* metadata, NOT evidence of benchmark */
    ir_u32 verified_caps;     /* verified by separate host probe + falsifier */
    ir_u64 watchdog_fuel;     /* logical operations limit, not wall time */
    ir_u64 watchdog_bytes;    /* independent processed byte limit */
    ir_u64 expected_generation; /* optimistic version check */
};

struct ir_state {
    ir_u64 generation;
    ir_u64 last_digest_fnv64; /* identifier only; NOT cryptographic */
    ir_u32 last_format;
    ir_u32 last_entries;
};

struct ir_receipt {
    ir_u32 status;
    ir_u32 format;
    ir_u32 warnings;
    ir_u32 backend_used;
    ir_u32 entries_observed;
    ir_u32 arch_machine;
    ir_u32 format_class;
    ir_u32 committed;
    ir_u64 bytes_seen;
    ir_u64 digest_fnv64;
    ir_u64 generation;
    ir_u32 claim_allowed;  /* ALWAYS FALSE */
    ir_u32 full_inventory; /* ALWAYS FALSE in L0 sniffing */
};

/* Only exported processing entry; no hidden network/device behavior.
 * On any non-OK result the state is unchanged (transactional rollback).
 */
ir_u32 inventarioraf_step(struct ir_state *state,
                          const struct ir_request *request,
                          struct ir_receipt *receipt);
#endif
