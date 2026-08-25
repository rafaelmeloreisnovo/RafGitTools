#ifndef RAFCODE_FEDERATION_H
#define RAFCODE_FEDERATION_H

typedef __UINT8_TYPE__ raf_u8;
typedef __UINT16_TYPE__ raf_u16;
typedef __UINT32_TYPE__ raf_u32;
typedef __INTPTR_TYPE__ raf_iptr;
typedef __SIZE_TYPE__ raf_size;

#define RAF_FED_WORK_MAGIC 0x31464152u
#define RAF_FED_RECEIPT_MAGIC 0x31504352u
#define RAF_FED_VERSION 0x0001u

#define RAF_FED_AXIS_LONGITUDINAL 0x01u
#define RAF_FED_AXIS_ORTHOGONAL 0x02u
#define RAF_FED_AXIS_TRANSVERSAL 0x04u
#define RAF_FED_AXIS_REQUIRED 0x07u

#define RAF_FED_ROLE_CONTROL 0x01u
#define RAF_FED_ROLE_EXECUTOR 0x02u
#define RAF_FED_ROLE_EVIDENCE 0x04u
#define RAF_FED_ROLE_VM 0x08u
#define RAF_FED_ROLE_REQUIRED 0x0fu

#define RAF_FED_ACTION_DISCOVER 1u
#define RAF_FED_ACTION_BIND_IDENTITY 2u
#define RAF_FED_ACTION_RESOLVE_AUTHORITY 3u
#define RAF_FED_ACTION_LOAD_MINIMUM_INDICES 4u
#define RAF_FED_ACTION_TYPE_GAP_OR_GOAL 5u
#define RAF_FED_ACTION_CLASSIFY_GOVERNANCE_DATA_PRIVACY_SECURITY 6u
#define RAF_FED_ACTION_RESOLVE_MAPA_ROUTE 7u
#define RAF_FED_ACTION_SELECT_GATE 8u
#define RAF_FED_ACTION_CAPTURE_BASELINE_AND_ROLLBACK 9u
#define RAF_FED_ACTION_EXECUTE_BOUNDED 10u
#define RAF_FED_ACTION_VERIFY_LOCAL 11u
#define RAF_FED_ACTION_VERIFY_CROSS_REPO_EDGES 12u
#define RAF_FED_ACTION_EMIT_RECEIPT 13u
#define RAF_FED_ACTION_APPEND_TRANSITION 14u
#define RAF_FED_ACTION_UPDATE_DRIVE_RECONSTRUCTION 15u
#define RAF_FED_ACTION_RECOMPUTE_F_GAP_F_NEXT 16u
#define RAF_FED_ACTION_MAX RAF_FED_ACTION_RECOMPUTE_F_GAP_F_NEXT

#define RAF_FED_STATE_TOKEN_VAZIO 0u
#define RAF_FED_STATE_UNCERTAIN 1u
#define RAF_FED_STATE_BLOCKED 2u
#define RAF_FED_STATE_READY_TO_TEST 3u
#define RAF_FED_STATE_TESTING 4u
#define RAF_FED_STATE_EVIDENCED_SCOPED 5u
#define RAF_FED_STATE_RESOLVED_NEGATIVE 6u
#define RAF_FED_STATE_RESOLVED 7u
#define RAF_FED_STATE_MAX RAF_FED_STATE_RESOLVED

#define RAF_FED_FLAG_CLAIM_ALLOWED 0x00000001u
#define RAF_FED_FLAG_PRIVATE_PATHS 0x00000002u
#define RAF_FED_FLAG_MUTATING 0x00000004u
#define RAF_FED_FLAG_ROLLBACK_BOUND 0x00000008u
#define RAF_FED_FLAG_RECEIPT_REQUIRED 0x00000010u
#define RAF_FED_FLAG_EVIDENCE_BOUND 0x00000020u
#define RAF_FED_FLAG_KNOWN 0x0000003fu

#define RAF_FED_ERROR_IO 0x00000001u
#define RAF_FED_ERROR_MAGIC 0x00000002u
#define RAF_FED_ERROR_VERSION 0x00000004u
#define RAF_FED_ERROR_AXES 0x00000008u
#define RAF_FED_ERROR_ROLES 0x00000010u
#define RAF_FED_ERROR_TRANSACTION 0x00000020u
#define RAF_FED_ERROR_ACTION 0x00000040u
#define RAF_FED_ERROR_STATE 0x00000080u
#define RAF_FED_ERROR_CLAIM 0x00000100u
#define RAF_FED_ERROR_PRIVACY 0x00000200u
#define RAF_FED_ERROR_ROLLBACK 0x00000400u
#define RAF_FED_ERROR_RECEIPT 0x00000800u
#define RAF_FED_ERROR_EVIDENCE 0x00001000u
#define RAF_FED_ERROR_FLAGS 0x00002000u
#define RAF_FED_ERROR_INPUT_ID 0x00004000u
#define RAF_FED_ERROR_ROUTE_ID 0x00008000u

typedef struct {
    raf_u32 magic;
    raf_u16 version;
    raf_u8 axis_mask;
    raf_u8 participant_mask;
    raf_u32 transaction_lo;
    raf_u32 transaction_hi;
    raf_u32 action;
    raf_u32 state_before;
    raf_u32 state_after;
    raf_u32 flags;
    raf_u32 input_digest[4];
    raf_u32 route_digest[4];
} raf_fed_work;

typedef struct {
    raf_u32 magic;
    raf_u16 version;
    raf_u16 status;
    raf_u32 error_mask;
    raf_u32 transaction_lo;
    raf_u32 transaction_hi;
    raf_u32 axis_role_action;
    raf_u32 state_before;
    raf_u32 state_after;
    raf_u32 safe_flags;
    raf_u32 input_fold;
    raf_u32 trace_tag[6];
} raf_fed_receipt;

typedef char raf_fed_work_size_must_be_64[(sizeof(raf_fed_work) == 64u) ? 1 : -1];
typedef char raf_fed_receipt_size_must_be_64[(sizeof(raf_fed_receipt) == 64u) ? 1 : -1];

void raf_fed_validate(const void *work, void *receipt, raf_u32 io_error);
void raf_entry(void) __attribute__((noreturn));

raf_iptr raf_sys_read(raf_iptr descriptor, void *buffer, raf_size count);
raf_iptr raf_sys_write(raf_iptr descriptor, const void *buffer, raf_size count);
void raf_sys_exit(raf_u32 status) __attribute__((noreturn));

#endif
