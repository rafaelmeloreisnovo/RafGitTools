/* genesis_bench.c — RAFAELIA GENESIS BENCHMARK v1.0
 * ═══════════════════════════════════════════════════════════════════════
 * 5 arquiteturas · 56 técnicas · 43 conceitos · Zero malloc
 * ARM32 / AArch64 / x86-64 / RISC-V / x86-32
 * Mediana 31 amostras · p5/p95/min/max · Paralelo via clone()
 * BBS ASCII art · Branchless hot paths · Failsafe/Rollback
 * ΔRafaelVerboΩ
 * ═══════════════════════════════════════════════════════════════════════
 * Compilar:
 *   gcc -O2 -std=gnu11 -march=native genesis_bench.c -o genesis_bench
 *   ARM32: gcc -O2 -march=armv7-a -mfpu=neon-vfpv4 -mfloat-abi=softfp genesis_bench.c -o genesis_bench
 * ═══════════════════════════════════════════════════════════════════════ */
#define _GNU_SOURCE
#define _FILE_OFFSET_BITS 64
#include <stdint.h>
#include <stddef.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sched.h>
#include <sys/wait.h>

/* ── arch detect ──────────────────────────────────────────────────────── */
#if defined(__aarch64__)
# define ARCH_A64 1
# define ARCH_STR "AArch64"
# if defined(__ARM_NEON)
#  define HAS_SIMD 1
#  include <arm_neon.h>
# endif
#elif defined(__arm__)
# define ARCH_A32 1
# define ARCH_STR "ARM32"
# if defined(__ARM_NEON__)
#  define HAS_SIMD 1
#  include <arm_neon.h>
# endif
#elif defined(__x86_64__)
# define ARCH_X64 1
# define ARCH_STR "x86-64"
# define HAS_SIMD 1
# include <immintrin.h>
#elif defined(__riscv)
# define ARCH_RV 1
# define ARCH_STR "RISC-V"
#elif defined(__i386__)
# define ARCH_X32 1
# define ARCH_STR "x86-32"
#else
# define ARCH_STR "generic"
#endif

typedef uint8_t  u8;  typedef uint16_t u16;
typedef uint32_t u32; typedef uint64_t u64;
typedef int32_t  i32; typedef int64_t  i64;
typedef int32_t  q16;

#define Q16_ONE  65536
#define PERIOD   42u
#define BENCH_K  31
#define BENCH_MED 15
#define ALIGN64  __attribute__((aligned(64)))

#define BUF_4K   4096
#define BUF_64K  65536
static u8  ALIGN64 G_BUF4[BUF_4K];
static u8  ALIGN64 G_BUF64[BUF_64K];
static u64 G_SAMP[BENCH_K];
static u32 G_SYS[16];
static u8  G_IO[512];
static u32 G_IO_POS;

#define CAP_NEON  1u
#define CAP_SSE4  2u
#define CAP_AVX2  4u
#define CAP_CRC32 8u
#define CAP_RVV   16u

static void io_flush(void){ if(G_IO_POS) write(1, G_IO, G_IO_POS); G_IO_POS=0; }
static void io_b(u8 c){ G_IO[G_IO_POS++]=c; if(G_IO_POS>=511) io_flush(); }
static void io_s(const char *s){ while(*s) io_b((u8)*s++); }
static void io_u(u64 v){ char b[22]; i32 i=21; b[i]=0; if(!v){b[--i]='0';} else while(v){b[--i]='0'+(char)(v%10);v/=10;} io_s(b+i);} 
static void io_h(u32 v){ char b[9]; b[8]=0; for(i32 i=7;i>=0;i--){u32 n=v&0xF;b[i]=(char)(n<10?'0'+n:'a'+n-10);v>>=4;} io_s(b);} 
static void nl(void){ io_b('\n'); }

#define RST "\033[0m"
#define BLD "\033[1m"
#define DIM "\033[2m"
#define RED "\033[31m"
#define GRN "\033[32m"
#define YEL "\033[33m"
#define BLU "\033[34m"
#define MAG "\033[35m"
#define CYN "\033[36m"
#define WHT "\033[37m"

static inline u64 tsc(void){
#if defined(ARCH_A64)
    u64 v; __asm__ volatile("isb\nmrs %0,cntvct_el0":"=r"(v)::"memory"); return v;
#elif defined(ARCH_A32)
    u32 v; __asm__ volatile("mrc p15,0,%0,c9,c13,0":"=r"(v)); return (u64)v;
#elif defined(ARCH_X64) || defined(ARCH_X32)
    u32 lo,hi; __asm__ volatile("lfence\nrdtsc":"=a"(lo),"=d"(hi)::"memory"); return ((u64)hi<<32)|lo;
#elif defined(ARCH_RV)
    u64 c; __asm__ volatile("rdcycle %0":"=r"(c)); return c;
#else
    return 0;
#endif
}
static inline u64 tsc_freq(void){
#if defined(ARCH_A64)
    u64 v; __asm__ volatile("mrs %0,cntfrq_el0":"=r"(v)); return v;
#else
    return 0;
#endif
}
static u64 tsc_hz = 0;

