#include "rafyml_runtime.h"

static int rafyml_string_valid(const rafyml_document *document, raf_u32 offset) {
    raf_u32 cursor;
    if (offset == 0u) return 1;
    if (offset >= document->string_size) return 0;
    for (cursor = offset; cursor < document->string_size; ++cursor) {
        if (document->strings[cursor] == 0u) return 1;
    }
    return 0;
}

const char *rafyml_string_at(const rafyml_document *document, raf_u32 offset) {
    if (!document || !document->strings || !rafyml_string_valid(document, offset)) return (const char *)0;
    return (const char *)(document->strings + offset);
}

int rafyml_validate_document(const rafyml_document *document) {
    raf_u32 index;
    if (!document || !document->nodes || !document->strings) return RAFYML_ERR_DOCUMENT;
    if (document->format_version != RAFYML_FORMAT_VERSION) return RAFYML_ERR_VERSION;
    if (document->node_count == 0u || document->root_index >= document->node_count) return RAFYML_ERR_RANGE;
    if (document->string_size == 0u || document->strings[0] != 0u) return RAFYML_ERR_STRING;

    for (index = 0u; index < document->node_count; ++index) {
        const rafyml_node *node = document->nodes + index;
        if (node->type > RAFYML_LIST || node->reserved != 0u) return RAFYML_ERR_NODE;
        if (!rafyml_string_valid(document, node->key_offset)) return RAFYML_ERR_STRING;
        if (node->type == RAFYML_STRING) {
            if (node->value_offset == 0u || !rafyml_string_valid(document, node->value_offset)) return RAFYML_ERR_STRING;
        } else if (node->value_offset != 0u) {
            return RAFYML_ERR_NODE;
        }
        if (node->next_sibling != RAFYML_NONE && node->next_sibling >= document->node_count) return RAFYML_ERR_RANGE;
        if (node->type == RAFYML_MAP || node->type == RAFYML_LIST) {
            raf_u32 cursor = node->first_child;
            raf_u32 count = 0u;
            if (node->child_count == 0u) {
                if (cursor != RAFYML_NONE) return RAFYML_ERR_GRAPH;
                continue;
            }
            if (cursor == RAFYML_NONE || cursor >= document->node_count) return RAFYML_ERR_GRAPH;
            while (cursor != RAFYML_NONE && count <= document->node_count) {
                ++count;
                cursor = document->nodes[cursor].next_sibling;
            }
            if (count != node->child_count || count > document->node_count) return RAFYML_ERR_GRAPH;
        } else if (node->first_child != RAFYML_NONE || node->child_count != 0u) {
            return RAFYML_ERR_GRAPH;
        }
        if (node->type == RAFYML_BOOL && node->bool_value > 1u) return RAFYML_ERR_NODE;
    }
    return RAFYML_VALID;
}
