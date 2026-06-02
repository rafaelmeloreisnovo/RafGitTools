#include "../raf_omega.h"

#include <stdio.h>

#define CHECK(name, expr) do { if (!(expr)) { printf("FAIL %s\n", name); return 1; } printf("PASS %s\n", name); } while (0)

static uint32_t failing_expert(const uint8_t *data, size_t len, int16_t out_embed[RAF_OMEGA_EMBED], void *ctx) {
  (void)data;
  (void)len;
  (void)out_embed;
  (void)ctx;
  return 0;
}

int main(void) {
  static const uint8_t a[] = "RAFAELIA omega prova coerencia amor";
  static const uint8_t b[] = { 0xff, 0x00, 0x91, 0x33, 0x7c, 0x21, 0x44, 0xee };
  RafOmegaRuntime rt;
  RafOmegaState st;
  RafOmegaState before;
  RafTorus7 t;
  RafTorus7 att;

  raf_omega_init(&rt, &st, 0xA11CE5EEDULL);
  CHECK("init_crc", st.crc32c != 0u);
  CHECK("no_experts_initially", rt.expert_count == 0u);
  CHECK("register_text", raf_omega_register_expert(&rt, raf_omega_text_expert, 0, 256u, 1u) == 1u);
  CHECK("register_signal", raf_omega_register_expert(&rt, raf_omega_signal_expert, 0, 192u, 2u) == 1u);
  CHECK("register_failing", raf_omega_register_expert(&rt, failing_expert, 0, 128u, 99u) == 1u);
  CHECK("entropy_nonzero", raf_omega_entropy_milli(a, sizeof(a) - 1u) > 0u);

  raf_omega_toroidal_map(a, sizeof(a) - 1u, raf_omega_entropy_milli(a, sizeof(a) - 1u), raf_omega_fnv1a64(a, sizeof(a) - 1u, 0u), &t);
  raf_omega_apply_operator(&st, &t, RAF_OMEGA_OP_DIRECT);
  CHECK("direct_tick", st.tick == 1u);
  before = st;
  raf_omega_apply_operator(&st, 0, RAF_OMEGA_OP_INVERSE);
  CHECK("inverse_changes", st.torus.s[0] != before.torus.s[0]);
  raf_omega_apply_operator(&st, 0, RAF_OMEGA_OP_RECURSIVE);
  raf_omega_attractor(st.attractor_index, &att);
  CHECK("recursive_collapses", st.torus.s[0] == att.s[0] && st.torus.s[6] == att.s[6]);

  CHECK("snapshot", raf_omega_snapshot(&rt, &st) == 1u);
  before = st;
  st.torus.s[0] ^= 0x7777u;
  st.crc32c = 0u;
  CHECK("rollback", raf_omega_rollback(&rt, &st) == 1u);
  CHECK("rollback_restores", st.torus.s[0] == before.torus.s[0]);

  before = st;
  RafOmegaResult r = raf_omega_cycle(&rt, &st, b, sizeof(b), 7u);
  CHECK("failover_flag", (r.flags & (1u << 2u)) != 0u);
  CHECK("failsafe_void_or_valid_token", r.token_id == RAF_OMEGA_VOID_TOKEN || r.token_id <= RAF_OMEGA_ATTRACTORS);
  CHECK("crc_after_cycle", st.crc32c != 0u);

  RafOmegaState far = before;
  for (uint32_t i = 0; i < RAF_OMEGA_DIMS; ++i) far.torus.s[i] ^= 0x8000u;
  CHECK("kl_detects_gap", raf_omega_log_confidence_milli(&before, &far) > RAF_OMEGA_KL_LIMIT_MILLI);
  printf("RESULT all omega hybrid tests passed\n");
  return 0;
}
