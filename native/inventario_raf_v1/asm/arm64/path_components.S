.text
.align 2
.global rafinv_v1_path_components_arm64
.type rafinv_v1_path_components_arm64, %function
rafinv_v1_path_components_arm64:
    cbz x0, .Lrafinv64_zero
    cbz x1, .Lrafinv64_zero
    mov w2, #1
.Lrafinv64_loop:
    ldrb w3, [x0], #1
    cmp w3, #47
    cinc w2, w2, eq
    subs x1, x1, #1
    b.ne .Lrafinv64_loop
    mov w0, w2
    ret
.Lrafinv64_zero:
    mov w0, wzr
    ret
.size rafinv_v1_path_components_arm64, .-rafinv_v1_path_components_arm64
