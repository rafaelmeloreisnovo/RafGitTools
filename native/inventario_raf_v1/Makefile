.POSIX:

HOST_CC ?= cc
ARMV7_CC ?= clang
AARCH64_CC ?= clang
NM ?= nm
READELF ?= readelf
LD_LLD ?= ld.lld
BUILD ?= build

CPPFLAGS_CORE = -nostdinc -DRAFINV_FREESTANDING=1 -Iinclude
CFLAGS_CORE = -std=c11 -Os -ffreestanding -fno-builtin -fno-stack-protector \
	-fno-unwind-tables -fno-asynchronous-unwind-tables -fno-ident \
	-fno-common -fvisibility=hidden -ffunction-sections -fdata-sections \
	-fomit-frame-pointer -Wall -Wextra -Werror -Wshadow -Wconversion \
	-Wsign-conversion -Wundef -Wstrict-prototypes -Wmissing-prototypes \
	-Wold-style-definition -Wcast-align

CORE = src/inventario_raf_core.c
HEADER = include/inventario_raf_core.h
TEST = tests/selftest.c

.PHONY: test host-test armv7-object aarch64-object abi-audit format-probe-gate clean

test: host-test armv7-object aarch64-object abi-audit format-probe-gate

format-probe-gate:
	$(MAKE) -C format_probe test properties objects

host-test: $(BUILD)/host/selftest
	$(BUILD)/host/selftest

$(BUILD)/host/selftest: $(CORE) $(TEST) $(HEADER)
	mkdir -p $(BUILD)/host
	$(HOST_CC) -std=c11 -Wall -Wextra -Werror -Iinclude $(CORE) $(TEST) -o $@

armv7-object:
	mkdir -p $(BUILD)/armv7
	$(ARMV7_CC) --target=armv7a-linux-gnueabihf $(CPPFLAGS_CORE) $(CFLAGS_CORE) \
		-march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4 -c $(CORE) \
		-o $(BUILD)/armv7/core.o
	$(ARMV7_CC) --target=armv7a-linux-gnueabihf -c asm/arm32/path_components.S \
		-o $(BUILD)/armv7/path_components.o
	$(LD_LLD) -r $(BUILD)/armv7/core.o $(BUILD)/armv7/path_components.o \
		-o $(BUILD)/armv7/combined.o

aarch64-object:
	mkdir -p $(BUILD)/aarch64
	$(AARCH64_CC) --target=aarch64-linux-gnu $(CPPFLAGS_CORE) $(CFLAGS_CORE) \
		-march=armv8-a -c $(CORE) -o $(BUILD)/aarch64/core.o
	$(AARCH64_CC) --target=aarch64-linux-gnu -c asm/arm64/path_components.S \
		-o $(BUILD)/aarch64/path_components.o
	$(LD_LLD) -r $(BUILD)/aarch64/core.o $(BUILD)/aarch64/path_components.o \
		-o $(BUILD)/aarch64/combined.o

abi-audit: $(BUILD)/armv7/combined.o $(BUILD)/aarch64/combined.o
	@for object in $^; do \
		undefined="$$($(NM) -u "$$object")"; \
		if [ -n "$$undefined" ]; then echo "FAIL: unresolved symbols in $$object"; echo "$$undefined"; exit 1; fi; \
		echo "PASS: $$object undefined symbols=0"; \
	done
	@$(READELF) -h $(BUILD)/armv7/combined.o | grep -q 'Machine:.*ARM'
	@$(READELF) -h $(BUILD)/aarch64/combined.o | grep -q 'Machine:.*AArch64'

clean:
	rm -rf $(BUILD)
