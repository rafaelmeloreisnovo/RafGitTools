/* RAFAELIA Android NativeActivity only: explicit hosted/platform boundary.
 * No app-owned Java classes, network, JNI calls, heap allocation or libc calls.
 * L0 remains separate ARM register logic; Android NDK APIs are unavoidable L2.
 */
#include <android/native_activity.h>
#include <android/native_window.h>
#include <android/input.h>
#include <android/looper.h>
#include "rafkbd_panel.h"
#define RAFKBD_CAPACITY 256u
static ANativeWindow *raf_window;
static AInputQueue *raf_queue;
static unsigned char raf_text[RAFKBD_CAPACITY];
static raf_u32 raf_length;
static void raf_paint(void) {
 ANativeWindow_Buffer surface;
 if(raf_window==(ANativeWindow *)0)return;
 if(ANativeWindow_lock(raf_window,&surface,(ARect *)0)!=0)return;
 if(surface.bits!=(void *)0 && surface.format==WINDOW_FORMAT_RGBA_8888 &&
    surface.width>0 && surface.height>0 && surface.stride>=surface.width)
   rafkbd_l1_paint((raf_u32 *)surface.bits,(raf_u32)surface.width,
     (raf_u32)surface.height,(raf_u32)surface.stride,raf_text,raf_length);
 ANativeWindow_unlockAndPost(raf_window);
}
static int raf_receive(int fd,int events,void *opaque) {
 AInputEvent *event;
 int action;
 float px,py;
 raf_u32 w,h,kx,ky,key;
 (void)fd;(void)events;(void)opaque;
 if(raf_queue==(AInputQueue *)0)return 1;
next_event:
 event=(AInputEvent *)0;
 if(AInputQueue_getEvent(raf_queue,&event)<0)return 1;
 if(AInputQueue_preDispatchEvent(raf_queue,event)!=0)goto next_event;
 if(AInputEvent_getType(event)!=AINPUT_EVENT_TYPE_MOTION)goto finish_unhandled;
 action=AMotionEvent_getAction(event)&AMOTION_EVENT_ACTION_MASK;
 if(action!=AMOTION_EVENT_ACTION_UP || raf_window==(ANativeWindow *)0)goto finish_unhandled;
 px=AMotionEvent_getX(event,0u);py=AMotionEvent_getY(event,0u);
 w=(raf_u32)ANativeWindow_getWidth(raf_window);
 h=(raf_u32)ANativeWindow_getHeight(raf_window);
 if(w<320u || h<320u || px<0.0f || py<0.0f ||
    px>=(float)w || py<(float)(h/2u) || py>=(float)h)goto finish_unhandled;
 kx=((raf_u32)px)*320u/w;
 ky=((raf_u32)(py-(float)(h/2u)))*160u/(h-h/2u);
 key=rafkbd_l0_probe(kx,ky);
 rafkbd_l1_edit(raf_text,RAFKBD_CAPACITY,&raf_length,key);
 AInputQueue_finishEvent(raf_queue,event,1);
 raf_paint();
 goto next_event;
finish_unhandled:
 AInputQueue_finishEvent(raf_queue,event,0);
 goto next_event;
}
static void raf_window_created(ANativeActivity *activity,ANativeWindow *window) {
 (void)activity;raf_window=window;
 ANativeWindow_setBuffersGeometry(window,0,0,WINDOW_FORMAT_RGBA_8888);
 raf_paint();
}
static void raf_window_destroyed(ANativeActivity *activity,ANativeWindow *window) {
 (void)activity;(void)window;raf_window=(ANativeWindow *)0;
}
static void raf_window_resized(ANativeActivity *activity,ANativeWindow *window) {
 (void)activity;(void)window;raf_paint();
}
static void raf_queue_created(ANativeActivity *activity,AInputQueue *queue) {
 ALooper *loop;(void)activity;raf_queue=queue;
 loop=ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
 if(loop!=(ALooper *)0)AInputQueue_attachLooper(queue,loop,17,raf_receive,(void *)0);
}
static void raf_queue_destroyed(ANativeActivity *activity,AInputQueue *queue) {
 (void)activity;AInputQueue_detachLooper(queue);raf_queue=(AInputQueue *)0;
}
static void raf_destroy(ANativeActivity *activity) {
 (void)activity;raf_window=(ANativeWindow *)0;raf_queue=(AInputQueue *)0;raf_length=0u;
}
__attribute__((visibility("default")))
void ANativeActivity_onCreate(ANativeActivity *activity,void *saved,size_t saved_size) {
 (void)saved;(void)saved_size;raf_length=0u;
 activity->callbacks->onDestroy=raf_destroy;
 activity->callbacks->onNativeWindowCreated=raf_window_created;
 activity->callbacks->onNativeWindowDestroyed=raf_window_destroyed;
 activity->callbacks->onNativeWindowResized=raf_window_resized;
 activity->callbacks->onInputQueueCreated=raf_queue_created;
 activity->callbacks->onInputQueueDestroyed=raf_queue_destroyed;
}
