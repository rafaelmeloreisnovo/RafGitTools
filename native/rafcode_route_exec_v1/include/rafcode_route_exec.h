#ifndef RAFCODE_ROUTE_EXEC_H
#define RAFCODE_ROUTE_EXEC_H

#include "rafcode_route.h"

typedef __INTPTR_TYPE__ raf_route_iptr;
typedef __SIZE_TYPE__ raf_route_size;

void raf_route_entry(void) __attribute__((noreturn));

raf_route_iptr raf_route_sys_read(
    raf_route_iptr descriptor,
    void *buffer,
    raf_route_size count
);

raf_route_iptr raf_route_sys_write(
    raf_route_iptr descriptor,
    const void *buffer,
    raf_route_size count
);

void raf_route_sys_exit(raf_route_u32 status) __attribute__((noreturn));

#endif
