#include "../omega_token_core.h"
/* Pure hosted executable test; core.o remains freestanding. */
int main(void) {
 static const raf_u8 glyph[3]={0xe2u,0x88u,0x85u};
 static const raf_u8 spaces[4]={32u,32u,32u,32u};
 static const raf_u8 nul[1]={0u};
 static const raf_u8 letter[1]={65u};
 raf_token_view t={RAF_EMPTY_GLYPH,3u,7u,3u,glyph};
 raf_u32 h;
 if(!raf_token_validate(&t))return 1;
 h=raf_token_fingerprint32(&t);
 t.kind=RAF_EMPTY_STRING;t.len=0u;t.data=0;
 if(!raf_token_validate(&t) || raf_token_fingerprint32(&t)==h)return 2;
 t.kind=RAF_SPACES_ONLY;t.len=4;t.data=spaces;
 if(!raf_token_validate(&t))return 3;
 t.kind=RAF_NUL_BYTE;t.len=1;t.data=nul;
 if(!raf_token_validate(&t))return 4;
 t.kind=RAF_JSON_NULL;t.len=0;t.data=0;
 if(!raf_token_validate(&t))return 5;
 t.kind=RAF_MISSING_FIELD;
 if(!raf_token_validate(&t))return 6;
 t.kind=RAF_ZERO_NUMBER;
 if(!raf_token_validate(&t))return 7;
 t.kind=RAF_SPACES_ONLY;t.len=1;t.data=letter;
 if(raf_token_validate(&t))return 8;
 t.kind=RAF_NUL_BYTE;t.len=1;t.data=letter;
 if(raf_token_validate(&t))return 9;
 t.kind=RAF_EMPTY_STRING;t.len=1;
 if(raf_token_validate(&t))return 10;
 t.kind=RAF_EMPTY_GLYPH;t.len=2;t.data=glyph;
 if(raf_token_validate(&t))return 11;
 t.kind=RAF_RAW_TEXT;t.len=1;t.data=letter;
 if(!raf_token_validate(&t))return 12;
 t.kind=99u;
 if(raf_token_validate(&t))return 13;
 return 0;
}
