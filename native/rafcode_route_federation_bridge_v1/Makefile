.POSIX:

CC ?= clang
NM ?= nm
READELF ?= readelf
BUILD ?= build

ROUTE_DIR = ../rafcode_route_v1
FED_DIR = ../rafcode_federation_v1

CPPFLAGS_COMMON = -nostdinc -Iinclude -I$(ROUTE_DIR)/include -I$(FED_DIR)/include
CFLAGS_COMMON = -std=c11 -Os -ffreestanding -fno-builtin -fno-stack-protector \
	-fno-unwind-tables -fno-asynchronous-unwind-tables -fno-ident -fno-common \
	-fvisibility=hidden -ffunction-sections -fdata-sections -fomit-frame-pointer \
	-fno-strict-aliasing -fwrapv -Wall -Wextra -Werror -Wshadow -Wconversion \
	-Wsign-conversion -Wundef -Wstrict-prototypes -Wmissing-prototypes \
	-Wold-style-definition -Wcast-align

BRIDGE = src/rafcode_route_fed_bridge.c
TEST = tests/test_route_fed_bridge.c

.PHONY: all test audit armv7-object aarch64-object clean

all: test audit

$(BUILD)/host:
	mkdir -p $@

$(BUILD)/host/bridge.o: $(BRIDGE) include/rafcode_route_fed_bridge.h | $(BUILD)/host
	$(CC) $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) -c $(BRIDGE) -o $@

$(BUILD)/host/route.o: $(ROUTE_DIR)/src/rafcode_route_core.c | $(BUILD)/host
	$(CC) -nostdinc -I$(ROUTE_DIR)/include $(CFLAGS_COMMON) -c $< -o $@

$(BUILD)/host/fed.o: $(FED_DIR)/src/rafcode_federation_core.c | $(BUILD)/host
	$(CC) -nostdinc -I$(FED_DIR)/include $(CFLAGS_COMMON) -c $< -o $@

test: $(BUILD)/host/bridge.o $(BUILD)/host/route.o $(BUILD)/host/fed.o
	$(CC) -std=c11 -Wall -Wextra -Werror \
		-Iinclude -I$(ROUTE_DIR)/include -I$(FED_DIR)/include \
		$(TEST) $(BUILD)/host/bridge.o $(BUILD)/host/route.o $(BUILD)/host/fed.o \
		-o $(BUILD)/host/test_route_fed_bridge
	$(BUILD)/host/test_route_fed_bridge

audit: $(BUILD)/host/bridge.o
	@if $(NM) -u $(BUILD)/host/bridge.o | grep .; then \
		echo "FAIL: undefined symbols in production bridge object"; exit 1; \
	else \
		echo "PASS: route-fed bridge undefined symbols=0"; \
	fi
	@if $(READELF) -r $(BUILD)/host/bridge.o | grep -Eq 'R_[A-Za-z0-9_]+'; then \
		echo "FAIL: relocations in production bridge object"; \
		$(READELF) -r $(BUILD)/host/bridge.o; exit 1; \
	else \
		echo "PASS: route-fed bridge relocations=0"; \
	fi

armv7-object:
	mkdir -p $(BUILD)/armv7
	$(CC) --target=armv7a-linux-androideabi24 $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) \
		-march=armv7-a -mfloat-abi=softfp -mfpu=neon-vfpv4 \
		-c $(BRIDGE) -o $(BUILD)/armv7/bridge.o

aarch64-object:
	mkdir -p $(BUILD)/aarch64
	$(CC) --target=aarch64-linux-android24 $(CPPFLAGS_COMMON) $(CFLAGS_COMMON) \
		-march=armv8-a -c $(BRIDGE) -o $(BUILD)/aarch64/bridge.o

clean:
	rm -rf $(BUILD)
