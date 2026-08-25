.syntax unified
.arch armv7-a
.arm

.section .text._start,"ax",%progbits
.global _start
.hidden _start
.type _start,%function
_start:
    bic sp, sp, #15
    bl raf_entry
    udf #0
.size _start, .-_start

.section .text.raf_sys_read,"ax",%progbits
.global raf_sys_read
.hidden raf_sys_read
.type raf_sys_read,%function
raf_sys_read:
    mov r7, #3
    svc #0
    bx lr
.size raf_sys_read, .-raf_sys_read

.section .text.raf_sys_write,"ax",%progbits
.global raf_sys_write
.hidden raf_sys_write
.type raf_sys_write,%function
raf_sys_write:
    mov r7, #4
    svc #0
    bx lr
.size raf_sys_write, .-raf_sys_write

.section .text.raf_sys_exit,"ax",%progbits
.global raf_sys_exit
.hidden raf_sys_exit
.type raf_sys_exit,%function
raf_sys_exit:
    mov r7, #1
    svc #0
    udf #0
.size raf_sys_exit, .-raf_sys_exit

.section .note.GNU-stack,"",%progbits
