.syntax unified
.text
.global raf_dep_sync
.type raf_dep_sync, %function
raf_dep_sync:
    mov r0, #1
    bx lr
