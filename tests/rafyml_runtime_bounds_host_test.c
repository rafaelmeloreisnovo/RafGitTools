/* Hosted-only falsifier: not part of freestanding runtime. */
#include "rafyml_runtime.h"
#include <stdio.h>
static raf_u8 pool[9] = {0,' ',' ',' ',' ',0,'x',0,0};
static rafyml_node nodes[3];
static rafyml_document d;
static unsigned tests, fails;
static void check(int expected, const char *case_name) {
    int actual = rafyml_validate_document(&d);
    ++tests;
    if (actual != expected) { ++fails; fprintf(stderr,"FAIL %s: %d != %d\n",case_name,actual,expected); }
}
static void reset(void) {
    unsigned i;
    for (i=0;i<3;i++) {
        nodes[i]=(rafyml_node){0};
        nodes[i].type = RAFYML_NULL;
        nodes[i].first_child = RAFYML_NONE;
        nodes[i].next_sibling = RAFYML_NONE;
    }
    nodes[0].type = RAFYML_MAP;
    nodes[0].first_child = 1u;
    nodes[0].child_count = 2u;
    nodes[1].next_sibling = 2u;
    d = (rafyml_document){nodes,pool,3u,9u,0u,RAFYML_FORMAT_VERSION};
}
int main(void) {
    unsigned v;
    reset();check(RAFYML_VALID,"valid original");
    reset();nodes[1].next_sibling = 3u;check(RAFYML_ERR_GRAPH,"malformed dangling sibling");
    reset();nodes[2].next_sibling = 0xffffffffu;check(RAFYML_VALID,"explicit none");
    reset();nodes[2].next_sibling = 2u;check(RAFYML_ERR_GRAPH,"cycle bounded");
    reset();nodes[1].next_sibling = 0u;check(RAFYML_ERR_GRAPH,"root sibling cycle");
    reset();nodes[0].first_child = 3u;check(RAFYML_ERR_GRAPH,"first child out of range");
    reset();nodes[0].child_count = 0u;check(RAFYML_ERR_GRAPH,"declared zero with child");
    reset();nodes[0].type = RAFYML_NULL;nodes[0].first_child=RAFYML_NONE;nodes[0].child_count=0u;nodes[1].next_sibling=RAFYML_NONE;check(RAFYML_VALID,"null root not coerced");
    reset();nodes[1].type=RAFYML_STRING;nodes[1].value_offset=1u;check(RAFYML_VALID,"four spaces string");
    reset();nodes[1].type=RAFYML_STRING;nodes[1].value_offset=6u;check(RAFYML_VALID,"single character string");
    reset();nodes[1].type=RAFYML_INT;nodes[1].int_value=0;check(RAFYML_VALID,"numeric zero");
    reset();nodes[1].type=RAFYML_BOOL;nodes[1].bool_value=2u;check(RAFYML_ERR_NODE,"bad bool");
    reset();nodes[1].type=RAFYML_STRING;nodes[1].value_offset=9u;check(RAFYML_ERR_STRING,"invalid string offset");
    reset();d.strings=0;check(RAFYML_ERR_DOCUMENT,"null pool");
    reset();d.string_size=0;check(RAFYML_ERR_STRING,"zero pool size");
    reset();d.root_index=3u;check(RAFYML_ERR_RANGE,"bad root");
    for (v=0u;v<1024u;v++) {
        reset();nodes[1].next_sibling = 3u + v;
        check(RAFYML_ERR_GRAPH,"fuzz dangling sibling");
    }
    printf("cases=%u failures=%u\n",tests,fails);
    return fails ? 1 : 0;
}
