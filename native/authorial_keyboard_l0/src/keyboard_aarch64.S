/* RAFAELIA L0 keyboard. Independent AArch64 register-level design.
 * w0=x, w1=y in pixels; w0=ASCII or 0xffffffff (outside grid).
 * 10 columns x 5 rows, 32 pixels per cell. No platform calls.
 */
.text
.balign 4
.global rafkbd_l0_probe
.type rafkbd_l0_probe, %function
rafkbd_l0_probe:
    cmp w0, #320
    b.hs .Linvalid
    cmp w1, #160
    b.hs .Linvalid
    lsr w2, w0, #5
    lsr w3, w1, #5
    add w3, w3, w3, lsl #2
    add w3, w2, w3, lsl #1
    adr x2, .Lkeys
    ldrb w0, [x2, w3, uxtw]
    ret
.Linvalid:
    mov w0, #-1
    ret
.balign 4
.Lkeys:
.include "layout.inc"
.size rafkbd_l0_probe, . - rafkbd_l0_probe
.section .note.GNU-stack,"",%progbits
