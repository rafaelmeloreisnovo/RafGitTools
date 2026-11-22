# L0 object-only freestanding builds; no Android SDK/JNI/JDK/Gradle.
CC ?= cc
CLANG ?= clang
OUT ?= /tmp/raf_pirc2_l0
FLAGS = -std=c11 -O2 -Wall -Wextra -Werror -Wconversion -Wshadow -ffreestanding -fno-builtin -fno-stack-protector
CORE = pirc2_token_l0.c
TEST = tests/test_pirc2_token_l0.c
.PHONY: test armv7 aarch64 inspect adversarial
test:
	mkdir -p "$(OUT)"
	$(CC) $(FLAGS) $(CORE) $(TEST) -o "$(OUT)/pirc2_host_test"
	"$(OUT)/pirc2_host_test"
	@echo "PASS_SCOPED: 19 C fixtures (host); no Android/device evidence"
armv7:
	mkdir -p "$(OUT)"
	$(CLANG) --target=armv7a-none-eabi $(FLAGS) -c $(CORE) -o "$(OUT)/pirc2_armv7.o"
aarch64:
	mkdir -p "$(OUT)"
	$(CLANG) --target=aarch64-none-elf $(FLAGS) -c $(CORE) -o "$(OUT)/pirc2_aarch64.o"
inspect:
	@echo "Run: nm -u $(OUT)/pirc2_armv7.o $(OUT)/pirc2_aarch64.o"

# Hosted-only falsifiers; never linked into core.
adversarial:
	mkdir -p "$(OUT)"
	$(CLANG) -std=c11 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer \
	  -Wall -Wextra -Wconversion -Wshadow -Werror \
	  $(CORE) tests/test_pirc2_adversarial.c -o "$(OUT)/pirc2_adversarial"
	ASAN_OPTIONS=detect_leaks=1:halt_on_error=1 UBSAN_OPTIONS=halt_on_error=1 "$(OUT)/pirc2_adversarial"
