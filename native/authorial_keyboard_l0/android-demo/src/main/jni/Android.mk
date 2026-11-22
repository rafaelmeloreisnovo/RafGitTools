LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := rafkbd_demo
LOCAL_SRC_FILES := rafkbd_activity.c ../../../../src/rafkbd_panel.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../../../src
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
 LOCAL_SRC_FILES += ../../../../src/keyboard_armv7.S
endif
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
 LOCAL_SRC_FILES += ../../../../src/keyboard_aarch64.S
endif
LOCAL_ASFLAGS := -I$(LOCAL_PATH)/../../../../src
LOCAL_CFLAGS := -O2 -ffreestanding -fno-builtin -fno-stack-protector -fvisibility=hidden -Wall -Wextra -Werror
LOCAL_LDLIBS := -landroid
include $(BUILD_SHARED_LIBRARY)
