/* Hosted adversarial harness; NOT part of the freestanding L0 runtime. */
#include "rafyml_l0_graph.h"
#include <stdio.h>
#define NONE RAFYML_NONE
static rafyml_node nodes[4];
static raf_u8 scratch[4];
static rafyml_document doc;
static int checks;
static int check(int want, const char *name) {
    int got = rafyml_l0_graph_check(&doc,scratch,4u);
    ++checks;
    if (got != want) { fprintf(stderr,"FAIL %s got %d expected %d\n",name,got,want); return 1; }
    return 0;
}
static void reset(void) {
    unsigned i;
    for(i=0;i<4;i++) { nodes[i]=(rafyml_node){0}; nodes[i].first_child=NONE;nodes[i].next_sibling=NONE;nodes[i].type=RAFYML_NULL; }
    nodes[0].type=RAFYML_MAP;
    nodes[0].first_child=1; nodes[0].child_count=3;
    nodes[1].next_sibling=2;nodes[2].next_sibling=3;
    doc=(rafyml_document){0};doc.nodes=nodes;doc.node_count=4;doc.root_index=0;
}
int main(void) {
 int bad=0;reset();bad+=check(0,"valid");
 reset();nodes[2].next_sibling=4;bad+=check(-11,"bad sibling offset");
 reset();nodes[3].next_sibling=2;bad+=check(-12,"sibling cycle");
 reset();nodes[3].next_sibling=1;bad+=check(-12,"cycle to first");
 reset();nodes[0].first_child=0;bad+=check(-12,"root as child");
 reset();nodes[0].child_count=2;bad+=check(-14,"child_count mismatch");
 reset();nodes[0].child_count=0;bad+=check(-14,"nonempty zero-count");
 reset();nodes[2].next_sibling=NONE;nodes[0].child_count=2;bad+=check(-13,"orphan");
 reset();nodes[0].next_sibling=1;bad+=check(-14,"root sibling");
 reset();nodes[1].first_child=2;bad+=check(-14,"scalar child");
 reset();nodes[1].type=7;bad+=check(-14,"invalid type");
 reset();doc.root_index=4;bad+=check(-11,"bad root");
 reset();doc.node_count=0;bad+=check(-11,"zero nodes");
 reset();doc.node_count=4097;bad+=check(-11,"oversized nodes");
 reset();doc.node_count=4;bad+=check(0,"restore");
 reset();doc.nodes=0;bad+=check(-10,"null nodes");
 printf("checks=%d failures=%d\n",checks,bad);return bad?1:0;
}
