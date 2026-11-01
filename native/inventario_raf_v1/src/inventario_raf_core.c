#include "inventario_raf_core.h"

#define RAFINV_V1_ASCII_CONTROL_LIMIT 0x20u

#if defined(__arm__) && !defined(__aarch64__)
uint32_t rafinv_v1_path_components_arm32(const uint8_t *path, uint32_t length);
#define RAFINV_V1_ASM_COMPONENTS rafinv_v1_path_components_arm32
#elif defined(__aarch64__)
uint32_t rafinv_v1_path_components_arm64(const uint8_t *path, uint32_t length);
#define RAFINV_V1_ASM_COMPONENTS rafinv_v1_path_components_arm64
#endif

static int is_separator(uint8_t value)
{
    return value == (uint8_t)'/' || value == (uint8_t)'\\' ||
           value == (uint8_t)'.' || value == (uint8_t)'_' ||
           value == (uint8_t)'-' || value == (uint8_t)' ' ||
           value == (uint8_t)'\t';
}

static int span_equal_ascii(const uint8_t *left, uint32_t left_length,
                            const char *right, uint32_t right_length)
{
    uint32_t index;
    if (left == (const uint8_t *)0 || left_length != right_length) {
        return 0;
    }
    for (index = 0u; index < left_length; ++index) {
        if (left[index] != (uint8_t)right[index]) {
            return 0;
        }
    }
    return 1;
}

static uint32_t path_components(const uint8_t *path, uint32_t length)
{
#if defined(RAFINV_V1_ASM_COMPONENTS)
    return RAFINV_V1_ASM_COMPONENTS(path, length);
#else
    return rafinv_v1_path_components_c(path, length);
#endif
}

uint32_t rafinv_v1_path_components_c(const uint8_t *path, uint32_t length)
{
    uint32_t count;
    uint32_t index;
    if (path == (const uint8_t *)0 || length == 0u) {
        return 0u;
    }
    count = 1u;
    for (index = 0u; index < length; ++index) {
        count += (uint32_t)(path[index] == (uint8_t)'/');
    }
    return count;
}

static int path_is_safe(const uint8_t *path, uint32_t length)
{
    uint32_t start;
    uint32_t index;
    if (path == (const uint8_t *)0 || length == 0u ||
        length > RAFINV_V1_MAX_PATH_BYTES || path[0] == (uint8_t)'/' ||
        path[length - 1u] == (uint8_t)'/') {
        return 0;
    }
    start = 0u;
    for (index = 0u; index <= length; ++index) {
        if (index < length && path[index] < RAFINV_V1_ASCII_CONTROL_LIMIT) {
            return 0;
        }
        if (index == length || path[index] == (uint8_t)'/') {
            uint32_t component_length = index - start;
            if (component_length == 0u ||
                (component_length == 1u && path[start] == (uint8_t)'.') ||
                (component_length == 2u && path[start] == (uint8_t)'.' &&
                 path[start + 1u] == (uint8_t)'.')) {
                return 0;
            }
            start = index + 1u;
        }
    }
    return 1;
}

static int tokenize(const uint8_t *path, uint32_t length,
                    rafinv_v1_result *result)
{
    uint32_t index;
    uint32_t start = 0u;
    int in_token = 0;
    for (index = 0u; index <= length; ++index) {
        int boundary = index == length || is_separator(path[index]);
        if (!boundary && !in_token) {
            if (result->token_count >= RAFINV_V1_MAX_TOKENS) {
                return RAFINV_V1_ENOSPC;
            }
            start = index;
            in_token = 1;
        } else if (boundary && in_token) {
            rafinv_v1_token *token = &result->tokens[result->token_count++];
            token->offset = start;
            token->length = index - start;
            in_token = 0;
        }
    }
    return RAFINV_V1_OK;
}

