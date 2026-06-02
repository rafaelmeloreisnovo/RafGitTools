#ifndef RAF_OMEGA_H
#define RAF_OMEGA_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define RAF_OMEGA_DIMS 7u
#define RAF_OMEGA_EMBED 64u
#define RAF_OMEGA_ATTRACTORS 42u
#define RAF_OMEGA_MAX_EXPERTS 8u
#define RAF_OMEGA_SNAPSHOTS 4u
#define RAF_OMEGA_VOID_TOKEN 0u
#define RAF_OMEGA_ALPHA_Q16 16384u
#define RAF_OMEGA_KL_LIMIT_MILLI 700u
#define RAF_OMEGA_PHI_ROLLBACK_Q16 19661u
#define RAF_OMEGA_DECAY_Q16 56756u

typedef int32_t raf_q16_t;

typedef struct {
  uint32_t s[RAF_OMEGA_DIMS];
} RafTorus7;

typedef struct {
  RafTorus7 torus;
  uint16_t entropy_milli;
  raf_q16_t coherence_q16;
  raf_q16_t H_q16;
  raf_q16_t C_q16;
  raf_q16_t phi_q16;
  uint32_t attractor_index;
  uint32_t tick;
  uint32_t rollback_count;
  uint64_t hash_chain;
  uint32_t crc32c;
} RafOmegaState;

typedef struct {
  RafOmegaState state;
  uint32_t crc32c;
  uint32_t valid;
} RafOmegaSnapshot;

typedef uint32_t (*RafOmegaExpertFn)(const uint8_t *data, size_t len, int16_t out_embed[RAF_OMEGA_EMBED], void *ctx);

typedef struct {
  RafOmegaExpertFn fn;
  void *ctx;
  uint16_t weight_q8;
  uint16_t domain_id;
} RafOmegaExpert;

typedef struct {
  RafOmegaExpert experts[RAF_OMEGA_MAX_EXPERTS];
  RafOmegaSnapshot snapshots[RAF_OMEGA_SNAPSHOTS];
  uint32_t expert_count;
  uint32_t snapshot_head;
  uint32_t fail_flags;
} RafOmegaRuntime;

typedef enum {
  RAF_OMEGA_OP_INVERSE = 0,
  RAF_OMEGA_OP_REVERSE = 1,
  RAF_OMEGA_OP_RECURSIVE = 2,
  RAF_OMEGA_OP_INDIRECT = 3,
  RAF_OMEGA_OP_ANALYTIC = 4,
  RAF_OMEGA_OP_RELATIVE = 5,
  RAF_OMEGA_OP_DIRECT = 6
} RafOmegaOperator;

typedef enum {
  RAF_OMEGA_CLASS_SPAM = 0,
  RAF_OMEGA_CLASS_HAM = 1,
  RAF_OMEGA_CLASS_UNSURE = 2
} RafOmegaClass;

typedef struct {
  uint32_t token_id;
  RafOmegaClass cls;
  uint32_t kl_milli;
  uint32_t attractor_index;
  uint32_t flags;
} RafOmegaResult;

uint64_t raf_omega_fnv1a64(const uint8_t *data, size_t len, uint64_t seed);
uint32_t raf_omega_crc32c(const uint8_t *data, size_t len, uint32_t seed);
uint16_t raf_omega_entropy_milli(const uint8_t *data, size_t len);
void raf_omega_attractor(uint32_t index, RafTorus7 *out);
void raf_omega_toroidal_map(const uint8_t *data, size_t len, uint16_t entropy_milli, uint64_t hash, RafTorus7 *out);
void raf_omega_init(RafOmegaRuntime *rt, RafOmegaState *st, uint64_t seed);
uint32_t raf_omega_register_expert(RafOmegaRuntime *rt, RafOmegaExpertFn fn, void *ctx, uint16_t weight_q8, uint16_t domain_id);
uint32_t raf_omega_snapshot(RafOmegaRuntime *rt, const RafOmegaState *st);
uint32_t raf_omega_rollback(RafOmegaRuntime *rt, RafOmegaState *st);
void raf_omega_apply_operator(RafOmegaState *st, const RafTorus7 *input, RafOmegaOperator op);
uint32_t raf_omega_log_confidence_milli(const RafOmegaState *predicted, const RafOmegaState *observed);
RafOmegaResult raf_omega_cycle(RafOmegaRuntime *rt, RafOmegaState *st, const uint8_t *data, size_t len, uint32_t observed_token);
uint32_t raf_omega_text_expert(const uint8_t *data, size_t len, int16_t out_embed[RAF_OMEGA_EMBED], void *ctx);
uint32_t raf_omega_signal_expert(const uint8_t *data, size_t len, int16_t out_embed[RAF_OMEGA_EMBED], void *ctx);

#ifdef __cplusplus
}
#endif

#endif
