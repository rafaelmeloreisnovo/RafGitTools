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
