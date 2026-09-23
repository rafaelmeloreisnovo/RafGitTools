.POSIX:

HOST_CC ?= clang
ARMV7_CC ?= clang
AARCH64_CC ?= clang
NM ?= nm
READELF ?= readelf
BUILD ?= build

CPPFLAGS_COMMON = -nostdinc -Iinclude -DRAF_ROUTE_FREESTANDING=1
CFLAGS_COMMON = -std=c11 -Os -ffreestanding -fno-builtin -fno-stack-protector \
	-fno-unwind-tables -fno-asynchronous-unwind-tables -fno-ident -fno-common \
	-fvisibility=hidden -ffunction-sections -fdata-sections -fomit-frame-pointer \
	-fno-strict-aliasing -fwrapv -Wall -Wextra -Werror -Wshadow -Wconversion \
	-Wsign-conversion -Wundef -Wstrict-prototypes -Wmissing-prototypes \
	-Wold-style-definition -Wcast-align

CORE = src/rafcode_route_core.c
HEADER = include/rafcode_route.h
TEST = tests/test_route_core.c

.PHONY: all host-object host-test armv7-object aarch64-object audit clean

all: host-test audit

host-object: $(BUILD)/host/rafcode_route_core.o

$(BUILD)/host/rafcode_route_core.o: $(CORE) $(HEADER)
	mkdir -p $(BUILD)/host
	$(HOST_CC) $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) -c $(CORE) -o $@

host-test: host-object
	$(HOST_CC) -std=c11 -Wall -Wextra -Werror -Iinclude $(TEST) $(BUILD)/host/rafcode_route_core.o -o $(BUILD)/host/test_route_core
	$(BUILD)/host/test_route_core

armv7-object: $(BUILD)/armv7/rafcode_route_core.o

$(BUILD)/armv7/rafcode_route_core.o: $(CORE) $(HEADER)
	mkdir -p $(BUILD)/armv7
	$(ARMV7_CC) --target=armv7a-linux-androideabi24 $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) \
		-march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4 -c $(CORE) -o $@

aarch64-object: $(BUILD)/aarch64/rafcode_route_core.o

$(BUILD)/aarch64/rafcode_route_core.o: $(CORE) $(HEADER)
	mkdir -p $(BUILD)/aarch64
	$(AARCH64_CC) --target=aarch64-linux-android24 $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) \
		-march=armv8-a -c $(CORE) -o $@

audit: host-object
	@if $(NM) -u $(BUILD)/host/rafcode_route_core.o | grep .; then \
		echo "FAIL: undefined symbols in production route core"; exit 1; \
	else \
		echo "PASS: route core undefined symbols=0"; \
	fi
	@if $(READELF) -r $(BUILD)/host/rafcode_route_core.o | grep -Eq 'R_[A-Za-z0-9_]+'; then \
		echo "FAIL: relocations in production route core"; \
		$(READELF) -r $(BUILD)/host/rafcode_route_core.o; exit 1; \
	else \
		echo "PASS: route core relocations=0"; \
	fi

clean:
	rm -rf $(BUILD)
