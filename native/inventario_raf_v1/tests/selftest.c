#include "inventario_raf_core.h"

#include <assert.h>
#include <string.h>

static void test_tokenizer_and_five_passes(void)
{
    static const uint8_t path[] = "001_PONTEIRO/docs/START-HERE.md";
    static const uint8_t mime[] = "text/markdown";
    rafinv_v1_result result;
    int32_t status = rafinv_v1_analyze(
        path, (uint32_t)(sizeof(path) - 1u), mime,
        (uint32_t)(sizeof(mime) - 1u), RAFINV_V1_INPUT_SIZE_KNOWN |
        RAFINV_V1_INPUT_PARENT_KNOWN, 17u, &result);
    assert(status == RAFINV_V1_OK);
    assert(result.pass_mask == RAFINV_V1_ALL_PASSES);
    assert(result.path_components == 3u);
    assert(result.kind_flags == RAFINV_V1_KIND_TEXT);
    assert(result.size_known == 1u && result.reported_bytes == 17u);
    assert(result.token_count == 6u);
    assert(memcmp(path + result.tokens[0].offset, "001", 3u) == 0);
}

static void test_empty_size_is_distinct_from_unknown(void)
{
    static const uint8_t path[] = "empty.json";
    rafinv_v1_result known;
    rafinv_v1_result unknown;
    assert(rafinv_v1_analyze(path, 10u, 0, 0u,
        RAFINV_V1_INPUT_SIZE_KNOWN, 0u, &known) == RAFINV_V1_OK);
    assert(rafinv_v1_analyze(path, 10u, 0, 0u, 0u, 0u, &unknown) == RAFINV_V1_OK);
    assert(known.size_known == 1u && known.reported_bytes == 0u);
    assert(unknown.size_known == 0u);
}

static void test_fail_closed_paths_and_capacity(void)
{
    static const uint8_t traversal[] = "folder/../secret";
    static const uint8_t absolute[] = "/root/file";
    rafinv_v1_result result;
    assert(rafinv_v1_analyze(traversal, (uint32_t)(sizeof(traversal) - 1u),
        0, 0u, 0u, 0u, &result) == RAFINV_V1_EPATH);
    assert(rafinv_v1_analyze(absolute, (uint32_t)(sizeof(absolute) - 1u),
        0, 0u, 0u, 0u, &result) == RAFINV_V1_EPATH);
}

static void test_token_vazio_is_valid(void)
{
    static const uint8_t path[] = "---";
    rafinv_v1_result result;
    assert(rafinv_v1_analyze(path, (uint32_t)(sizeof(path) - 1u),
        0, 0u, 0u, 0u, &result) == RAFINV_V1_OK);
    assert(result.token_count == 0u);
    assert(result.pass_mask == RAFINV_V1_ALL_PASSES);
}

static void test_utf8_token_bytes_are_preserved(void)
{
    static const uint8_t path[] = "001_PONTEIRO/Ol\xc3\xa1.json";
    rafinv_v1_result first;
    rafinv_v1_result second;
    uint32_t offset;
    assert(rafinv_v1_analyze(path, (uint32_t)(sizeof(path) - 1u),
        0, 0u, 0u, 0u, &first) == RAFINV_V1_OK);
    assert(rafinv_v1_analyze(path, (uint32_t)(sizeof(path) - 1u),
        0, 0u, 0u, 0u, &second) == RAFINV_V1_OK);
    assert(first.token_count == 4u);
    offset = first.tokens[2].offset;
    assert(first.tokens[2].length == 4u);
    assert(path[offset] == (uint8_t)'O');
    assert(path[offset + 2u] == 0xc3u && path[offset + 3u] == 0xa1u);
    assert(memcmp(&first, &second, sizeof(first)) == 0);
}

int main(void)
{
    test_tokenizer_and_five_passes();
    test_empty_size_is_distinct_from_unknown();
    test_fail_closed_paths_and_capacity();
    test_token_vazio_is_valid();
    test_utf8_token_bytes_are_preserved();
    return 0;
}
