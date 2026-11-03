# Cross-source exact-byte matcher V1

State: local host test only; claim_allowed=false. This is a bounded reusable primitive, not a global scan, semantic matcher, provider integration or proof of authorship. No source content or credentials are included.

`rafaelia_cross_source_match_v1` scans caller-owned source and needle buffers, reports one byte offset, and advances a caller-owned cursor to permit overlapping matches and resumption within the same immutable buffer. Return values: -1 invalid arguments, 0 exhausted, 1 match. Persisting the cursor must bind it to exact provider identity, version/hash and needle; a changed source requires a new scan. Empty needles are rejected. No normalization, Unicode folding, tokenization, provenance validation, pagination, network or storage is implemented. An exact match indicates matching bytes only, not origin, authorship or execution.

Freestanding core has no allocation, I/O, system functions or libc calls. Provider adapters necessarily have separate platform boundaries and are not implemented here. No existing variable, ABI or file has been renamed. Hosted test has a main and is not part of the core.

Local verification: C11 x86-64 object compiled with -ffreestanding -fno-builtin -Wall -Wextra -Wconversion -Werror; `nm -u` returned no undefined references; hosted synthetic test exited 0 for overlapping matches, exhaustion, empty needle, invalid cursor and a needle longer than source. ARM, CI, real repositories and Drive cross-provider execution NOT_RUN. The tests do not prove complete coverage.

Next: immutable version-bound provider adapters, incremental checkpoint and scoped source/edge receipts, privacy and permission boundaries, real cross-provider canary, and target-ABI build and run. Do not represent absent matches as absence from the whole corpus. Rollback: close/revert this isolated branch; no data migration.
