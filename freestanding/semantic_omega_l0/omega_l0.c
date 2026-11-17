#include "omega_l0.h"

static raf_u32 is_digit(raf_byte c) { return c >= '0' && c <= '9'; }
static raf_u32 is_space(raf_byte c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
}
/* Complete JSON number lexer; identify numerical zero without arithmetic,
 * overflow, libc or floating-point rounding. 1=zero,2=nonzero,0=invalid.
 */
static raf_u32 number_kind(raf_span x) {
    raf_u32 i=0, zero=1, frac=0, exp=0;
    if (!x.p || !x.n) return 0;
    if (x.p[i]=='-') { ++i; if(i==x.n)return 0; }
    if (x.p[i]=='0') { ++i; if(i<x.n && is_digit(x.p[i]))return 0; }
    else if (x.p[i]>='1' && x.p[i]<='9') {
        zero=0;
        do { ++i; } while (i<x.n && is_digit(x.p[i]));
    } else return 0;
    if (i<x.n && x.p[i]=='.') {
        ++i;
        while(i<x.n && is_digit(x.p[i])) {
            ++frac;
            if(x.p[i]!='0')zero=0;
            ++i;
        }
        if(!frac)return 0;
    }
    if(i<x.n && (x.p[i]=='e'||x.p[i]=='E')) {
        ++i;
        if(i<x.n && (x.p[i]=='+' || x.p[i]=='-'))++i;
        while(i<x.n && is_digit(x.p[i])) { ++exp; ++i; }
        if(!exp)return 0;
    }
    return i==x.n ? (zero ? 1u : 2u) : 0u;
}
raf_u32 raf_omega_classify_v1(const raf_omega_atom *a) {
    raf_u32 i, numeric;
    if(!a || a->reserved || a->present>1 || a->scalar>1 || a->kind>RAFK_TOKEN_ABORTED)
        return RAFO_INVALID;
    if(a->bytes.n && !a->bytes.p)return RAFO_INVALID;
    if(!a->present) {
        return a->kind==RAFK_ABSENT && !a->bytes.n && !a->bytes.p && !a->scalar
            ? RAFO_ABSENT : RAFO_INVALID;
    }
    if(a->kind==RAFK_ABSENT) return RAFO_INVALID;
    if(a->kind!=RAFK_JSON_BOOL && a->scalar)return RAFO_INVALID;
    if(a->kind==RAFK_JSON_BOOL) {
        return a->bytes.n || a->bytes.p ? RAFO_INVALID :
               (a->scalar ? RAFO_BOOL_TRUE : RAFO_BOOL_FALSE);
    }
    if(a->kind==RAFK_JSON_NULL || a->kind==RAFK_TOKEN_UNRESOLVED ||
       a->kind==RAFK_TOKEN_ABORTED) {
        if(a->bytes.n || a->bytes.p) return RAFO_INVALID;
        return a->kind==RAFK_JSON_NULL ? RAFO_NULL :
               a->kind==RAFK_TOKEN_UNRESOLVED ? RAFO_UNKNOWN : RAFO_ABORTED;
    }
    if(a->kind==RAFK_JSON_NUMBER) {
        numeric=number_kind(a->bytes);
        return numeric==1u ? RAFO_NUM_ZERO :
               numeric==2u ? RAFO_NUM_NONZERO : RAFO_INVALID;
    }
    if(a->kind!=RAFK_STRING)return RAFO_INVALID;
    if(!a->bytes.n)return RAFO_EMPTY;
    for(i=0;i<a->bytes.n;++i)if(!is_space(a->bytes.p[i]))return RAFO_TEXT;
    return RAFO_SPACES;
}
raf_u32 raf_omega_molho_v1(raf_span lexeme,raf_u32 context,
                            raf_u32 authority_checked) {
    if(lexeme.n && !lexeme.p)return RAFS_UNMATCHED;
    if(lexeme.n!=5 || lexeme.p[0]!='m'||lexeme.p[1]!='o'||
       lexeme.p[2]!='l'||lexeme.p[3]!='h'||lexeme.p[4]!='o')
        return RAFS_UNMATCHED;
    if(authority_checked!=1u || context==RAFC_NONE || context>RAFC_WET_VERB)
        return RAFS_AMBIGUOUS;
    return context==RAFC_FOOD_SAUCE ? RAFS_FOOD :
           context==RAFC_KEY_BUNDLE ? RAFS_KEYS : RAFS_WET;
}
raf_u32 raf_pirc_preflight_v1(const raf_pirc_l0 *c) {
    raf_u32 v=0;
    if(!c)return RAFV_LEVEL;
    if(c->level>5)v|=RAFV_LEVEL;
    if(c->evidence_present>1 || c->falsifier_present>1 || c->claim_allowed>1)
        v|=RAFV_BAD_BOOLEAN;
    if(c->claim_allowed)v|=RAFV_UNSUPPORTED_PROMOTION;
    if(c->level<=2 && !c->evidence_present)v|=RAFV_EVIDENCE_ABSENT;
    if(c->level==3 && !c->falsifier_present)v|=RAFV_FALSIFIER_ABSENT;
    if(c->level==5 && (c->evidence_present || c->claim_allowed))v|=RAFV_N5_ASSERTION;
    return v;
}
