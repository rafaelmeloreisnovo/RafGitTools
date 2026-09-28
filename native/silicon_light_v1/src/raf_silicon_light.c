#include "raf_silicon_light.h"

static raf_sl_u32 raf_sl_rotl32(raf_sl_u32 x, raf_sl_u32 r) {
    r &= 31u;
    return (x << r) | (x >> ((32u - r) & 31u));
}

static raf_sl_u32 raf_sl_mix32(raf_sl_u32 x) {
    x ^= x >> 16u;
    x *= 0x7FEB352Du;
    x ^= x >> 15u;
    x *= 0x846CA68Bu;
    x ^= x >> 16u;
    return x;
}

static __attribute__((always_inline)) inline raf_sl_u32
raf_sl_tag32_core(const void *data, raf_sl_u32 n, raf_sl_u32 seed) {
    const raf_sl_u8 *p = (const raf_sl_u8 *)data;
    raf_sl_u32 h = seed ^ 0x811C9DC5u;
    raf_sl_u32 i;

    if (p == (const raf_sl_u8 *)0) {
        return raf_sl_mix32(h ^ 0xA5A5A5A5u);
    }

    for (i = 0u; i < n; ++i) {
        h ^= (raf_sl_u32)p[i];
        h *= 0x01000193u;
    }

    return raf_sl_mix32(h ^ n);
}

void raf_sl_zero(void *dst, raf_sl_u32 n) {
    raf_sl_u8 *d = (raf_sl_u8 *)dst;
    raf_sl_u32 i;

    if (d == (raf_sl_u8 *)0) {
        return;
    }

    for (i = 0u; i < n; ++i) {
        d[i] = 0u;
    }
}

void raf_sl_copy(void *dst, const void *src, raf_sl_u32 n) {
    raf_sl_u8 *d = (raf_sl_u8 *)dst;
    const raf_sl_u8 *s = (const raf_sl_u8 *)src;
    raf_sl_u32 i;

    if (d == (raf_sl_u8 *)0 || s == (const raf_sl_u8 *)0 || d == s) {
        return;
    }

    /*
     * V1 has memcpy semantics: source and destination must not overlap.
     * Avoid relational pointer comparisons between unrelated C objects.
     */
    for (i = 0u; i < n; ++i) {
        d[i] = s[i];
    }
}

void raf_sl_xor(void *dst, const void *src, raf_sl_u32 n) {
    raf_sl_u8 *d = (raf_sl_u8 *)dst;
    const raf_sl_u8 *s = (const raf_sl_u8 *)src;
    raf_sl_u32 i;

    if (d == (raf_sl_u8 *)0 || s == (const raf_sl_u8 *)0) {
        return;
    }

    for (i = 0u; i < n; ++i) {
        d[i] ^= s[i];
    }
}

raf_sl_u32 raf_sl_equal_ct(const void *a, const void *b, raf_sl_u32 n) {
    const raf_sl_u8 *aa = (const raf_sl_u8 *)a;
    const raf_sl_u8 *bb = (const raf_sl_u8 *)b;
    raf_sl_u32 i;
    raf_sl_u32 diff = 0u;

    if (aa == (const raf_sl_u8 *)0 || bb == (const raf_sl_u8 *)0) {
        return 0u;
    }

    for (i = 0u; i < n; ++i) {
        diff |= (raf_sl_u32)(aa[i] ^ bb[i]);
    }

    return (diff == 0u) ? 1u : 0u;
}

raf_sl_u32 raf_sl_tag32(const void *data, raf_sl_u32 n, raf_sl_u32 seed) {
    return raf_sl_tag32_core(data, n, seed);
}

raf_sl_i32 raf_sl_q16_mul(raf_sl_i32 a, raf_sl_i32 b) {
    raf_sl_i64 aa = (raf_sl_i64)a;
    raf_sl_i64 bb = (raf_sl_i64)b;
    raf_sl_u64 ua = (aa < 0) ? (raf_sl_u64)(-aa) : (raf_sl_u64)aa;
    raf_sl_u64 ub = (bb < 0) ? (raf_sl_u64)(-bb) : (raf_sl_u64)bb;
    raf_sl_u64 scaled = (ua * ub) >> 16u;
    raf_sl_u32 negative = ((a < 0) != (b < 0)) ? 1u : 0u;

    if (negative != 0u) {
        if (scaled >= (raf_sl_u64)2147483648u) {
            return (raf_sl_i32)(-2147483647 - 1);
        }
        return -(raf_sl_i32)scaled;
    }

    if (scaled > (raf_sl_u64)2147483647u) {
        return (raf_sl_i32)2147483647;
    }

    return (raf_sl_i32)scaled;
}

raf_sl_u32 raf_sl_state_init(raf_sl_state *s, raf_sl_u32 seed) {
    raf_sl_u32 i;
    raf_sl_u32 x;

    if (s == (raf_sl_state *)0) {
        return RAF_SL_ERR_NULL;
    }

    x = seed ^ 0x534C5631u;
    for (i = 0u; i < 8u; ++i) {
        x = raf_sl_mix32(x + (0x9E3779B9u ^ i));
        s->lane[i] = x;
    }

    s->counter = 0u;
    s->tag = raf_sl_mix32(seed ^ 0xC001D00Du);
    return RAF_SL_OK;
}

raf_sl_u32 raf_sl_state_step(raf_sl_state *s, const void *input, raf_sl_u32 n) {
    raf_sl_u32 i;
    raf_sl_u32 in_tag;
    raf_sl_u32 carry;

    if (s == (raf_sl_state *)0) {
        return RAF_SL_ERR_NULL;
    }
    if (input == (const void *)0 && n != 0u) {
        return RAF_SL_ERR_NULL;
    }
    if (n > 65536u) {
        return RAF_SL_ERR_RANGE;
    }

    in_tag = raf_sl_tag32_core(input, n, s->tag ^ s->counter);
    carry = in_tag;

    for (i = 0u; i < 8u; ++i) {
        raf_sl_u32 x = s->lane[i] ^ carry ^ (0x9E3779B9u + i);
        x = raf_sl_rotl32(raf_sl_mix32(x), (i * 3u) + 5u);
        s->lane[i] = x;
        carry = raf_sl_mix32(carry + x + i);
    }

    s->counter += 1u;
    s->tag = raf_sl_mix32(in_tag ^ carry ^ s->counter);
    return RAF_SL_OK;
}
