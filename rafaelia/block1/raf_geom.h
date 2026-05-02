#ifndef RAF_GEOM_H
#define RAF_GEOM_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef int32_t q16_t;

enum {
  Q16_ONE     = 65536,
  Q16_HALF    = 32768,
  Q16_SQRT3_2 = 56756,
  Q16_PI      = 205887,
  Q16_PHI     = 106039,
  Q16_PI_SIN279 = -203280,
};

typedef enum {
  ATT_SOURCE = 0,
  ATT_LIMIT,
  ATT_SPIRAL,
  ATT_TORUS,
  ATT_STRANGE,
  ATT_HOMO,
} AttractorClass;

typedef struct {
  uint32_t s[7];
  q16_t H;
  q16_t C;
  q16_t phi;
  q16_t coherence;
  uint32_t tick;
} Toroid7;

q16_t q16_mul(q16_t a, q16_t b);
q16_t q16_div(q16_t a, q16_t b);
q16_t q16_clamp01(q16_t x);
q16_t q16_spiral(q16_t v);
q16_t q16_phi_ethica(q16_t H, q16_t C);
q16_t q16_fraf_next(q16_t fn);

void toroid7_init(Toroid7 *t, uint32_t seed);
void toroid7_step(Toroid7 *t, const uint32_t coords[7], q16_t Hin, q16_t Cin);
q16_t toroid7_coherence(const Toroid7 *t);
AttractorClass toroid7_classify(const Toroid7 *t);
const char *attractor_name(AttractorClass c);

#ifdef __cplusplus
}
#endif

#endif
