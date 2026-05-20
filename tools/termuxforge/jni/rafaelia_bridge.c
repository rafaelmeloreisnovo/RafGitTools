#include <jni.h>
#include <stdint.h>

JNIEXPORT jstring JNICALL
Java_org_rafaelia_forge_MainActivity_rafaelia_1seal(JNIEnv *env, jobject self) {
    (void)self;
    return (*env)->NewStringUTF(env, "RAFCODE-TERMUXFORGE-42");
}

JNIEXPORT jint JNICALL
Java_org_rafaelia_forge_MainActivity_phiFromCH(JNIEnv *env, jobject self, jint c_q16, jint h_q16) {
    (void)env;
    (void)self;
    const int32_t one_q16 = 0x10000;
    int32_t one_minus_h = one_q16 - h_q16;
    int64_t prod = (int64_t)one_minus_h * (int64_t)c_q16;
    return (jint)(prod >> 16);
}

JNIEXPORT jboolean JNICALL
Java_org_rafaelia_forge_MainActivity_periodGuard(JNIEnv *env, jobject self, jint n) {
    (void)env;
    (void)self;
    return (n % 42 == 0) ? JNI_TRUE : JNI_FALSE;
}
