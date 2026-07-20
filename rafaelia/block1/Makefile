CC      ?= cc
AR      ?= ar
CFLAGS  ?= -std=c11 -Wall -Wextra -O2 -fno-builtin -fno-exceptions -lm
LDFLAGS ?= -lm

.PHONY: all demo clean

all: libraf_geom.a demo

libraf_geom.a: raf_geom.o
	$(AR) rcs $@ $^

raf_geom.o: raf_geom.c raf_geom.h
	$(CC) $(CFLAGS) -c raf_geom.c -o $@

demo: raf_geom_demo.o libraf_geom.a
	$(CC) raf_geom_demo.o libraf_geom.a $(LDFLAGS) -o $@

raf_geom_demo.o: raf_geom_demo.c raf_geom.h
	$(CC) $(CFLAGS) -c raf_geom_demo.c -o $@

clean:
	rm -f raf_geom.o raf_geom_demo.o libraf_geom.a demo
