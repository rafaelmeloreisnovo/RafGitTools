.arch armv8-a

.section .text._start,"ax",%progbits
.global _start
.hidden _start
.type _start,%function
_start:
    bl raf_route_entry
    brk #0
.size _start, .-_start

.section .text.raf_route_sys_read,"ax",%progbits
.global raf_route_sys_read
.hidden raf_route_sys_read
.type raf_route_sys_read,%function
raf_route_sys_read:
    mov x8, #63
    svc #0
    ret
.size raf_route_sys_read, .-raf_route_sys_read

.section .text.raf_route_sys_write,"ax",%progbits
.global raf_route_sys_write
.hidden raf_route_sys_write
.type raf_route_sys_write,%function
raf_route_sys_write:
    mov x8, #64
    svc #0
    ret
.size raf_route_sys_write, .-raf_route_sys_write

.section .text.raf_route_sys_exit,"ax",%progbits
.global raf_route_sys_exit
.hidden raf_route_sys_exit
.type raf_route_sys_exit,%function
raf_route_sys_exit:
    mov x8, #93
    svc #0
    brk #0
.size raf_route_sys_exit, .-raf_route_sys_exit

.section .note.GNU-stack,"",%progbits
