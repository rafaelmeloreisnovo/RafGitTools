#include <stddef.h>
int rafaelia_cross_source_match_v1(const unsigned char *, size_t,
                                  const unsigned char *, size_t, size_t *, size_t *);
int main(void) {
    const unsigned char s[] = {'a','b','a','b','a',0,'a'};
    const unsigned char n[] = {'a','b','a'};
    size_t c = 0, o = 99;
    if (rafaelia_cross_source_match_v1(s,7,n,3,&c,&o)!=1 || o!=0 || c!=1) return 1;
    if (rafaelia_cross_source_match_v1(s,7,n,3,&c,&o)!=1 || o!=2 || c!=3) return 2;
    if (rafaelia_cross_source_match_v1(s,7,n,3,&c,&o)!=0 || c!=7) return 3;
    if (rafaelia_cross_source_match_v1(s,7,n,0,&c,&o)!=-1) return 4;
    c=8; if (rafaelia_cross_source_match_v1(s,7,n,3,&c,&o)!=-1) return 5;
    c=0; if (rafaelia_cross_source_match_v1(s,2,n,3,&c,&o)!=0 || c!=2) return 6;
    return 0;
}
