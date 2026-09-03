# Rafaelia Kernel JNI Contract

**Version**: 1.0  
**Authority**: RafGitTools (Kotlin bridge layer)  
**Status**: IMPLEMENTED (Kotlin layer) | TOKEN_VAZIO_LLAMA_HEADER (JNI layer)  
**Target Library**: `librafcore.so` (NDK-compiled)  
**Calling Layer**: `RafaeliaKernelBridge.kt`

---

## Overview

The JNI contract defines the interface between the Kotlin kernel bridge and
the native C/C++ implementation in `kernel/native/raf_kernel_jni.c`.

The Kotlin layer is **IMPLEMENTED** and testable without the native layer.

The native layer remains **TOKEN_VAZIO** pending availability of `llama.h`
and the language model runtime.

---

## Function Contracts

### 1. `nativeAsmHealth(): Int`

**Purpose**: Health check for native assembler core library.

**Signature**:
```kotlin
external fun nativeAsmHealth(): Int

```

**Native Signature**:
```c
JNIEXPORT jint JNICALL Java_com_rafgittools_kernel_RafaeliaKernelBridge_nativeAsmHealth
  (JNIEnv *env, jobject obj)

```

**Returns**:
- `>= 8`: Library is loaded and healthy
- `< 8`: Library is degraded or unavailable
- Negative: Error

**Kotlin Usage**:
```kotlin
val health = bridge.nativeAsmHealth()
if (health >= 8) { /* proceed */ }

```

**Implementation Notes**:
- No external dependencies required
- Should check if core native library initialized
- Called before any LLM context initialization

---

### 2. `nativeAbiMask(): Int`

**Purpose**: Return bitmask of supported instruction sets (ABI).

**Signature**:
```kotlin
external fun nativeAbiMask(): Int

```

**Native Signature**:
```c
JNIEXPORT jint JNICALL Java_com_rafgittools_kernel_RafaeliaKernelBridge_nativeAbiMask
  (JNIEnv *env, jobject obj)

```

**Returns**: Bitmask:
- Bit 0: arm64-v8a (AArch64)
- Bit 1: armeabi-v7a (ARMv7)
- Bit 2: x86_64
- Bit 3: x86

**Example**:
```c
// AArch64 only
return 0x01;

// AArch64 + ARMv7
return 0x03;

```

---

### 3. `nativeContextInit(ctiPath: String, maxTokens: Int): Long`

**Purpose**: Initialize LLM context with CTI (Context Transfer Interface) path.

**Signature**:
```kotlin
external fun nativeContextInit(ctiPath: String, maxTokens: Int): Long

```

**Native Signature**:
```c
JNIEXPORT jlong JNICALL Java_com_rafgittools_kernel_RafaeliaKernelBridge_nativeContextInit
  (JNIEnv *env, jobject obj, jstring ctiPath, jint maxTokens)

```

**Parameters**:
- `ctiPath`: Absolute path to CTI initialization file (or memory buffer identifier)
  - **CRITICAL**: This path MUST be passed through to `llama_context_init()`
  - **Currently TOKEN_VAZIO**: llama.h not available; contract pending
  - Suggested llama.h integration:

    ```c
    llama_context *ctx = llama_context_init(
        ctiPath,              // <-- from Kotlin parameter
        maxTokens,
        /*other params*/
    );

    ```

- `maxTokens`: Maximum context window (typically 2048-4096)

**Returns**: Context ID (positive integer) or error code (negative)

**Error Codes**:
- `-1`: CTI path invalid or inaccessible
- `-2`: Model loading failed
- `-3`: Token limit out of range
- `-4`: Memory allocation failed

**Kotlin Usage**:
```kotlin
val contextId = nativeContextInit(ctiPath, 4096)
if (contextId < 0) { /* handle error */ }

```

**TOKEN_VAZIO Closure Path**:
1. Obtain or write `llama.h` header (from llama.cpp project or GGML)
2. Update `kernel/native/raf_kernel_jni.c` to include llama.h
3. Call `llama_context_init(ctiPath, maxTokens, ...)` with Kotlin-passed `ctiPath`
4. Return valid context ID
5. Test with `RafaeliaKernelBridgeTest` (recompile native layer)

---

### 4. `nativeInvokeTool(contextId: Long, toolName: String, arguments: String): String`

**Purpose**: Invoke a tool (function) via the LLM runtime.

**Signature**:
```kotlin
external fun nativeInvokeTool(contextId: Long, toolName: String, arguments: String): String

```

**Native Signature**:
```c
JNIEXPORT jstring JNICALL Java_com_rafgittools_kernel_RafaeliaKernelBridge_nativeInvokeTool
  (JNIEnv *env, jobject obj, jlong contextId, jstring toolName, jstring arguments)

```

**Parameters**:
- `contextId`: Context ID from `nativeContextInit()`
- `toolName`: Registered tool name (e.g., "execute_command", "read_file")
- `arguments`: JSON string with tool arguments

**Returns**: JSON string with tool result or error

**Example Arguments**:
```json
{"command": "ls -la /tmp"}

```

**Example Return**:
```json
{
  "exit_code": 0,
  "stdout": "total 48\n...",
  "stderr": ""
}

```

---

### 5. `nativeRunToolLoop(contextId: Long, prompt: String, maxIterations: Int): String`

**Purpose**: Execute a single turn of the LLM (potentially requesting tools).

**Signature**:
```kotlin
external fun nativeRunToolLoop(
    contextId: Long,
    prompt: String,
    maxIterations: Int
): String

```

