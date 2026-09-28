cmake_minimum_required(VERSION 3.22.1)
project(rafminimal C)

add_library(rafcore STATIC core.c)
set_target_properties(rafcore PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_compile_options(rafcore PRIVATE
    -O3 -ffreestanding -fno-builtin
    -ffunction-sections -fdata-sections
    -Wall -Wextra -Werror
)

add_library(rafminimal SHARED native_bridge.c)
target_compile_options(rafminimal PRIVATE -O3 -Wall -Wextra -Werror)
target_link_libraries(rafminimal PRIVATE rafcore)
target_link_options(rafminimal PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
