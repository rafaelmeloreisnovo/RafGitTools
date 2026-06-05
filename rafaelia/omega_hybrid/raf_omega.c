#include "raf_omega.h"

#define FNV_OFFSET 1469598103934665603ULL
#define FNV_PRIME 1099511628211ULL
#define GOLD32 0x9E3779B9u
#define CRC32C_POLY_REV 0x82F63B78u

static raf_q16_t q16_mul(raf_q16_t a, raf_q16_t b) { return (raf_q16_t)(((int64_t)a * (int64_t)b) >> 16); }
static raf_q16_t q16_clamp01(raf_q16_t x) { return x < 0 ? 0 : (x > 65536 ? 65536 : x); }
static uint32_t rot32(uint32_t x, uint32_t r) { return (x >> (r & 31u)) | (x << ((32u - r) & 31u)); }
static uint64_t rot64(uint64_t x, uint32_t r) { return (x >> (r & 63u)) | (x << ((64u - r) & 63u)); }
static uint32_t torus_abs_delta(uint32_t a, uint32_t b) {
  uint32_t d = (a - b) & 0xFFFFu;
  uint32_t rd = (b - a) & 0xFFFFu;
  return d < rd ? d : rd;
}

uint64_t raf_omega_fnv1a64(const uint8_t *data, size_t len, uint64_t seed) {
  uint64_t h = seed ? seed : FNV_OFFSET;
  if (!data && len) return h ^ 0xBAD0BAD0BAD0BAD0ULL;
  for (size_t i = 0; i < len; ++i) {
    h ^= (uint64_t)data[i];
    h *= FNV_PRIME;
  }
  return h;
}

uint32_t raf_omega_crc32c(const uint8_t *data, size_t len, uint32_t seed) {
  uint32_t crc = ~seed;
  if (!data && len) return ~crc;
  for (size_t i = 0; i < len; ++i) {
    crc ^= data[i];
    for (uint32_t b = 0; b < 8u; ++b) {
      uint32_t mask = 0u - (crc & 1u);
      crc = (crc >> 1) ^ (CRC32C_POLY_REV & mask);
    }
  }
  return ~crc;
}

uint16_t raf_omega_entropy_milli(const uint8_t *data, size_t len) {
  uint8_t seen[256];
  uint32_t unique = 0;
  uint32_t transitions = 0;
  for (uint32_t i = 0; i < 256u; ++i) seen[i] = 0u;
  if (!data || !len) return 0;
  for (size_t i = 0; i < len; ++i) {
    uint8_t b = data[i];
    unique += (seen[b] ^ 1u);
    seen[b] = 1u;
    if (i) transitions += (uint32_t)(data[i] != data[i - 1]);
  }
  uint32_t e = (unique * 6000u) / 256u;
  if (len > 1u) e += (transitions * 2000u) / (uint32_t)(len - 1u);
  return (uint16_t)(e > 8000u ? 8000u : e);
}

static uint32_t prime_at(uint32_t index) {
  static const uint16_t primes[RAF_OMEGA_ATTRACTORS] = {
    2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,
    79,83,89,97,101,103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181
  };
  return primes[index % RAF_OMEGA_ATTRACTORS];
}

static uint32_t fib_at(uint32_t index) {
  uint32_t a = 1u, b = 1u;
  for (uint32_t i = 0; i < index; ++i) {
    uint32_t n = (a + b) & 0xFFFFu;
    a = b;
    b = n ? n : 1u;
  }
  return b;
}

void raf_omega_attractor(uint32_t index, RafTorus7 *out) {
  if (!out) return;
  uint32_t p = prime_at(index);
  uint32_t f = fib_at(index + 1u);
  for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) {
    uint32_t mix = (p * (d + 3u) * GOLD32) ^ rot32(f + index * 0x45D9F3Bu, d * 5u);
    out->s[d] = mix & 0xFFFFu;
  }
}

void raf_omega_toroidal_map(const uint8_t *data, size_t len, uint16_t entropy_milli, uint64_t hash, RafTorus7 *out) {
  (void)data;
  (void)len;
  if (!out) return;
  for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) {
    uint32_t mix = (uint32_t)rot64(hash, d * 9u);
    out->s[d] = ((mix ^ entropy_milli) * GOLD32) & 0xFFFFu;
  }
}

