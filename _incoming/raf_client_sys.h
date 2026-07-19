#pragma once
/* raf_client_sys.h — syscalls ARM32/ARM64/x86-64/RISCV64 + arena BSS + I/O
 * nomalloc nolibc noGC — zero stdlib, zero heap, zero abstraction
 * ARM32  : open=5   read=3   write=4   close=6   exit_group=248
 * ARM64  : openat=56 read=63 write=64  close=57  exit_group=94
 * x86-64 : open=2   read=0   write=1   close=3   exit_group=231
 * RISCV64: openat=56 read=63 write=64  close=57  exit_group=94
 */
typedef unsigned char      u8;
typedef unsigned short     u16;
typedef unsigned int       u32;
typedef unsigned long long u64;
typedef signed   int       s32;
typedef signed   long long s64;
#if defined(__aarch64__) || defined(__x86_64__) || \
    (defined(__riscv) && (__riscv_xlen == 64))
typedef unsigned long long usize;
#else
typedef unsigned int       usize;
#endif
#define AI  __attribute__((always_inline)) static inline
#define NR  __attribute__((noreturn))
#define PK  __attribute__((packed))
#define CLA __attribute__((aligned(64)))

/* ── 512KB BSS arena (nomalloc) ─────────────────────────────────────── */
#define CA_SZ (512u*1024u)
static u8  _CA[CA_SZ] CLA;
static u32 _CP=0u, _CMK=0u;
AI void* CA(u32 n,u32 a){u32 m=a-1u,c=(_CP+m)&~m;if(c+n>CA_SZ)return(void*)0;void*p=_CA+c;_CP=c+n;return p;}
AI void  CR(void){_CP=0u;}
AI void  CMK(void){_CMK=_CP;}
AI void  CRS(void){_CP=_CMK;}

/* ── syscall wrappers ───────────────────────────────────────────────── */
#if defined(__arm__)
AI s32 _sc3(u32 r,u32 a,u32 b,u32 c){
    register s32 r0 __asm__("r0")=(s32)a; register u32 r1 __asm__("r1")=b;
    register u32 r2 __asm__("r2")=c;     register u32 r7 __asm__("r7")=r;
    __asm__ volatile("svc #0":"+r"(r0):"r"(r1),"r"(r2),"r"(r7):"memory","cc");
    return r0;
}
AI s32 _sc2(u32 r,u32 a,u32 b){return _sc3(r,a,b,0u);}
AI s32 _sc1(u32 r,u32 a)      {return _sc3(r,a,0u,0u);}
AI s32 OPEN(const char*p,u32 f){return _sc2(5u,(u32)(usize)p,f);}
AI s32 READ(s32 f,void*b,u32 n){return _sc3(3u,(u32)f,(u32)(usize)b,n);}
AI s32 WR  (u32 f,const void*b,u32 n){return _sc3(4u,f,(u32)(usize)b,n);}
AI s32 CLOSE(s32 f){return _sc1(6u,(u32)f);}
NR void EX(s32 c){_sc1(248u,(u32)c);__builtin_unreachable();}

#elif defined(__aarch64__)
#define AT_FDCWD (-100)
AI s64 _sc4(u64 r,u64 a,u64 b,u64 c,u64 d){
    register u64 x8 __asm__("x8")=r;
    register s64 x0 __asm__("x0")=(s64)a; register u64 x1 __asm__("x1")=b;
    register u64 x2 __asm__("x2")=c;     register u64 x3 __asm__("x3")=d;
    __asm__ volatile("svc #0":"+r"(x0):"r"(x8),"r"(x1),"r"(x2),"r"(x3):"memory","cc");
    return x0;
}
AI s64 _sc3(u64 r,u64 a,u64 b,u64 c){
    register u64 x8 __asm__("x8")=r;
    register s64 x0 __asm__("x0")=(s64)a; register u64 x1 __asm__("x1")=b;
    register u64 x2 __asm__("x2")=c;
    __asm__ volatile("svc #0":"+r"(x0):"r"(x8),"r"(x1),"r"(x2):"memory","cc");
    return x0;
}
AI s32 _sc1(u64 r,u64 a){
    register u64 x8 __asm__("x8")=r; register s64 x0 __asm__("x0")=(s64)a;
    __asm__ volatile("svc #0":"+r"(x0):"r"(x8):"memory","cc");
    return (s32)x0;
}
AI s32 OPEN(const char*p,u32 f){return(s32)_sc4(56u,(u64)(s64)AT_FDCWD,(u64)(usize)p,(u64)f,0u);}
AI s32 READ(s32 f,void*b,u32 n){return(s32)_sc3(63u,(u64)f,(u64)(usize)b,(u64)n);}
AI s32 WR  (u32 f,const void*b,u32 n){return(s32)_sc3(64u,(u64)f,(u64)(usize)b,(u64)n);}
AI s32 CLOSE(s32 f){return(s32)_sc1(57u,(u64)f);}
NR void EX(s32 c){_sc1(94u,(u64)c);__builtin_unreachable();}

