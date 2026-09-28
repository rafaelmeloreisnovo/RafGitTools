#include <jni.h>
#include "core.h"
#include "silicon_light/raf_silicon_light.h"

JNIEXPORT jint JNICALL
Java___JNI_PACKAGE___NativeBridge_nativeHealth(JNIEnv *env, jclass clazz) {
    (void)env;
    (void)clazz;

    if (raf_sl_q16_mul(65536, 32768) != 32768) {
        return (jint)0;
    }

    return (jint)raf_core_health();
}
