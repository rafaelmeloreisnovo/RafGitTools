#include "omega_token_core.h"
/* FNV-1a fingerprint. Not collision-resistant and not a provenance hash.
 * SHA-256 of the complete host receipt is recorded by the hosted gate.
 */
static raf_u32 mix(raf_u32 h,raf_u32 x) {
 raf_u32 i;
 for(i=0;i<4;i++){ h=(h^(x&255u))*16777619u; x>>=8; }
 return h;
}
int raf_token_validate(const raf_token_view *t) {
 raf_u32 i;
 if(!t || (t->len && !t->data) || t->len>1048576u) return 0;
 switch(t->kind) {
  case RAF_EMPTY_GLYPH:
    return t->len==3u && t->data[0]==0xe2u && t->data[1]==0x88u && t->data[2]==0x85u;
  case RAF_EMPTY_STRING: case RAF_JSON_NULL: case RAF_MISSING_FIELD: case RAF_ZERO_NUMBER:
    return t->len==0u;
  case RAF_SPACES_ONLY:
    if(!t->len)return 0;
    for(i=0;i<t->len;i++)if(t->data[i]!=0x20u)return 0;
    return 1;
  case RAF_NUL_BYTE: return t->len==1u && t->data[0]==0u;
  case RAF_RAW_TEXT: case RAF_UNKNOWN: case RAF_NOISE: case RAF_ABORTED:
  case RAF_REVERSE: case RAF_INVERSE: case RAF_IGNORED: case RAF_UNTOKENIZED:
  case RAF_CENSORED: case RAF_ANTIDERIVED: case RAF_ANTITENSOR:
  case RAF_HEURISTIC: case RAF_INVARIANT: case RAF_UNWEIGHED:
  case RAF_UNARY: case RAF_ZERO_ORDER: case RAF_RECLUSE:
  case RAF_INDETERMINATE: case RAF_DETERMINATE:
    return t->len>0u;
  default: return 0;
 }
}
raf_u32 raf_token_fingerprint32(const raf_token_view *t) {
 raf_u32 h=2166136261u,i;
 if(!raf_token_validate(t))return 0u;
 h=mix(h,t->kind);h=mix(h,t->provenance_id);
 h=mix(h,t->original_order);h=mix(h,t->len);
 for(i=0;i<t->len;i++)h=(h^t->data[i])*16777619u;
 return h;
}
