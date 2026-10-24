# S12 — Zero-dependency L0 and host boundary
The current root AGENTS freestanding policy is retained. This file is a new bounded machine contract in `contracts/freestanding_service_s12.v1.json`; validate it using `python3 scripts/validate_freestanding_service_s12.py contracts/freestanding_service_s12.v1.json` and `python3 -m unittest discover -s tests -p test_freestanding_service_s12.py`.

**RafGitTools is an evidence/router consumer, not the owner of the C source.** RafPolimata owns the 13-class candidate header probe under `freestanding/service_matrix`; termux-app owns Android adapters. Mapa owns federation, and the connected account's selection or a merged PR is never proof of runtime success.

Thirteen distinct candidate header groups: ELF, DEX, CDEX, OAT, VDEX, ART, ZIP, AR, WASM, PNG, GZIP, MZ, MACHO. Seven friction lanes: JNI/Java; ART/JIT; SDK/NDK; heap/GC; Gradle/R8/D8/JDK; ELF/loader/syscall; flags/branches/tails/shadow/hex. This is **13×7 planning**, not 91 completed functions.

PASS from the Python contract validator establishes only structure. BUILD evidence additionally needs compiled objects, `nm -u`, `readelf` machine and regression fixtures, and CI at exact HEAD. Android success additionally needs packaged signed APK and physical install/launch/device receipt. `TOKEN_VAZIO != 0`. Do not auto-merge, delete or copy raw device content. No native OS functions enter the core; legal platform-only calls remain in declared hosted boundaries.

Research references (prior art, not copied code): https://www.alphaxiv.org/abs/2405.03883; https://www.alphaxiv.org/abs/2505.02521; https://www.alphaxiv.org/abs/2210.07311.