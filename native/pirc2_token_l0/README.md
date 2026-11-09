# RAFAELIA PIRC-2 token L0 — bounded freestanding scanner

Scope: independent byte classification helper. **Not** a language model, keyboard,
Magick replacement, or Android APK. The whole existing repository is unchanged
outside this new, isolated directory. The repository's existing license governs.

- Accepts an explicit present flag, byte pointer and length (up to 4096 bytes).
- Separates ABSENT, EMPTY, ASCII-whitespace-only, actual NUL byte(s),
  ordinary UTF-8 text, malformed UTF-8, invalid caller input and oversize input.
- ASCII "0", ASCII "NULL" and molho remain ordinary text, not zero,
  SQL NULL or a uniquely determined meaning. Semantic-level is always N5:
  bytes cannot prove sense, origin, translation or truth.
- Keeps raw source bytes in caller storage; does not normalize, transcode,
  remove punctuation, allocate, touch files, use syscalls or call platform APIs.
- Rejects UTF-8 overlong encodings, surrogates, truncated sequences, invalid
  continuations and code points above U+10FFFF. Does not perform grapheme
  segmentation or Unicode whitespace categorization.
- Caller proves the buffer is readable for count bytes. Portable C cannot
  prove pointer capacity from the pointer alone. Count cap limits work, not
  memory safety if the caller lies about allocation size.

Build/test independently (host only required for harness):

    make -C native/pirc2_token_l0 test
    make -C native/pirc2_token_l0 armv7 aarch64
    nm -u /tmp/raf_pirc2_l0/pirc2_armv7.o /tmp/raf_pirc2_l0/pirc2_aarch64.o

L0 .o targets have no C library, Android SDK, NDK, Gradle, Java, linker
runtime or JNI dependency in their source. A working compiler/linker and
target ABI remain necessary to produce/use an artifact. The Makefile is a
hosted build adapter, not part of the freestanding runtime.

Falsifiers: absent != empty != whitespace != NUL; "0" and "NULL" must
never be coerced; malformed UTF-8 must be rejected; molho must remain N5;
oversize must read zero input bytes; cross-target objects must have no undefined
external symbols. Host success does not prove physical ARM execution.

PIRC-2 epistemic contract remains in the control plane; this L0 classifies
bytes only. No factual claims promoted. PIRC-2 parabolas are a separate
structured inference layer, not silently emitted from byte sequences.

Rollback: close the DRAFT pull request without merging. Main stays intact.
Evidence state: source/test artifacts and exact HEAD gates must be recorded
separately. claim_allowed=false; Android_E2E=NOT_RUN.
