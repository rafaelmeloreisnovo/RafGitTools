#include "../pirc2_token_l0.h"
/* Host-linked assertions are test adapters, never runtime dependencies. */
static int check(const unsigned char *ptr, raf_pirc2_u32 count,
                 raf_pirc2_u32 present, raf_pirc2_u32 kind,
                 raf_pirc2_u32 flags, raf_pirc2_u32 scanned) {
  raf_pirc2_scan_result r = raf_pirc2_scan(ptr, count, present);
  return !(r.kind == kind && r.flags == flags &&
           r.bytes_examined == scanned && r.semantic_level == RAF_PIRC2_N5);
}
int main(void) {
  static const unsigned char zero_byte[] = {0u};
  static const unsigned char embedded[] = {'a', 0u, 'b'};
  static const unsigned char ws[] = {' ', ' ', ' ', ' '};
  static const unsigned char num_zero[] = {'0'};
  static const unsigned char nul_word[] = {'N','U','L','L'};
  static const unsigned char molho[] = {'m','o','l','h','o'};
  static const unsigned char unicode[] = {0xc3u, 0xa7u, ' ', 0xe9u, 0xbeu, 0x8du};
  static const unsigned char bad_overlong[] = {0xc0u, 0xafu};
  static const unsigned char bad_cont[] = {0x80u};
  static const unsigned char bad_short[] = {0xe2u, 0x82u};
  static const unsigned char bad_surrogate[] = {0xedu, 0xa0u, 0x80u};
  static const unsigned char bad_high[] = {0xf4u, 0x90u, 0x80u, 0x80u};
  static const unsigned char max_scalar[] = {0xf4u, 0x8fu, 0xbfu, 0xbfu};
  static unsigned char many[RAF_PIRC2_MAX_BYTES];
  int errors = 0;
  errors += check(0, 0u, 0u, RAF_PIRC2_ABSENT, 0u, 0u);
  errors += check(0, 0u, 1u, RAF_PIRC2_EMPTY, 0u, 0u);
  errors += check(0, 1u, 1u, RAF_PIRC2_INVALID_INPUT, 0u, 0u);
  errors += check(0, 0u, 2u, RAF_PIRC2_INVALID_INPUT, 0u, 0u);
  errors += check(ws, 4u, 1u, RAF_PIRC2_ASCII_SPACE, 0u, 4u);
  errors += check(num_zero, 1u, 1u, RAF_PIRC2_TEXT, 0u, 1u);
  errors += check(nul_word, 4u, 1u, RAF_PIRC2_TEXT, 0u, 4u);
  errors += check(zero_byte, 1u, 1u, RAF_PIRC2_NUL_CONTAINING,
                  RAF_PIRC2_HAS_NUL, 1u);
  errors += check(embedded, 3u, 1u, RAF_PIRC2_NUL_CONTAINING,
                  RAF_PIRC2_HAS_NUL, 3u);
  errors += check(molho, 5u, 1u, RAF_PIRC2_TEXT, 0u, 5u);
  errors += check(unicode, 6u, 1u, RAF_PIRC2_TEXT,
                  RAF_PIRC2_HAS_NONASCII, 6u);
  errors += check(bad_overlong, 2u, 1u, RAF_PIRC2_INVALID_UTF8,
                  RAF_PIRC2_HAS_NONASCII, 0u);
  errors += check(bad_cont, 1u, 1u, RAF_PIRC2_INVALID_UTF8,
                  RAF_PIRC2_HAS_NONASCII, 0u);
  errors += check(bad_short, 2u, 1u, RAF_PIRC2_INVALID_UTF8,
                  RAF_PIRC2_HAS_NONASCII, 0u);
  errors += check(bad_surrogate, 3u, 1u, RAF_PIRC2_INVALID_UTF8,
                  RAF_PIRC2_HAS_NONASCII, 0u);
  errors += check(bad_high, 4u, 1u, RAF_PIRC2_INVALID_UTF8,
                  RAF_PIRC2_HAS_NONASCII, 0u);
  errors += check(max_scalar, 4u, 1u, RAF_PIRC2_TEXT,
                  RAF_PIRC2_HAS_NONASCII, 4u);
  errors += check(many, RAF_PIRC2_MAX_BYTES + 1u, 1u,
                  RAF_PIRC2_LIMIT_EXCEEDED, 0u, 0u);
  errors += check(many, RAF_PIRC2_MAX_BYTES, 1u,
                  RAF_PIRC2_NUL_CONTAINING, RAF_PIRC2_HAS_NUL,
                  RAF_PIRC2_MAX_BYTES);
  return errors;
}
