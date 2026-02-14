.PHONY: clean all

SDK_DIR := $(firstword $(ANDROID_SDK_ROOT) $(ANDROID_HOME) $(wildcard $(HOME)/Android/Sdk) $(wildcard $(HOME)/android-sdk) $(wildcard /usr/local/lib/android/sdk))

local.properties:
	@if [ -n "$(SDK_DIR)" ]; then \
		echo "sdk.dir=$(SDK_DIR)" > local.properties; \
		echo "Generated local.properties with sdk.dir=$(SDK_DIR)"; \
	else \
		echo "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME."; \
		exit 1; \
	fi

clean: local.properties
	./gradlew clean --no-daemon

all: local.properties
	./gradlew :app:assembleDebug --no-daemon
