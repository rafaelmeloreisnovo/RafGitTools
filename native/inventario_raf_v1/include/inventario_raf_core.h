#ifndef INVENTARIO_RAF_CORE_V1_H
#define INVENTARIO_RAF_CORE_V1_H

#if defined(RAFINV_FREESTANDING)
typedef __UINT8_TYPE__ uint8_t;
typedef __UINT32_TYPE__ uint32_t;
typedef __UINT64_TYPE__ uint64_t;
typedef __INT32_TYPE__ int32_t;
#else
#include <stdint.h>
#endif

#define RAFINV_V1_MAX_PATH_BYTES 4096u
#define RAFINV_V1_MAX_TOKENS 256u

#define RAFINV_V1_INPUT_DIRECTORY (1u << 0)
#define RAFINV_V1_INPUT_SIZE_KNOWN (1u << 1)
#define RAFINV_V1_INPUT_PARENT_KNOWN (1u << 2)

#define RAFINV_V1_KIND_DIRECTORY (1u << 0)
#define RAFINV_V1_KIND_JSON (1u << 1)
#define RAFINV_V1_KIND_ARCHIVE (1u << 2)
#define RAFINV_V1_KIND_TEXT (1u << 3)
#define RAFINV_V1_KIND_UNKNOWN (1u << 4)

#define RAFINV_V1_PASS_PATH (1u << 0)
#define RAFINV_V1_PASS_TOKENIZER (1u << 1)
#define RAFINV_V1_PASS_KIND (1u << 2)
#define RAFINV_V1_PASS_SIZE (1u << 3)
#define RAFINV_V1_PASS_RELATION (1u << 4)
#define RAFINV_V1_ALL_PASSES ((1u << 5) - 1u)

#define RAFINV_V1_OK 0
#define RAFINV_V1_EINVAL (-1)
#define RAFINV_V1_EPATH (-2)
#define RAFINV_V1_ENOSPC (-3)

typedef struct rafinv_v1_token {
    uint32_t offset;
    uint32_t length;
} rafinv_v1_token;

typedef struct rafinv_v1_result {
    int32_t status;
    uint32_t token_count;
    uint32_t path_components;
    uint32_t pass_mask;
    uint32_t kind_flags;
    uint32_t size_known;
    uint32_t parent_known;
    uint64_t reported_bytes;
    rafinv_v1_token tokens[RAFINV_V1_MAX_TOKENS];
} rafinv_v1_result;

/*
 * Freestanding metadata-only analyzer. Inputs are caller-owned byte spans;
 * this core performs no allocation, file I/O, syscalls, or content reads.
 * Token offsets refer to the original UTF-8 byte span and do not copy names.
 */
int32_t rafinv_v1_analyze(const uint8_t *path, uint32_t path_length,
                         const uint8_t *mime, uint32_t mime_length,
                         uint32_t input_flags, uint64_t reported_bytes,
                         rafinv_v1_result *result);

uint32_t rafinv_v1_path_components_c(const uint8_t *path, uint32_t length);

#endif
