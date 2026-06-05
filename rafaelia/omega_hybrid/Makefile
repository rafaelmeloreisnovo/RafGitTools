CC ?= cc
AR ?= ar
CFLAGS ?= -std=c99 -Wall -Wextra -Werror -O2 -ffreestanding -fno-builtin -fno-exceptions -fno-unwind-tables
LDFLAGS ?=

.PHONY: all test clean

all: libraf_omega.a demo test_raf_omega

libraf_omega.a: raf_omega.o
	$(AR) rcs $@ $^

raf_omega.o: raf_omega.c raf_omega.h
	$(CC) $(CFLAGS) -c raf_omega.c -o $@

demo: demo.o libraf_omega.a
	$(CC) demo.o libraf_omega.a $(LDFLAGS) -o $@

demo.o: demo.c raf_omega.h
	$(CC) $(CFLAGS) -c demo.c -o $@

test_raf_omega: tests/test_raf_omega.o libraf_omega.a
	$(CC) tests/test_raf_omega.o libraf_omega.a $(LDFLAGS) -o $@

tests/test_raf_omega.o: tests/test_raf_omega.c raf_omega.h
	$(CC) $(CFLAGS) -c tests/test_raf_omega.c -o $@

test: test_raf_omega
	./test_raf_omega

clean:
	rm -f raf_omega.o demo.o tests/test_raf_omega.o libraf_omega.a demo test_raf_omega
