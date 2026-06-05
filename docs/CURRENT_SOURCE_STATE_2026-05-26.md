# RafGitTools — Current Source State — 2026-05-26

## Metadata
- Date: 2026-05-26
- Branch: work
- Last commit: fe3e493 Merge pull request #211 from rafaelmeloreisnovo/codex/resolve-pending-issues-in-the-system

## 1. Executive Summary
RafGitTools é atualmente um cliente Android Git/GitHub em desenvolvimento. A base Kotlin/JGit/GitHub API está funcional/parcial, o build nativo cobre armeabi-v7a e arm64-v8a, e a camada RAFAELIA/Termux ARM32 permanece experimental.

## 2. Source of Truth
A fonte de verdade deste repositório é:
- código-fonte em `app/` e módulos correlatos;
- testes em `app/src/test` e `app/src/androidTest`;
- configuração Gradle (`build.gradle`, `app/build.gradle`);
- CMake e ASM (`app/src/main/cpp`);
- scripts de validação (`scripts/native/verify_apks.sh`, scripts de build);
- workflows CI em `.github/workflows`;
- este relatório e `docs/STATUS_REPORT.md`.

## 3. Implemented
- Estrutura Android com módulo `app` e flavors `dev`/`production`.
- Gradle com compileSdk/targetSdk 34, minSdk 24 e JDK 17.
- Jetpack Compose + MVVM + Clean Architecture.
- `JGitService.kt` com operações Git relevantes.
- `GithubApiService.kt` com endpoints GitHub implementados.
- Room configurado (DAO/Database/entities).
- Hilt configurado (`di/AppModule.kt`, Application).
- CMake + native bridge JNI (`librafcore`).
- Script `scripts/native/verify_apks.sh` para auditoria de ABIs/libs em APK.

## 4. Partial
- Git local via JGit: funcional avançado, mas cobertura de testes e fluxos extremos incompletos.
- GitHub API: funcional avançado, porém sem cobertura end-to-end total.
- UI Compose: funcional, com fluxos ainda em evolução.
- SSH: presente, mas sem matriz completa de validação documental + testes automatizados.
- Segurança e cache: presentes, com cobertura parcial.
- Release/assinatura: fluxo existe, depende de segredos e validação contínua.
- CI: múltiplos workflows, ainda com lacunas de evidência de cobertura total.

## 5. Stubs / Not Production Ready
- Terminal (`terminal/TerminalEmulator.kt`) ainda não pronto para produção.
- GPG (`security/GpgKeyManager.kt`) com `NotImplementedError`.
- LFS (`gitlfs/LfsManager.kt`) com `NotImplementedError`.
- Worktree (`worktree/WorktreeManager.kt`) com `NotImplementedError`.
- Webhooks (`webhook/WebhookHandler.kt`) com `NotImplementedError`.
- Multi-platform manager com funcionalidades placeholder.
- ASM ARM32/ARM64 atual como health/sanity layer (não kernel final RAFAELIA).

## 6. ARM32 State
- Android APK ABI support: `armeabi-v7a` configurado no Gradle.
- Native app bridge: `rafcore` + JNI + dispatcher ASM existem.
- ARM32 real implementation: ainda mínimo/stub para sanity checks.
- Termux ARM32 experimental: `_incoming/termux_arm32_build.sh` é material avançado, ainda não integrado oficialmente.

## 7. Risks
- Documentação com promessas acima da evidência executável.
- Status manual pode defasar rapidamente.
- Cobertura de testes ainda baixa frente ao escopo declarado.
- Stubs podem ser confundidos com features concluídas.
- Divergências possíveis entre docs e `app/build.gradle`.
- Scripts em `_incoming` sem promoção controlada.
- CI parcial para claims de release.

## 8. Required Next Steps
- Atualizar README para separar visão de estado técnico.
- Promover runtime check ARM32 para caminho oficial em `scripts/termux/`.
- Adicionar e manter `docs/ARM32_TERMUX_STATE.md`.
- Reforçar CI com validações mínimas de APK/ABI e testes.
- Adicionar testes de sanidade nativa/JNI.
- Tornar native bridge mais verificável via bitmask ABI.
- Atualizar `docs/STATUS_REPORT.md` com labels baseadas em evidências.
