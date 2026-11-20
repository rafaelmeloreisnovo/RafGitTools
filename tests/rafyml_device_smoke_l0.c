/* Test-only Linux EABI exit adapter. RAFAELIA L0 parser itself is unchanged. */
#include "rafyml_runtime.h"

static const raf_u8 raf_fixture_pool[] = {0u, ' ', ' ', ' ', ' ', 0u};
static const rafyml_node raf_valid_nodes[2] = {
    {0u, 0u, 1u, RAFYML_NONE, 1u, 0, RAFYML_MAP, 0u, 0u},
    {0u, 1u, RAFYML_NONE, RAFYML_NONE, 0u, 0, RAFYML_STRING, 0u, 0u}
};
static const rafyml_node raf_bad_nodes[2] = {
    {0u, 0u, 2u, RAFYML_NONE, 1u, 0, RAFYML_MAP, 0u, 0u},
    {0u, 1u, RAFYML_NONE, RAFYML_NONE, 0u, 0, RAFYML_STRING, 0u, 0u}
};

static unsigned raf_smoke_result(void) {
    const rafyml_document valid = {raf_valid_nodes, raf_fixture_pool, 2u, 6u, 0u, RAFYML_FORMAT_VERSION};
    const rafyml_document invalid = {raf_bad_nodes, raf_fixture_pool, 2u, 6u, 0u, RAFYML_FORMAT_VERSION};
    const char *spaces;
    if (rafyml_validate_document(&valid) != RAFYML_VALID) return 11u;
    spaces = rafyml_string_at(&valid, 1u);
    if (spaces == (const char *)0) return 12u;
    if (spaces[0] != ' ' || spaces[1] != ' ' || spaces[2] != ' ' || spaces[3] != ' ' || spaces[4] != 0) return 13u;
    if (rafyml_validate_document(&invalid) != RAFYML_ERR_GRAPH) return 14u;
    if (rafyml_string_at(&valid, 6u) != (const char *)0) return 15u;
    return 0u;
}

#if defined(RAFYML_HOSTED_TEST)
int main(void) { return (int)raf_smoke_result(); }
#elif defined(__aarch64__)
/* Syscall belongs exclusively to TEST ADAPTER; core C stays syscall-free. */
__attribute__((used,noreturn)) void _start(void) {
    register unsigned long status __asm__("x0") = (unsigned long)raf_smoke_result();
    register unsigned long nr __asm__("x8") = 93ul; /* Linux exit */
    __asm__ volatile("svc #0" : : "r"(status), "r"(nr) : "memory");
    for (;;) { }
}
#elif defined(__arm__)
__attribute__((used,noreturn)) void _start(void) {
    register unsigned long status __asm__("r0") = (unsigned long)raf_smoke_result();
    register unsigned long nr __asm__("r7") = 1ul; /* Linux EABI exit */
    __asm__ volatile("svc #0" : : "r"(status), "r"(nr) : "memory");
    for (;;) { }
}
#else
#error Unsupported target: only ARMv7 and AArch64, or hosted test
#endif
