# RAFAELIA Authorial Keyboard L0 — isolated ARM input kernel V1

**State:** `SOURCE_IMPLEMENTED / HOST_ASSEMBLED_SCOPED`. **Not:** Android UI, APK, physical keyboard or web-session proof. `claim_allowed=false`; `release_allowed=false`.

## Source, authorship, licensing

This module was independently designed in this session at the register level. It neither copies nor imports BrowserRaf, third-party keyboards, classes, libraries, control-flow bodies or scripts. The familiar QWERTY letter arrangement is a functional UI convention rather than copied software source. Root `LICENSE` (GPL-3.0-or-later) remains authoritative for this folder. This statement records our construction method, **not** independent legal or novelty certification. No source copyright or license has been altered.

## Concrete L0 / source reconstruction

| Path | Role |
|---|---|
| `src/layout.inc` | Shared 50-byte ASCII table, 10 columns x 5 rows |
| `src/keyboard_armv7.S` | ARMv7 register-only coordinate decoder |
| `src/keyboard_aarch64.S` | AArch64 register-only coordinate decoder |
| `Makefile` | Hosted Clang assembler and gate, absent from runtime |
| `tests/check_contract.py` | Hosted reference/binary ELF audit, absent from runtime |

The native core uses zero C variables, C conditionals, C loops, heap, libc, syscalls, platform headers, dynamic calls, stack allocation or mutable globals. It does perform machine comparisons and conditional branches for safe bounds checks. Architecture-specific opcodes and registers cannot be eliminated without eliminating computation. `void` does not improve this ABI because the core intentionally returns a key code in a register; no C runtime entry is required.

`rafkbd_l0_probe`: input unsigned ARMv7 `r0=x` / `r1=y`, or AArch64 `w0=x` / `w1=y` (logical pixel coordinates); output `r0`/`w0` = ASCII key or `0xffffffff` when outside the 320×160 surface. Each of 50 cells is 32×32 pixels. `0xffffffff` is an ABI invalid-coordinate sentinel **not** RAFAELIA epistemic `TOKEN_VAZIO`.

The five rows are digits 0–9; qwertyuiop; asdfghjkl; ; zxcvbnm,./; and control codes Backspace, Tab, Space×4, LF, @, #, ?. These codes do **not** paint screen pixels or deliver touch events. Rendering, platform interrupts/input capture, physical keyboard, layout resize/rotation, security/privacy field handling, accessibility and Unicode accents remain outside L0.

## Architecture and boundaries

```text
L0 — own register decoder + own fixed ASCII table          SOURCE_IMPLEMENTED
  ↓ register ABI contract; no OS crossing inside L0
L1 — human-reviewed touch-to-coordinate + key-to-text adapter  NOT_IMPLEMENTED
  ↓
L2 — Android/BrowserRaf display surface and text editor        NOT_IMPLEMENTED
  ↓
L3 — exact-head CI / Android physical device / release        NOT_RUN for device
```

BrowserRaf is a separate producer and is not overwritten. A browser for real web login additionally needs secure browser state and rendering, separate from keyboard input. Freestanding L0 does not imply ISA-independent object code, no compiler, or no Android device services in the surrounding application.

## Build / evidence / falsifier

```sh
make -C native/authorial_keyboard_l0 clean all
```

The `START.yml` **Coherence** lane owns the cross-ISA assembler/object audit on the PR head. Hosted tests check 150 in-grid coordinate fixtures, 6 negative boundary fixtures and 6 ELF object/table/symbol/relocation predicates = **162 scoped checks**. Reference-model checks are not ISA runtime execution and do not grant physical/device PASS.

P0: independent review of license/authorship and compiler/readelf results. P1: minimal Android input/display adapter only with a deterministic device fixture. P2: accented Portuguese / shift / compose / accessibility / privacy hardening. P3: BrowserRaf integration after distinct web-security tests.

**Rollback:** close the unmerged draft branch; main unchanged. Preserve predecessor commits; new corrections supersede instead of silently deleting.

**R3:** `F_ok`=separate ARMv7/AArch64 L0 source + hosted object gate; `F_gap`=ISA emulator/device, L1/L2 display/input, full browser; `F_next`=exact-head Coherence CI and independent review before the Android adapter.
