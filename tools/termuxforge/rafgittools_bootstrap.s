; RafGitTools hermetic bootstrap.
; This is deliberately the smallest input accepted by ApkC's internal assembler.
; It creates the NativeActivity entry points needed for an offline ABI/build
; carrier; it is not the Kotlin/Compose RafGitTools application.

.sym1
ANativeActivity_onCreate:
    ret

.sym2
android_main:
    ret
