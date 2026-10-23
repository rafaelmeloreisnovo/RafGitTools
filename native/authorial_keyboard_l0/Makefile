# Build tools are hosted; the output objects and their core are freestanding.
CLANG ?= clang
NM ?= nm
READELF ?= readelf
PYTHON ?= python3
BUILD ?= build

.PHONY: all objects audit clean
all: objects audit

objects: $(BUILD)/armv7/keyboard.o $(BUILD)/aarch64/keyboard.o

$(BUILD)/armv7/keyboard.o: src/keyboard_armv7.S src/layout.inc
	mkdir -p $(BUILD)/armv7
	$(CLANG) --target=armv7a-linux-androideabi24 -march=armv7-a -marm -ffreestanding -nostdinc -fno-builtin -Isrc -c $< -o $@

$(BUILD)/aarch64/keyboard.o: src/keyboard_aarch64.S src/layout.inc
	mkdir -p $(BUILD)/aarch64
	$(CLANG) --target=aarch64-linux-android24 -march=armv8-a -ffreestanding -nostdinc -fno-builtin -Isrc -c $< -o $@

audit: objects
	$(PYTHON) tests/check_contract.py $(BUILD)/armv7/keyboard.o $(BUILD)/aarch64/keyboard.o

clean:
	rm -rf $(BUILD)
