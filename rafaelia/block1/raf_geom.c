#include "raf_geom.h"

#include <string.h>

static const uint32_t KAM7[7] = { 0x1357U, 0x9BDFU, 0x2468U, 0xACE1U, 0x1F2EU, 0x55AAU, 0xC33CU };

static inline int32_t i32_abs(int32_t x) { return x < 0 ? -x : x; }

q16_t q16_mul(q16_t a, q16_t b) {
  int64_t v = (int64_t)a * (int64_t)b;
  return (q16_t)(v >> 16);
}

q16_t q16_div(q16_t a, q16_t b) {
  if (b == 0) return 0;
  int64_t n = ((int64_t)a << 16);
  return (q16_t)(n / b);
}

q16_t q16_clamp01(q16_t x) {
  if (x < 0) return 0;
  if (x > Q16_ONE) return Q16_ONE;
  return x;
}

q16_t q16_spiral(q16_t v) {
  return q16_mul(v, Q16_SQRT3_2);
}

q16_t q16_phi_ethica(q16_t H, q16_t C) {
  return q16_mul(q16_clamp01(Q16_ONE - H), q16_clamp01(C));
}

q16_t q16_fraf_next(q16_t fn) {
  return q16_spiral(fn) - Q16_PI_SIN279;
}

static uint32_t xorshift32(uint32_t *s) {
  uint32_t x = *s;
  x ^= x << 13;
  x ^= x >> 17;
  x ^= x << 5;
  *s = x;
  return x;
}

void toroid7_init(Toroid7 *t, uint32_t seed) {
  if (!t) return;
  memset(t, 0, sizeof(*t));
  if (seed == 0) seed = 0xCAFEBABEU;
  for (int i = 0; i < 7; ++i) {
    t->s[i] = xorshift32(&seed) & 0xFFFFU;
  }
  t->H = Q16_HALF;
  t->C = Q16_HALF;
  t->phi = q16_phi_ethica(t->H, t->C);
}

q16_t toroid7_coherence(const Toroid7 *t) {
  if (!t) return 0;

  int64_t dot = 0;
  int64_t en_s = 0;
  int64_t en_k = 0;

  for (int i = 0; i < 7; ++i) {
    int32_t a = (int32_t)(t->s[i] & 0xFFFFU);
    int32_t b = (int32_t)KAM7[i];
    dot += (int64_t)a * b;
    en_s += (int64_t)a * a;
    en_k += (int64_t)b * b;
  }

  if (en_s == 0 || en_k == 0) return 0;

  int64_t den = (en_s + en_k) / 2;
  q16_t c = (q16_t)((dot << 16) / den);
  if (c < -Q16_ONE) c = -Q16_ONE;
  if (c >  Q16_ONE) c =  Q16_ONE;
  return c;
}

void toroid7_step(Toroid7 *t, const uint32_t coords[7], q16_t Hin, q16_t Cin) {
  if (!t || !coords) return;

  for (int i = 0; i < 7; ++i) {
    uint32_t oldv = t->s[i] & 0xFFFFU;
    uint32_t inv  = coords[i] & 0xFFFFU;
    uint32_t next = (oldv - (oldv >> 2) + (inv >> 2)) & 0xFFFFU;
    t->s[i] = next;
  }

  t->s[4] = (uint32_t)(q16_spiral((q16_t)t->s[4]) & 0xFFFFU);

  t->H = q16_clamp01((q16_t)(t->H - (t->H >> 2) + (Hin >> 2)));
  t->C = q16_clamp01((q16_t)(t->C - (t->C >> 2) + (Cin >> 2)));
  t->phi = q16_phi_ethica(t->H, t->C);
  t->coherence = toroid7_coherence(t);
  t->tick++;
}

AttractorClass toroid7_classify(const Toroid7 *t) {
  if (!t) return ATT_HOMO;
  q16_t coh = t->coherence;
  q16_t h = t->H;

  if (coh < -(Q16_ONE / 2)) return ATT_SOURCE;
  if (coh < -(Q16_ONE / 6)) return ATT_LIMIT;
  if (coh <  (Q16_ONE / 6)) return ATT_SPIRAL;
  if (coh <  (Q16_ONE / 2)) return ATT_TORUS;
  if (h > (q16_t)(0.9 * Q16_ONE)) return ATT_STRANGE;
  return ATT_HOMO;
}

const char *attractor_name(AttractorClass c) {
  switch (c) {
    case ATT_SOURCE: return "SOURCE";
    case ATT_LIMIT:  return "LIMIT";
    case ATT_SPIRAL: return "SPIRAL";
    case ATT_TORUS:  return "TORUS";
    case ATT_STRANGE:return "STRANGE";
    case ATT_HOMO:   return "HOMO";
    default: return "UNKNOWN";
  }
}
