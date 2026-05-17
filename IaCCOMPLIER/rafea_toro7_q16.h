#ifndef RAFEAT7_Q16_H
#define RAFEAT7_Q16_H

#include <stdint.h>
#include <stddef.h>

#define RAFEAT7_Q_ONE 65536u
#define RAFEAT7_ALPHA_SHIFT 2u
#define RAFEAT7_DIM 7u
#define RAFEAT7_ATTRACTORS 42u
#define RAFEAT7_STEP_R 5u
#define RAFEAT7_STEP_C 11u
#define RAFEAT7_CELLS 42u
#define RAFEAT7_VOID_ATTRACTOR 22u
#define RAFEAT7_PHI64 0x100000001B3ull
#define RAFEAT7_ROOT3_OVER2_Q16 56756u
#define RAFEAT7_PI_SIN279_ABS_Q16 64777u
#define RAFEAT7_GOLDEN_Q16 106039u
#define RAFEAT7_OK 0u
#define RAFEAT7_ERR_ORDER 1u
#define RAFEAT7_ERR_VOID 2u

#define RAFEAT7_MIX64(h, b) ((((uint64_t)(h)) ^ ((uint64_t)(uint8_t)(b))) * RAFEAT7_PHI64)
#define RAFEAT7_QMUL(a, b) ((uint32_t)((((uint64_t)(uint32_t)(a)) * ((uint64_t)(uint32_t)(b))) >> 16))
#define RAFEAT7_WRAP_Q16(x) ((uint32_t)(x) & 0xffffu)
#define RAFEAT7_AVG4(oldv, inv) ((uint32_t)(oldv) + ((((uint32_t)(inv)) - ((uint32_t)(oldv))) >> RAFEAT7_ALPHA_SHIFT))
#define RAFEAT7_PHI_CH(cq, hq) RAFEAT7_QMUL((uint32_t)(cq), (uint32_t)(RAFEAT7_Q_ONE - ((uint32_t)(hq) & 0xffffu)))
#define RAFEAT7_OPEN_NEXT(mask, idx) ((((uint32_t)(mask)) & ((1u << (idx)) - 1u)) == ((1u << (idx)) - 1u))
#define RAFEAT7_OPEN(mask, idx) ((uint32_t)(mask) | (uint32_t)(1u << (idx)))

#define RAFEAT7_GCD_PROOF_TEXT "gcd(5,6)=1; gcd(11,7)=1; 6*7=42; traversal period is 42."
#define RAFEAT7_FALSIFY_TEXT "Falsify if any step repeats before 42 cells, if lane order skips a prior lane, or if attractor count differs from 42."