static void state_crc(RafOmegaState *st) {
  st->crc32c = 0u;
  st->crc32c = raf_omega_crc32c((const uint8_t *)st, sizeof(*st), 0u);
}

static raf_q16_t coherence_for(const RafTorus7 *a, const RafTorus7 *b) {
  uint32_t acc = 0;
  for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) acc += 65535u - torus_abs_delta(a->s[d], b->s[d]);
  return (raf_q16_t)((acc << 16) / (65535u * RAF_OMEGA_DIMS));
}

static uint32_t nearest_attractor(const RafTorus7 *t, RafTorus7 *att_out) {
  uint32_t best = 0;
  uint32_t best_dist = 0xFFFFFFFFu;
  RafTorus7 a;
  for (uint32_t i = 0; i < RAF_OMEGA_ATTRACTORS; ++i) {
    uint32_t dist = 0;
    raf_omega_attractor(i, &a);
    for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) dist += torus_abs_delta(t->s[d], a.s[d]);
    if (dist < best_dist) {
      best_dist = dist;
      best = i;
      if (att_out) *att_out = a;
    }
  }
  return best;
}

void raf_omega_init(RafOmegaRuntime *rt, RafOmegaState *st, uint64_t seed) {
  if (rt) {
    for (uint32_t i = 0; i < RAF_OMEGA_MAX_EXPERTS; ++i) rt->experts[i] = (RafOmegaExpert){0};
    for (uint32_t i = 0; i < RAF_OMEGA_SNAPSHOTS; ++i) rt->snapshots[i] = (RafOmegaSnapshot){0};
    rt->expert_count = 0;
    rt->snapshot_head = 0;
    rt->fail_flags = 0;
  }
  if (!st) return;
  *st = (RafOmegaState){0};
  uint64_t h = seed ? seed : FNV_OFFSET;
  for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) st->torus.s[d] = ((uint32_t)rot64(h ^ (uint64_t)(d + 1u) * GOLD32, d * 11u)) & 0xFFFFu;
  st->entropy_milli = 0;
  st->H_q16 = 32768;
  st->C_q16 = 32768;
  st->phi_q16 = q16_mul(65536 - st->H_q16, st->C_q16);
  st->attractor_index = nearest_attractor(&st->torus, 0);
  RafTorus7 a;
  raf_omega_attractor(st->attractor_index, &a);
  st->coherence_q16 = coherence_for(&st->torus, &a);
  st->tick = 0;
  st->rollback_count = 0;
  st->hash_chain = h;
  state_crc(st);
}

uint32_t raf_omega_register_expert(RafOmegaRuntime *rt, RafOmegaExpertFn fn, void *ctx, uint16_t weight_q8, uint16_t domain_id) {
  if (!rt || !fn || rt->expert_count >= RAF_OMEGA_MAX_EXPERTS) return 0;
  uint32_t i = rt->expert_count++;
  rt->experts[i].fn = fn;
  rt->experts[i].ctx = ctx;
  rt->experts[i].weight_q8 = weight_q8 ? weight_q8 : 256u;
  rt->experts[i].domain_id = domain_id;
  return 1;
}

uint32_t raf_omega_snapshot(RafOmegaRuntime *rt, const RafOmegaState *st) {
  if (!rt || !st) return 0;
  uint32_t i = rt->snapshot_head % RAF_OMEGA_SNAPSHOTS;
  rt->snapshots[i].state = *st;
  rt->snapshots[i].state.crc32c = 0u;
  rt->snapshots[i].crc32c = raf_omega_crc32c((const uint8_t *)&rt->snapshots[i].state, sizeof(RafOmegaState), 0u);
  rt->snapshots[i].valid = 1u;
  rt->snapshot_head = (rt->snapshot_head + 1u) % RAF_OMEGA_SNAPSHOTS;
  return 1;
}

uint32_t raf_omega_rollback(RafOmegaRuntime *rt, RafOmegaState *st) {
  if (!rt || !st) return 0;
  for (uint32_t n = 0; n < RAF_OMEGA_SNAPSHOTS; ++n) {
    uint32_t i = (rt->snapshot_head + RAF_OMEGA_SNAPSHOTS - 1u - n) % RAF_OMEGA_SNAPSHOTS;
    RafOmegaSnapshot *sn = &rt->snapshots[i];
    if (!sn->valid) continue;
    RafOmegaState tmp = sn->state;
    tmp.crc32c = 0u;
    if (raf_omega_crc32c((const uint8_t *)&tmp, sizeof(tmp), 0u) == sn->crc32c) {
      *st = sn->state;
      st->rollback_count++;
      state_crc(st);
      return 1;
    }
  }
  return 0;
}

