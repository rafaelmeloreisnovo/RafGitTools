#include "raf_omega.h"

#include <stdio.h>

static const char *cls_name(RafOmegaClass cls) {
  switch (cls) {
    case RAF_OMEGA_CLASS_SPAM: return "SPAM";
    case RAF_OMEGA_CLASS_HAM: return "HAM";
    case RAF_OMEGA_CLASS_UNSURE: return "UNSURE";
    default: return "UNKNOWN";
  }
}

int main(void) {
  static const uint8_t sample[] = "cachorro brincando no rio / dog playing in river";
  RafOmegaRuntime rt;
  RafOmegaState st;
  raf_omega_init(&rt, &st, 0x52414641454C4941ULL);
  (void)raf_omega_register_expert(&rt, raf_omega_text_expert, 0, 256u, 1u);
  (void)raf_omega_register_expert(&rt, raf_omega_signal_expert, 0, 192u, 2u);

  for (uint32_t i = 0; i < 7u; ++i) {
    RafOmegaResult r = raf_omega_cycle(&rt, &st, sample, sizeof(sample) - 1u, i + 1u);
    printf("cycle=%u token=%u class=%s attractor=%u kl_milli=%u phi_q16=%d rollbacks=%u flags=0x%08x\n",
           i + 1u, r.token_id, cls_name(r.cls), r.attractor_index, r.kl_milli,
           st.phi_q16, st.rollback_count, r.flags);
  }
  return 0;
}