**Native Signature**:
```c
JNIEXPORT jstring JNICALL Java_com_rafgittools_kernel_RafaeliaKernelBridge_nativeRunToolLoop
  (JNIEnv *env, jobject obj, jlong contextId, jstring prompt, jint maxIterations)

```

**Parameters**:
- `contextId`: Context ID from `nativeContextInit()`
- `prompt`: Input prompt or continuation
- `maxIterations`: Max iterations within this turn (usually 1 for single turn)

**Returns**: JSON response (either text or tool request)

**Response Types**:

### Type 1: Final Response (Text)

```json
{
  "type": "text",
  "text": "The answer is 42."
}
```

### Type 2: Tool Request (TOKEN_VAZIO_LLAMA_LOOP)

```json
{
  "type": "tool_use",
  "name": "execute_command",
  "input": {
    "command": "ls"
  }
}

```

**Multi-Turn Continuation (Currently TOKEN_VAZIO)**:
- **Current state**: Single turn; model output collected and returned to Kotlin
- **Multi-turn requirement**: Model should continue requesting tools until:
  - Max iterations reached, OR
  - Model explicitly signals completion, OR
  - Tool execution fails
- **Kotlin handles loop**: `RafaeliaKernelBridge.executeToolLoop()` wraps native single turns

---

### 6. `nativeContextCleanup(contextId: Long)`

**Purpose**: Free LLM context resources.

**Signature**:
```kotlin
external fun nativeContextCleanup(contextId: Long)

```

**Native Signature**:
```c
JNIEXPORT void JNICALL Java_com_rafgittools_kernel_RafaeliaKernelBridge_nativeContextCleanup
  (JNIEnv *env, jobject obj, jlong contextId)

```

**Parameters**:
- `contextId`: Context ID to free

**Side Effects**:
- Deallocates model memory
- Closes CTI resources
- Invalidates context ID for future calls

**Kotlin Usage**:
```kotlin
try {
    // ... use context ...
} finally {
    nativeContextCleanup(contextId)
}

```

---

## Data Type Mappings

| Kotlin | JNI | C |
| -------- | ----- | --- |
| `Int` | `jint` | `int` |
| `Long` | `jlong` | `long long` |
| `String` | `jstring` | `const char*` (UTF-8) |
| `Boolean` | `jboolean` | `jboolean` |

**String Handling**:
```c
// Get UTF-8 C string from jstring
const char *cstr = (*env)->GetStringUTFChars(env, javaString, NULL);

// Release when done
(*env)->ReleaseStringUTFChars(env, javaString, cstr);

// Create jstring from C string
jstring result = (*env)->NewStringUTF(env, cstr);

```

---

## Error Handling Strategy

**Kotlin Layer** (implemented):
- Catch exceptions from JNI calls
- Return `ToolLoopResult.Error` on failure
- Log via Android `Log.e()`

**Native Layer** (to implement):
- Do NOT throw exceptions across JNI boundary
- Return:
  - Negative integers for `*_Int()` functions (error code)
  - Null or error JSON string for text-returning functions
  - Empty string as fallback
- Use JNI error reporting:

  ```c
  (*env)->ThrowNew(env, exceptionClass, "error message");

  ```

---

## Testing Strategy

### Kotlin Unit Tests (Implemented)
- JSON parsing validation
- State machine logic
- No JNI dependency

**Run**:
```bash
./gradlew app:testDebugUnitTest -Pandroid.testInstrumentationRunnerArguments.class=com.rafgittools.kernel.RafaeliaKernelBridgeTest

```

### Integration Tests (TOKEN_VAZIO_FIXTURES)
- Requires native library + llama.h
- Physical device or emulator
- Mocked LLM responses

**Run** (when native available):
```bash
./gradlew app:connectedAndroidTest

```

---

## TOKEN_VAZIO Closure Plan

### Immediate (This Session)
- [x] Kotlin bridge implemented (`RafaeliaKernelBridge.kt`)
- [x] JNI contract documented (this file)
- [ ] Native stubs created (optional skeleton)

### Cycle 4 (Implementations)
- [ ] Obtain or write `llama.h`
- [ ] Implement `nativeContextInit()` with llama.h integration
- [ ] Implement `nativeRunToolLoop()` with single-turn execution
- [ ] Implement `nativeInvokeTool()` routing
- [ ] Compile NDK native library

### Cycle 5 (Multi-Turn Loop)
- [ ] Implement multi-turn tool call loop in native layer
- [ ] Close TOKEN_VAZIO_LLAMA_LOOP
- [ ] Integration test on device

### Cycle 6 (Validation)
- [ ] Cross-repository tracing (Mapa → RafGitTools → kernel)
- [ ] Federated authority mapping updated

---

## Authority & Responsibility

| Layer | Owner | Responsibility |
| ------- | ------- | --- |
| Kotlin Bridge | RafGitTools | Loop control, state management, error handling |
| JNI Shim | RafGitTools | Function dispatch, string marshalling |
| Native (C) | RafGitTools + GGML/llama.cpp | LLM inference, context management |
| LLM Runtime | llama.cpp | Model execution, tokenization, tool routing |

---

## Epistemic State

```yaml
RafaeliaKernelBridge.kt:     IMPLEMENTED
raf_kernel_jni.c:             TOKEN_VAZIO (awaits llama.h + implementation)
llama.h availability:         TOKEN_VAZIO (external dependency)
Multi-turn loop (native):      TOKEN_VAZIO_LLAMA_LOOP
Physical device tests:         TOKEN_VAZIO_RUNNER

```

**Claim Gate**: Cycle 6 (FEDERATION_CERTIFIED when cross-repo validation complete)

---

**Last Updated**: 2026-09-03  
**Next Review**: After llama.h availability  
**Contact**: RafGitTools Authority
