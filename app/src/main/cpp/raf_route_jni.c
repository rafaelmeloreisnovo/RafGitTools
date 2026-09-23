#include <jni.h>

#include "rafcode_route.h"

/*
 * Hosted Android/JNI adapter only.
 *
 * The resolver itself remains in native/rafcode_route_v1 and is independently
 * audited as freestanding. This file only maps two DirectByteBuffers to the
 * fixed 32-byte request/receipt ABI.
 */
JNIEXPORT jint JNICALL
Java_com_rafgittools_workspace_ManifoldRouteNativeBridge_resolveNative(
        JNIEnv *env,
        jobject instance,
        jobject request_buffer,
        jobject receipt_buffer)
{
    void *request_pointer;
    void *receipt_pointer;
    jlong request_capacity;
    jlong receipt_capacity;
    raf_route_receipt *receipt;

    (void)instance;

    request_pointer = (*env)->GetDirectBufferAddress(env, request_buffer);
    receipt_pointer = (*env)->GetDirectBufferAddress(env, receipt_buffer);
    request_capacity = (*env)->GetDirectBufferCapacity(env, request_buffer);
    receipt_capacity = (*env)->GetDirectBufferCapacity(env, receipt_buffer);

    if (request_pointer == (void *)0 || receipt_pointer == (void *)0) {
        return (jint)-1;
    }
    if (request_capacity < (jlong)sizeof(raf_route_request) ||
        receipt_capacity < (jlong)sizeof(raf_route_receipt)) {
        return (jint)-2;
    }

    raf_route_resolve(request_pointer, receipt_pointer);
    receipt = (raf_route_receipt *)receipt_pointer;
    return (jint)receipt->status;
}
