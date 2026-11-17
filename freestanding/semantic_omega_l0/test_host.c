/* Hosted test driver only. The separate omega_l0.o is freestanding. */
#include "omega_l0.h"
#define A(p,n,k,present,scalar) ((raf_omega_atom){{(const raf_byte *)(p),(n)},(k),(present),(scalar),0})
#define CHECK(q) do { if(!(q))return __LINE__; } while(0)
int main(void) {
    static const raf_byte spaces[4]={' ',' ',' ',' '};
    static const raf_byte text_null[]="NULL";
    static const raf_byte z[]="-0.000e-19";
    static const raf_byte other[]="20.5e4";
    static const raf_byte wrong[]="01";
    static const raf_byte molho[]="molho";
    static const raf_byte utf8[]="\xE9\xBE\x8D"; /* glyph 龍; no translation */
    raf_omega_atom a;
    raf_pirc_l0 c;
    raf_span s={(const raf_byte*)molho,5};
    CHECK(raf_omega_classify_v1(0)==RAFO_INVALID);
    a=A(0,0,RAFK_ABSENT,0,0);CHECK(raf_omega_classify_v1(&a)==RAFO_ABSENT);
    a=A(0,0,RAFK_JSON_NULL,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_NULL);
    a=A(0,0,RAFK_STRING,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_EMPTY);
    a=A(spaces,4,RAFK_STRING,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_SPACES);
    a=A(text_null,4,RAFK_STRING,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_TEXT);
    a=A(z,10,RAFK_JSON_NUMBER,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_NUM_ZERO);
    a=A(other,6,RAFK_JSON_NUMBER,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_NUM_NONZERO);
    a=A(wrong,2,RAFK_JSON_NUMBER,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_INVALID);
    a=A(0,0,RAFK_JSON_BOOL,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_BOOL_FALSE);
    a=A(0,0,RAFK_JSON_BOOL,1,1);CHECK(raf_omega_classify_v1(&a)==RAFO_BOOL_TRUE);
    a=A(0,0,RAFK_TOKEN_UNRESOLVED,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_UNKNOWN);
    a=A(0,0,RAFK_TOKEN_ABORTED,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_ABORTED);
    a=A(0,1,RAFK_STRING,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_INVALID);
    a=A(spaces,4,RAFK_ABSENT,0,0);CHECK(raf_omega_classify_v1(&a)==RAFO_INVALID);
    a=A(0,0,RAFK_JSON_BOOL,1,2);CHECK(raf_omega_classify_v1(&a)==RAFO_INVALID);
    a=A(utf8,3,RAFK_STRING,1,0);CHECK(raf_omega_classify_v1(&a)==RAFO_TEXT);
    CHECK(raf_omega_molho_v1(s,RAFC_NONE,1)==RAFS_AMBIGUOUS);
    CHECK(raf_omega_molho_v1(s,RAFC_FOOD_SAUCE,0)==RAFS_AMBIGUOUS);
    CHECK(raf_omega_molho_v1(s,RAFC_FOOD_SAUCE,1)==RAFS_FOOD);
    CHECK(raf_omega_molho_v1(s,RAFC_KEY_BUNDLE,1)==RAFS_KEYS);
    CHECK(raf_omega_molho_v1(s,RAFC_WET_VERB,1)==RAFS_WET);
    s.p=utf8;s.n=3;CHECK(raf_omega_molho_v1(s,RAFC_FOOD_SAUCE,1)==RAFS_UNMATCHED);
    c=(raf_pirc_l0){5,0,0,0};CHECK(raf_pirc_preflight_v1(&c)==0);
    c=(raf_pirc_l0){0,0,0,0};CHECK((raf_pirc_preflight_v1(&c)&RAFV_EVIDENCE_ABSENT)!=0);
    c=(raf_pirc_l0){3,0,0,0};CHECK((raf_pirc_preflight_v1(&c)&RAFV_FALSIFIER_ABSENT)!=0);
    c=(raf_pirc_l0){5,0,0,1};CHECK((raf_pirc_preflight_v1(&c)&RAFV_N5_ASSERTION)!=0);
    c=(raf_pirc_l0){0,1,1,1};CHECK((raf_pirc_preflight_v1(&c)&RAFV_UNSUPPORTED_PROMOTION)!=0);
    c=(raf_pirc_l0){6,0,0,0};CHECK((raf_pirc_preflight_v1(&c)&RAFV_LEVEL)!=0);
    c=(raf_pirc_l0){2,1,0,0};CHECK(raf_pirc_preflight_v1(&c)==0);
    return 0;
}
