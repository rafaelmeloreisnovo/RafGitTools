#ifndef RAFGIT_CI_WIRE_VERIFY_H
#define RAFGIT_CI_WIRE_VERIFY_H
/* RafGitTools authorial independent consumer of RAFAELIA CI wire/v2.
 * Public wire layout contract only: does NOT import RafPolimata source.
 * No libc, heap, syscalls, runtime or third-party code; caller owns 40 bytes.
 * Wire input is UNTRUSTED. CRC32C detects accidental corruption, NOT origin.
 * Fail-closed states: INVALID=-1, HOLD=2, REVIEW=1; NONE IS CI PASS.
 */
#define RAFGIT_CI_WIRE_BYTES 40u
#define RAFGIT_CI_HOLD_MASK (2u|4u|16u|64u|128u|1024u|2048u)
#define RAFGIT_CI_UNKNOWN_MASK (~(1u|2u|4u|8u|16u|32u|64u|128u|256u|512u|1024u|2048u))
#if defined(__GNUC__) || defined(__clang__)
#define RAFGIT_CI_INLINE static __inline__ __attribute__((always_inline))
#else
#define RAFGIT_CI_INLINE static inline
#endif
_Static_assert(sizeof(unsigned int)==4u,"wire requires uint32 ABI");
_Static_assert((unsigned char)256u==0u,"wire requires octets");
RAFGIT_CI_INLINE unsigned int rafgit_ci_u32(const unsigned char *p) {
    return (unsigned int)p[0] | ((unsigned int)p[1]<<8u) |
           ((unsigned int)p[2]<<16u) | ((unsigned int)p[3]<<24u);
}
RAFGIT_CI_INLINE unsigned int rafgit_ci_crc32c(const unsigned char *p,unsigned int n) {
    unsigned int c=0xffffffffu,i,j;
    for(i=0u;i<n;++i){
        c^=p[i];
        for(j=0u;j<8u;++j) c=(c>>1u)^(0x82f63b78u & (0u-(c&1u)));
    }
    return ~c;
}
RAFGIT_CI_INLINE int rafgit_ci_wire_verify(const unsigned char *p,unsigned int n) {
    unsigned int flags,state;
    if (!p || n!=RAFGIT_CI_WIRE_BYTES) return -1;
    if(p[0]!='R'||p[1]!='C'||p[2]!='I'||p[3]!='2'||rafgit_ci_u32(p+4)!=2u) return -1;
    if(rafgit_ci_u32(p+36)!=rafgit_ci_crc32c(p,36u)) return -1;
    flags=rafgit_ci_u32(p+8); state=rafgit_ci_u32(p+24);
    if ((flags & RAFGIT_CI_UNKNOWN_MASK)!=0u || (flags & 256u)==0u) return -1;
    if(rafgit_ci_u32(p+28)!=15u||rafgit_ci_u32(p+32)!=0u) return -1;
    /* No claim that CRC or an untrusted flag field attests to the source. */
    if((flags & RAFGIT_CI_HOLD_MASK)!=0u) return state==2u ? 2:-1;
    return state==1u ? 1:-1;
}
#endif
