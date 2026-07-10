# RafGitTools — Current Source State — 2026-07-10

## 1. Síntese executiva

O RafGitTools já possui base material de cliente Android para GitHub e Git local. O código está mais avançado que parte da documentação histórica.

O fechamento desta revisão concentra-se no caminho crítico:

```text
entrada no app
→ escolha de autenticação
→ validação GitHub
→ armazenamento cifrado
→ sessão autenticada
→ API GitHub / operações Git locais
→ APK verificável
```

## 2. Fonte de verdade

A ordem de confiança é:

1. `app/src/main/kotlin/` e `app/src/main/cpp/`;
2. testes em `app/src/test/` e `app/src/androidTest/`;
3. `app/build.gradle`, Gradle Wrapper e CMake;
4. workflows em `.github/workflows/`;
5. este relatório e `docs/STATUS_REPORT.md`;
6. documentos históricos e roadmaps.

Promessas em documentos antigos não substituem código, teste, APK ou log de execução.

## 3. Cliente GitHub implementado

### Autenticação

A interface oferece:

- OAuth Device Flow aberto no navegador;
- código do dispositivo;
- Personal Access Token, explicitamente tratado como token e não como senha;
- importação da sessão `gh` em Termux/ambiente local;
- chave SSH para operações Git;
- modo somente local/offline.

A senha do GitHub não é solicitada nem armazenada pelo RafGitTools.

O fluxo PAT segue agora:

```text
token digitado/importado
→ normalização mínima
→ GET https://api.github.com/user
→ identidade confirmada
→ AES-256-GCM no Android Keystore
→ DataStore
→ AuthTokenCache em memória
→ Authorization: Bearer
```

Token rejeitado não é persistido como sessão autenticada.

### API GitHub

A base Retrofit/OkHttp contém endpoints e repositórios para:

- usuário autenticado;
- repositórios;
- issues;
- pull requests;
- commits;
- releases;
- notificações;
- busca e detalhes relacionados.

A cobertura funcional é avançada, mas a classificação continua `PARTIAL` enquanto não houver matriz end-to-end completa em dispositivo.

### Git local

`JGitService.kt` contém operações locais relevantes, incluindo clone, commit, branch, push, pull e leitura de estado. A base é avançada, mas operações de rede, credenciais, conflitos e armazenamento precisam continuar sob testes de regressão.

## 4. Interface e navegação

A aplicação usa:

- Kotlin;
- Jetpack Compose + Material 3;
- MVVM / Clean Architecture;
- Hilt;
- Room;
- Coroutines + Flow;
- navegação Compose.

As telas para GitHub e repositórios locais já existem no código. A maturidade de cada fluxo deve ser medida por teste e não somente pela presença da tela.

## 5. Build atual

Fonte: `app/build.gradle`.

| Item | Valor |
|---|---|
| compileSdk | 34 |
| targetSdk | 34 |
| minSdk | 24 |
| Java/Kotlin target | 17 |
| Retrofit | 2.9.0 |
| OkHttp | 4.12.0 |
| Room | 2.6.1 |
| Hilt | 2.48 |
| JGit | 7.5.0.202512021534-r |
| ABIs | `armeabi-v7a`, `arm64-v8a` |
| variante principal de validação | `devDebug` |

O workflow canônico de fechamento é:

```text
.github/workflows/android-client-build.yml
```

Contrato:

```text
testes de autenticação
→ testes dev completos
→ lintDevDebug
→ assembleDevDebug
→ presença/tamanho do APK
→ SHA-256
→ artefato RafGitTools-devDebug
```

## 6. Configuração OAuth

O Device Flow exige um Client ID público de OAuth App configurado no build:

- `GITHUB_CLIENT_ID_DEV`;
- `GITHUB_CLIENT_ID_PRODUCTION`.

Sem esses valores, o aplicativo informa claramente que o OAuth pelo navegador não está configurado. PAT, SSH e modo local permanecem caminhos distintos.

Nenhum Client Secret deve ser embarcado no APK.

## 7. Segurança materializada

- token cifrado com AES-GCM e chave do Android Keystore;
- token fora de logs de produção;
- cabeçalhos GitHub atuais (`application/vnd.github+json` e versão da API);
- validação de identidade após autenticação;
- limpeza de sessão e cache no logout;
- release dependente de assinatura configurada;
- artefato debug acompanhado de checksum.

## 8. Classificação honesta

| Bloco | Estado em 2026-07-10 |
|---|---|
| estrutura Android / Compose / Hilt / Room | IMPLEMENTED |
| login PAT seguro | IMPLEMENTED + TESTS ADDED |
| OAuth Device Flow | IMPLEMENTED; exige Client ID e validação em execução |
| API GitHub | PARTIAL ADVANCED |
| Git local via JGit | PARTIAL ADVANCED |
| SSH | PARTIAL |
| APK devDebug | BUILD CONTRACT ADDED; resultado depende da execução CI/local |
| GPG | STUB |
| LFS | STUB |
| Worktree | STUB |
| Webhooks | STUB |
| terminal embutido | EXPERIMENTAL/PLANNED |
| ASM nativo | HEALTH/SANITY, não kernel final |

## 9. Critério de “cliente GitHub concluído”

O núcleo pode ser chamado de candidato funcional quando estas evidências existirem juntas:

1. APK `devDebug` compilado;
2. PAT válido abre sessão e carrega `/user` e repositórios;
3. token inválido não cria sessão;
4. OAuth Device Flow funciona em build com Client ID;
5. logout remove credencial e cache;
6. clone/commit/push/pull passam numa matriz controlada;
7. issues e pull requests passam em conta/repositório de teste;
8. crash-free smoke test em ARM32 e ARM64.

## 10. Lacunas que não devem ser escondidas

- a infraestrutura GitHub Actions do repositório apresentou execuções `startup_failure` antes de criar jobs;
- um build precisa efetivamente chegar ao compilador para gerar evidência de APK;
- OAuth pelo navegador depende da configuração externa do OAuth App;
- cobertura end-to-end em aparelho ainda é a régua final.

## Retroalimentar[3]

- **F_ok:** código de cliente GitHub, autenticação, API, JGit e UI já formam um sistema muito além de um protótipo documental.
- **F_gap:** falta converter a infraestrutura de build em evidência verde e executar a matriz em aparelho.
- **F_next:** corrigir o bloqueio de Actions, gerar APK com SHA-256 e validar login/repos/issues/PRs em dispositivo real.
