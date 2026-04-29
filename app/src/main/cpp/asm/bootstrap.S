#if defined(__aarch64__)
#include "arm64/bootstrap.S"
#elif defined(__arm__)
#include "arm32/bootstrap.S"
#else
.globl raf_bootstrap
raf_bootstrap:
    mov $1, %eax
    ret
#endif
