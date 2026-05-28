#if defined(__aarch64__)
#include "arm64/dep_mem.S"
#elif defined(__arm__)
#include "arm32/dep_mem.S"
#else
.globl raf_dep_mem
raf_dep_mem:
    mov $1, %eax
    ret
#endif
