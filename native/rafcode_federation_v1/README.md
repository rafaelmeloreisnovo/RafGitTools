# RafCode Federation V1 — freestanding fixed-frame kernel

State: `IMPLEMENTED_PENDING_CROSS_ABI_RECEIPTS`  
Owner: `rafaelmeloreisnovo/RafGitTools`  
Federated authority: `rafaelmeloreisnovo/Mapa`  
Claim gate: `claim_allowed=false`

This module is the minimal executable control-plane envelope for the existing RAFAELIA federation contract. It binds the three Drive/Mapa memory axes (`LONGITUDINAL`, `ORTHOGONAL`, `TRANSVERSAL`) to the four runtime participants (`CONTROL`, `EXECUTOR`, `EVIDENCE`, `VM`) without importing JSON, libc, CRT, allocator, heap, garbage collector, dynamic loader or platform library into the binary.

It is a loaderless Linux/Android userspace ELF using raw kernel syscalls. It is not physical bare-metal firmware and it does not by itself prove device execution or end-to-end federation.

## Fixed wire contract

- stdin: one little-endian `raf_fed_work` frame, exactly 64 bytes;
- stdout: one little-endian `raf_fed_receipt` frame, exactly 64 bytes;
- exit `0`: structurally accepted;
- exit `2`: rejected fail-closed;
- exit `3`: output syscall did not accept the complete receipt.

One read and one write are deliberate: the runtime contract is a fixed atomic frame, not a streaming parser. A short read is rejected and the zero-initialized frame prevents stack disclosure.

State codes reproduce the eight canonical gap states (`TOKEN_VAZIO` through `RESOLVED`), and action codes reproduce all 16 steps of the Mapa federated transition flow. They are defined in the header and repeated in the machine-readable contract; unknown codes fail closed.

## Hotpath

`raf_fed_validate(void *, void *, u32)` is branchless at source level and has zero source loops. The compiler emits its assembly as a named intermediate, the build verifies zero branches and zero calls, and only then assembles the exact checked text into the linked object. The x86-64 object receives a second disassembly gate. Boundary code has zero source loops as well. The validator rejects:

- incomplete `L/O/T` axes or participant masks;
- missing transaction, input identity or route identity;
- action/state values outside the bounded contract;
- `claim_allowed=true` or private-path exposure;
- mutating work without rollback binding;
- missing receipt binding;
- an evidenced/resolved state without evidence binding;
- unknown flag bits or a short input frame.

The emitted `trace_tag` is a deterministic, non-cryptographic structural tag. It is not HMAC, a signature or an authenticity proof; that boundary remains `TOKEN_VAZIO_HMAC_NOT_IMPLEMENTED`.

## Preprocessor, compiler and linker gates

Preprocessor:

```text
-nostdinc -Iinclude -DRAF_FED_FREESTANDING=1
```

Compiler:

```text
-std=c11 -Os -ffreestanding -fno-builtin -fno-stack-protector
-fno-unwind-tables -fno-asynchronous-unwind-tables -fno-ident -fno-common
-fvisibility=hidden -ffunction-sections -fdata-sections -fomit-frame-pointer
-fno-optimize-sibling-calls
-Werror -Wshadow -Wconversion -Wsign-conversion -Wmissing-prototypes
```

Linker:

```text
-nostdlib -static -Wl,-no-pie -e _start --gc-sections --strip-all
--build-id=none --no-undefined -z noexecstack
```

The linker script asserts zero writable static data, zero BSS/global state, zero relocation sections and zero GOT/PLT indirection. Relocation and indirection occupy separate zero-sized output classes so GNU ld and lld enforce the same negative contract. The ELF audit additionally requires the expected machine, one `PT_LOAD`, no `PT_INTERP`, no `DT_NEEDED`, no relocation and no undefined symbol.

These are runtime guarantees. The build uses only the selected compiler, assembler, linker and POSIX audit tools; the workflow performs a native Git fetch instead of importing a checkout action.

## Build and verify

```sh
make host
make test
make audit
```

Cross-build sources are isolated by ABI:

```sh
make armv7
make aarch64
```

ARMv7 uses `-march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4`; AArch64 uses `-march=armv8-a`. Each ABI owns its syscall assembly. Cross-compilation is evidence only after its exact toolchain/run/hash receipt exists; physical execution remains a separate device gate.

## Authority and provenance

- triaxial memory/index: `Mapa:data/memory/RAFAELIA_TRIAXIAL_MEMORY_INDEX_V1.json`;
- federated work contract: `Mapa:data/control-plane/RAFAELIA_FEDERATED_WORK_SERVICE_CONTRACT.v1.json`;
- runtime participants: `RafGitTools:contracts/rafaelia-federated-runtime-v1.json`;
- local machine contract: `contract/rafcode-federation-v1.json`.

`VISÃO != ARTEFATO != EXECUÇÃO != EVIDÊNCIA != CLAIM`.
