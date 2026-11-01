# InventarioRAF core V1

This is a bounded, metadata-only tokenizer/record analyzer for SAF inventory
rows. Android's SAF provider remains the authenticated I/O boundary; the
freestanding core receives one path/MIME/size/parent record at a time. It does
not enumerate Drive, open file bodies, allocate heap memory, or perform syscalls.

The five deterministic passes are path safety, token spans, coarse file kind,
known-versus-unknown size, and parent relation presence. Tokens are byte spans
into caller-owned UTF-8 and are not copied by the core. This does not claim
linguistic stemming, semantic understanding of conversation content, or a
performance improvement.

ABI routing is compile-time: ARMv7 uses AAPCS32 assembly, AArch64 uses AAPCS64
assembly, and other architectures use the C fallback. The Android app currently
packages only `armeabi-v7a` and `arm64-v8a`; support for more ABIs requires a
separate build and artifact gate. `-Wall -Wextra -Werror` applies to the core.

The core's `TOKEN_VAZIO` boundary is explicit: a missing size is represented by
`size_known=0`, while a reported zero-byte item is `size_known=1` and
`reported_bytes=0`. Token spans and counts are structural metadata only.

Host self-test:

```sh
cc -std=c11 -Wall -Wextra -Werror -ffreestanding -fno-builtin \
  -fno-stack-protector -I native/inventario_raf_v1/include \
  native/inventario_raf_v1/src/inventario_raf_core.c \
  native/inventario_raf_v1/tests/selftest.c -o /tmp/inventario-raf-selftest
/tmp/inventario-raf-selftest
```

The host self-test exercises the C fallback. ARM assembly and Android packaging
still require exact-ABI CI plus physical device/provider evidence.
