.syntax unified
.text
.align 2
.global rafinv_v1_path_components_arm32
.type rafinv_v1_path_components_arm32, %function
rafinv_v1_path_components_arm32:
    cmp r0, #0
    beq .Lrafinv32_zero
    cmp r1, #0
    beq .Lrafinv32_zero
    mov r2, #1
.Lrafinv32_loop:
    ldrb r3, [r0], #1
    cmp r3, #47
    addeq r2, r2, #1
    subs r1, r1, #1
    bne .Lrafinv32_loop
    mov r0, r2
    bx lr
.Lrafinv32_zero:
    mov r0, #0
    bx lr
.size rafinv_v1_path_components_arm32, .-rafinv_v1_path_components_arm32
