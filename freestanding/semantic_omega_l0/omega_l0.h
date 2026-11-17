#ifndef RAF_OMEGA_SEMANTIC_L0_V1_H
#define RAF_OMEGA_SEMANTIC_L0_V1_H
/* Standalone C11 freestanding API. No libc, heap, JNI, NDK, SDK, or syscalls.
 * No source of evidence is validated here: typed input is supplied by a trusted
 * OUTER parser/custodian; these functions are only bounded structural gates.
 * ABI requires CHAR_BIT=8, sizeof(unsigned)=4 on the target compiler.
 */
_Static_assert(__CHAR_BIT__ == 8, "8-bit byte required");
_Static_assert(sizeof(unsigned int) == 4, "32-bit unsigned required");
typedef unsigned char raf_byte;
typedef unsigned int raf_u32;
typedef struct { const raf_byte *p; raf_u32 n; } raf_span;

enum raf_omega_input_kind {
    RAFK_ABSENT=0, RAFK_JSON_NULL=1, RAFK_STRING=2,
    RAFK_JSON_NUMBER=3, RAFK_JSON_BOOL=4,
    RAFK_TOKEN_UNRESOLVED=5, RAFK_TOKEN_ABORTED=6
};
enum raf_omega_state {
    RAFO_INVALID=0, RAFO_ABSENT=1, RAFO_NULL=2,
    RAFO_EMPTY=3, RAFO_SPACES=4, RAFO_TEXT=5,
    RAFO_NUM_ZERO=6, RAFO_NUM_NONZERO=7,
    RAFO_BOOL_FALSE=8, RAFO_BOOL_TRUE=9,
    RAFO_UNKNOWN=10, RAFO_ABORTED=11
};
typedef struct {
    raf_span bytes;
    raf_byte kind;
    raf_byte present;
    raf_byte scalar;
    raf_byte reserved;
} raf_omega_atom;

enum raf_omega_context {
    RAFC_NONE=0, RAFC_FOOD_SAUCE=1,
    RAFC_KEY_BUNDLE=2, RAFC_WET_VERB=3
};
enum raf_omega_sense {
    RAFS_UNMATCHED=0, RAFS_AMBIGUOUS=1,
    RAFS_FOOD=2, RAFS_KEYS=3, RAFS_WET=4
};
/* A context tag is a caller-provided hypothesis, NOT provenance proof.
 * authority_checked=0 means no sense may be selected. */
raf_u32 raf_omega_classify_v1(const raf_omega_atom *a);
raf_u32 raf_omega_molho_v1(raf_span lexeme, raf_u32 context,
                            raf_u32 authority_checked);

/* PIRC-2 structural filter: not an evidentiary verification.
 * ZERO does not mean PASS; zero only means no structural violation observed.
 * Levels 0..5; evidence_present merely means nonempty typed reference,
 * NOT that its contents are genuine; promotions never authorized here.
 */
typedef struct {
    raf_byte level;
    raf_byte evidence_present;
    raf_byte falsifier_present;
    raf_byte claim_allowed;
} raf_pirc_l0;
enum raf_pirc_violation {
    RAFV_LEVEL=1u, RAFV_UNSUPPORTED_PROMOTION=2u,
    RAFV_EVIDENCE_ABSENT=4u, RAFV_FALSIFIER_ABSENT=8u,
    RAFV_N5_ASSERTION=16u, RAFV_BAD_BOOLEAN=32u
};
raf_u32 raf_pirc_preflight_v1(const raf_pirc_l0 *claim);
#endif
