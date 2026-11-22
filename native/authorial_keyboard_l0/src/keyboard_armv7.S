/* RAFAELIA L0 keyboard. Independent ARMv7 register-level design.
 * r0=x, r1=y in pixels; r0=ASCII or 0xffffffff (outside grid).
 * 10 columns x 5 rows, 32 pixels per cell. No platform calls.
 */
.syntax unified
.arm
.text
.balign 4
.global rafkbd_l0_probe
.type rafkbd_l0_probe, %function
rafkbd_l0_probe:
    cmp r0, #320
    bhs .Linvalid
    cmp r1, #160
    bhs .Linvalid
    lsr r2, r0, #5
    lsr r3, r1, #5
    add r3, r3, r3, lsl #2
    add r3, r2, r3, lsl #1
    adr r2, .Lkeys
    ldrb r0, [r2, r3]
    bx lr
.Linvalid:
    mvn r0, #0
    bx lr
.balign 4
.Lkeys:
.include "layout.inc"
.size rafkbd_l0_probe, . - rafkbd_l0_probe
.section .note.GNU-stack,"",%progbits