void raf_omega_apply_operator(RafOmegaState *st, const RafTorus7 *input, RafOmegaOperator op) {
  if (!st) return;
  RafTorus7 att;
  switch (op) {
    case RAF_OMEGA_OP_INVERSE:
      for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) st->torus.s[d] = (0x10000u - st->torus.s[d]) & 0xFFFFu;
      break;
    case RAF_OMEGA_OP_REVERSE:
      if (input) for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) st->torus.s[d] = input->s[d] & 0xFFFFu;
      break;
    case RAF_OMEGA_OP_RECURSIVE:
      st->attractor_index = nearest_attractor(&st->torus, &att);
      for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) st->torus.s[d] = att.s[d];
      break;
    case RAF_OMEGA_OP_INDIRECT:
      st->attractor_index = nearest_attractor(&st->torus, &att);
      for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) st->torus.s[d] = (st->torus.s[d] + ((att.s[d] - st->torus.s[d]) >> 3)) & 0xFFFFu;
      break;
    case RAF_OMEGA_OP_ANALYTIC:
      for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) st->torus.s[d] = (st->torus.s[d] ^ rot32(st->torus.s[(d + 1u) % RAF_OMEGA_DIMS], 3u)) & 0xFFFFu;
      break;
    case RAF_OMEGA_OP_RELATIVE:
      for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) {
        uint32_t x = st->torus.s[d] & 0xFFFFu;
        st->torus.s[d] = x < 32768u ? ((x * x) >> 15) : (65535u - (((65535u - x) * (65535u - x)) >> 15));
      }
      break;
    case RAF_OMEGA_OP_DIRECT:
    default:
      if (input) for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) st->torus.s[d] = ((st->torus.s[d] * 3u + input->s[d]) >> 2) & 0xFFFFu;
      break;
  }
  st->attractor_index = nearest_attractor(&st->torus, &att);
  st->coherence_q16 = coherence_for(&st->torus, &att);
  st->phi_q16 = q16_mul(q16_clamp01(65536 - st->H_q16), q16_clamp01(st->C_q16));
  st->hash_chain = raf_omega_fnv1a64((const uint8_t *)st->torus.s, sizeof(st->torus.s), st->hash_chain);
  st->tick++;
  state_crc(st);
}

uint32_t raf_omega_log_confidence_milli(const RafOmegaState *predicted, const RafOmegaState *observed) {
  if (!predicted || !observed) return 8000u;
  uint32_t acc = 0;
  for (uint32_t d = 0; d < RAF_OMEGA_DIMS; ++d) acc += torus_abs_delta(predicted->torus.s[d], observed->torus.s[d]);
  uint32_t geom = (acc * 1000u) / (32768u * RAF_OMEGA_DIMS);
  uint32_t entropy = predicted->entropy_milli > observed->entropy_milli ? predicted->entropy_milli - observed->entropy_milli : observed->entropy_milli - predicted->entropy_milli;
  return geom + (entropy / 8u);
}

static void fuse_experts(RafOmegaRuntime *rt, const uint8_t *data, size_t len, uint8_t bytes[RAF_OMEGA_EMBED]) {
  int32_t acc[RAF_OMEGA_EMBED];
  int16_t tmp[RAF_OMEGA_EMBED];
  for (uint32_t i = 0; i < RAF_OMEGA_EMBED; ++i) acc[i] = 0;
  if (!rt || rt->expert_count == 0u) {
    for (uint32_t i = 0; i < RAF_OMEGA_EMBED; ++i) bytes[i] = (uint8_t)((i * 17u + len) & 0xFFu);
    return;
  }
  for (uint32_t e = 0; e < rt->expert_count; ++e) {
    for (uint32_t i = 0; i < RAF_OMEGA_EMBED; ++i) tmp[i] = 0;
    uint32_t ok = rt->experts[e].fn(data, len, tmp, rt->experts[e].ctx);
    uint32_t w = ok ? rt->experts[e].weight_q8 : 0u;
    rt->fail_flags |= ok ? 0u : (1u << (e & 31u));
    for (uint32_t i = 0; i < RAF_OMEGA_EMBED; ++i) acc[i] += (int32_t)tmp[i] * (int32_t)w;
  }
  for (uint32_t i = 0; i < RAF_OMEGA_EMBED; ++i) bytes[i] = (uint8_t)(((acc[i] >> 8) + 32768) & 0xFF);
}