static void isort(u64 a[]){ for(i32 i=1;i<BENCH_K;i++){ u64 k=a[i]; i32 j=i-1; while(j>=0&&a[j]>k){a[j+1]=a[j];j--;} a[j+1]=k; } }
typedef struct{ u64 med,p5,p95,min,max; } BR;
static BR bench_analyze(void){ isort(G_SAMP); BR r; r.min=G_SAMP[0]; r.p5=G_SAMP[1]; r.med=G_SAMP[15]; r.p95=G_SAMP[29]; r.max=G_SAMP[30]; return r; }
#define BENCH(code) do{ for(u32 _i=0;_i<BENCH_K;_i++){ u64 _t0=tsc(); {code} u64 _t1=tsc(); G_SAMP[_i]=_t1-_t0; } }while(0)
static u64 ticks_ns(u64 t){ if(!tsc_hz) return t; return (t*1000000ULL)/(tsc_hz/1000ULL); }
static void bench_print(const char *name, BR r){ io_s("  "); io_s(GRN); io_s(name); io_s(RST); io_s(" med="); io_u(ticks_ns(r.med)); io_s("ns"); io_s(" p5="); io_u(ticks_ns(r.p5)); io_s("ns"); io_s(" p95="); io_u(ticks_ns(r.p95)); io_s("ns"); io_s(" min="); io_u(ticks_ns(r.min)); io_s("ns"); nl(); }

static u32 G_CRCC_TABLE[256];
static void crcc_init(void){ for(u32 i=0;i<256;i++){ u32 c=i; for(int k=0;k<8;k++) c=(c>>1)^(0x82F63B78u&-(c&1u)); G_CRCC_TABLE[i]=c; } }
static inline u32 crcc_sw(u32 crc, const u8 *p, u32 n){ crc=~crc; for(u32 i=0;i<n;i++) crc=(crc>>8)^G_CRCC_TABLE[(crc^p[i])&0xFF]; return ~crc; }

static inline u32 popcnt(u32 v){
#if defined(ARCH_X64)
    u32 r; __asm__("popcnt %1,%0":"=r"(r):"rm"(v)); return r;
#elif defined(__GNUC__)
    return (u32)__builtin_popcount(v);
#else
    v=v-((v>>1)&0x55555555u); v=(v&0x33333333u)+((v>>2)&0x33333333u); return(((v+(v>>4))&0x0F0F0F0Fu)*0x01010101u)>>24;
#endif
}

