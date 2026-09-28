#ifndef RAF_SILICON_LIGHT_H
#define RAF_SILICON_LIGHT_H

/*
 * Silicon Light Core V1
 *
 * L0 contract:
 * - freestanding C11;
 * - no libc/system headers;
 * - no allocator/heap;
 * - no syscalls/filesystem/network;
 * - no JNI/Android API;
 * - no external runtime symbols.
 */

typedef unsigned char raf_sl_u8;
typedef unsigned short raf_sl_u16;
typedef unsigned int raf_sl_u32;
typedef signed int raf_sl_i32;
typedef unsigned long long raf_sl_u64;
typedef signed long long raf_sl_i64;

_Static_assert(sizeof(raf_sl_u8) == 1, "raf_sl_u8 width");
_Static_assert(sizeof(raf_sl_u16) == 2, "raf_sl_u16 width");
_Static_assert(sizeof(raf_sl_u32) == 4, "raf_sl_u32 width");
_Static_assert(sizeof(raf_sl_u64) == 8, "raf_sl_u64 width");

enum {
    RAF_SL_OK = 0u,
    RAF_SL_ERR_NULL = 1u,
    RAF_SL_ERR_RANGE = 2u
};

typedef struct raf_sl_state {
    raf_sl_u32 lane[8];
    raf_sl_u32 counter;
    raf_sl_u32 tag;
} raf_sl_state;

void raf_sl_zero(void *dst, raf_sl_u32 n);
void raf_sl_copy(void *dst, const void *src, raf_sl_u32 n);
void raf_sl_xor(void *dst, const void *src, raf_sl_u32 n);
raf_sl_u32 raf_sl_equal_ct(const void *a, const void *b, raf_sl_u32 n);
raf_sl_u32 raf_sl_tag32(const void *data, raf_sl_u32 n, raf_sl_u32 seed);
raf_sl_i32 raf_sl_q16_mul(raf_sl_i32 a, raf_sl_i32 b);
raf_sl_u32 raf_sl_state_init(raf_sl_state *s, raf_sl_u32 seed);
raf_sl_u32 raf_sl_state_step(raf_sl_state *s, const void *input, raf_sl_u32 n);

#endif
