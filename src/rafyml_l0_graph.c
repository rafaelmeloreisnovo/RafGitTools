#include "rafyml_l0_graph.h"
/* Additive L0 verifier, separate from existing rafyml_validate_document().
 * Precondition: nodes points to at least node_count readable elements;
 * this cannot be inferred from an untrusted raw pointer.
 */
int rafyml_l0_graph_check(const rafyml_document *doc, raf_u8 *scratch, raf_u32 capacity) {
    raf_u32 i, cursor, count, n;
    if (!doc || !doc->nodes || !scratch) return RAFYML_L0_GRAPH_BAD_ARGUMENT;
    n = doc->node_count;
    if (n == 0u || n > 4096u || capacity < n || doc->root_index >= n)
        return RAFYML_L0_GRAPH_RANGE;
    for (i = 0u; i < n; ++i) scratch[i] = 0u;
    for (i = 0u; i < n; ++i) {
        const rafyml_node *parent = doc->nodes + i;
        if (parent->type > RAFYML_LIST) return RAFYML_L0_GRAPH_SHAPE;
        if (parent->next_sibling != RAFYML_NONE && parent->next_sibling >= n)
            return RAFYML_L0_GRAPH_RANGE;
        if (parent->type != RAFYML_MAP && parent->type != RAFYML_LIST) {
            if (parent->child_count || parent->first_child != RAFYML_NONE)
                return RAFYML_L0_GRAPH_SHAPE;
            continue;
        }
        if (parent->child_count == 0u) {
            if (parent->first_child != RAFYML_NONE) return RAFYML_L0_GRAPH_SHAPE;
            continue;
        }
        if (parent->child_count >= n || parent->first_child == RAFYML_NONE)
            return RAFYML_L0_GRAPH_RANGE;
        cursor = parent->first_child;
        count = 0u;
        while (cursor != RAFYML_NONE) {
            if (cursor >= n) return RAFYML_L0_GRAPH_RANGE;
            if (cursor == doc->root_index || scratch[cursor])
                return RAFYML_L0_GRAPH_CYCLE_OR_ALIAS;
            scratch[cursor] = 1u;
            ++count;
            if (count > parent->child_count) return RAFYML_L0_GRAPH_SHAPE;
            cursor = doc->nodes[cursor].next_sibling;
        }
        if (count != parent->child_count) return RAFYML_L0_GRAPH_SHAPE;
    }
    if (scratch[doc->root_index] || doc->nodes[doc->root_index].next_sibling != RAFYML_NONE)
        return RAFYML_L0_GRAPH_SHAPE;
    for (i = 0u; i < n; ++i) {
        if (i != doc->root_index && !scratch[i]) return RAFYML_L0_GRAPH_ORPHAN;
    }
    return RAFYML_L0_GRAPH_OK;
}
