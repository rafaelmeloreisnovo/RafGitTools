#ifndef RAFGIT_CI_G4_VERIFY_H
#define RAFGIT_CI_G4_VERIFY_H
/* RafGitTools independent authorial RCG4/32-byte reader.
 * No source-copy/parser dependency on RafPolimata. No libc/malloc/syscalls.
 * Return: -1 malformed, 2 structure sane BUT SOURCE_UNVERIFIED.
 * It is impossible to return PASS or authenticate GitHub Actions from here.
 */
#include "rafgit_ci_wire_verify.h"
#define RAFGIT_CI_G4_BYTES 32u
#define RAFGIT_CI_G4_UNVERIFIED 2
#define RAFGIT_CI_G4_INVALID (-1)
#define RAFGIT_CI_G4_VOID_ALL 127u
#define RAFGIT_CI_G4_KNOWN_GAPS 255u
RAFGIT_CI_INLINE int rafgit_ci_g4_verify(const unsigned char *p,unsigned int n){
    unsigned int state,urgency,gaps,voids;
    if(!p || n!=RAFGIT_CI_G4_BYTES)return RAFGIT_CI_G4_INVALID;
    if(p[0]!='R'||p[1]!='C'||p[2]!='G'||p[3]!='4'||rafgit_ci_u32(p+4)!=4u)return RAFGIT_CI_G4_INVALID;
    if(rafgit_ci_u32(p+28)!=rafgit_ci_crc32c(p,28u))return RAFGIT_CI_G4_INVALID;
    state=rafgit_ci_u32(p+8);urgency=rafgit_ci_u32(p+12);
    gaps=rafgit_ci_u32(p+16);voids=rafgit_ci_u32(p+20);
    if(voids!=RAFGIT_CI_G4_VOID_ALL || (gaps & ~RAFGIT_CI_G4_KNOWN_GAPS)!=0u)return RAFGIT_CI_G4_INVALID;
    if(urgency>1u || state<1u || state>3u)return RAFGIT_CI_G4_INVALID;
    if(state==1u && (gaps!=0u || urgency!=1u))return RAFGIT_CI_G4_INVALID;
    if(state==2u && gaps==0u)return RAFGIT_CI_G4_INVALID;
    if(state==3u && ((gaps&16u)==0u || urgency!=0u))return RAFGIT_CI_G4_INVALID;
    /* Fingerprint is neither authenticated nor sufficient to verify source. */
    return RAFGIT_CI_G4_UNVERIFIED;
}
#endif
