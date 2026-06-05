# RafGitTools - Relatório de Status / Status Report

**Data / Date**: 2026-05-26
**Status Geral / Overall Status**: 🟡 In Development

## Status labels
- **COMPLETE** = implementado + usado + testado.
- **PARTIAL** = implementado parcialmente ou sem cobertura completa.
- **STUB** = arquivo existe, mas contém NotImplementedError ou retorno mínimo.
- **EXPERIMENTAL** = existe em `_incoming` ou scripts não integrados.
- **PLANNED** = roadmap sem implementação funcional.

## Current technical classification
- Git local via JGit: **PARTIAL (advanced)**.
- GitHub API: **PARTIAL (advanced)**.
- UI Compose: **PARTIAL (advanced)**.
- SSH: **PARTIAL** (implementado, ainda sem evidência completa de testes de regressão em toda matriz).
- GPG: **STUB/PLANNED**.
- LFS: **STUB/PLANNED**.
- Worktree: **STUB/PLANNED**.
- Webhooks: **STUB/PLANNED**.
- Terminal: **PLANNED**.
- Native ASM: **STUB/HEALTH**.
- Termux ARM32 path: **EXPERIMENTAL**.

## Mismatch corrections
Dependências e versões devem seguir o `app/build.gradle` como fonte de verdade.

- Retrofit: **2.9.0** (corrige possíveis menções antigas a 3.0.0).
- OkHttp: **4.11.0**.
- Room: **2.6.1**.
- Hilt: **2.48**.
- JSch: **0.2.9** (se docs antigos citarem 0.2.18, considerar desatualizado).
- SDK alvo: compileSdk/targetSdk **34**; minSdk **24**.
- ABIs Android suportadas: **armeabi-v7a** e **arm64-v8a**.

## Evidence files
- `app/build.gradle`
- `app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt`
- `app/src/main/kotlin/com/rafgittools/data/github/GithubApiService.kt`
- `app/src/main/cpp/CMakeLists.txt`
- `app/src/main/cpp/native_bridge.c`
- `scripts/native/verify_apks.sh`
- `scripts/termux_arm32_runtime_check.sh`
- `_incoming/termux_arm32_build.sh`