#define RAFEAT7_ATTRACTOR_TABLE(X) \
    X(0, 0x0000u, 0x0000u, 0x0000u, 0x0000u, 0x0000u, 0x0000u, 0x0000u) \
    X(1, 0x0618u, 0x0a3du, 0x0e62u, 0x1287u, 0x16acu, 0x1ad1u, 0x1ef6u) \
    X(2, 0x0c30u, 0x147au, 0x1cc4u, 0x250eu, 0x2d58u, 0x35a2u, 0x3decu) \
    X(3, 0x1248u, 0x1eb7u, 0x2b26u, 0x3795u, 0x4404u, 0x5073u, 0x5ce2u) \
    X(4, 0x1860u, 0x28f4u, 0x3988u, 0x4a1cu, 0x5ab0u, 0x6b44u, 0x7bd8u) \
    X(5, 0x1e78u, 0x3331u, 0x47eau, 0x5ca3u, 0x715cu, 0x8615u, 0x9aceu) \
    X(6, 0x2490u, 0x3d6eu, 0x564cu, 0x6f2au, 0x8808u, 0xa0e6u, 0xb9c4u) \
    X(7, 0x2aa8u, 0x47abu, 0x64aeu, 0x81b1u, 0x9eb4u, 0xbbb7u, 0xd8bau) \
    X(8, 0x30c0u, 0x51e8u, 0x7310u, 0x9438u, 0xb560u, 0xd688u, 0xf7b0u) \
    X(9, 0x36d8u, 0x5c25u, 0x8172u, 0xa6bfu, 0xcc0cu, 0xf159u, 0x16a6u) \
    X(10, 0x3cf0u, 0x6662u, 0x8fd4u, 0xb946u, 0xe2b8u, 0x0c2au, 0x359cu) \
    X(11, 0x4308u, 0x709fu, 0x9e36u, 0xcbd3u, 0xf970u, 0x270du, 0x54aau) \
    X(12, 0x4920u, 0x7adcu, 0xac98u, 0xde5au, 0x101cu, 0x41deu, 0x73a0u) \
    X(13, 0x4f38u, 0x8519u, 0xbafau, 0xf0e1u, 0x26c8u, 0x5cafu, 0x9296u) \
    X(14, 0x5550u, 0x8f56u, 0xc95cu, 0x0368u, 0x3d74u, 0x7780u, 0xb18cu) \
    X(15, 0x5b68u, 0x9993u, 0xd7beu, 0x15efu, 0x5420u, 0x9251u, 0xd082u) \
    X(16, 0x6180u, 0xa3d0u, 0xe620u, 0x2876u, 0x6accu, 0xad22u, 0xef78u) \
    X(17, 0x6798u, 0xae0du, 0xf482u, 0x3afdu, 0x8178u, 0xc7f3u, 0x0e6eu) \
    X(18, 0x6db0u, 0xb84au, 0x02e4u, 0x4d84u, 0x9824u, 0xe2c4u, 0x2d64u) \
    X(19, 0x73c8u, 0xc287u, 0x1146u, 0x600bu, 0xaed0u, 0xfd95u, 0x4c5au) \
    X(20, 0x79e0u, 0xccc4u, 0x1fa8u, 0x7292u, 0xc57cu, 0x1866u, 0x6b50u) \
    X(21, 0x7ff8u, 0xd701u, 0x2e0au, 0x8519u, 0xdc28u, 0x3337u, 0x8a46u) \
    X(22, 0x8610u, 0xe13eu, 0x3c6cu, 0x97a0u, 0xf2d4u, 0x4e08u, 0xa93cu) \
    X(23, 0x8c28u, 0xeb7bu, 0x4aceu, 0xaa27u, 0x0980u, 0x68d9u, 0xc832u) \
    X(24, 0x9240u, 0xf5b8u, 0x5930u, 0xbcaeu, 0x202cu, 0x83aau, 0xe728u) \
    X(25, 0x9858u, 0xfff5u, 0x6792u, 0xcf35u, 0x36d8u, 0x9e7bu, 0x061eu) \
    X(26, 0x9e70u, 0x0a32u, 0x75f4u, 0xe1bcu, 0x4d84u, 0xb94cu, 0x2514u) \
    X(27, 0xa488u, 0x146fu, 0x8456u, 0xf443u, 0x6430u, 0xd41du, 0x440au) \
    X(28, 0xaaa0u, 0x1eacu, 0x92b8u, 0x06cau, 0x7adcu, 0xeee8u, 0x6300u) \
    X(29, 0xb0b8u, 0x28e9u, 0xa11au, 0x1951u, 0x9188u, 0x09bfu, 0x81f6u) \
    X(30, 0xb6d0u, 0x3326u, 0xaf7cu, 0x2bd8u, 0xa834u, 0x2490u, 0xa0ecu) \
    X(31, 0xbce8u, 0x3d63u, 0xbddeu, 0x3e5fu, 0xbee0u, 0x3f61u, 0xbfe2u) \
    X(32, 0xc300u, 0x47a0u, 0xcc40u, 0x50e6u, 0xd58cu, 0x5a32u, 0xded8u) \
    X(33, 0xc918u, 0x51ddu, 0xdaa2u, 0x636du, 0xec38u, 0x7503u, 0xfdd2u) \
    X(34, 0xcf30u, 0x5c1au, 0xe904u, 0x75f4u, 0x02e4u, 0x8fd4u, 0x1cc8u) \
    X(35, 0xd548u, 0x6657u, 0xf766u, 0x887bu, 0x1990u, 0xaaadu, 0x3bbeu) \
    X(36, 0xdb60u, 0x7094u, 0x05c8u, 0x9b02u, 0x303cu, 0xc57eu, 0x5ab4u) \
    X(37, 0xe178u, 0x7ad1u, 0x142au, 0xad89u, 0x46e8u, 0xe04fu, 0x79aau) \
    X(38, 0xe790u, 0x850eu, 0x228cu, 0xc010u, 0x5d94u, 0xfb20u, 0x98a0u) \
    X(39, 0xeda8u, 0x8f4bu, 0x30eeu, 0xd297u, 0x7440u, 0x15f1u, 0xb796u) \
    X(40, 0xf3c0u, 0x9988u, 0x3f50u, 0xe51eu, 0x8aecu, 0x30c2u, 0xd68cu) \
    X(41, 0xf9d8u, 0xa3c5u, 0x4db2u, 0xf7a5u, 0xa198u, 0x4b93u, 0xf582u)

