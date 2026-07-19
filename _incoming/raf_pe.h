#pragma once
/* raf_pe.h — PE/COFF (Windows Portable Executable) packed structs + parse
 * nomalloc nolibc — zero stdlib, zero heap, zero abstraction
 * Formats: PE32 (0x010B) and PE32+ / 64-bit (0x020B)
 * DOS stub magic: MZ (bytes[0]='M' bytes[1]='Z')
 * PE signature at e_lfanew: 'P' 'E' 0x00 0x00
 * Machine codes: AMD64=0x8664 ARM64=0xAA64 ARM=0x01C4 I386=0x014C RISCV64=0x5064
 */
#include "raf_client_sys.h"

/* ── Machine type constants ─────────────────────────────────────────── */
#define IMAGE_FILE_MACHINE_AMD64   0x8664u
#define IMAGE_FILE_MACHINE_ARM64   0xAA64u
#define IMAGE_FILE_MACHINE_ARM     0x01C4u
#define IMAGE_FILE_MACHINE_I386    0x014Cu
#define IMAGE_FILE_MACHINE_RISCV64 0x5064u
#define IMAGE_FILE_MACHINE_RISCV32 0x5032u
#define IMAGE_FILE_MACHINE_THUMB   0x01C2u

/* ── Optional header magic ──────────────────────────────────────────── */
#define IMAGE_NT_OPTIONAL_HDR32_MAGIC 0x010Bu
#define IMAGE_NT_OPTIONAL_HDR64_MAGIC 0x020Bu

/* ── DOS header (only fields we need) ──────────────────────────────── */
typedef struct PK {
    u8  e_magic[2];  /* 'M' 'Z'                                        */
    u8  _pad[58];    /* skip e_cblp..e_lfarlc fields                   */
    u32 e_lfanew;    /* offset to PE signature (at byte 60)            */
} DosH;             /* total: 64 bytes                                  */

/* ── COFF file header (20 bytes, immediately after PE\0\0) ─────────── */
typedef struct PK {
    u16 machine;              /* IMAGE_FILE_MACHINE_* constants         */
    u16 numberOfSections;
    u32 timeDateStamp;
    u32 pointerToSymbolTable; /* deprecated in PE, usually 0            */
    u32 numberOfSymbols;
    u16 sizeOfOptionalHeader;
    u16 characteristics;
} CoffH;

/* ── Optional header prefix (first 2 bytes determine PE32 vs PE32+) ── */
typedef struct PK {
    u16 magic;      /* 0x010B = PE32, 0x020B = PE32+                   */
} PeOptMagic;

/* ── Parse context ──────────────────────────────────────────────────── */
typedef struct PK {
    u16 mach;       /* machine type (IMAGE_FILE_MACHINE_*)              */
    u16 sections;   /* number of sections                              */
    u8  bits;       /* 32 or 64                                        */
    u16 chars;      /* COFF characteristics                            */
} PeCtx;

/* ── Macros ─────────────────────────────────────────────────────────── */

/* PE_MAGIC_OK: first two bytes are 'M' 'Z' */
#define PE_MAGIC_OK(p) ((p)[0]=='M'&&(p)[1]=='Z')

/* PE_SIG_OK: 4 bytes at offset pe_off are 'P' 'E' '\0' '\0' */
#define PE_SIG_OK(p,off) \
    ((p)[(off)]==0x50u&&(p)[(off)+1u]==0x45u&& \
     (p)[(off)+2u]==0x00u&&(p)[(off)+3u]==0x00u)

/* PE_PARSE: populate PeCtx from buffer; on bad magic/size ctx.mach stays 0 */
#define PE_PARSE(buf,len,ctx) do { \
    const u8*_p=(const u8*)(buf); \
    u32 _l=(u32)(len); \
    (ctx).mach=0u;(ctx).sections=0u;(ctx).bits=0u;(ctx).chars=0u; \
    if(_l<64u||!PE_MAGIC_OK(_p))break; \
    { \
        u32 _off=((const DosH*)_p)->e_lfanew; \
        if(_off+24u>_l)break; \
        if(!PE_SIG_OK(_p,_off))break; \
        { \
            const CoffH*_c=(const CoffH*)(_p+_off+4u); \
            (ctx).mach=_c->machine; \
            (ctx).sections=_c->numberOfSections; \
            (ctx).chars=_c->characteristics; \
            if(_c->sizeOfOptionalHeader>=2u&&_off+24u+2u<=_l){ \
                u16 _mg=((const PeOptMagic*)(_p+_off+24u))->magic; \
                (ctx).bits=(_mg==IMAGE_NT_OPTIONAL_HDR64_MAGIC)?64u:32u; \
            } else { \
                (ctx).bits=32u; \
            } \
        } \
    } \
}while(0)

/* PE_MACH_STR: short ASCII tag for machine type */
AI const char* PE_MACH_STR(u16 m){
    if(m==IMAGE_FILE_MACHINE_AMD64)  return "amd64";
    if(m==IMAGE_FILE_MACHINE_ARM64)  return "arm64";
    if(m==IMAGE_FILE_MACHINE_ARM)    return "arm";
    if(m==IMAGE_FILE_MACHINE_THUMB)  return "thumb";
    if(m==IMAGE_FILE_MACHINE_I386)   return "i386";
    if(m==IMAGE_FILE_MACHINE_RISCV64)return "riscv64";
    if(m==IMAGE_FILE_MACHINE_RISCV32)return "riscv32";
    return "unk";
}
