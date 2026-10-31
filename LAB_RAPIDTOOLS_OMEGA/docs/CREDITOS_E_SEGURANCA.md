# Licenças, autoria, segurança e scope

* Original files under LAB_RAPIDTOOLS_OMEGA are newly authored for this laboratory. The parent repository's actual LICENSE/COPYING and THIRD_PARTY_LICENSES.md remain the governing licenses; this text is not a unilateral license change.
* Portable freestanding core and RLE are experimental implementations, not claims of compatibility or novel patentable standards.
* POSIX adapter uses OS libc, pthread, mmap and clock_gettime. Its dependency separation is explicit and testable.
* GLSL shader source only; not copied from Vulkan SDK, ANGLE, Flutter or third-party shader packages.
* No secrets, OAuth tokens, connected provider data, private Drive IDs or user files may be embedded in public lab fixtures.
* Refuse remote dispatch even if a proposal names PAT_AGENTS, K-Secrets or other connected credentials. Per-repo authorization is not inherited.
* All tests run only with synthetic data. Do not import user NOVOexport content or scientific patient data into the public repository.
* Do not promote lab output into the Android application; require explicit review and change of producer contract before release.
