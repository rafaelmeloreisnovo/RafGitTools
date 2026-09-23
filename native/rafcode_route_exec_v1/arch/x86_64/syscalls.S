.section .text._start,"ax",@progbits
.global _start
.hidden _start
.type _start,@function
_start:
    andq $-16, %rsp
    call raf_route_entry
    ud2
.size _start, .-_start

.section .text.raf_route_sys_read,"ax",@progbits
.global raf_route_sys_read
.hidden raf_route_sys_read
.type raf_route_sys_read,@function
raf_route_sys_read:
    xorl %eax, %eax
    syscall
    ret
.size raf_route_sys_read, .-raf_route_sys_read

.section .text.raf_route_sys_write,"ax",@progbits
.global raf_route_sys_write
.hidden raf_route_sys_write
.type raf_route_sys_write,@function
raf_route_sys_write:
    movl $1, %eax
    syscall
    ret
.size raf_route_sys_write, .-raf_route_sys_write

.section .text.raf_route_sys_exit,"ax",@progbits
.global raf_route_sys_exit
.hidden raf_route_sys_exit
.type raf_route_sys_exit,@function
raf_route_sys_exit:
    movl $60, %eax
    syscall
    ud2
.size raf_route_sys_exit, .-raf_route_sys_exit

.section .note.GNU-stack,"",@progbits
