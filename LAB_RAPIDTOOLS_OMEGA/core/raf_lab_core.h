#ifndef RAF_LAB_CORE_H
#define RAF_LAB_CORE_H
/* Pure freestanding contract. No libc, malloc, pthread, SDK, NDK or I/O. */
typedef unsigned char raf_u8;
typedef unsigned int raf_u32;
typedef unsigned long long raf_u64;
enum raf_mode { RAF_STREAM_WRITE=1, RAF_CACHE_REUSE=2, RAF_COMPUTE_ONLY=3,
                RAF_STRIDE_READ=4, RAF_Q16_GEOMETRY=5 };
#define RAF_Q16_SQRT3_OVER2 56756u
#define RAF_Q16_PHI 107375u
#define RAF_Q16_PI 205887u
#define RAF_DEFAULT_STRIDE 4093u
void raf_lab_init(raf_u8 *data, raf_u64 bytes);
raf_u8 raf_lab_seed(raf_u64 index);
raf_u64 raf_lab_partition(raf_u8 *data, raf_u64 bytes,
    raf_u64 first, raf_u64 last, raf_u32 rounds, raf_u32 mode, raf_u32 stride);
raf_u64 raf_lab_digest(const raf_u8 *data, raf_u64 bytes);
raf_u64 raf_lab_reference(raf_u64 bytes, raf_u32 rounds, raf_u32 mode, raf_u32 stride);
raf_u32 raf_lab_rle_pack(const raf_u8 *src, raf_u32 n, raf_u8 *dst, raf_u32 capacity);
raf_u32 raf_lab_rle_unpack(const raf_u8 *src, raf_u32 n, raf_u8 *dst, raf_u32 capacity);
#endif