static uint32_t classify_kind(const uint8_t *path, uint32_t path_length,
                              const uint8_t *mime, uint32_t mime_length,
                              uint32_t input_flags)
{
    uint32_t start = path_length;
    uint32_t index = path_length;
    if ((input_flags & RAFINV_V1_INPUT_DIRECTORY) != 0u) {
        return RAFINV_V1_KIND_DIRECTORY;
    }
    while (index > 0u && path[index - 1u] != (uint8_t)'/') {
        --index;
    }
    start = index;
    for (index = path_length; index > start; --index) {
        if (path[index - 1u] == (uint8_t)'.') {
            const uint8_t *extension = path + index;
            uint32_t extension_length = path_length - index;
            if (span_equal_ascii(extension, extension_length, "json", 4u) ||
                span_equal_ascii(extension, extension_length, "JSON", 4u)) {
                return RAFINV_V1_KIND_JSON;
            }
            if (span_equal_ascii(extension, extension_length, "zip", 3u) ||
                span_equal_ascii(extension, extension_length, "ZIP", 3u) ||
                span_equal_ascii(extension, extension_length, "7z", 2u) ||
                span_equal_ascii(extension, extension_length, "gz", 2u)) {
                return RAFINV_V1_KIND_ARCHIVE;
            }
            if (span_equal_ascii(extension, extension_length, "txt", 3u) ||
                span_equal_ascii(extension, extension_length, "md", 2u) ||
                span_equal_ascii(extension, extension_length, "csv", 3u)) {
                return RAFINV_V1_KIND_TEXT;
            }
            break;
        }
    }
    if (span_equal_ascii(mime, mime_length, "application/json", 16u)) {
        return RAFINV_V1_KIND_JSON;
    }
    if (span_equal_ascii(mime, mime_length, "application/zip", 15u) ||
        span_equal_ascii(mime, mime_length, "application/x-7z-compressed", 27u) ||
        span_equal_ascii(mime, mime_length, "application/gzip", 16u)) {
        return RAFINV_V1_KIND_ARCHIVE;
    }
    if (mime_length >= 5u && mime[0] == (uint8_t)'t' &&
        mime[1] == (uint8_t)'e' && mime[2] == (uint8_t)'x' &&
        mime[3] == (uint8_t)'t' && mime[4] == (uint8_t)'/') {
        return RAFINV_V1_KIND_TEXT;
    }
    return RAFINV_V1_KIND_UNKNOWN;
}

int32_t rafinv_v1_analyze(const uint8_t *path, uint32_t path_length,
                         const uint8_t *mime, uint32_t mime_length,
                         uint32_t input_flags, uint64_t reported_bytes,
                         rafinv_v1_result *result)
{
    int32_t status;
    if (result == (rafinv_v1_result *)0 ||
        (mime_length != 0u && mime == (const uint8_t *)0) ||
        (input_flags & ~(RAFINV_V1_INPUT_DIRECTORY |
                         RAFINV_V1_INPUT_SIZE_KNOWN |
                         RAFINV_V1_INPUT_PARENT_KNOWN)) != 0u) {
        return RAFINV_V1_EINVAL;
    }
    {
        uint32_t index;
        uint8_t *bytes = (uint8_t *)result;
        for (index = 0u; index < (uint32_t)sizeof(*result); ++index) {
            bytes[index] = 0u;
        }
    }
    if (!path_is_safe(path, path_length)) {
        result->status = RAFINV_V1_EPATH;
        return result->status;
    }
    result->pass_mask |= RAFINV_V1_PASS_PATH;
    result->path_components = path_components(path, path_length);
    if (result->path_components == 0u) {
        result->status = RAFINV_V1_EPATH;
        return result->status;
    }
    status = tokenize(path, path_length, result);
    if (status != RAFINV_V1_OK) {
        result->status = status;
        return status;
    }
    result->pass_mask |= RAFINV_V1_PASS_TOKENIZER;
    result->kind_flags = classify_kind(path, path_length, mime, mime_length,
                                       input_flags);
    result->pass_mask |= RAFINV_V1_PASS_KIND;
    result->size_known = (input_flags & RAFINV_V1_INPUT_SIZE_KNOWN) != 0u;
    result->reported_bytes = result->size_known ? reported_bytes : 0u;
    result->pass_mask |= RAFINV_V1_PASS_SIZE;
    result->parent_known = (input_flags & RAFINV_V1_INPUT_PARENT_KNOWN) != 0u;
    result->pass_mask |= RAFINV_V1_PASS_RELATION;
    result->status = RAFINV_V1_OK;
    return RAFINV_V1_OK;
}
