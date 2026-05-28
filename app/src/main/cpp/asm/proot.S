#if defined(__aarch64__)
#include "arm64/proot.S"
#elif defined(__arm__)
#include "arm32/proot.S"
#else
.globl raf_proot
raf_proot:
    mov $1, %eax
    ret
#endif
