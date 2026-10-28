/* RAFAELIA Ω SEMCHAOS V1 — freestanding typed-token core.
 * Source status: experimental; no libc, allocation, I/O or implicit normalization.
 * This is NOT a tokenizer/transformer or cryptographic hash.
 */
#ifndef RAFAELIA_OMEGA_TOKEN_CORE_H
#define RAFAELIA_OMEGA_TOKEN_CORE_H
typedef unsigned char raf_u8;
typedef unsigned int raf_u32;
typedef char raf_require_u32_is_32bit[(sizeof(raf_u32)==4)?1:-1];
enum raf_token_kind {
 RAF_EMPTY_GLYPH=1, RAF_EMPTY_STRING=2, RAF_SPACES_ONLY=3,
 RAF_NUL_BYTE=4, RAF_JSON_NULL=5, RAF_MISSING_FIELD=6,
 RAF_ZERO_NUMBER=7, RAF_RAW_TEXT=8, RAF_UNKNOWN=9,
 RAF_NOISE=10, RAF_ABORTED=11, RAF_REVERSE=12, RAF_INVERSE=13,
 RAF_IGNORED=14, RAF_UNTOKENIZED=15, RAF_CENSORED=16,
 RAF_ANTIDERIVED=17, RAF_ANTITENSOR=18, RAF_HEURISTIC=19,
 RAF_INVARIANT=20, RAF_UNWEIGHED=21, RAF_UNARY=22, RAF_ZERO_ORDER=23,
 RAF_RECLUSE=24, RAF_INDETERMINATE=25, RAF_DETERMINATE=26
};
typedef struct raf_token_view {
 raf_u32 kind, provenance_id, original_order, len;
 const raf_u8 *data;
} raf_token_view;
int raf_token_validate(const raf_token_view *t);
raf_u32 raf_token_fingerprint32(const raf_token_view *t);
#endif
