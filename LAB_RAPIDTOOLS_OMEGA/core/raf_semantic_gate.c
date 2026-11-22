#include "raf_semantic_gate.h"

/* Exact ASCII comparison, bounded by n; no normalization or implicit prefix match. */
static raf_gate_u32 raf_gate_match(const raf_gate_u8 *p, raf_gate_u32 n,
                                  const char *literal, raf_gate_u32 literal_n) {
    raf_gate_u32 i;
    if (n != literal_n) return 0u;
    for (i = 0u; i < n; ++i)
        if (p[i] != (raf_gate_u8)literal[i]) return 0u;
    return 1u;
}

raf_gate_u32 raf_gate_classify(const raf_gate_u8 *p, raf_gate_u32 n,
                              raf_gate_u32 kind, raf_gate_u32 integer_value) {
    raf_gate_u32 i, whitespace = 1u, four_spaces = (n == 4u);
    if (kind == RAF_GATE_NULL)
        return (p == 0 && n == 0u && integer_value == 0u)
               ? RAF_GATE_TYPED_NULL : RAF_GATE_INVALID;
    if (kind == RAF_GATE_INTEGER)
        return (p == 0 && n == 0u) ?
               (integer_value == 0u ? RAF_GATE_INTEGER_ZERO : RAF_GATE_INTEGER_NONZERO)
               : RAF_GATE_INVALID;
    if (kind != RAF_GATE_BYTES || p == 0 || n > RAF_GATE_MAX_BYTES || integer_value != 0u)
        return RAF_GATE_INVALID;
    if (n == 0u) return RAF_GATE_EMPTY_BYTES;
    if (n == 1u && p[0] == 0u) return RAF_GATE_RAW_NUL_BYTE;
    if (n == 1u && p[0] == (raf_gate_u8)'0') return RAF_GATE_TEXT_ZERO;
    for (i = 0u; i < n; ++i) {
        raf_gate_u8 v = p[i];
        if (v != (raf_gate_u8)' ') four_spaces = 0u;
        if (v != (raf_gate_u8)' ' && v != 9u && v != 10u && v != 13u)
            whitespace = 0u;
    }
    if (four_spaces) return RAF_GATE_FOUR_SPACES;
    if (whitespace) return RAF_GATE_OTHER_WHITESPACE;
    if (raf_gate_match(p, n, "molho de tomate", sizeof("molho de tomate") - 1u))
        return RAF_GATE_TOMATO_LITERAL;
    if (raf_gate_match(p, n, "molho de chaves", sizeof("molho de chaves") - 1u))
        return RAF_GATE_KEYS_LITERAL;
    return RAF_GATE_ABSTAIN;
}
