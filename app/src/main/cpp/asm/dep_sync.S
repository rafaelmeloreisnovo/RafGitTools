#if defined(__aarch64__)
#include "arm64/dep_sync.S"
#elif defined(__arm__)
#include "arm32/dep_sync.S"
#else
.globl raf_dep_sync
raf_dep_sync:
    mov $1, %eax
    ret
#endif
