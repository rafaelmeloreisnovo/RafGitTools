#include "pirc2_token_l0.h"

static raf_pirc2_scan_result raf_pirc2_result(
    raf_pirc2_u32 kind, raf_pirc2_u32 count, raf_pirc2_u32 flags) {
  raf_pirc2_scan_result out;
  out.kind = kind;
  out.bytes_examined = count;
  out.flags = flags;
  out.semantic_level = RAF_PIRC2_N5; /* No source evidence in a byte sequence. */
  return out;
}
static raf_pirc2_u32 raf_pirc2_cont(unsigned char byte) {
  return ((byte & 0xc0u) == 0x80u) ? 1u : 0u;
}
raf_pirc2_scan_result raf_pirc2_scan(
    const unsigned char *bytes, raf_pirc2_u32 count, raf_pirc2_u32 present) {
  raf_pirc2_u32 i = 0u, flags = 0u, only_space = 1u;
  if (present == 0u) return raf_pirc2_result(RAF_PIRC2_ABSENT, 0u, 0u);
  if (present != 1u) return raf_pirc2_result(RAF_PIRC2_INVALID_INPUT, 0u, 0u);
  if (count > RAF_PIRC2_MAX_BYTES)
    return raf_pirc2_result(RAF_PIRC2_LIMIT_EXCEEDED, 0u, 0u);
  if (count == 0u) return raf_pirc2_result(RAF_PIRC2_EMPTY, 0u, 0u);
  if (bytes == (const unsigned char *)0)
    return raf_pirc2_result(RAF_PIRC2_INVALID_INPUT, 0u, 0u);

  while (i < count) {
    unsigned char b = bytes[i];
    if (b == 0u) { flags |= RAF_PIRC2_HAS_NUL; only_space = 0u; ++i; continue; }
    if (b < 0x80u) {
      if (b != 32u && (b < 9u || b > 13u)) only_space = 0u;
      ++i;
      continue;
    }
    flags |= RAF_PIRC2_HAS_NONASCII;
    only_space = 0u;
    /* Strict UTF-8: reject overlong forms, surrogates and > U+10FFFF. */
    {
      raf_pirc2_u32 remain = count - i, width = 0u;
      unsigned char b1 = 0u;
      if (b >= 0xc2u && b <= 0xdfu) width = 2u;
      else if (b >= 0xe0u && b <= 0xefu) width = 3u;
      else if (b >= 0xf0u && b <= 0xf4u) width = 4u;
      if (width == 0u || remain < width)
        return raf_pirc2_result(RAF_PIRC2_INVALID_UTF8, i, flags);
      b1 = bytes[i+1u];
      if (!raf_pirc2_cont(b1))
        return raf_pirc2_result(RAF_PIRC2_INVALID_UTF8, i, flags);
      if ((b == 0xe0u && b1 < 0xa0u) ||
          (b == 0xedu && b1 > 0x9fu) ||
          (b == 0xf0u && b1 < 0x90u) ||
          (b == 0xf4u && b1 > 0x8fu))
        return raf_pirc2_result(RAF_PIRC2_INVALID_UTF8, i, flags);
      if (width >= 3u && !raf_pirc2_cont(bytes[i+2u]))
        return raf_pirc2_result(RAF_PIRC2_INVALID_UTF8, i, flags);
      if (width == 4u && !raf_pirc2_cont(bytes[i+3u]))
        return raf_pirc2_result(RAF_PIRC2_INVALID_UTF8, i, flags);
      i += width;
    }
  }
  if ((flags & RAF_PIRC2_HAS_NUL) != 0u)
    return raf_pirc2_result(RAF_PIRC2_NUL_CONTAINING, count, flags);
  if (only_space != 0u)
    return raf_pirc2_result(RAF_PIRC2_ASCII_SPACE, count, flags);
  return raf_pirc2_result(RAF_PIRC2_TEXT, count, flags);
}
