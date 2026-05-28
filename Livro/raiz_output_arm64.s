// RAFAELIA ROOT C→ASM — ARM64/AArch64 GNU as
.text
.global optimized_function
.type optimized_function, %function
optimized_function:
    mov x9, #10 // uid=1 line=2 h=5282148ee7815e18
    mov x10, x9 // uid=2 line=3 h=39ebe1359dca0dce
    add x10, x10, #5 // uid=3 line=3 h=9e203e32266acef2
    mov x11, x10 // uid=4 line=5 h=a81724331894d0da
    mov x12, x10 // uid=6 line=7 h=d4885251d3c932da
    add x11, x11, #10 // uid=5 line=5 h=e22d715116eb05a2
    sub x12, x12, #5 // uid=7 line=7 h=a2e308d10292d46e
    cmp x10, #20 // uid=8 line=4 h=115f5f4ee8963e9c
    csel x13, x11, x12, lt // uid=9 line=4 h=b71f7dd607afbbc1
    mov x14, x13 // uid=10 line=9 h=a07a8ee2dcac9140
    add x14, x14, x9 // uid=11 line=9 h=6a5cfaae4f075919
    ret
.size optimized_function, .-optimized_function
