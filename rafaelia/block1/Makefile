CC       ?= cc
AR       ?= ar
CFLAGS   ?= -std=c11 -Wall -Wextra -Wpedantic -O2 -fno-builtin
CPPFLAGS ?=
LDFLAGS  ?=
LDLIBS   ?=

.PHONY: all check clean

all: libraf_geom.a demo

libraf_geom.a: raf_geom.o
	$(AR) rcs $@ $^

raf_geom.o: raf_geom.c raf_geom.h
	$(CC) $(CPPFLAGS) $(CFLAGS) -c $< -o $@

demo: raf_geom_demo.o libraf_geom.a
	$(CC) $(LDFLAGS) raf_geom_demo.o libraf_geom.a $(LDLIBS) -o $@

raf_geom_demo.o: raf_geom_demo.c raf_geom.h
	$(CC) $(CPPFLAGS) $(CFLAGS) -c $< -o $@

check: all
	./demo | grep -Eq '^H=-?[0-9]+\.[0-9]{6}  C=-?[0-9]+\.[0-9]{6}  phi=-?[0-9]+\.[0-9]{6}  coh=-?[0-9]+\.[0-9]{6}$$'
	./demo | grep -Eq '^tick=42 attractor=(SOURCE|LIMIT|SPIRAL|TORUS|STRANGE|HOMO)$$'

clean:
	rm -f raf_geom.o raf_geom_demo.o libraf_geom.a demo
