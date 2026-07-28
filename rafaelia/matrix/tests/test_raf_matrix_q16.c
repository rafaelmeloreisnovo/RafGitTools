#include <assert.h>
#include <stdint.h>

#include "../raf_matrix_q16.h"

static void test_isqrt(void)
{
    assert(raf_isqrt_u64(0u) == 0u);
    assert(raf_isqrt_u64(1u) == 1u);
    assert(raf_isqrt_u64(2u) == 1u);
    assert(raf_isqrt_u64(4u) == 2u);
    assert(raf_isqrt_u64(15u) == 3u);
    assert(raf_isqrt_u64(81u) == 9u);
    assert(raf_isqrt_u64(UINT64_C(4294967295) * UINT64_C(4294967295))
           == UINT32_MAX);
}

static void test_matrix(void)
{
    RafMatrixQ16 matrix;
    raf_matrix_q16_init(&matrix);

    assert(raf_matrix_q16_singularity(&matrix) == RAF_SQRT2_Q16);
    assert(raf_matrix_q16_get(&matrix, 0u, 0u, 0u) == 0u);
    assert(!raf_matrix_q16_set(&matrix, 10u, 0u, 0u, RAF_Q16_ONE));

    assert(raf_matrix_q16_set(
        &matrix, RAF_MATRIX_CORE_LO, RAF_MATRIX_CORE_LO, RAF_MATRIX_CORE_LO,
        4u * RAF_Q16_ONE));
    assert(raf_matrix_q16_set(
        &matrix, RAF_MATRIX_CORE_HI, RAF_MATRIX_CORE_HI, RAF_MATRIX_CORE_HI,
        9u * RAF_Q16_ONE));
    assert(raf_matrix_q16_cross_e(&matrix) == 6u * RAF_Q16_ONE);

    assert(raf_matrix_q16_set(&matrix, 0u, 0u, 0u, 1u));
    assert(raf_matrix_q16_set(&matrix, 9u, 9u, 0u, 2u));
    assert(raf_matrix_q16_set(&matrix, 9u, 0u, 9u, 3u));
    assert(raf_matrix_q16_set(&matrix, 0u, 9u, 9u, 4u));
    raf_matrix_q16_refresh_tetra_parity(&matrix);
    assert(matrix.frac_parity[0] == 1u);
    assert(matrix.frac_parity[1] == 2u);
    assert(matrix.frac_parity[2] == 3u);
    assert(matrix.frac_parity[3] == 4u);
}

static void test_mean(void)
{
    RafMatrixQ16 matrix;
    raf_matrix_q16_init(&matrix);
    for (uint32_t i = 0; i < RAF_MATRIX_DIM; ++i) {
        for (uint32_t j = 0; j < RAF_MATRIX_DIM; ++j) {
            for (uint32_t k = 0; k < RAF_MATRIX_DIM; ++k) {
                matrix.body[i][j][k] = RAF_Q16_ONE;
            }
        }
    }
    assert(raf_matrix_q16_mean(&matrix) == RAF_Q16_ONE);
}

int main(void)
{
    test_isqrt();
    test_matrix();
    test_mean();
    return 0;
}
