/* Standalone RafGitTools consumer: no RafPolimata source required. */
#include "rafgit_ci_wire_verify.h"
#define CHECK(x) if(!(x))return __LINE__
static unsigned char fixture[40]={
 0x52u,0x43u,0x49u,0x32u,0x02u,0x00u,0x00u,0x00u,
 0x21u,0x03u,0x00u,0x00u,0x01u,0x00u,0x00u,0x00u,
 0x03u,0x00u,0x00u,0x00u,0x25u,0xd4u,0x60u,0x69u,
 0x01u,0x00u,0x00u,0x00u,0x0fu,0x00u,0x00u,0x00u,
 0x00u,0x00u,0x00u,0x00u,0x17u,0x66u,0xffu,0x39u,
};
static int test(void){unsigned int i;
 CHECK(rafgit_ci_wire_verify(fixture,40u)==1);
 CHECK(rafgit_ci_wire_verify(0,40u)==-1);
 CHECK(rafgit_ci_wire_verify(fixture,39u)==-1);
 for (i=0u;i<40u;++i){unsigned char old=fixture[i];fixture[i]^=1u;
   CHECK(rafgit_ci_wire_verify(fixture,40u)==-1);fixture[i]=old;}
 fixture[24]=2u; /* MUST recompute CRC to distinguish mismatched status */
 {unsigned int crc=rafgit_ci_crc32c(fixture,36u);
  fixture[36]=(unsigned char)crc;fixture[37]=(unsigned char)(crc>>8);
  fixture[38]=(unsigned char)(crc>>16);fixture[39]=(unsigned char)(crc>>24);}
 CHECK(rafgit_ci_wire_verify(fixture,40u)==-1);
 return 0;}
int main(void){return test();}
