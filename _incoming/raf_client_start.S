/* raf_client_start.S — Entry ARM32/ARM64/x86-64/RISCV64
 * Freestanding bootstrap: zero stdlib, zero CRT, zero interpreter.
 * Single documented transition: _start → client_main.
 * After return: exit_group(0) via direct syscall.
 */
#if defined(__arm__)
.syntax unified
.thumb
.text
.align 2
.global _start
.global client_main
.thumb_func
_start:
    mov  r11,#0
    mov  lr,#0
    bl   client_main
    mov  r7,#248
    mov  r0,#0
    svc  #0
.h: b .h

#elif defined(__aarch64__)
.text
.align 4
.global _start
.global client_main
_start:
    mov  x29,xzr
    mov  x30,xzr
    and  sp,sp,#-16
    bl   client_main
    mov  x0,xzr
    mov  x8,#94
    svc  #0
.h: b .h

#elif defined(__x86_64__)
.text
.globl _start
.globl client_main
_start:
    xor  %rbp,%rbp
    call client_main
    mov  $231,%rax
    xor  %rdi,%rdi
    syscall

#elif defined(__riscv)
.text
.align 2
.global _start
.global client_main
_start:
    mv   fp,zero        /* clear frame pointer (s0)                    */
    mv   ra,zero        /* clear return address                        */
    call client_main
    li   a7,94          /* exit_group                                  */
    li   a0,0
    ecall
.h: j .h
#endif
.section .note.GNU-stack,"",@progbits