#define RAFEAT7_ROW(i,a,b,c,d,e,f,g) { (uint16_t)(a), (uint16_t)(b), (uint16_t)(c), (uint16_t)(d), (uint16_t)(e), (uint16_t)(f), (uint16_t)(g) },
#define RAFEAT7_DECLARE_ATTRACTORS(name) static const uint16_t name[RAFEAT7_ATTRACTORS][RAFEAT7_DIM] = { RAFEAT7_ATTRACTOR_TABLE(RAFEAT7_ROW) }
#define RAFEAT7_IDX(r, c) ((uint32_t)(((uint32_t)(r) * 7u) + (uint32_t)(c)))
#define RAFEAT7_NEXT_R(r) ((uint32_t)(((uint32_t)(r) + RAFEAT7_STEP_R) % 6u))
#define RAFEAT7_NEXT_C(c) ((uint32_t)(((uint32_t)(c) + RAFEAT7_STEP_C) % 7u))
#define RAFEAT7_VOID_FLAG(i) ((uint32_t)(((uint32_t)(i) == RAFEAT7_VOID_ATTRACTOR) ? RAFEAT7_ERR_VOID : RAFEAT7_OK))

#define RAFEAT7_SEED(h, p, n, out7) do { \
    const uint8_t *r0_ = (const uint8_t *)(p); \
    size_t r1_ = (size_t)(n); \
    uint64_t r2_ = (uint64_t)(h); \
    size_t r3_ = 0u; \
    uint32_t r4_ = 0u; \
    uint32_t r5_[8] = { 0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u }; \
    uint32_t r6_ = 0u; \
    uint32_t r7_ = 0u; \
    uint32_t r8_ = 0u; \
    for (r3_ = 0u; r3_ < r1_; ++r3_) { \
        r4_ = (uint32_t)r0_[r3_]; \
        r2_ = RAFEAT7_MIX64(r2_, r4_); \
        r5_[r4_ >> 5] |= (uint32_t)(1u << (r4_ & 31u)); \
        r6_ += (uint32_t)((r3_ != 0u) & (r0_[r3_] != r0_[r3_ - 1u])); \
    } \
    for (r3_ = 0u; r3_ < 8u; ++r3_) { \
        r8_ = r5_[r3_]; \
        r8_ = r8_ - ((r8_ >> 1) & 0x55555555u); \
        r8_ = (r8_ & 0x33333333u) + ((r8_ >> 2) & 0x33333333u); \
        r7_ += (((r8_ + (r8_ >> 4)) & 0x0f0f0f0fu) * 0x01010101u) >> 24; \
    } \
    (out7)[0] = RAFEAT7_WRAP_Q16((uint32_t)r2_); \
    (out7)[1] = RAFEAT7_WRAP_Q16((uint32_t)(r2_ >> 16)); \
    (out7)[2] = RAFEAT7_WRAP_Q16((uint32_t)(r2_ >> 32)); \
    (out7)[3] = RAFEAT7_WRAP_Q16((uint32_t)(r2_ >> 48)); \
    (out7)[4] = RAFEAT7_WRAP_Q16((uint32_t)((r7_ * 6000u) / 256u)); \
    (out7)[5] = RAFEAT7_WRAP_Q16((uint32_t)((r1_ > 1u) ? ((r6_ * 2000u) / (uint32_t)(r1_ - 1u)) : 0u)); \
    (out7)[6] = RAFEAT7_WRAP_Q16((out7)[4] + (out7)[5]); \
} while (0)