#elif defined(__x86_64__)
AI s64 _sc3(u64 r,u64 a,u64 b,u64 c){
    s64 x;
    __asm__ volatile("syscall":"=a"(x):"a"(r),"D"(a),"S"(b),"d"(c):"rcx","r11","memory");
    return x;
}
AI s32 _sc1(u64 r,u64 a){
    s64 x;
    __asm__ volatile("syscall":"=a"(x):"a"(r),"D"(a):"rcx","r11","memory");
    return (s32)x;
}
AI s32 OPEN(const char*p,u32 f){return(s32)_sc3(2u,(u64)(usize)p,(u64)f,0u);}
AI s32 READ(s32 f,void*b,u32 n){return(s32)_sc3(0u,(u64)f,(u64)(usize)b,(u64)n);}
AI s32 WR  (u32 f,const void*b,u32 n){return(s32)_sc3(1u,(u64)f,(u64)(usize)b,(u64)n);}
AI s32 CLOSE(s32 f){return(s32)_sc1(3u,(u64)f);}
NR void EX(s32 c){_sc1(231u,(u64)c);__builtin_unreachable();}

#elif defined(__riscv) && (__riscv_xlen == 64)
/* RISCV64 Linux ecall: syscall# in a7, args in a0-a5, result in a0  */
/* Numbers identical to AArch64 (same "new" Linux ABI set)            */
#define AT_FDCWD (-100)
AI s64 _sc4(u64 r,u64 a,u64 b,u64 c,u64 d){
    register u64 a7 __asm__("a7")=r;
    register s64 a0 __asm__("a0")=(s64)a; register u64 a1 __asm__("a1")=b;
    register u64 a2 __asm__("a2")=c;     register u64 a3 __asm__("a3")=d;
    __asm__ volatile("ecall":"+r"(a0):"r"(a7),"r"(a1),"r"(a2),"r"(a3):"memory");
    return a0;
}
AI s64 _sc3(u64 r,u64 a,u64 b,u64 c){
    register u64 a7 __asm__("a7")=r;
    register s64 a0 __asm__("a0")=(s64)a; register u64 a1 __asm__("a1")=b;
    register u64 a2 __asm__("a2")=c;
    __asm__ volatile("ecall":"+r"(a0):"r"(a7),"r"(a1),"r"(a2):"memory");
    return a0;
}
AI s64 _sc1(u64 r,u64 a){
    register u64 a7 __asm__("a7")=r; register s64 a0 __asm__("a0")=(s64)a;
    __asm__ volatile("ecall":"+r"(a0):"r"(a7):"memory");
    return a0;
}
AI s32 OPEN(const char*p,u32 f){return(s32)_sc4(56u,(u64)(s64)AT_FDCWD,(u64)(usize)p,(u64)f,0u);}
AI s32 READ(s32 f,void*b,u32 n){return(s32)_sc3(63u,(u64)f,(u64)(usize)b,(u64)n);}
AI s32 WR  (u32 f,const void*b,u32 n){return(s32)_sc3(64u,(u64)f,(u64)(usize)b,(u64)n);}
AI s32 CLOSE(s32 f){return(s32)_sc1(57u,(u64)f);}
NR void EX(s32 c){_sc1(94u,(u64)c);__builtin_unreachable();}
#endif

/* ── CRC32C (Castagnoli, poly 0x82F63B78) branchless ────────────────── */
#define CRC32C_P 0x82F63B78u
AI u32 _crc(const u8*b,u32 n){
    u32 c=0xFFFFFFFFu,i,j;
    for(i=0u;i<n;i++){c^=(u32)b[i];for(j=0u;j<8u;j++){u32 m=0u-(c&1u);c=(c>>1)^(CRC32C_P&m);}}
    return c^0xFFFFFFFFu;
}

/* ── Adler-32 (for DEX checksum verification) ───────────────────────── */
AI u32 _a32(const u8*b,u32 n){
    u32 a=1u,s=0u,i;
    for(i=0u;i<n;i++){a=(a+(u32)b[i])%65521u;s=(s+a)%65521u;}
    return(s<<16)|a;
}

/* ── I/O sem printf ─────────────────────────────────────────────────── */
AI void CP(const char*s){u32 n=0u;while(s[n])n++;if(n)WR(1u,s,n);}
AI void CN(u32 v){
    char b[12];s32 i=11;b[i]='\n';i--;
    if(!v){b[i--]='0';}else{while(v){b[i--]='0'+(char)(v%10u);v/=10u;}}
    WR(1u,(const void*)(b+i+1),(u32)(11-i));
}
AI void CH(u32 v){
    static const char H[]="0123456789abcdef";
    char b[11];b[0]='0';b[1]='x';b[10]='\n';
    s32 i;for(i=9;i>=2;i--){b[i]=H[v&0xFu];v>>=4;}WR(1u,b,11u);
}

/* ── zero-fill (no libc memset) ─────────────────────────────────────── */
AI void MZ(void*d,u32 n){u8*p=(u8*)d;while(n--)p[n]=0u;}

/* ── ULEB128 decode (pointer auto-advances) ─────────────────────────── */
#define ULEB128(p,out) do{\
    u32 _s=0u,_v=0u;u8 _b;\
    do{_b=*(p)++;_v|=(u32)(_b&0x7Fu)<<_s;_s+=7u;}while(_b&0x80u);\
    (out)=_v;\
}while(0)
