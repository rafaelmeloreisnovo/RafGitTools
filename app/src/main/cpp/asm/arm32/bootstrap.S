.syntax unified
.text
.global raf_bootstrap
.type raf_bootstrap, %function
raf_bootstrap:
    mov r0, #1
    bx lr
