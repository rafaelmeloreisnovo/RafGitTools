cmake_minimum_required(VERSION 3.22.1)
project(rafminimal C)

set(RAF_L0_FLAGS
    -Os
    -ffreestanding
    -fno-builtin
    -fno-stack-protector
    -fno-unwind-tables
    -fno-asynchronous-unwind-tables
    -fno-vectorize
    -fno-slp-vectorize
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
    -Werror
)

add_library(rafsilicon STATIC silicon_light/raf_silicon_light.c)
target_include_directories(rafsilicon PUBLIC silicon_light)
target_compile_options(rafsilicon PRIVATE ${RAF_L0_FLAGS})
set_target_properties(rafsilicon PROPERTIES POSITION_INDEPENDENT_CODE ON)

add_library(rafcore STATIC core.c)
set_target_properties(rafcore PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_compile_options(rafcore PRIVATE
    -O3 -ffreestanding -fno-builtin
    -ffunction-sections -fdata-sections
    -Wall -Wextra -Werror
)

add_library(rafminimal SHARED native_bridge.c)
target_include_directories(rafminimal PRIVATE silicon_light)
target_compile_options(rafminimal PRIVATE -O3 -Wall -Wextra -Werror)
target_link_libraries(rafminimal PRIVATE rafcore rafsilicon)
target_link_options(rafminimal PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
