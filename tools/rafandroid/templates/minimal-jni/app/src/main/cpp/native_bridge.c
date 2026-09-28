#include <jni.h>
#include "core.h"

JNIEXPORT jint JNICALL
Java___JNI_PACKAGE___NativeBridge_nativeHealth(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;
    return (jint)raf_core_health();
}
