/* SPDX-License-Identifier: GPL-3.0-or-later */
/* L1 JNI hosted boundary; not L0. */
#include <jni.h>
#include "atlas_core.h"
JNIEXPORT jlong JNICALL Java_org_rafaelia_atlas_MainActivity_classifyName(
 JNIEnv*env,jclass cls,jstring name,jboolean dir){
 (void)cls;
 if(!name)return 8;
 const char*s=(*env)->GetStringUTFChars(env,name,0);
 if(!s)return -1;
 jsize len=(*env)->GetStringUTFLength(env,name);
 atlas_traits t=atlas_classify(s,(unsigned)len,dir==JNI_TRUE);
 (*env)->ReleaseStringUTFChars(env,name,s);
 return(jlong)(t.kind&255u)|((jlong)(t.numeric_prefix&65535u)<<8)
      |((jlong)(t.extension_offset&65535u)<<24);
}
