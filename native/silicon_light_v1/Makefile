.POSIX:

HOST_CC ?= clang
ARMV7_CC ?= clang
AARCH64_CC ?= clang
NM ?= nm
READELF ?= readelf
BUILD ?= build

CPPFLAGS = -nostdinc -Iinclude -DRAF_SILICON_LIGHT_FREESTANDING=1
CFLAGS = -std=c11 -Os -ffreestanding -fno-builtin -fno-stack-protector \
	-fno-unwind-tables -fno-asynchronous-unwind-tables -fno-ident -fno-common \
	-fvisibility=hidden -fno-vectorize -fno-slp-vectorize \
	-ffunction-sections -fdata-sections -fomit-frame-pointer \
	-fno-strict-aliasing -fwrapv -Wall -Wextra -Werror -Wshadow -Wconversion \
	-Wsign-conversion -Wundef -Wstrict-prototypes -Wmissing-prototypes \
	-Wold-style-definition -Wcast-align

CORE = src/raf_silicon_light.c
HEADER = include/raf_silicon_light.h
TEST = tests/test_silicon_light.c

.PHONY: all host-object host-test audit audit-source armv7-object aarch64-object audit-armv7 audit-aarch64 clean

all: host-test audit

host-object: $(BUILD)/host/raf_silicon_light.o

$(BUILD)/host/raf_silicon_light.o: $(CORE) $(HEADER)
	mkdir -p $(BUILD)/host
	$(HOST_CC) $(CPPFLAGS) $(CFLAGS) -c $(CORE) -o $@

host-test: host-object
	$(HOST_CC) -std=c11 -Wall -Wextra -Werror -Iinclude $(TEST) $(BUILD)/host/raf_silicon_light.o -o $(BUILD)/host/test_silicon_light
	$(BUILD)/host/test_silicon_light

audit-source:
	@if grep -REn '#[[:space:]]*include[[:space:]]*<' src include; then echo 'FAIL: external/system header'; exit 1; else echo 'PASS: system headers=0'; fi
	@if grep -REn '\b(malloc|calloc|realloc|free|syscall|open|close|read|write|socket|connect|pthread_|dlopen|dlsym|JNIEnv|JavaVM)\b' src include; then echo 'FAIL: forbidden runtime primitive'; exit 1; else echo 'PASS: forbidden runtime primitives=0'; fi

audit: audit-source host-object
	@if $(NM) -u $(BUILD)/host/raf_silicon_light.o | grep .; then echo 'FAIL: undefined symbols'; exit 1; else echo 'PASS: undefined symbols=0'; fi
	@if $(READELF) -r $(BUILD)/host/raf_silicon_light.o | grep -Eq 'R_[A-Za-z0-9_]+'; then echo 'FAIL: host relocations'; $(READELF) -r $(BUILD)/host/raf_silicon_light.o; exit 1; else echo 'PASS: host relocations=0'; fi

armv7-object:
	mkdir -p $(BUILD)/armv7
	$(ARMV7_CC) --target=armv7a-linux-androideabi24 $(CPPFLAGS) $(CFLAGS) \
		-march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4 \
		-c $(CORE) -o $(BUILD)/armv7/raf_silicon_light.o

aarch64-object:
	mkdir -p $(BUILD)/aarch64
	$(AARCH64_CC) --target=aarch64-linux-android24 $(CPPFLAGS) $(CFLAGS) \
		-march=armv8-a -c $(CORE) -o $(BUILD)/aarch64/raf_silicon_light.o

audit-armv7: armv7-object
	@if $(NM) -u $(BUILD)/armv7/raf_silicon_light.o | grep .; then echo 'FAIL: ARMv7 undefined symbols'; exit 1; else echo 'PASS: ARMv7 undefined symbols=0'; fi

audit-aarch64: aarch64-object
	@if $(NM) -u $(BUILD)/aarch64/raf_silicon_light.o | grep .; then echo 'FAIL: AArch64 undefined symbols'; exit 1; else echo 'PASS: AArch64 undefined symbols=0'; fi

clean:
	rm -rf $(BUILD)
