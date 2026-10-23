/* RAFAELIA -- toolchain matrix L0 / authorial fixed-size byte gate.
 * INPUT : eight rows x eight bytes; one-byte unsigned version parts.
 * OUTPUT: [0]=typed conflict mask, [1]=evidence guard (1=not validated).
 * External symbols: rmr_toolchain_l0 only. No libc, heap, syscalls, OS calls,
 * imported helpers, mutable globals, loops or conditional branches in source.
 * This pure L0 kernel neither interprets YAML/Gradle nor proves a DEX build.
 */
__attribute__((visibility("default")))
void rmr_toolchain_l0(const unsigned char *const p, unsigned char *const o)
{
    /* Bit 0: Gradle >=9 with Kotlin plugin <2: upstream unsupported. */
    o[0] = (unsigned char)(
        (((p[0] >= 9u) & (p[16] < 2u))                          << 0u) |
        (((p[16] != p[24]) | (p[17] != p[25]) |
          (p[18] != p[26]))                                    << 1u) |
        (((p[16] == 1u) & (p[17] == 9u) & (p[18] == 24u) &
          ((p[32] != 1u) | (p[33] != 5u) | (p[34] != 14u)))     << 2u) |
        ((p[40] != 17u)                                        << 3u) |
        (((p[48] & 3u) != 3u)                                  << 4u) |
        ((p[56] != 0u)                                         << 5u)
    );
    /* Never infer that Gradle, Android, DEX or physical devices passed. */
    o[1] = 1u;
}
