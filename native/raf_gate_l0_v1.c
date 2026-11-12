/* SPDX-License-Identifier: GPL-3.0-or-later
 * RAFAELIA L0: synthetic five-boundary gate, not a proof verifier.
 * No headers, allocations, libc, syscalls, runtime introspection or I/O.
 */
#if !defined(__SIZEOF_INT__) || (__SIZEOF_INT__ != 4)
#error "Expected 32-bit unsigned int"
#endif
#if !defined(__CHAR_BIT__) || (__CHAR_BIT__ != 8)
#error "Expected 8-bit char"
#endif

typedef unsigned int raf_gate_word;
typedef struct {
    raf_gate_word source;
    raf_gate_word artifact;
    raf_gate_word execution;
    raf_gate_word evidence;
    raf_gate_word authority;
} raf_gate_input_v1;

typedef char raf_gate_input_size_v1[(sizeof(raf_gate_input_v1) == 20u) ? 1 : -1];

enum {
    RAF_GATE_V1_UNSET = 0u,
    RAF_GATE_V1_VERIFIED_SCOPED = 1u,
    RAF_GATE_V1_FAILED = 2u,
    RAF_GATE_V1_NOT_RUN = 3u,
    RAF_GATE_V1_TOKEN_VAZIO = 4u
};

/* 0=UNSET, 1=VERIFIED_IN_OWN_SCOPE, 2=FAIL, 3=NOT_RUN, 4=TOKEN_VAZIO.
 * Status code 0 is NOT a zero-valued observation; data uses a separate type.
 * Only the value 1 clears its own blocker. This function does NOT
 * authenticate its inputs, inspect hardware or authorize claims.
 * Output 0 means candidate for independent review, not global PASS.
 */
raf_gate_word raf_gate_blockers_v1(const raf_gate_input_v1 *input)
{
    if (input == (const raf_gate_input_v1 *)0) {
        return 0x80000000u;
    }
    return ((raf_gate_word)(input->source != RAF_GATE_V1_VERIFIED_SCOPED) << 0u) |
           ((raf_gate_word)(input->artifact != RAF_GATE_V1_VERIFIED_SCOPED) << 1u) |
           ((raf_gate_word)(input->execution != RAF_GATE_V1_VERIFIED_SCOPED) << 2u) |
           ((raf_gate_word)(input->evidence != RAF_GATE_V1_VERIFIED_SCOPED) << 3u) |
           ((raf_gate_word)(input->authority != RAF_GATE_V1_VERIFIED_SCOPED) << 4u);
}
