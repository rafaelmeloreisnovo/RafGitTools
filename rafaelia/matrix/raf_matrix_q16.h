#ifndef RAF_MATRIX_Q16_H
#define RAF_MATRIX_Q16_H

/*
 * RAFAELIA Matrix Q16
 *
 * Provenance:
 *   source_repo   = rafaelmeloreisnovo/Vectras-VM-Android
 *   source_path   = .ci/matrixbitraf.c
 *   source_commit = 6ab34fcfbbaa7fa3536507d9c42d066d2fe94365
 *   source_blob   = 8fb9960fb821a26eef1538ca9a4270dadd5d3a6d
 *   classification = rafaelia_original
 *   sole_author    = TOKEN_VAZIO (genealogy gate pending)
 *
 * Mathematical note:
 *   Integer square root and geometric mean are standard mathematics.
 *   This module does not implement [RJ-RPM107-2023].
 */

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define RAF_MATRIX_DIM             10u
#define RAF_MATRIX_TOTAL           1000u
#define RAF_MATRIX_PARITY_TETRA    4u
#define RAF_MATRIX_DUAL_CORE       2u
#define RAF_MATRIX_VN3D_DEGREE     6u
#define RAF_MATRIX_CORE_LO         4u
#define RAF_MATRIX_CORE_HI         5u

#define RAF_Q16_ONE                0x00010000u
#define RAF_SQRT2_Q16              0x00016A0Au
#define RAF_SQRT2_HALF_Q16         0x0000B505u
#define RAF_SQRT3_HALF_Q16         0x0000DDB4u

static const int8_t RAF_MATRIX_VN3D_OFFSETS[RAF_MATRIX_VN3D_DEGREE][3] = {
    {  1,  0,  0 },
    { -1,  0,  0 },
    {  0,  1,  0 },
    {  0, -1,  0 },
    {  0,  0,  1 },
    {  0,  0, -1 },
};

typedef struct {
    uint32_t body[RAF_MATRIX_DIM][RAF_MATRIX_DIM][RAF_MATRIX_DIM];
    uint32_t frac_parity[RAF_MATRIX_PARITY_TETRA];
    uint32_t dual_core[RAF_MATRIX_DUAL_CORE];
    uint32_t singularity_q16;
} RafMatrixQ16;

static inline bool raf_matrix_q16_in_bounds(uint32_t i, uint32_t j, uint32_t k)
{
    return i < RAF_MATRIX_DIM && j < RAF_MATRIX_DIM && k < RAF_MATRIX_DIM;
}

static inline uint32_t raf_q16_mul_u32(uint32_t a_q16, uint32_t b_q16)
{
    return (uint32_t)((((uint64_t)a_q16 * (uint64_t)b_q16) + 0x8000u) >> 16);
}

static inline uint32_t raf_q16_div_u32(uint32_t a_q16, uint32_t b_q16)
{
    if (b_q16 == 0u) return 0u;
    return (uint32_t)((((uint64_t)a_q16 << 16) + (b_q16 >> 1)) / b_q16);
}

/* Restoring base-4 integer square root: floor(sqrt(x)), zero libm. */
static inline uint32_t raf_isqrt_u64(uint64_t x)
{
    uint64_t op = x;
    uint64_t res = 0u;
    uint64_t one = UINT64_C(1) << 62;

    while (one > op) one >>= 2;

    while (one != 0u) {
        if (op >= res + one) {
            op -= res + one;
            res = (res >> 1) + one;
        } else {
            res >>= 1;
        }
        one >>= 2;
    }

    return (uint32_t)res;
}

static inline void raf_matrix_q16_init(RafMatrixQ16 *matrix)
{
    if (matrix == NULL) return;

    for (uint32_t i = 0; i < RAF_MATRIX_DIM; ++i) {
        for (uint32_t j = 0; j < RAF_MATRIX_DIM; ++j) {
            for (uint32_t k = 0; k < RAF_MATRIX_DIM; ++k) {
                matrix->body[i][j][k] = 0u;
            }
        }
    }

    for (uint32_t n = 0; n < RAF_MATRIX_PARITY_TETRA; ++n) {
        matrix->frac_parity[n] = 0u;
    }
    for (uint32_t n = 0; n < RAF_MATRIX_DUAL_CORE; ++n) {
        matrix->dual_core[n] = 0u;
    }

    matrix->singularity_q16 = RAF_SQRT2_Q16;
}

static inline uint32_t raf_matrix_q16_get(
    const RafMatrixQ16 *matrix, uint32_t i, uint32_t j, uint32_t k)
{
    if (matrix == NULL || !raf_matrix_q16_in_bounds(i, j, k)) return 0u;
    return matrix->body[i][j][k];
}

static inline bool raf_matrix_q16_set(
    RafMatrixQ16 *matrix, uint32_t i, uint32_t j, uint32_t k, uint32_t value_q16)
{
    if (matrix == NULL || !raf_matrix_q16_in_bounds(i, j, k)) return false;
    matrix->body[i][j][k] = value_q16;
    return true;
}

static inline void raf_matrix_q16_refresh_dual_core(RafMatrixQ16 *matrix)
{
    if (matrix == NULL) return;
    matrix->dual_core[0] =
        matrix->body[RAF_MATRIX_CORE_LO][RAF_MATRIX_CORE_LO][RAF_MATRIX_CORE_LO];
    matrix->dual_core[1] =
        matrix->body[RAF_MATRIX_CORE_HI][RAF_MATRIX_CORE_HI][RAF_MATRIX_CORE_HI];
}

/* E = sqrt(N1*N2); Q16.16 inputs produce a Q16.16 result. */
static inline uint32_t raf_matrix_q16_cross_e(RafMatrixQ16 *matrix)
{
    if (matrix == NULL) return 0u;
    raf_matrix_q16_refresh_dual_core(matrix);
    return raf_isqrt_u64(
        (uint64_t)matrix->dual_core[0] * (uint64_t)matrix->dual_core[1]);
}

static inline uint32_t raf_matrix_q16_singularity(const RafMatrixQ16 *matrix)
{
    return matrix == NULL ? RAF_SQRT2_Q16 : matrix->singularity_q16;
}

static inline void raf_matrix_q16_refresh_tetra_parity(RafMatrixQ16 *matrix)
{
    if (matrix == NULL) return;
    matrix->frac_parity[0] = matrix->body[0][0][0];
    matrix->frac_parity[1] = matrix->body[9][9][0];
    matrix->frac_parity[2] = matrix->body[9][0][9];
    matrix->frac_parity[3] = matrix->body[0][9][9];
}

static inline uint32_t raf_matrix_q16_mean(const RafMatrixQ16 *matrix)
{
    if (matrix == NULL) return 0u;

    uint64_t acc = 0u;
    for (uint32_t i = 0; i < RAF_MATRIX_DIM; ++i) {
        for (uint32_t j = 0; j < RAF_MATRIX_DIM; ++j) {
            for (uint32_t k = 0; k < RAF_MATRIX_DIM; ++k) {
                acc += matrix->body[i][j][k];
            }
        }
    }
    return (uint32_t)(acc / RAF_MATRIX_TOTAL);
}

#endif /* RAF_MATRIX_Q16_H */
