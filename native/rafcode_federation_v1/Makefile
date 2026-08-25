.POSIX:

HOST_CC ?= gcc
ARMV7_CC ?= clang
AARCH64_CC ?= clang
BUILD ?= build

CPPFLAGS_COMMON = -nostdinc -Iinclude -DRAF_FED_FREESTANDING=1
CFLAGS_COMMON = -std=c11 -Os -ffreestanding -fno-builtin -fno-stack-protector \
	-fno-unwind-tables -fno-asynchronous-unwind-tables -fno-ident -fno-common \
	-fvisibility=hidden -ffunction-sections -fdata-sections -fomit-frame-pointer \
	-fno-optimize-sibling-calls \
	-fno-strict-aliasing -fwrapv -Wall -Wextra -Werror -Wshadow -Wconversion \
	-Wsign-conversion -Wundef -Wstrict-prototypes -Wmissing-prototypes \
	-Wold-style-definition -Wcast-align
HOST_ARCH_CFLAGS = -m64 -fcf-protection=none
LINK_COMMON = -nostdlib -static -no-pie -Wl,-e,_start -Wl,--gc-sections \
	-Wl,--strip-all -Wl,--build-id=none -Wl,--no-undefined -Wl,-z,noexecstack \
	-Wl,-T,linker/rafcode_federation.ld

CORE = src/rafcode_federation_core.c
ENTRY = src/rafcode_federation_entry.c

.PHONY: all host armv7 aarch64 test audit clean

all: host

host: $(BUILD)/host/rafcode-federation

$(BUILD)/host/rafcode-federation: $(CORE) $(ENTRY) arch/x86_64/syscalls.S include/rafcode_federation.h linker/rafcode_federation.ld
	mkdir -p $(BUILD)/host
	$(HOST_CC) $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) $(HOST_ARCH_CFLAGS) -c $(CORE) -o $(BUILD)/host/core.o
	$(HOST_CC) $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) $(HOST_ARCH_CFLAGS) -c $(ENTRY) -o $(BUILD)/host/entry.o
	$(HOST_CC) $(CPPFLAGS_COMMON) -m64 -c arch/x86_64/syscalls.S -o $(BUILD)/host/syscalls.o
	$(HOST_CC) $(LINK_COMMON) -Wl,--defsym,RAF_IMAGE_BASE=0x400000 \
		-Wl,-Map,$(BUILD)/host/rafcode-federation.map \
		$(BUILD)/host/core.o $(BUILD)/host/entry.o $(BUILD)/host/syscalls.o \
		-o $@

armv7: $(BUILD)/armv7/rafcode-federation

$(BUILD)/armv7/rafcode-federation: $(CORE) $(ENTRY) arch/armv7/syscalls.S include/rafcode_federation.h linker/rafcode_federation.ld
	mkdir -p $(BUILD)/armv7
	$(ARMV7_CC) --target=armv7a-linux-androideabi24 $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) \
		-march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4 -c $(CORE) -o $(BUILD)/armv7/core.o
	$(ARMV7_CC) --target=armv7a-linux-androideabi24 $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) \
		-march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4 -c $(ENTRY) -o $(BUILD)/armv7/entry.o
	$(ARMV7_CC) --target=armv7a-linux-androideabi24 $(CPPFLAGS_COMMON) \
		-march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4 -c arch/armv7/syscalls.S -o $(BUILD)/armv7/syscalls.o
	$(ARMV7_CC) --target=armv7a-linux-androideabi24 -fuse-ld=lld $(LINK_COMMON) \
		-Wl,--icf=all -Wl,--defsym,RAF_IMAGE_BASE=0x10000 \
		-Wl,-Map,$(BUILD)/armv7/rafcode-federation.map \
		$(BUILD)/armv7/core.o $(BUILD)/armv7/entry.o $(BUILD)/armv7/syscalls.o \
		-o $@

aarch64: $(BUILD)/aarch64/rafcode-federation

$(BUILD)/aarch64/rafcode-federation: $(CORE) $(ENTRY) arch/aarch64/syscalls.S include/rafcode_federation.h linker/rafcode_federation.ld
	mkdir -p $(BUILD)/aarch64
	$(AARCH64_CC) --target=aarch64-linux-android24 $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) \
		-march=armv8-a -c $(CORE) -o $(BUILD)/aarch64/core.o
	$(AARCH64_CC) --target=aarch64-linux-android24 $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) \
		-march=armv8-a -c $(ENTRY) -o $(BUILD)/aarch64/entry.o
	$(AARCH64_CC) --target=aarch64-linux-android24 $(CPPFLAGS_COMMON) \
		-march=armv8-a -c arch/aarch64/syscalls.S -o $(BUILD)/aarch64/syscalls.o
	$(AARCH64_CC) --target=aarch64-linux-android24 -fuse-ld=lld $(LINK_COMMON) \
		-Wl,--icf=all -Wl,--defsym,RAF_IMAGE_BASE=0x10000 \
		-Wl,-Map,$(BUILD)/aarch64/rafcode-federation.map \
		$(BUILD)/aarch64/core.o $(BUILD)/aarch64/entry.o $(BUILD)/aarch64/syscalls.o \
		-o $@

test: host
	sh tests/smoke.sh $(BUILD)/host/rafcode-federation

audit: host
	sh tools/audit_elf.sh $(BUILD)/host/rafcode-federation
	sh tools/audit_hotpath_x86_64.sh $(BUILD)/host/core.o

clean:
	rm -rf $(BUILD)
