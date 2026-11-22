#include "../pirc2_token_l0.h"
#include <stdio.h> /* Hosted harness ONLY, never linked into the core. */
/* Hosted falsifier, not part of the freestanding target image. */
static unsigned int checks;
static unsigned int failures;
static void expect(const unsigned char *p, raf_pirc2_u32 n,
                   raf_pirc2_u32 present, raf_pirc2_u32 kind,
                   raf_pirc2_u32 flags, raf_pirc2_u32 examined)
{
  raf_pirc2_scan_result result = raf_pirc2_scan(p, n, present);
  ++checks;
  if (result.kind != kind || result.flags != flags ||
      result.bytes_examined != examined || result.semantic_level != RAF_PIRC2_N5)
    ++failures;
}
int main(void)
{
  unsigned char buf[RAF_PIRC2_MAX_BYTES + 1u];
  unsigned int i, j;
  static const unsigned char molho[] = { 'm','o','l','h','o' };
  static const unsigned char zero[] = { '0' };
  static const unsigned char literal_null[] = { 'N','U','L','L' };
  static const unsigned char spaces[] = { 32u,32u,32u,32u };
  static const unsigned char invalid_after_nul[] = { 0u, 0xc0u, 0xafu };
  static const unsigned char valid_2[] = { 0xc2u, 0x80u };
  static const unsigned char valid_3[] = { 0xe0u, 0xa0u, 0x80u };
  static const unsigned char valid_4[] = { 0xf0u, 0x90u, 0x80u, 0x80u };
  static const unsigned char bad_3[] = { 0xedu, 0xa0u, 0x80u };
  static const unsigned char bad_4[] = { 0xf4u, 0x90u, 0x80u, 0x80u };
  for (i = 0u; i < 256u; ++i) {
    unsigned char one = (unsigned char)i;
    raf_pirc2_u32 kind = RAF_PIRC2_TEXT, flags = 0u, scanned = 1u;
    if (i == 0u) { kind = RAF_PIRC2_NUL_CONTAINING; flags = RAF_PIRC2_HAS_NUL; }
    else if (i == 32u || (i >= 9u && i <= 13u)) kind = RAF_PIRC2_ASCII_SPACE;
    else if (i >= 128u) { kind = RAF_PIRC2_INVALID_UTF8; flags = RAF_PIRC2_HAS_NONASCII; scanned = 0u; }
    expect(&one, 1u, 1u, kind, flags, scanned);
  }
  for (i = 0xc2u; i <= 0xdfu; ++i) {
    unsigned char two[2]; two[0] = (unsigned char)i;
    for (j = 0u; j < 256u; ++j) {
      two[1] = (unsigned char)j;
      expect(two, 2u, 1u,
             (j >= 0x80u && j <= 0xbfu) ? RAF_PIRC2_TEXT : RAF_PIRC2_INVALID_UTF8,
             RAF_PIRC2_HAS_NONASCII, (j >= 0x80u && j <= 0xbfu) ? 2u : 0u);
    }
  }
  expect(valid_2, 2u, 1u, RAF_PIRC2_TEXT, RAF_PIRC2_HAS_NONASCII, 2u);
  expect(valid_3, 3u, 1u, RAF_PIRC2_TEXT, RAF_PIRC2_HAS_NONASCII, 3u);
  expect(valid_4, 4u, 1u, RAF_PIRC2_TEXT, RAF_PIRC2_HAS_NONASCII, 4u);
  expect(bad_3, 3u, 1u, RAF_PIRC2_INVALID_UTF8, RAF_PIRC2_HAS_NONASCII, 0u);
  expect(bad_4, 4u, 1u, RAF_PIRC2_INVALID_UTF8, RAF_PIRC2_HAS_NONASCII, 0u);
  expect(invalid_after_nul, 3u, 1u, RAF_PIRC2_INVALID_UTF8,
         RAF_PIRC2_HAS_NUL | RAF_PIRC2_HAS_NONASCII, 1u);
  expect(molho, 5u, 1u, RAF_PIRC2_TEXT, 0u, 5u); /* N5, not meaning. */
  expect(zero, 1u, 1u, RAF_PIRC2_TEXT, 0u, 1u);
  expect(literal_null, 4u, 1u, RAF_PIRC2_TEXT, 0u, 4u);
  expect(spaces, 4u, 1u, RAF_PIRC2_ASCII_SPACE, 0u, 4u);
  expect((const unsigned char *)0, 0u, 0u, RAF_PIRC2_ABSENT, 0u, 0u);
  expect((const unsigned char *)0, 0u, 1u, RAF_PIRC2_EMPTY, 0u, 0u);
  expect((const unsigned char *)0, 1u, 1u, RAF_PIRC2_INVALID_INPUT, 0u, 0u);
  expect((const unsigned char *)0, 0u, 2u, RAF_PIRC2_INVALID_INPUT, 0u, 0u);
  /* Count limit must be tested BEFORE dereferencing input. */
  expect(zero, RAF_PIRC2_MAX_BYTES + 1u, 1u, RAF_PIRC2_LIMIT_EXCEEDED, 0u, 0u);
  for (i = 0u; i <= RAF_PIRC2_MAX_BYTES; ++i) buf[i] = 32u;
  expect(buf, RAF_PIRC2_MAX_BYTES, 1u, RAF_PIRC2_ASCII_SPACE, 0u, RAF_PIRC2_MAX_BYTES);
  expect(buf, RAF_PIRC2_MAX_BYTES + 1u, 1u, RAF_PIRC2_LIMIT_EXCEEDED, 0u, 0u);
  printf("PIRC2_ADVERSARIAL assertions=%u failures=%u\n", checks, failures);
  return failures != 0u;
}