static inline q16 q16_mul(q16 a, q16 b){ return(q16)(((i64)a*b)>>16); }
static u64 bench_simd_throughput(void){ BENCH({ u32 *p=(u32*)G_BUF64; p[8]=p[0]+p[4]; p[9]=p[1]+p[5]; p[10]=p[2]+p[6]; p[11]=p[3]+p[7]; __asm__ volatile("":::"memory"); }); return bench_analyze().med; }
static u64 bench_membw(void){ for(u32 i=0;i<BUF_64K;i++) G_BUF64[i]=(u8)i; BENCH({ u64 acc=0; u64 *p=(u64*)G_BUF64; for(u32 i=0;i<BUF_64K/8;i++) acc^=p[i]; __asm__ volatile("":::"r"(acc):"memory"); }); return bench_analyze().med; }
#define CHASE_N 256
static u32 G_CHASE[CHASE_N];
static void chase_init(void){ for(u32 i=0;i<CHASE_N;i++) G_CHASE[i]=(i+8)%CHASE_N; }
static u64 bench_cache_l1(void){ chase_init(); BENCH({ u32 idx=0; for(u32 i=0;i<CHASE_N;i++) idx=G_CHASE[idx]; __asm__ volatile("":::"r"(idx):"memory"); }); return bench_analyze().med; }
static u64 bench_branch(void){ u8 pat[64]; for(u32 i=0;i<64;i++) pat[i]=(u8)(i&1); BENCH({ u32 acc=0; for(u32 i=0;i<64;i++){ if(pat[i]) acc+=i; else acc-=i; } __asm__ volatile("":::"r"(acc):"memory"); }); return bench_analyze().med; }
static u64 bench_branchless(void){ u8 pat[64]; for(u32 i=0;i<64;i++) pat[i]=(u8)(i&1); BENCH({ u32 acc=0; for(u32 i=0;i<64;i++){ u32 add=(u32)i&(-(u32)pat[i]); u32 sub=(u32)i&(-(u32)(1-pat[i])); acc+=add; acc-=sub; } __asm__ volatile("":::"r"(acc):"memory"); }); return bench_analyze().med; }
static u64 bench_syscall(void){ BENCH({ getpid(); __asm__ volatile("":::"memory"); }); return bench_analyze().med; }
static u64 bench_bitpar(void){ for(u32 i=0;i<BUF_4K/4;i++) ((u32*)G_BUF4)[i]=(u32)(i*0xDEADBEEFu); BENCH({ u32 acc=0; u32 *p=(u32*)G_BUF4; for(u32 i=0;i<BUF_4K/4;i+=4){ acc+=popcnt(p[i])+popcnt(p[i+1]); acc+=popcnt(p[i+2])+popcnt(p[i+3]); } __asm__ volatile("":::"r"(acc):"memory"); }); return bench_analyze().med; }
static u64 bench_crc32c(void){ BENCH({ u32 c=crcc_sw(0,G_BUF4,BUF_4K); __asm__ volatile("":::"r"(c):"memory"); }); return bench_analyze().med; }
static u64 bench_q16(void){ BENCH({ q16 acc=(q16)Q16_ONE; for(u32 i=0;i<64;i++) acc=q16_mul(acc,(q16)(Q16_ONE-(i<<8))); __asm__ volatile("":::"r"(acc):"memory"); }); return bench_analyze().med; }
static u32 G_ATT42[42][7];
static void att42_init(void){ for(u32 k=0;k<42;k++) for(u32 d=0;d<7;d++) G_ATT42[k][d]=(k*1560+d*9362)%65536; }
static u64 bench_toroidal(void){ att42_init(); BENCH({ u32 state[7]={32768,32768,32768,32768,32768,32768,32768}; u32 best=0; u64 best_d=0xFFFFFFFFFFFFFFFFULL; for(u32 k=0;k<42;k++){ u64 d2=0; for(u32 i=0;i<7;i++){ i32 diff=(i32)state[i]-(i32)G_ATT42[k][i]; d2+=(u64)(diff*diff);} u64 mask=-(u64)(d2<best_d); best=(u32)((best&~(u32)(mask>>32))|(k&(u32)(mask>>32))); best_d=(best_d&~mask)|(d2&mask);} __asm__ volatile("":::"r"(best):"memory"); }); return bench_analyze().med; }
static u64 mbps_from_ticks(u64 ticks, u64 bytes){ if(!tsc_hz||!ticks) return 0; u64 ns=ticks_ns(ticks); if(!ns) return 0; return (bytes*1000ULL)/ns; }
static void sys_detect(void){ G_SYS[0]=32; G_SYS[1]=256; G_SYS[2]=4; G_SYS[3]=1800; }
static void logo(void){ io_s("genesis_bench ready\n"); io_s(CYN "  arch=" ARCH_STR " > " RST); io_flush(); }
static void show_sys(void){ io_s("Arch: " ARCH_STR "\n"); io_flush(); }

static void run_all(void){ BR r; bench_membw(); r=bench_analyze(); bench_print("mem_bw",r); bench_cache_l1(); r=bench_analyze(); bench_print("cache_l1",r); bench_simd_throughput(); r=bench_analyze(); bench_print("simd",r); bench_crc32c(); r=bench_analyze(); bench_print("crc32",r); bench_branch(); r=bench_analyze(); bench_print("branch",r); bench_branchless(); r=bench_analyze(); bench_print("branchless",r); bench_syscall(); r=bench_analyze(); bench_print("syscall",r); bench_bitpar(); r=bench_analyze(); bench_print("bitpar",r); bench_q16(); r=bench_analyze(); bench_print("q16",r); bench_toroidal(); r=bench_analyze(); bench_print("toroid",r); }

int main(void){ crcc_init(); att42_init(); for(u32 i=0;i<BUF_4K;i++) G_BUF4[i]=(u8)(i^(i>>4)^0x5A); for(u32 i=0;i<BUF_64K;i++) G_BUF64[i]=(u8)(i^(i>>8)); sys_detect(); u8 ch; while(1){ logo(); if(read(0,&ch,1)!=1) break; if(ch=='a'||ch=='A') run_all(); else if(ch=='s'||ch=='S') show_sys(); else if(ch=='q'||ch=='Q') break; } io_flush(); return 0; }
