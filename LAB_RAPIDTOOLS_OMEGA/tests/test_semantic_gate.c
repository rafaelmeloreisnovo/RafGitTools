#include "raf_semantic_gate.h"
#define CHECK(x) do { if (!(x)) return __LINE__ & 255; } while (0)
#define TEST_LITERAL(s,tag) CHECK(raf_gate_classify((const raf_gate_u8 *)(s), (raf_gate_u32)(sizeof(s)-1u), RAF_GATE_BYTES, 0u) == (tag))
int main(void) {
    static const raf_gate_u8 nil_byte[1] = {0u};
    static const raf_gate_u8 buffer[1] = {'x'};
    raf_gate_u32 i;
    CHECK(raf_gate_classify(0, 0u, RAF_GATE_NULL, 0u) == RAF_GATE_TYPED_NULL);
    CHECK(raf_gate_classify(buffer, 0u, RAF_GATE_BYTES, 0u) == RAF_GATE_EMPTY_BYTES);
    CHECK(raf_gate_classify(0, 0u, RAF_GATE_INTEGER, 0u) == RAF_GATE_INTEGER_ZERO);
    CHECK(raf_gate_classify(0, 0u, RAF_GATE_INTEGER, 1u) == RAF_GATE_INTEGER_NONZERO);
    TEST_LITERAL("0", RAF_GATE_TEXT_ZERO);
    TEST_LITERAL("    ", RAF_GATE_FOUR_SPACES);
    TEST_LITERAL(" ", RAF_GATE_OTHER_WHITESPACE);
    TEST_LITERAL("\t\n", RAF_GATE_OTHER_WHITESPACE);
    CHECK(raf_gate_classify(nil_byte, 1u, RAF_GATE_BYTES, 0u) == RAF_GATE_RAW_NUL_BYTE);
    TEST_LITERAL("molho de tomate", RAF_GATE_TOMATO_LITERAL);
    TEST_LITERAL("molho de chaves", RAF_GATE_KEYS_LITERAL);
    TEST_LITERAL("molho", RAF_GATE_ABSTAIN);
    TEST_LITERAL("molho de lenha", RAF_GATE_ABSTAIN);
    TEST_LITERAL("molho de chaves no sofa-cama", RAF_GATE_ABSTAIN);
    TEST_LITERAL("Molho de tomate", RAF_GATE_ABSTAIN);
    TEST_LITERAL("0x00", RAF_GATE_ABSTAIN);
    TEST_LITERAL("\xE2\x88\x85", RAF_GATE_ABSTAIN); /* UTF-8 EMPTY SET is text, not null */
    TEST_LITERAL("c|ss|\xC3\xA7|z|sh|x", RAF_GATE_ABSTAIN);
    CHECK(raf_gate_classify(0, 0u, RAF_GATE_BYTES, 0u) == RAF_GATE_INVALID);
    CHECK(raf_gate_classify(0, 1u, RAF_GATE_BYTES, 0u) == RAF_GATE_INVALID);
    CHECK(raf_gate_classify(buffer, 4097u, RAF_GATE_BYTES, 0u) == RAF_GATE_INVALID);
    CHECK(raf_gate_classify(buffer, 1u, RAF_GATE_BYTES, 1u) == RAF_GATE_INVALID);
    CHECK(raf_gate_classify(buffer, 1u, RAF_GATE_NULL, 0u) == RAF_GATE_INVALID);
    CHECK(raf_gate_classify(buffer, 1u, RAF_GATE_INTEGER, 0u) == RAF_GATE_INVALID);
    CHECK(raf_gate_classify(0, 0u, 0u, 0u) == RAF_GATE_INVALID);
    CHECK(raf_gate_classify(0, 0u, RAF_GATE_NULL, 1u) == RAF_GATE_INVALID);
    /* Single-byte coverage detects accidental broad acceptance and prefix aliases. */
    for (i = 1u; i < 256u; ++i) {
        raf_gate_u8 b = (raf_gate_u8)i;
        raf_gate_u32 label = raf_gate_classify(&b, 1u, RAF_GATE_BYTES, 0u);
        if (b == (raf_gate_u8)'0') CHECK(label == RAF_GATE_TEXT_ZERO);
        else if (b == (raf_gate_u8)' ' || b == 9u || b == 10u || b == 13u)
            CHECK(label == RAF_GATE_OTHER_WHITESPACE);
        else CHECK(label == RAF_GATE_ABSTAIN);
        CHECK(b == (raf_gate_u8)i); /* input must be immutable */
    }
    return 0;
}
