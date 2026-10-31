.PHONY: all test clean coreobj arm32-obj arm64-obj
CC ?= cc
CFLAGS ?= -O2 -std=c11 -Wall -Wextra -Werror -fno-builtin
BUILDDIR := build
CORE := core/raf_lab_core.c
HDR := core/raf_lab_core.h
all: $(BUILDDIR)/raf_lab_host $(BUILDDIR)/test_core
$(BUILDDIR):
	mkdir -p $(BUILDDIR)
$(BUILDDIR)/raf_lab_host: $(CORE) $(HDR) host/raf_lab_host.c | $(BUILDDIR)
	$(CC) $(CFLAGS) -Icore $(CORE) host/raf_lab_host.c -pthread -o $@
$(BUILDDIR)/test_core: $(CORE) $(HDR) tests/test_core.c | $(BUILDDIR)
	$(CC) $(CFLAGS) -Icore $(CORE) tests/test_core.c -o $@
coreobj: | $(BUILDDIR)
	$(CC) $(CFLAGS) -ffreestanding -c $(CORE) -o $(BUILDDIR)/raf_lab_core.o
test: all coreobj
	./$(BUILDDIR)/test_core
	python3 -m unittest discover -s tests -p 'test_*.py' -v
	python3 tools/run_lab.py --smoke
arm32-obj: | $(BUILDDIR)
	clang --target=armv7a-none-eabi -std=c11 -O2 -ffreestanding -fno-builtin -nostdlib -c $(CORE) -o $(BUILDDIR)/raf_core_arm32.o
arm64-obj: | $(BUILDDIR)
	clang --target=aarch64-none-elf -std=c11 -O2 -ffreestanding -fno-builtin -nostdlib -c $(CORE) -o $(BUILDDIR)/raf_core_arm64.o
clean:
	rm -rf $(BUILDDIR)
