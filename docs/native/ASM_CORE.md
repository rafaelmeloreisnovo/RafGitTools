# ASM Core

Núcleo low-level em Assembly integrado via NDK/JNI.

## Módulos ASM
- bootstrap
- kernel
- proot
- dep_io
- dep_sched
- dep_mem
- dep_sync
- dep_flags

## ABIs suportadas
- armeabi-v7a
- arm64-v8a

## Build local
```bash
scripts/native/build_apks_signed_unsigned.sh
```

## Build CI
Workflow: `.github/workflows/native-asm-ci.yml`.
