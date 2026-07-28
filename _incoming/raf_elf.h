#pragma once
/* raf_elf.h — ELF32/ELF64 packed structs + parse context
 * nomalloc nolibc — zero stdlib, zero heap, zero abstraction
 * ELF32 header: 52 bytes  ELF64 header: 64 bytes
 * e_machine: EM_386=3 EM_ARM=40 EM_X86_64=62 EM_AARCH64=183 EM_RISCV=243
 * e_type: ET_EXEC=2 ET_DYN=3 ET_CORE=4
 */
#include "raf_client_sys.h"

/* ── ELF32 ─────────────────────────────────────────────────────────── */
typedef struct PK {
    u8  e_ident[16]; /* magic[4] class bits data version OS ABI pad  */
    u16 e_type;      /* ET_EXEC=2 ET_DYN=3                           */
    u16 e_machine;   /* EM_ARM=40 EM_386=3                           */
    u32 e_version;
    u32 e_entry;
    u32 e_phoff;     /* program header table offset                  */
    u32 e_shoff;     /* section header table offset                  */
    u32 e_flags;
    u16 e_ehsize;    /* size of this header: 52                      */
    u16 e_phentsize;
    u16 e_phnum;
    u16 e_shentsize;
    u16 e_shnum;
    u16 e_shstrndx;
} EHdr32;

typedef struct PK {
    u32 p_type;
    u32 p_offset;
    u32 p_vaddr;
    u32 p_paddr;
    u32 p_filesz;
    u32 p_memsz;
    u32 p_flags;
    u32 p_align;
} EPHdr32;

typedef struct PK {
    u32 sh_name;
    u32 sh_type;
    u32 sh_flags;
    u32 sh_addr;
    u32 sh_offset;
    u32 sh_size;
    u32 sh_link;
    u32 sh_info;
    u32 sh_addralign;
    u32 sh_entsize;
} ESHdr32;

/* ── ELF64 ─────────────────────────────────────────────────────────── */
typedef struct PK {
    u8  e_ident[16];
    u16 e_type;
    u16 e_machine;   /* EM_X86_64=62 EM_AARCH64=183                 */
    u32 e_version;
    u64 e_entry;
    u64 e_phoff;
    u64 e_shoff;
    u32 e_flags;
    u16 e_ehsize;    /* size of this header: 64                      */
    u16 e_phentsize;
    u16 e_phnum;
    u16 e_shentsize;
    u16 e_shnum;
    u16 e_shstrndx;
} EHdr64;

typedef struct PK {
    u32 p_type;
    u32 p_flags;
    u64 p_offset;
    u64 p_vaddr;
    u64 p_paddr;
    u64 p_filesz;
    u64 p_memsz;
    u64 p_align;
} EPHdr64;

typedef struct PK {
    u32 sh_name;
    u32 sh_type;
    u64 sh_flags;
    u64 sh_addr;
    u64 sh_offset;
    u64 sh_size;
    u32 sh_link;
    u32 sh_info;
    u64 sh_addralign;
    u64 sh_entsize;
} ESHdr64;

/* ── Parse context (4 fields, no heap) ────────────────────────────── */
typedef struct PK {
    u8  cl;    /* 1=ELF32 2=ELF64                                    */
    u16 mach;  /* e_machine                                          */
    u16 phnum; /* program header count                               */
    u16 shnum; /* section header count                               */
    u16 etype; /* e_type                                             */
    u32 flags; /* e_flags                                            */
} ECtx;

/* ── e_machine constants ────────────────────────────────────────────── */
#define EM_386    3u
#define EM_ARM    40u
#define EM_X86_64 62u
#define EM_AA64   183u
#define EM_RISCV  243u

/* ── e_type constants ───────────────────────────────────────────────── */
#define ET_EXEC 2u
#define ET_DYN  3u
#define ET_CORE 4u

/* ── Macros ─────────────────────────────────────────────────────────── */
#define ELF_MAGIC_OK(p) \
    ((p)[0]==0x7fu&&(p)[1]=='E'&&(p)[2]=='L'&&(p)[3]=='F')

#define ELF_IS64(p) ((p)[4]==2u)

#define ELF_MACHINE(p) \
    (ELF_IS64(p) ? ((const EHdr64*)(p))->e_machine \
                 : ((const EHdr32*)(p))->e_machine)

#define ELF_PHNUM(p) \
    (ELF_IS64(p) ? ((const EHdr64*)(p))->e_phnum \
                 : ((const EHdr32*)(p))->e_phnum)

#define ELF_SHNUM(p) \
    (ELF_IS64(p) ? ((const EHdr64*)(p))->e_shnum \
                 : ((const EHdr32*)(p))->e_shnum)

#define ELF_ETYPE(p) \
    (ELF_IS64(p) ? ((const EHdr64*)(p))->e_type \
                 : ((const EHdr32*)(p))->e_type)

#define ELF_FLAGS(p) \
    (ELF_IS64(p) ? ((const EHdr64*)(p))->e_flags \
                 : ((const EHdr32*)(p))->e_flags)

/* ELF_PARSE: populate ECtx from buffer; returns 1 on ok, 0 on bad magic/size */
#define ELF_PARSE(buf,len,ctx) do { \
    const u8*_p=(const u8*)(buf); \
    u32 _l=(u32)(len); \
    (ctx).cl=0u;(ctx).mach=0u;(ctx).phnum=0u; \
    (ctx).shnum=0u;(ctx).etype=0u;(ctx).flags=0u; \
    if(_l<52u||!ELF_MAGIC_OK(_p))break; \
    (ctx).cl=(u8)(_p[4]); \
    if((ctx).cl!=1u&&(ctx).cl!=2u){(ctx).cl=0u;break;} \
    if((ctx).cl==2u&&_l<64u){(ctx).cl=0u;break;} \
    (ctx).mach=ELF_MACHINE(_p); \
    (ctx).phnum=(u16)ELF_PHNUM(_p); \
    (ctx).shnum=(u16)ELF_SHNUM(_p); \
    (ctx).etype=(u16)ELF_ETYPE(_p); \
    (ctx).flags=ELF_FLAGS(_p); \
}while(0)

/* ELF_MACH_STR: short ASCII tag for e_machine (no null-term inline) */
AI const char* ELF_MACH_STR(u16 m){
    if(m==EM_ARM)   return "arm32";
    if(m==EM_AA64)  return "arm64";
    if(m==EM_X86_64)return "x86-64";
    if(m==EM_386)   return "x86-32";
    if(m==EM_RISCV) return "riscv";
    return "unk";
}

/* ELF_TYPE_STR: short ASCII tag for e_type */
AI const char* ELF_TYPE_STR(u16 t){
    if(t==ET_EXEC)return "exec";
    if(t==ET_DYN) return "dyn";
    if(t==ET_CORE)return "core";
    return "unk";
}
