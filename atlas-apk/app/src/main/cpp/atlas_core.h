/* SPDX-License-Identifier: GPL-3.0-or-later */
#ifndef RAFAELIA_ATLAS_CORE_H
#define RAFAELIA_ATLAS_CORE_H
/* L0 freestanding: no malloc/libc/JNI/Android APIs/syscall/global state. */
typedef struct { unsigned kind,numeric_prefix,extension_offset; } atlas_traits;
atlas_traits atlas_classify(const char *name,unsigned len,int is_directory);
#endif
