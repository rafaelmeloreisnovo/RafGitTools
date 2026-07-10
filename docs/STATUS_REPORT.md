# RafGitTools — Relatório de Status

**Data:** 2026-07-10  
**Estado geral:** 🟡 Cliente GitHub/Git funcional avançado, em fechamento de build e validação end-to-end  
**Relatório detalhado:** `docs/CURRENT_SOURCE_STATE_2026-07-10.md`

## Semântica de status

- **IMPLEMENTED:** código integrado e usado pelo app.
- **TESTS ADDED:** testes existem no repositório; o resultado da execução deve ser consultado no CI/local.
- **PARTIAL ADVANCED:** implementação extensa, mas sem matriz completa de regressão/end-to-end.
- **STUB:** arquivo ou API existe, porém contém `NotImplementedError`, retorno mínimo ou placeholder.
- **EXPERIMENTAL:** material fora do caminho principal ou sem validação de produção.
- **BUILD BLOCKED:** pipeline não chegou ao compilador por falha de infraestrutura/Actions.

## Classificação técnica atual

| Componente | Status | Evidência principal |
|---|---|---|
| Android + Compose + Hilt + Room | IMPLEMENTED | `app/`, `app/build.gradle` |
| Login PAT | IMPLEMENTED + TESTS ADDED | `AuthScreen.kt`, `AuthViewModel.kt`, `OAuthDeviceFlowManager.kt` |
| OAuth Device Flow | IMPLEMENTED / CONFIG REQUIRED | `GITHUB_CLIENT_ID_DEV`, `GITHUB_CLIENT_ID_PRODUCTION` |
| Importação `gh` / Termux | IMPLEMENTED + TESTS ADDED | `GhCliAuthImporter`, `AuthViewModel` |
| SSH | PARTIAL | `SshKeyManager`, JGit SSH |
| API GitHub | PARTIAL ADVANCED | `GithubApiService.kt`, `GithubRepository.kt` |
| Git local via JGit | PARTIAL ADVANCED | `JGitService.kt` |
| UI GitHub/Git | PARTIAL ADVANCED | `ui/screens/`, `MainActivity.kt` |
| Build `devDebug` | BUILD CONTRACT ADDED | `.github/workflows/android-client-build.yml` |
| APK verificável | PENDING BUILD EVIDENCE | APK + `SHA256SUMS.txt` |
| GPG | STUB |
| LFS | STUB |
| Worktree | STUB |
| Webhooks | STUB |
| Terminal embutido | EXPERIMENTAL/PLANNED |
| Native ASM | HEALTH/SANITY |

## Autenticação — caminho real

```text
método escolhido
→ credencial/código
→ validação direta no GitHub
→ identidade `/user`
→ armazenamento cifrado
→ cache de sessão
→ Authorization: Bearer
```

A tela não solicita a senha do GitHub. O campo protegido é um **Personal Access Token**, não uma senha.

## Dependências verificadas no `app/build.gradle`

- Retrofit: **2.9.0**
- OkHttp: **4.12.0**
- Room: **2.6.1**
- Hilt: **2.48**
- JSch: **0.2.9**
- JGit: **7.5.0.202512021534-r**
- compileSdk/targetSdk: **34**
- minSdk: **24**
- ABIs: **armeabi-v7a**, **arm64-v8a**
- Java/Kotlin target: **17**

## Evidência de build esperada

O workflow `Android Client Build` foi adicionado para executar:

1. testes de autenticação;
2. testes unitários `devDebug`;
3. `lintDevDebug`;
4. `assembleDevDebug`;
5. verificação do APK;
6. SHA-256;
7. upload do artefato `RafGitTools-devDebug`.

No momento desta atualização, execuções de Actions do repositório estavam encerrando como `startup_failure` antes da criação de jobs. Portanto, não se deve declarar APK compilado enquanto o pipeline não produzir o arquivo.

## Fonte de verdade

1. código e testes;
2. Gradle/CMake;
3. workflows e logs;
4. `CURRENT_SOURCE_STATE_2026-07-10.md`;
5. este status;
6. roadmaps/documentos históricos.

## Retroalimentar[3]

- **F_ok:** documentação agora acompanha o código de autenticação e cliente GitHub.
- **F_gap:** falta evidência de compilação por bloqueio de Actions anterior ao runner.
- **F_next:** fazer o workflow chegar ao job, corrigir eventuais erros reais do compilador e gerar o APK com checksum.
