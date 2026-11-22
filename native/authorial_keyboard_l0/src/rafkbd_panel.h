/* Authorial keyboard L1: caller-owned memory; zero libc/heap/platform ABI. */
#ifndef RAFKBD_PANEL_H
#define RAFKBD_PANEL_H
typedef unsigned int raf_u32;
raf_u32 rafkbd_l0_probe(raf_u32, raf_u32);
void rafkbd_l1_edit(unsigned char *, raf_u32, raf_u32 *, raf_u32);
void rafkbd_l1_paint(raf_u32 *, raf_u32, raf_u32, raf_u32, const unsigned char *, raf_u32);
#endif
