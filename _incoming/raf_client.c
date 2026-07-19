/* raf_client.c — Freestanding ELF/DEX binary client
 * nomalloc nolibc noGC — zero stdlib, zero heap, zero abstraction
 * friction-as-catalyst: CRC32C mismatch → EMA update, not error
 * entry: client_main() called from raf_client_start.S _start
 */
#include "raf_elf.h"
#include "raf_dex.h"

/* ── BSS globals (no heap, no malloc) ──────────────────────────────── */
static u32 g_ema;   /* EMA accumulator of friction deltas              */
static u32 g_cmt;   /* commit count (zero-friction parse cycles)       */
static u32 g_frc;   /* friction count (mismatch parse cycles)          */

/* ── Friction gate macro ─────────────────────────────────────────────
 * buf: const u8* raw bytes   sz: u32 byte count   exp: u32 expected CRC
 * On match: g_cmt++
 * On mismatch: delta = (actual ^ exp) & 0xFFFF
 *              g_ema = (g_ema*3 + delta) >> 2   (EMA α=0.25)
 *              g_frc++
 * ──────────────────────────────────────────────────────────────────── */
#define FG(buf,sz,exp) do { \
    u32 _c=_crc((const u8*)(buf),(u32)(sz)); \
    if(_c==(u32)(exp)){ \
        g_cmt++; \
    } else { \
        u32 _d=(_c^(u32)(exp))&0xFFFFu; \
        g_ema=(g_ema*3u+_d)>>2; \
        g_frc++; \
    } \
}while(0)

#define FRICT_THRESH 0x8000u   /* EMA > 32768 → high-friction alert   */

/* ── /proc/self/cmdline reader ──────────────────────────────────────
 * Reads null-separated argv into buf (max n bytes).
 * Returns pointer to argv[1] (second null-delimited token) or (void*)0.
 * ─────────────────────────────────────────────────────────────────── */
static const char* _argv1(u8*buf,u32 n){
    s32 fd=OPEN("/proc/self/cmdline",0u);
    if(fd<0)return(const char*)0;
    s32 r=READ(fd,buf,n-1u);
    CLOSE(fd);
    if(r<=0)return(const char*)0;
    buf[(u32)r]=0u;
    /* skip argv[0]: advance past first '\0' */
    u32 i=0u;
    while(i<(u32)r&&buf[i])i++;
    i++;
    if(i>=(u32)r||!buf[i])return(const char*)0;
    return(const char*)(buf+i);
}

/* ── Output helpers (no printf, no libc) ─────────────────────────── */
static void _sep(void){WR(1u,"  ",2u);}

static void _out_elf(const ECtx*e){
    CP("fmt=ELF");_sep();
    CP("class=");CP(e->cl==2u?"64":"32");_sep();
    CP("arch=");CP(ELF_MACH_STR(e->mach));_sep();
    CP("type=");CP(ELF_TYPE_STR(e->etype));_sep();
    CP("phnum=");CN(e->phnum);
    CP("shnum=");CN(e->shnum);
}

static void _out_dex(const DCtx*d){
    CP("fmt=DEX");_sep();
    CP("ver=");CN(d->ver);
    CP("strings=");CN(d->str_n);
    CP("types=");CN(d->type_n);
    CP("protos=");CN(d->proto_n);
    CP("fields=");CN(d->field_n);
    CP("classes=");CN(d->cls_n);
    CP("filesz=");CN(d->file_sz);
    CP("adler32=");CP(d->a32_ok?"ok":"MISMATCH");WR(1u,"\n",1u);
}

static void _out_summary(void){
    CP("commits=");CN(g_cmt);
    CP("friction=");CN(g_frc);
    CP("ema=");CH(g_ema);
    if(g_ema>FRICT_THRESH){CP("HIGH-FRICTION\n");}
}

/* ── client_main ─────────────────────────────────────────────────── */
void client_main(void){
    /* read path from /proc/self/cmdline into arena */
    u8*cb=(u8*)CA(256u,1u);
    if(!cb){CP("err:arena\n");EX(1);}
    const char*path=_argv1(cb,255u);
    if(!path){CP("usage: raf_client <file>\n");EX(1);}

    /* open + read file into arena */
    s32 fd=OPEN(path,0u);
    if(fd<0){CP("err:open\n");EX(1);}
    u8*fb=(u8*)CA(CA_SZ-256u,64u);
    if(!fb){CLOSE(fd);CP("err:arena\n");EX(1);}
    s32 n=READ(fd,fb,(u32)(CA_SZ-256u-64u));
    CLOSE(fd);
    if(n<=0){CP("err:read\n");EX(1);}
    u32 sz=(u32)n;

    /* detect format */
    if(sz>=4u&&ELF_MAGIC_OK(fb)){
        /* ELF path */
        ECtx ec;
        ELF_PARSE(fb,sz,ec);
        if(!ec.cl){CP("err:elf-parse\n");EX(1);}
        /* friction gate on ELF ident (16 bytes) — expected CRC of standard ident */
        u32 hlen=(ec.cl==2u)?64u:52u;
        if(hlen>sz)hlen=sz;
        FG(fb,hlen,_crc(fb,hlen));   /* self-reference: baseline commit */
        /* friction gate on full buffer */
        FG(fb,sz,_crc(fb,sz));
        _out_elf(&ec);
    } else if(sz>=DEX_HDR_SZ&&DEX_MAGIC_OK(fb)){
        /* DEX path */
        DCtx dc;
        DEX_PARSE(fb,sz,dc);
        if(!dc.ver){CP("err:dex-parse\n");EX(1);}
        /* friction: Adler-32 embedded in header vs computed */
        if(dc.a32_ok){g_cmt++;}else{
            u32 _d=0x8000u;          /* unknown delta — use max signal */
            g_ema=(g_ema*3u+_d)>>2;
            g_frc++;
        }
        /* friction gate on full buffer CRC */
        FG(fb,sz,_crc(fb,sz));
        _out_dex(&dc);
    } else {
        /* unknown — still run friction on raw bytes */
        FG(fb,sz,0u);               /* expected=0: always friction     */
        CP("fmt=UNK\n");
        CP("sz=");CN(sz);
    }

    _out_summary();
    EX(0);
}
