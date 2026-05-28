#if defined(__aarch64__)
#include "arm64/dep_flags.S"
#elif defined(__arm__)
#include "arm32/dep_flags.S"
#else
.globl raf_dep_flags
raf_dep_flags:
    mov $1, %eax
    ret
#endif
