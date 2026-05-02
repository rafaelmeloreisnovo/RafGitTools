#include "raf_geom.h"

#include <stdio.h>

static void print_q16(const char *k, q16_t v) {
  printf("%s=%.6f", k, (double)v / (double)Q16_ONE);
}

int main(void) {
  Toroid7 t;
  toroid7_init(&t, 0x12345678U);

  for (int i = 0; i < 42; ++i) {
    uint32_t in[7] = {
      (uint32_t)(i * 137U), (uint32_t)(i * 193U), (uint32_t)(i * 251U),
      (uint32_t)(i * 313U), (uint32_t)(i * 389U), (uint32_t)(i * 457U),
      (uint32_t)(i * 521U)
    };
    toroid7_step(&t, in, (q16_t)(Q16_HALF + (i % 5) * 1000), (q16_t)(Q16_HALF - (i % 7) * 800));
  }

  print_q16("H", t.H); printf("  ");
  print_q16("C", t.C); printf("  ");
  print_q16("phi", t.phi); printf("  ");
  print_q16("coh", t.coherence); printf("\n");
  printf("tick=%u attractor=%s\n", t.tick, attractor_name(toroid7_classify(&t)));

  return 0;
}
