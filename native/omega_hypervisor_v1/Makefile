.POSIX:
HOST_CC ?= clang
ARMV7_CC ?= clang
AARCH64_CC ?= clang
NM ?= nm
BUILD ?= build

CPPFLAGS=-nostdinc -Iinclude -DRAF_OMEGA_HYPERVISOR_L0=1
CFLAGS=-std=c11 -Os -ffreestanding -fno-builtin -fno-stack-protector \
 -fno-unwind-tables -fno-asynchronous-unwind-tables -fno-ident -fno-common \
 -fvisibility=hidden -ffunction-sections -fdata-sections -fomit-frame-pointer \
 -fno-strict-aliasing -fwrapv -Wall -Wextra -Werror -Wshadow -Wconversion \
 -Wsign-conversion -Wundef -Wstrict-prototypes -Wmissing-prototypes

CORE=src/raf_omega_hypervisor.c
HEADER=include/raf_omega_hypervisor.h
TEST=tests/test_omega_hypervisor.c

.PHONY: all host-test audit audit-source armv7-object aarch64-object audit-armv7 audit-aarch64 clean
all: host-test audit

$(BUILD)/host/core.o: $(CORE) $(HEADER)
	mkdir -p $(BUILD)/host
	$(HOST_CC) $(CPPFLAGS) $(CFLAGS) -c $(CORE) -o $@

host-test: $(BUILD)/host/core.o
	$(HOST_CC) -std=c11 -Wall -Wextra -Werror -Iinclude $(TEST) $(BUILD)/host/core.o -o $(BUILD)/host/test
	$(BUILD)/host/test

audit-source:
	@if grep -REn '#[[:space:]]*include[[:space:]]*<' src include; then echo 'FAIL system header'; exit 1; else echo 'PASS system headers=0'; fi
	@if grep -REn '\b(malloc|calloc|realloc|free|syscall|open|close|read|write|socket|connect|pthread_|dlopen|dlsym|JNIEnv|JavaVM|printf|fprintf|fopen)\b' src include; then echo 'FAIL forbidden runtime primitive'; exit 1; else echo 'PASS forbidden runtime primitives=0'; fi

audit: audit-source $(BUILD)/host/core.o
	@if $(NM) -u $(BUILD)/host/core.o | grep .; then echo 'FAIL undefined symbols'; exit 1; else echo 'PASS undefined symbols=0'; fi

armv7-object:
	mkdir -p $(BUILD)/armv7
	$(ARMV7_CC) --target=armv7a-linux-androideabi24 $(CPPFLAGS) $(CFLAGS) -march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4 -c $(CORE) -o $(BUILD)/armv7/core.o

aarch64-object:
	mkdir -p $(BUILD)/aarch64
	$(AARCH64_CC) --target=aarch64-linux-android24 $(CPPFLAGS) $(CFLAGS) -march=armv8-a -c $(CORE) -o $(BUILD)/aarch64/core.o

audit-armv7: armv7-object
	@if $(NM) -u $(BUILD)/armv7/core.o | grep .; then echo 'FAIL ARMv7 undefined symbols'; exit 1; else echo 'PASS ARMv7 undefined symbols=0'; fi

audit-aarch64: aarch64-object
	@if $(NM) -u $(BUILD)/aarch64/core.o | grep .; then echo 'FAIL AArch64 undefined symbols'; exit 1; else echo 'PASS AArch64 undefined symbols=0'; fi

clean:
	rm -rf $(BUILD)
