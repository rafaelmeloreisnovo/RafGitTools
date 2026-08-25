#pragma once
/* raf_dex.h — DEX 035/037/038/039 packed structs + parse context
 * nomalloc nolibc — zero stdlib, zero heap, zero abstraction
 * DEX header: 112 bytes packed (AOSP spec §dex-format)
 * magic: 64 65 78 0a <ver3> 00  ("dex\n035\0" .. "dex\n039\0")
 * checksum: Adler-32 of bytes [12..end]
 * SHA-1: bytes [12..end] (not verified here — only Adler-32)
 */
#include "raf_client_sys.h"

/* ── DEX header (112 bytes) ─────────────────────────────────────────── */
typedef struct PK {
    u8  magic[8];      /* "dex\n035\0" .. "dex\n039\0"               */
    u32 checksum;      /* Adler-32 of bytes[12..file_size-1]          */
    u8  sha1[20];      /* SHA-1 of bytes[32..file_size-1] (skip here) */
    u32 file_size;     /* total size of file in bytes                 */
    u32 header_size;   /* = 112                                       */
    u32 endian_tag;    /* 0x12345678 little-endian                    */
    u32 link_size;
    u32 link_off;
    u32 map_off;
    u32 string_ids_size;
    u32 string_ids_off;
    u32 type_ids_size;
    u32 type_ids_off;
    u32 proto_ids_size;
    u32 proto_ids_off;
    u32 field_ids_size;
    u32 field_ids_off;
    u32 method_ids_size;
    u32 method_ids_off;
    u32 class_defs_size;
    u32 class_defs_off;
    u32 data_size;
    u32 data_off;
} DexH;

/* ── Parse context (6 fields) ───────────────────────────────────────── */
typedef struct PK {
    u16 ver;      /* 035/037/038/039 as u16 decimal              */
    u32 str_n;    /* string_ids_size                             */
    u32 type_n;   /* type_ids_size                               */
    u32 proto_n;  /* proto_ids_size                              */
    u32 field_n;  /* field_ids_size                              */
    u32 cls_n;    /* class_defs_size                             */
    u32 file_sz;  /* file_size from header                       */
    u32 a32_ok;   /* 1 = Adler-32 matches, 0 = friction signal   */
} DCtx;

/* ── Constants ──────────────────────────────────────────────────────── */
#define DEX_ENDIAN_OK 0x12345678u
#define DEX_HDR_SZ    112u

/* ── Macros ──────────────────────────────────────────────────────────── */

/* DEX_MAGIC_OK: first 4 bytes == "dex\n" and version digit range '0'-'9' */
#define DEX_MAGIC_OK(p) \
    ((p)[0]=='d'&&(p)[1]=='e'&&(p)[2]=='x'&&(p)[3]=='\n'&& \
     (p)[4]>='0'&&(p)[4]<='9'&&(p)[7]=='\0')

/* DEX_VER: extract version as u16 from bytes [4..6] ("035".."039") */
#define DEX_VER(p) \
    ((u16)(((u32)((p)[4]-'0')*100u)+((u32)((p)[5]-'0')*10u)+((u32)((p)[6]-'0'))))

/* DEX_STR_CNT: string_ids_size from header */
#define DEX_STR_CNT(p)   (((const DexH*)(p))->string_ids_size)

/* DEX_CLASS_CNT: class_defs_size from header */
#define DEX_CLASS_CNT(p) (((const DexH*)(p))->class_defs_size)

/* DEX_ADLER32: compute Adler-32 of buf[12..len-1] and compare to header */
/* Returns 1 if match (zero friction), 0 if mismatch (friction signal)   */
#define DEX_ADLER32(buf,len,ok) do { \
    const u8*_b=(const u8*)(buf); \
    u32 _l=(u32)(len); \
    u32 _stored=((const DexH*)_b)->checksum; \
    u32 _computed=(_l>12u)?_a32(_b+12u,_l-12u):0u; \
    (ok)=(_stored==_computed)?1u:0u; \
}while(0)

/* DEX_PARSE: populate DCtx from buffer; breaks on bad magic/size       */
#define DEX_PARSE(buf,len,ctx) do { \
    const u8*_p=(const u8*)(buf); \
    u32 _l=(u32)(len); \
    (ctx).ver=0u;(ctx).str_n=0u;(ctx).type_n=0u; \
    (ctx).proto_n=0u;(ctx).field_n=0u;(ctx).cls_n=0u; \
    (ctx).file_sz=0u;(ctx).a32_ok=0u; \
    if(_l<DEX_HDR_SZ||!DEX_MAGIC_OK(_p))break; \
    { \
        const DexH*_h=(const DexH*)_p; \
        if(_h->endian_tag!=DEX_ENDIAN_OK)break; \
        (ctx).ver=(u16)DEX_VER(_p); \
        (ctx).str_n=_h->string_ids_size; \
        (ctx).type_n=_h->type_ids_size; \
        (ctx).proto_n=_h->proto_ids_size; \
        (ctx).field_n=_h->field_ids_size; \
        (ctx).cls_n=_h->class_defs_size; \
        (ctx).file_sz=_h->file_size; \
        DEX_ADLER32(_p,_l,(ctx).a32_ok); \
    } \
}while(0)
