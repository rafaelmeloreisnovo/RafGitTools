# SPDX-License-Identifier: GPL-3.0-or-later
# Build-time tools are host-owned; runtime core remains freestanding.
CC ?= cc
CLANG ?= clang
BASE = -std=c11 -O2 -Wall -Wextra -Wconversion -Werror -fno-builtin -fno-stack-protector
L0 = -ffreestanding -nostdinc -fPIC
BUILD = build
.PHONY: test properties objects clean

test:
	mkdir -p $(BUILD)
	$(CC) $(BASE) inventarioraf.c test_inventarioraf.c -o $(BUILD)/selftest
	$(BUILD)/selftest

properties:
	mkdir -p $(BUILD)
	$(CLANG) $(BASE) -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer inventarioraf.c fuzz_inventarioraf.c -o $(BUILD)/properties
	$(BUILD)/properties

objects:
	mkdir -p $(BUILD)
	$(CLANG) --target=armv7a-linux-androideabi24 $(BASE) $(L0) -c inventarioraf.c -o $(BUILD)/inventarioraf-armv7.o
	$(CLANG) --target=aarch64-linux-android24 $(BASE) $(L0) -c inventarioraf.c -o $(BUILD)/inventarioraf-arm64.o
	$(CLANG) --target=armv7a-linux-androideabi24 -nostdlib -fuse-ld=lld -shared -Wl,--no-undefined,-z,relro,-z,now $(BUILD)/inventarioraf-armv7.o -o $(BUILD)/libinventarioraf-armv7.so
	$(CLANG) --target=aarch64-linux-android24 -nostdlib -fuse-ld=lld -shared -Wl,--no-undefined,-z,relro,-z,now $(BUILD)/inventarioraf-arm64.o -o $(BUILD)/libinventarioraf-arm64.so
	@for obj in $(BUILD)/libinventarioraf-armv7.so $(BUILD)/libinventarioraf-arm64.so; do \
	  readelf -h "$$obj" | grep -F 'DYN (Shared object file)' >/dev/null || exit 1; \
	  if readelf -d "$$obj" | grep -q '(NEEDED)'; then echo "BLOCKED: DT_NEEDED $$obj"; exit 1; fi; \
	done

clean:
	rm -rf $(BUILD)