RafOmegaResult raf_omega_cycle(RafOmegaRuntime *rt, RafOmegaState *st, const uint8_t *data, size_t len, uint32_t observed_token) {
  RafOmegaResult r = { RAF_OMEGA_VOID_TOKEN, RAF_OMEGA_CLASS_UNSURE, 8000u, 0u, 0u };
  if (!rt || !st || (!data && len)) { r.flags = 1u; return r; }
  RafOmegaState predicted = *st;
  uint8_t embed[RAF_OMEGA_EMBED];
  fuse_experts(rt, data, len, embed);
  uint16_t entropy = raf_omega_entropy_milli(embed, RAF_OMEGA_EMBED);
  uint64_t h = raf_omega_fnv1a64(embed, RAF_OMEGA_EMBED, st->hash_chain ^ observed_token);
  RafTorus7 input;
  raf_omega_toroidal_map(embed, RAF_OMEGA_EMBED, entropy, h, &input);
  raf_omega_snapshot(rt, st);
  st->entropy_milli = entropy;
  st->H_q16 = q16_clamp01((raf_q16_t)((st->H_q16 * 3 + ((uint32_t)entropy << 3)) >> 2));
  st->C_q16 = q16_clamp01((raf_q16_t)((st->C_q16 * 3 + st->coherence_q16) >> 2));
  raf_omega_apply_operator(st, &input, RAF_OMEGA_OP_DIRECT);
  r.kl_milli = raf_omega_log_confidence_milli(&predicted, st);
  if (r.kl_milli > RAF_OMEGA_KL_LIMIT_MILLI || st->phi_q16 < (raf_q16_t)RAF_OMEGA_PHI_ROLLBACK_Q16) {
    r.flags |= 2u;
    (void)raf_omega_rollback(rt, st);
    r.token_id = RAF_OMEGA_VOID_TOKEN;
  } else {
    raf_omega_apply_operator(st, 0, RAF_OMEGA_OP_RECURSIVE);
    r.attractor_index = st->attractor_index;
    r.cls = st->attractor_index < 3u ? RAF_OMEGA_CLASS_SPAM : (st->attractor_index < 6u ? RAF_OMEGA_CLASS_HAM : RAF_OMEGA_CLASS_UNSURE);
    r.token_id = r.cls == RAF_OMEGA_CLASS_UNSURE ? RAF_OMEGA_VOID_TOKEN : (st->attractor_index + 1u);
  }
  r.attractor_index = st->attractor_index;
  r.flags |= rt->fail_flags;
  return r;
}

uint32_t raf_omega_text_expert(const uint8_t *data, size_t len, int16_t out_embed[RAF_OMEGA_EMBED], void *ctx) {
  (void)ctx;
  if (!out_embed || (!data && len)) return 0;
  for (uint32_t i = 0; i < RAF_OMEGA_EMBED; ++i) out_embed[i] = 0;
  for (size_t i = 0; i < len; ++i) {
    uint8_t b = data[i];
    out_embed[i & 63u] += (int16_t)((b >= 'A' && b <= 'Z') ? (b + 32) : b);
    out_embed[(i * 7u + 3u) & 63u] ^= (int16_t)((uint16_t)b << (i & 3u));
  }
  return 1;
}

uint32_t raf_omega_signal_expert(const uint8_t *data, size_t len, int16_t out_embed[RAF_OMEGA_EMBED], void *ctx) {
  (void)ctx;
  if (!out_embed || (!data && len)) return 0;
  uint8_t prev = len ? data[0] : 0;
  for (uint32_t i = 0; i < RAF_OMEGA_EMBED; ++i) out_embed[i] = 0;
  for (size_t i = 0; i < len; ++i) {
    uint8_t b = data[i];
    int16_t grad = (int16_t)b - (int16_t)prev;
    out_embed[i & 63u] += grad;
    out_embed[(i * 5u + 1u) & 63u] += (int16_t)((grad * (int16_t)((i % 9u) + 1u)) >> 1);
    prev = b;
  }
  return 1;
}
