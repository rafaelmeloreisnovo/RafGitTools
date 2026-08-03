#ifndef RAFYML_RUNTIME_H
#define RAFYML_RUNTIME_H

/* Freestanding fixed-width contract. */
typedef unsigned char raf_u8;
typedef unsigned short raf_u16;
typedef unsigned int raf_u32;
typedef signed long long raf_i64;

_Static_assert(sizeof(raf_u8) == 1, "raf_u8 width");
_Static_assert(sizeof(raf_u16) == 2, "raf_u16 width");
_Static_assert(sizeof(raf_u32) == 4, "raf_u32 width");
_Static_assert(sizeof(raf_i64) == 8, "raf_i64 width");

#define RAFYML_NONE 0xffffffffu
#define RAFYML_FORMAT_VERSION 1u

enum rafyml_type {
    RAFYML_NULL = 0,
    RAFYML_BOOL = 1,
    RAFYML_INT = 2,
    RAFYML_STRING = 3,
    RAFYML_MAP = 4,
    RAFYML_LIST = 5
};

typedef struct rafyml_node {
    raf_u32 key_offset;
    raf_u32 value_offset;
    raf_u32 first_child;
    raf_u32 next_sibling;
    raf_u32 child_count;
    raf_i64 int_value;
    raf_u8 type;
    raf_u8 bool_value;
    raf_u16 reserved;
} rafyml_node;

typedef struct rafyml_document {
    const rafyml_node *nodes;
    const raf_u8 *strings;
    raf_u32 node_count;
    raf_u32 string_size;
    raf_u32 root_index;
    raf_u32 format_version;
} rafyml_document;

enum rafyml_validation {
    RAFYML_VALID = 0,
    RAFYML_ERR_DOCUMENT = -1,
    RAFYML_ERR_VERSION = -2,
    RAFYML_ERR_RANGE = -3,
    RAFYML_ERR_STRING = -4,
    RAFYML_ERR_NODE = -5,
    RAFYML_ERR_GRAPH = -6
};

int rafyml_validate_document(const rafyml_document *document);
const char *rafyml_string_at(const rafyml_document *document, raf_u32 offset);

#endif
