# Manifold Route JNI Adapter V1

State: `IMPLEMENTED_SOURCE / CI_PENDING`

This layer is intentionally **hosted**, not freestanding.

```text
Android/Kotlin
→ two DirectByteBuffer frames (32 B each)
→ raf_route_jni.c
→ rafcode_route_v1
→ route receipt
→ ManifoldRouteBinding
```

The purpose is to make the already-audited route core callable from the workbench without moving Android, JNI, provider or serialization dependencies into the freestanding core.

## Dependency boundary

The JNI adapter depends on `jni.h` and the Android dynamic loader. The route core does not.

No Gson, Retrofit, OkHttp, Drive, GitHub or model runtime is used by the native adapter.

## Evidence levels

- JVM unit tests validate byte-frame encoding/decoding only.
- Android CI build validates that `librafroute.so` compiles and is packaged for configured ABIs.
- Neither proves physical JNI invocation on ARM hardware.
- Physical ARM32/ARM64 execution remains `TOKEN_VAZIO` until a device receipt exists.

`JNI_BUILD_PASS != DEVICE_EXECUTED`.
