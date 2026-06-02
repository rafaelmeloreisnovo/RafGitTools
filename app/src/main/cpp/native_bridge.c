#include <jni.h>

extern jint raf_bootstrap(void);
extern jint raf_kernel(void);
extern jint raf_proot(void);
extern jint raf_dep_io(void);
extern jint raf_dep_sched(void);
extern jint raf_dep_mem(void);
extern jint raf_dep_sync(void);
extern jint raf_dep_flags(void);

JNIEXPORT jint JNICALL
Java_com_rafgittools_platform_MultiPlatformManager_nativeAsmHealth(JNIEnv* env, jobject thiz) {
    (void)env;
    (void)thiz;
    return raf_bootstrap() + raf_kernel() + raf_proot() + raf_dep_io() + raf_dep_sched() + raf_dep_mem() + raf_dep_sync() + raf_dep_flags();
}


JNIEXPORT jint JNICALL
Java_com_rafgittools_platform_MultiPlatformManager_nativeAbiMask(JNIEnv* env, jobject thiz) {
    (void)env;
    (void)thiz;

    // This is a native sanity/ABI health layer, not the final RAFAELIA kernel.
    jint mask = 0;
    mask |= (1 << 0);

#if defined(__arm__)
    mask |= (1 << 1);
#elif defined(__aarch64__)
    mask |= (1 << 2);
#elif defined(__x86_64__) || defined(__i386__)
    mask |= (1 << 3);
#else
    mask |= (1 << 3);
#endif

    const jint bootstrap = raf_bootstrap();
    const jint kernel = raf_kernel();
    const jint dep_ok =
        (raf_dep_io() > 0) &&
        (raf_dep_sched() > 0) &&
        (raf_dep_mem() > 0) &&
        (raf_dep_sync() > 0) &&
        (raf_dep_flags() > 0);

    if (bootstrap > 0) mask |= (1 << 4);
    if (kernel > 0) mask |= (1 << 5);
    if (dep_ok) mask |= (1 << 6);

    return mask;
}
