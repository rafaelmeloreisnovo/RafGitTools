#if defined(__aarch64__)
#include "arm64/dep_io.S"
#elif defined(__arm__)
#include "arm32/dep_io.S"
#else
.globl raf_dep_io
raf_dep_io:
    mov $1, %eax
    ret
#endif
