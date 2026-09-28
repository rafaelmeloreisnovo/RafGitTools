#include "raf_silicon_light.h"
#include <stdio.h>

static int fail(const char *message) {
    puts(message);
    return 1;
}

int main(void) {
    raf_sl_state a;
    raf_sl_state b;
    unsigned char x[8] = {1u, 2u, 3u, 4u, 5u, 6u, 7u, 8u};
    unsigned char y[8] = {0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u};

    if (raf_sl_state_init(&a, 42u) != RAF_SL_OK) {
        return fail("init a");
    }
    if (raf_sl_state_init(&b, 42u) != RAF_SL_OK) {
        return fail("init b");
    }
    if (raf_sl_state_step(&a, x, 8u) != RAF_SL_OK) {
        return fail("step a");
    }
    if (raf_sl_state_step(&b, x, 8u) != RAF_SL_OK) {
        return fail("step b");
    }
    if (raf_sl_equal_ct(&a, &b, (raf_sl_u32)sizeof(a)) == 0u) {
        return fail("determinism");
    }

    raf_sl_copy(y, x, 8u);
    if (raf_sl_equal_ct(x, y, 8u) == 0u) {
        return fail("copy");
    }

    raf_sl_xor(y, x, 8u);
    raf_sl_zero(x, 8u);
    if (raf_sl_equal_ct(x, y, 8u) == 0u) {
        return fail("xor/zero");
    }

    if (raf_sl_q16_mul(65536, 32768) != 32768) {
        return fail("q16 positive");
    }
    if (raf_sl_q16_mul(-65536, 32768) != -32768) {
        return fail("q16 negative");
    }
    if (raf_sl_q16_mul(2147483647, 131072) != 2147483647) {
        return fail("q16 saturation");
    }

    puts("SILICON_LIGHT_SELFTEST=PASS");
    return 0;
}
