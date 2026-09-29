# Knowledge Campus L0 V1

Freestanding primitive kernel for content IDs, typed relation IDs and bounded HOT/WARM/COLD/ARCHIVE slot scoring.

Boundary:
- C11 freestanding;
- no system headers;
- no libc/allocator;
- no JNI/Android API;
- no external runtime symbols;
- fixed-size state only.

This kernel does not parse JSON, access GitHub, access Drive, perform network I/O or own private corpus bodies. Those are upper-layer responsibilities.

The score is a logistics heuristic, not an epistemic score and not evidence quality.
