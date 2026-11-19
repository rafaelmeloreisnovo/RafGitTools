/* Link-only entrypoint; not a physical-device, Android loader, or startup test. */
#include "rafyml_runtime.h"

static const raf_u8 raf_link_pool[] = {0u};
static const rafyml_node raf_link_nodes[] = {
    {0u, 0u, RAFYML_NONE, RAFYML_NONE, 0u, 0, RAFYML_NULL, 0u, 0u}
};
static volatile int raf_link_result = -99;
static volatile raf_u32 raf_link_string_present = 0u;

__attribute__((used)) void _start(void) {
    const rafyml_document d = {
        raf_link_nodes, raf_link_pool, 1u, 1u, 0u, RAFYML_FORMAT_VERSION
    };
    raf_link_result = rafyml_validate_document(&d);
    raf_link_string_present = rafyml_string_at(&d, 0u) != (const char *)0;
    for (;;) { }
}
