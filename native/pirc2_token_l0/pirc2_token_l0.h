#ifndef RAFAELIA_PIRC2_TOKEN_L0_H
#define RAFAELIA_PIRC2_TOKEN_L0_H
/* Pure byte view: no stdlib, malloc, JNI, OS APIs, or runtime allocation. */
typedef unsigned int raf_pirc2_u32;
enum raf_pirc2_kind {
  RAF_PIRC2_ABSENT = 1,
  RAF_PIRC2_EMPTY = 2,
  RAF_PIRC2_ASCII_SPACE = 3,
  RAF_PIRC2_TEXT = 4,
  RAF_PIRC2_NUL_CONTAINING = 5,
  RAF_PIRC2_INVALID_UTF8 = 6,
  RAF_PIRC2_INVALID_INPUT = 7,
  RAF_PIRC2_LIMIT_EXCEEDED = 8
};
enum {
  RAF_PIRC2_HAS_NUL = 1u,
  RAF_PIRC2_HAS_NONASCII = 2u
};
enum { RAF_PIRC2_MAX_BYTES = 4096u, RAF_PIRC2_N5 = 5u };
typedef struct {
  raf_pirc2_u32 kind;
  raf_pirc2_u32 bytes_examined;
  raf_pirc2_u32 flags;
  raf_pirc2_u32 semantic_level;
} raf_pirc2_scan_result;
/*
 * present=0: field absent (not the same as count=0).
 * present=1: field exists. bytes must reference at least count readable bytes
 * when count>0. No pointer length can be proven in portable C.
 * This function NEVER infers a lexical sense, provenance, truth, or translation.
 */
raf_pirc2_scan_result raf_pirc2_scan(
    const unsigned char *bytes, raf_pirc2_u32 count, raf_pirc2_u32 present);
#endif
