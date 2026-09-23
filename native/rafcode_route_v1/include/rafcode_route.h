#ifndef RAFCODE_ROUTE_H
#define RAFCODE_ROUTE_H

typedef __UINT16_TYPE__ raf_route_u16;
typedef __UINT32_TYPE__ raf_route_u32;

#define RAF_ROUTE_REQUEST_MAGIC 0x31515252u
#define RAF_ROUTE_RECEIPT_MAGIC 0x31505252u
#define RAF_ROUTE_VERSION 0x0001u

#define RAF_ROUTE_TRIGGER_GENERAL 1u
#define RAF_ROUTE_TRIGGER_MATHEMATICS_GEOMETRY 2u
#define RAF_ROUTE_TRIGGER_MEMORY_CONVERSATIONS 3u
#define RAF_ROUTE_TRIGGER_CODE_RUNTIME 4u
#define RAF_ROUTE_TRIGGER_SCIENCE_CLAIMS 5u
#define RAF_ROUTE_TRIGGER_NOVO 6u
#define RAF_ROUTE_TRIGGER_ATLAS 7u
#define RAF_ROUTE_TRIGGER_GAP 8u
#define RAF_ROUTE_TRIGGER_EVID 9u
#define RAF_ROUTE_TRIGGER_LEARN 10u
#define RAF_ROUTE_TRIGGER_MAX RAF_ROUTE_TRIGGER_LEARN

#define RAF_ROUTE_ID_GENERAL 1u
#define RAF_ROUTE_ID_MATHEMATICS_GEOMETRY 2u
#define RAF_ROUTE_ID_MEMORY_CONVERSATIONS 3u
#define RAF_ROUTE_ID_CODE_RUNTIME 4u
#define RAF_ROUTE_ID_SCIENCE_CLAIMS 5u
#define RAF_ROUTE_ID_NOVO 6u
#define RAF_ROUTE_ID_ATLAS 7u
#define RAF_ROUTE_ID_GAP 8u
#define RAF_ROUTE_ID_EVID 9u
#define RAF_ROUTE_ID_LEARN 10u

#define RAF_ROUTE_FLAG_SOURCE_BOUND 0x00000001u
#define RAF_ROUTE_FLAG_AMBIGUOUS 0x00000002u
#define RAF_ROUTE_FLAG_EXPANSION_REQUIRED 0x00000004u
#define RAF_ROUTE_FLAG_KNOWN 0x00000007u

#define RAF_ROUTE_ERROR_MAGIC 0x00000001u
#define RAF_ROUTE_ERROR_VERSION 0x00000002u
#define RAF_ROUTE_ERROR_TRIGGER 0x00000004u
#define RAF_ROUTE_ERROR_SOURCE_BOUND 0x00000008u
#define RAF_ROUTE_ERROR_AMBIGUOUS 0x00000010u
#define RAF_ROUTE_ERROR_FLAGS 0x00000020u
#define RAF_ROUTE_ERROR_SOURCE_ID 0x00000040u
#define RAF_ROUTE_ERROR_RESERVED 0x00000080u

typedef struct {
    raf_route_u32 magic;
    raf_route_u16 version;
    raf_route_u16 trigger_code;
    raf_route_u32 flags;
    raf_route_u32 source_digest[4];
    raf_route_u32 reserved;
} raf_route_request;

typedef struct {
    raf_route_u32 magic;
    raf_route_u16 version;
    raf_route_u16 status;
    raf_route_u32 error_mask;
    raf_route_u32 trigger_code;
    raf_route_u32 route_code;
    raf_route_u32 route_tag;
    raf_route_u32 safe_flags;
    raf_route_u32 source_fold;
} raf_route_receipt;

typedef char raf_route_request_size_must_be_32[(sizeof(raf_route_request) == 32u) ? 1 : -1];
typedef char raf_route_receipt_size_must_be_32[(sizeof(raf_route_receipt) == 32u) ? 1 : -1];

void raf_route_resolve(const void *request_pointer, void *receipt_pointer);

#endif
