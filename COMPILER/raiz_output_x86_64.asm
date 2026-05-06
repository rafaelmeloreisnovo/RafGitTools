; RAFAELIA ROOT C→ASM — x86-64 NASM/Intel
section .text
global optimized_function
optimized_function:
    push rbp
    mov rbp, rsp
    mov rax, 10 ; uid=1 line=2 h=5282148ee7815e18
    mov rcx, rax ; uid=2 line=3 h=39ebe1359dca0dce
    add rcx, 5 ; uid=3 line=3 h=9e203e32266acef2
    mov rdx, rcx ; uid=4 line=5 h=a81724331894d0da
    mov rsi, rcx ; uid=6 line=7 h=d4885251d3c932da
    add rdx, 10 ; uid=5 line=5 h=e22d715116eb05a2
    sub rsi, 5 ; uid=7 line=7 h=a2e308d10292d46e
    cmp rcx, 20 ; uid=8 line=4 h=115f5f4ee8963e9c
    mov rdi, rsi ; uid=9 line=4 h=b71f7dd607afbbc1
    cmovl rdi, rdx ; uid=9 line=4 h=b71f7dd607afbbc1
    mov r8, rdi ; uid=10 line=9 h=a07a8ee2dcac9140
    add r8, rax ; uid=11 line=9 h=6a5cfaae4f075919
    pop rbp
    ret
