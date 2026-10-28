/* RafGitTools selftest: independent RCG4 reader, no upstream producer. */
#include "rafgit_ci_g4_verify.h"
#define CHECK(x) if(!(x))return __LINE__
static unsigned char out[32];
static void put(unsigned int index,unsigned int x){
    out[index]=(unsigned char)x;out[index+1u]=(unsigned char)(x>>8u);
    out[index+2u]=(unsigned char)(x>>16u);out[index+3u]=(unsigned char)(x>>24u);
}
static void fixcrc(void){put(28u,rafgit_ci_crc32c(out,28u));}
static int test(void){unsigned int i;
    out[0]='R';out[1]='C';out[2]='G';out[3]='4';
    put(4u,4u);put(8u,1u);put(12u,1u);put(16u,0u);
    put(20u,127u);put(24u,0x12345678u);fixcrc();
    CHECK(rafgit_ci_g4_verify(out,32u)==2);
    CHECK(rafgit_ci_g4_verify(out,31u)==-1);
    CHECK(rafgit_ci_g4_verify(0,32u)==-1);
    for(i=0u;i<32u;++i){unsigned char old=out[i];out[i]^=1u;
        CHECK(rafgit_ci_g4_verify(out,32u)==-1);out[i]=old;}
    /* Altering gaps while recomputing CRC cannot forge structural REVIEW. */
    put(16u,1u);fixcrc();CHECK(rafgit_ci_g4_verify(out,32u)==-1);
    put(8u,2u);fixcrc();CHECK(rafgit_ci_g4_verify(out,32u)==2);
    put(8u,3u);put(12u,0u);put(16u,16u);fixcrc();
    CHECK(rafgit_ci_g4_verify(out,32u)==2);
    put(16u,0u);fixcrc();CHECK(rafgit_ci_g4_verify(out,32u)==-1);
    return 0;
}
int main(void){return test();}
