cmake_minimum_required(VERSION 3.22.1)
project(rafgittools_native_core LANGUAGES C ASM)

set(CMAKE_C_STANDARD 11)

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
find_library(log-lib log)
target_link_libraries(rafcore ${log-lib})
