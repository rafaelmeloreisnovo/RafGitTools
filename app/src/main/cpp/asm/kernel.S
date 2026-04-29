#if defined(__aarch64__)
#include "arm64/kernel.S"
#elif defined(__arm__)
#include "arm32/kernel.S"
#else
.globl raf_kernel
raf_kernel:
    mov $1, %eax
    ret
#endif
