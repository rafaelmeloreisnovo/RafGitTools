# Makefile — RAFAELIA Browser
ARCH=$(shell uname -m)
CC?=clang
CF=-O2 -fPIE -fno-stack-protector -fno-asynchronous-unwind-tables \
   -fomit-frame-pointer -fno-builtin -fno-plt \
   -ffunction-sections -fdata-sections \
   -Wall -Wno-unused-function -Wno-unused-variable \
   -Wno-unused-but-set-variable -I.
LF=-pie -nostdlib -Wl,--gc-sections -Wl,--build-id=none -e _start

.PHONY: all run clean help

all:
ifeq ($(ARCH),aarch64)
	$(CC) $(CF) -march=armv8.2-a+crc+crypto br_main.c $(LF) -o browser
else ifeq ($(ARCH),x86_64)
	$(CC) $(CF) -march=native -static br_main.c $(LF) -o browser
endif
	@for CC32 in arm-linux-gnueabihf-gcc arm-linux-gnueabi-gcc; do \
	  command -v $$CC32 &>/dev/null && \
	  $$CC32 $(CF) -mthumb -march=armv7-a+neon-vfpv4 -mfloat-abi=softfp \
	    -mfpu=neon-vfpv4 br_start.S br_main.c $(LF) -o browser_arm32 && \
	  echo "ARM32: browser_arm32" && break || true; done
	@ls -lh browser* 2>/dev/null || true

run: all
	@[ -f ./browser ] && ./browser $(URL) || true
	@[ -f ./browser_arm32 ] && { \
	  command -v qemu-arm && qemu-arm ./browser_arm32 $(URL) || \
	  echo "qemu-arm ./browser_arm32 $(URL)"; } || true

fetch: all
	@./browser http://example.com/ 2>/dev/null || true

clean:
	rm -f browser browser_arm32

help:
	@echo "make           — compila"
	@echo "make run URL=http://example.com/"
	@echo "make fetch     — busca example.com"
