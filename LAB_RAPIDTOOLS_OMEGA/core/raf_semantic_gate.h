#ifndef RAF_SEMANTIC_GATE_H
#define RAF_SEMANTIC_GATE_H
/* L0: no platform headers, allocator, libc, syscalls, JNI or Android runtime. */
typedef unsigned char raf_gate_u8;
typedef unsigned int raf_gate_u32;
enum raf_gate_kind {
    RAF_GATE_BYTES = 1u,
    RAF_GATE_INTEGER = 2u,
    RAF_GATE_NULL = 3u
};
enum raf_gate_result {
    RAF_GATE_INVALID = 1u,
    RAF_GATE_TYPED_NULL = 2u,
    RAF_GATE_EMPTY_BYTES = 3u,
    RAF_GATE_FOUR_SPACES = 4u,
    RAF_GATE_OTHER_WHITESPACE = 5u,
    RAF_GATE_INTEGER_ZERO = 6u,
    RAF_GATE_INTEGER_NONZERO = 7u,
    RAF_GATE_TEXT_ZERO = 8u,
    RAF_GATE_RAW_NUL_BYTE = 9u,
    RAF_GATE_TOMATO_LITERAL = 10u,
    RAF_GATE_KEYS_LITERAL = 11u,
    RAF_GATE_ABSTAIN = 12u
};
#define RAF_GATE_MAX_BYTES 4096u
/* Results are byte-level syntactic labels, NEVER phonetic, semantic or provenance proof.
   Caller retains original input, length and kind for reconstruction. */
raf_gate_u32 raf_gate_classify(const raf_gate_u8 *p, raf_gate_u32 n,
                              raf_gate_u32 kind, raf_gate_u32 integer_value);
#endif
