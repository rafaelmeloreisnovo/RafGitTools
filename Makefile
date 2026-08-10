.PHONY: clean all audit-validate runtime-receipt runtime-receipt-tests

SDK_DIR := $(firstword $(ANDROID_SDK_ROOT) $(ANDROID_HOME) $(wildcard $(HOME)/Android/Sdk) $(wildcard $(HOME)/android-sdk) $(wildcard /usr/local/lib/android/sdk))
RUNTIME_APK ?=
RUNTIME_RECEIPT ?= app/build/reports/runtime/android-runtime-receipt.json
RUNTIME_RECEIPT_ARGS ?=

local.properties:
	@if [ -n "$(SDK_DIR)" ]; then \
		echo "sdk.dir=$(SDK_DIR)" > local.properties; \
		echo "Generated local.properties with sdk.dir=$(SDK_DIR)"; \
	else \
		echo "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME."; \
		exit 1; \
	fi

clean: local.properties
	./scripts/gradlew_with_java17.sh clean --no-daemon

all: local.properties
	./scripts/gradlew_with_java17.sh :app:assembleDebug --no-daemon


audit-validate:
	python3 scripts/validate_bug_report.py --input BUG_REPORT.md --output BUG_REPORT_VALIDATED.md --threshold 0.05

runtime-receipt:
	@test -n "$(RUNTIME_APK)" || (echo "Set RUNTIME_APK=/path/to.apk" >&2; exit 2)
	python3 scripts/runtime/capture_android_runtime_receipt.py \
		--apk "$(RUNTIME_APK)" \
		--output "$(RUNTIME_RECEIPT)" \
		--expected-commit "$$(git rev-parse HEAD)" \
		$(RUNTIME_RECEIPT_ARGS)

runtime-receipt-tests:
	python3 -m unittest discover -s tests -p 'test_android_runtime_receipt.py' -v
