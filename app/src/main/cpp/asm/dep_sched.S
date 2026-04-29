#if defined(__aarch64__)
#include "arm64/dep_sched.S"
#elif defined(__arm__)
#include "arm32/dep_sched.S"
#else
.globl raf_dep_sched
raf_dep_sched:
    mov $1, %eax
    ret
#endif