#define RAFEAT7_OPEN7(in7, c_in, h_in, out7, err_out) do { \
    uint32_t rf_o0_ = 0u; \
    uint32_t rf_o1_ = RAFEAT7_OK; \
    uint32_t rf_o2_ = 0u; \
    if (RAFEAT7_OPEN_NEXT(rf_o0_, 0u)) { (out7)[0] = RAFEAT7_WRAP_Q16((in7)[0]); rf_o0_ = RAFEAT7_OPEN(rf_o0_, 0u); } else { rf_o1_ |= RAFEAT7_ERR_ORDER; } \
    if (RAFEAT7_OPEN_NEXT(rf_o0_, 1u)) { (out7)[1] = RAFEAT7_WRAP_Q16((in7)[1] + RAFEAT7_QMUL((c_in), RAFEAT7_GOLDEN_Q16)); rf_o0_ = RAFEAT7_OPEN(rf_o0_, 1u); } else { rf_o1_ |= RAFEAT7_ERR_ORDER; } \
    if (RAFEAT7_OPEN_NEXT(rf_o0_, 2u)) { (out7)[2] = RAFEAT7_WRAP_Q16((in7)[2] + RAFEAT7_QMUL((h_in), RAFEAT7_ROOT3_OVER2_Q16)); rf_o0_ = RAFEAT7_OPEN(rf_o0_, 2u); } else { rf_o1_ |= RAFEAT7_ERR_ORDER; } \
    if (RAFEAT7_OPEN_NEXT(rf_o0_, 3u)) { (out7)[3] = RAFEAT7_WRAP_Q16((in7)[3] + RAFEAT7_PHI_CH((c_in), (h_in))); rf_o0_ = RAFEAT7_OPEN(rf_o0_, 3u); } else { rf_o1_ |= RAFEAT7_ERR_ORDER; } \
    if (RAFEAT7_OPEN_NEXT(rf_o0_, 4u)) { (out7)[4] = RAFEAT7_WRAP_Q16((in7)[4] + (uint32_t)(c_in)); rf_o0_ = RAFEAT7_OPEN(rf_o0_, 4u); } else { rf_o1_ |= RAFEAT7_ERR_ORDER; } \
    if (RAFEAT7_OPEN_NEXT(rf_o0_, 5u)) { (out7)[5] = RAFEAT7_WRAP_Q16((in7)[5] + (uint32_t)(h_in)); rf_o0_ = RAFEAT7_OPEN(rf_o0_, 5u); } else { rf_o1_ |= RAFEAT7_ERR_ORDER; } \
    if (RAFEAT7_OPEN_NEXT(rf_o0_, 6u)) { rf_o2_ = RAFEAT7_WRAP_Q16((in7)[6] + RAFEAT7_QMUL(RAFEAT7_PHI_CH((c_in), (h_in)), RAFEAT7_ROOT3_OVER2_Q16)); (out7)[6] = rf_o2_; rf_o0_ = RAFEAT7_OPEN(rf_o0_, 6u); } else { rf_o1_ |= RAFEAT7_ERR_ORDER; } \
    (err_out) = (uint32_t)(rf_o1_ | (uint32_t)((rf_o0_ != 0x7fu) ? RAFEAT7_ERR_ORDER : RAFEAT7_OK)); \
} while (0)

#define RAFEAT7_STEP(cell_out, r_out, c_out, idx_out, err_out) do { \
    uint32_t r0_ = (uint32_t)(r_out); \
    uint32_t r1_ = (uint32_t)(c_out); \
    uint32_t r2_ = RAFEAT7_IDX(r0_, r1_); \
    (cell_out) = r2_; \
    (idx_out) = r2_; \
    (err_out) = RAFEAT7_VOID_FLAG(r2_); \
    (r_out) = RAFEAT7_NEXT_R(r0_); \
    (c_out) = RAFEAT7_NEXT_C(r1_); \
} while (0)

#define RAFEAT7_COLLAPSE_42(seed7, c0, h0, sink7, void_seen, err_out) do { \
    RAFEAT7_DECLARE_ATTRACTORS(r0_); \
    uint16_t r1_[RAFEAT7_DIM]; \
    uint32_t r2_ = 0u; \
    uint32_t r3_ = 0u; \
    uint32_t r4_ = 0u; \
    uint32_t r5_ = 0u; \
    uint32_t r6_ = 0u; \
    uint32_t r7_ = RAFEAT7_WRAP_Q16(c0); \
    uint32_t r8_ = RAFEAT7_WRAP_Q16(h0); \
    uint32_t r9_ = RAFEAT7_OK; \
    r1_[0] = (uint16_t)(seed7)[0]; r1_[1] = (uint16_t)(seed7)[1]; r1_[2] = (uint16_t)(seed7)[2]; r1_[3] = (uint16_t)(seed7)[3]; \
    r1_[4] = (uint16_t)(seed7)[4]; r1_[5] = (uint16_t)(seed7)[5]; r1_[6] = (uint16_t)(seed7)[6]; \
    for (r2_ = 0u; r2_ < RAFEAT7_CELLS; ++r2_) { \
        RAFEAT7_STEP(r4_, r5_, r6_, r3_, r9_); \
        (void)r4_; \
        (void_seen) |= (uint32_t)(r9_ & RAFEAT7_ERR_VOID); \
        r7_ = RAFEAT7_AVG4(r7_, r0_[r3_][4]); \
        r8_ = RAFEAT7_AVG4(r8_, r0_[r3_][5]); \
        RAFEAT7_OPEN7(r1_, r7_, r8_, r1_, r9_); \
        (err_out) |= r9_; \
    } \
    (sink7)[0] = r1_[0]; (sink7)[1] = r1_[1]; (sink7)[2] = r1_[2]; (sink7)[3] = r1_[3]; \
    (sink7)[4] = r1_[4]; (sink7)[5] = r1_[5]; (sink7)[6] = r1_[6]; \
} while (0)

#endif
