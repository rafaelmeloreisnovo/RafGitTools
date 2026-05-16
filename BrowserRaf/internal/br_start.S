/* br_start.S — Entry ARM32/ARM64/x86-64 */
#if defined(__arm__)
.syntax unified
.thumb
.text
.align 2
.global _start
.thumb_func
_start:
    mov  r11,#0
    mov  lr,#0
    bl   _start
    mov  r7,#248
    mov  r0,#0
    svc  #0
.h: b .h
#elif defined(__aarch64__)
.text
.align 4
.global _start
_start:
    mov  x29,xzr
    mov  x30,xzr
    and  sp,sp,#-16
    bl   _start
    mov  x0,xzr
    mov  x8,#94
    svc  #0
.h: b .h
#elif defined(__x86_64__)
.text
.globl _start
_start:
    xor  %rbp,%rbp
    call _start
    mov  $231,%rax
    xor  %rdi,%rdi
    syscall
#endif
.section .note.GNU-stack,"",@progbits
