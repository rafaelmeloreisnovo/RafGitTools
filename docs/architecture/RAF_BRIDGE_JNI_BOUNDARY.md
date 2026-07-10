# Raf Bridge — JNI Boundary

Status: explicit architectural decision

## Decision

JNI is **not used** in the v0.1 Kiwi/APK bridge.

The current path requires only:

```text
loopback ServerSocket
+ JSON contract validation
+ loopback HttpURLConnection
+ Android foreground service
```

Adding JNI here would not reduce abstraction. It would add:

- another ABI boundary;
- native memory ownership;
- extra build variants;
- crash surface outside the managed runtime;
- more difficult auditing;
- no measurable benefit for the current I/O path.

Therefore:

```text
no native need -> no JNI
```

## When JNI becomes justified

JNI is allowed only when the model runtime itself is embedded in the APK, for example direct llama.cpp inference without a separate localhost server.

At that point the native boundary must remain authorial and narrow:

```c
int raf_model_open(const char *model_path);
int raf_model_chat(const char *json_request, char *out, size_t out_size);
void raf_model_close(void);
```

Rules:

1. fixed C ABI;
2. no reflection;
3. no generic native command dispatcher;
4. no shell execution;
5. explicit buffer sizes;
6. deterministic error codes;
7. one ownership rule per buffer;
8. ARMv7 and ARM64 tests;
9. fuzzing of JSON/native boundaries;
10. moral contract validation stays in Java before native inference.

The invariant is:

```text
JNI serves inference only.
JNI never becomes an unrestricted execution tunnel.
```
