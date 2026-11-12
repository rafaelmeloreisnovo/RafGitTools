/* SPDX-License-Identifier: GPL-3.0-or-later
 * Hosted-only deterministic adversarial tests for synthetic inputs.
 */
typedef unsigned int raf_gate_word;
typedef struct {
    raf_gate_word source;
    raf_gate_word artifact;
    raf_gate_word execution;
    raf_gate_word evidence;
    raf_gate_word authority;
} raf_gate_input_v1;
extern raf_gate_word raf_gate_blockers_v1(const raf_gate_input_v1 *);
#define CHECK(test) do { if (!(test)) return __LINE__; } while (0)
int main(void)
{
    raf_gate_input_v1 x = {1u, 1u, 1u, 1u, 1u};
    CHECK(raf_gate_blockers_v1(&x) == 0u);
    CHECK(raf_gate_blockers_v1((const raf_gate_input_v1 *)0) == 0x80000000u);
    x.source = 0u; CHECK(raf_gate_blockers_v1(&x) == 1u); x.source = 1u;
    x.source = 4u; CHECK(raf_gate_blockers_v1(&x) == 1u); x.source = 1u;
    x.artifact = 0u; CHECK(raf_gate_blockers_v1(&x) == 2u); x.artifact = 1u;
    x.execution = 3u; CHECK(raf_gate_blockers_v1(&x) == 4u); x.execution = 1u;
    x.evidence = 2u; CHECK(raf_gate_blockers_v1(&x) == 8u); x.evidence = 1u;
    x.authority = 999u; CHECK(raf_gate_blockers_v1(&x) == 16u); x.authority = 1u;
    x.source = 0u; x.artifact = 0u; x.execution = 0u;
    x.evidence = 0u; x.authority = 0u;
    CHECK(raf_gate_blockers_v1(&x) == 31u);
    x.source = 1u; x.artifact = 1u; x.execution = 1u;
    x.evidence = 1u; x.authority = 1u;
    CHECK(raf_gate_blockers_v1(&x) == 0u);
    return 0;
}
