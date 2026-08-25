cmake_minimum_required(VERSION 3.22.1)
project(rafgittools_native_core LANGUAGES C CXX ASM)

# C++17 required for llamaRafaelia cognitive kernel integration.
# armeabi-v7a compiles __arm__ paths; arm64-v8a compiles __aarch64__ paths.

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(rafcore SHARED
    native_bridge.c
    asm/bootstrap.S
    asm/kernel.S
    asm/proot.S
    asm/dep_io.S
    asm/dep_sched.S
    asm/dep_mem.S
    asm/dep_sync.S
    asm/dep_flags.S
)

target_include_directories(rafcore PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_compile_options(rafcore PRIVATE -Wall -Wextra)
find_library(log-lib log)
target_link_libraries(rafcore ${log-lib})

# ---------------------------------------------------------------------------
# RAFAELIA engine JNI bridge — 7D toroidal EMA, CRC32C, hardware profile.
# B7 adds caller-owned triple buffers, 16 deterministic lanes and optional
# Vulkan/OpenCL callbacks. GPU claims remain disabled until a device receipt.
# ---------------------------------------------------------------------------
set(RAFAELIA_NATIVE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../kernel/native")

add_library(rafaelia SHARED
    "${RAFAELIA_NATIVE_DIR}/rafaelia_jni.c"
    raf_b7_orchestrator.c
)
target_include_directories(rafaelia PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${RAFAELIA_NATIVE_DIR}
)
target_compile_options(rafaelia PRIVATE -Wall -Wextra -O2 -ffunction-sections -fdata-sections)
target_link_libraries(rafaelia PRIVATE ${log-lib})

# ---------------------------------------------------------------------------
# RAF Kernel: llamaRafaelia cognitive kernel via add_subdirectory.
# runtime-src/ is populated by CI from runtime-lock.json — not committed.
# If absent (local dev), this block is silently skipped; only rafcore is built.
# ---------------------------------------------------------------------------
set(RAF_RUNTIME_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../runtime-src")
set(LLAMA_RUNTIME_SRC "${RAF_RUNTIME_ROOT}/llama-rafaelia")

if (EXISTS "${LLAMA_RUNTIME_SRC}/CMakeLists.txt")
    message(STATUS "RAF Kernel: llamaRafaelia found — building raf_llama_kernel")

    set(LLAMA_BUILD_TESTS     OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_TOOLS     OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES  OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER    OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_COMMON    OFF CACHE BOOL "" FORCE)
    set(LLAMA_CURL            OFF CACHE BOOL "" FORCE)
    set(GGML_OPENMP           OFF CACHE BOOL "" FORCE)
    set(GGML_LLAMAFILE        OFF CACHE BOOL "" FORCE)

    add_subdirectory(
        "${LLAMA_RUNTIME_SRC}"
        "${CMAKE_CURRENT_BINARY_DIR}/llama-rafaelia"
    )

    set(KERNEL_NATIVE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../kernel/native")

    add_library(raf_llama_kernel SHARED
        "${KERNEL_NATIVE_DIR}/raf_kernel_jni.c"
    )
    target_include_directories(raf_llama_kernel PRIVATE
        "${LLAMA_RUNTIME_SRC}/include"
        "${KERNEL_NATIVE_DIR}"
    )
    target_link_libraries(raf_llama_kernel PRIVATE llama ${log-lib})
else()
    message(STATUS "RAF Kernel: runtime-src/llama-rafaelia absent — skipped (CI populates via runtime-lock.json)")
endif()
