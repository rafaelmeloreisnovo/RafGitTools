#ifndef RAFYML_L0_GRAPH_H
#define RAFYML_L0_GRAPH_H
#include "rafyml_runtime.h"
/* Caller owns scratch[doc->node_count]. No allocation and no I/O. */
#define RAFYML_L0_GRAPH_OK 0
#define RAFYML_L0_GRAPH_BAD_ARGUMENT -10
#define RAFYML_L0_GRAPH_RANGE -11
#define RAFYML_L0_GRAPH_CYCLE_OR_ALIAS -12
#define RAFYML_L0_GRAPH_ORPHAN -13
#define RAFYML_L0_GRAPH_SHAPE -14
int rafyml_l0_graph_check(const rafyml_document *doc, raf_u8 *scratch, raf_u32 capacity);
#endif
