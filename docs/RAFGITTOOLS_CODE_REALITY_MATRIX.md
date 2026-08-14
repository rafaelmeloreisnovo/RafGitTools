# RAFGITTOOLS_CODE_REALITY_MATRIX

Status: **ATIVO — reconciliado com fonte + evidência executada**  
Última atualização: **2026-08-14**  
Último commit de aplicação com BUILD verificado: `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`  
Workflow: `Android Client Build` run `31821491676` — PASS  
Head documental observado depois do build: `8ee151e9ea46f5fb1d2048ab0e8a5574b2e3693d`

## Regra

```text
SOURCE = caminho/classe/função existe e está integrado
TESTED = teste realmente executado para o commit de build
BUILD_VERIFIED = APK compilado, verificado e hash-bound
DEVICE_VERIFIED = receipt físico do mesmo APK
```

`SOURCE`, `TESTED`, `BUILD_VERIFIED` e `DEVICE_VERIFIED` são eixos independentes.

## Evidência global do corte

- custody/structural tests: PASS;
- authentication unit tests: PASS;
- full dev unit tests: PASS;
- Android lint: PASS;
- `assembleDevDebug`: PASS;
- APK verification/build receipt: PASS;
- APK SHA-256: `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`;
- `armeabi-v7a`: PRESENT;
- `arm64-v8a`: PRESENT;
- DEVICE: `TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED`.

## Matriz de realidade

| ID | Área | Fonte principal | Capacidade real em fonte | UI | Teste/build observado | Runtime real | Status canônico |
|---|---|---|---|---|---|---|---|
| AUTH-001 | auth | `ui/screens/auth/AuthViewModel.kt` | PAT + bootstrap de identidade | SIM | auth tests PASS | device pendente | `IMPLEMENTED / TESTED` |
| AUTH-002 | auth | `OAuthDeviceFlowManager.kt` | OAuth Device Flow | SIM | app/tests compilados | Client ID real pendente | `IMPLEMENTED / CONFIG_REQUIRED` |
| AUTH-003 | auth | `GhCliAuthImporter.kt` | importação de credencial `gh` com validações | SIM | app/tests compilados | Termux real pendente | `IMPLEMENTED / RUNTIME_GATED` |
| AUTH-004 | auth | `TokenRefreshManager.kt` + `AuthInterceptor.kt` | lifecycle capability-aware, refresh rotativo, retry único e fail-closed | INDIRETA | auth + full unit tests PASS | refresh GitHub App real pendente | `IMPLEMENTED / TESTED / FIXTURE_GATED` |
| AUTH-005 | auth | `SshKeyManager` | geração/gestão de chaves e transporte SSH | SIM/PARCIAL | BUILD PASS | servidor/chave reais pendentes | `PARTIAL_RUNTIME` |
| AUTH-006 | auth | `BiometricAuthManager` | autenticação biométrica | SIM | BUILD PASS | device regression pendente | `IMPLEMENTED / DEVICE_GATED` |
| AUTH-007 | auth | `MultiAccountManager` | múltiplas contas + switching | SIM | BUILD PASS | regressão real pendente | `IMPLEMENTED / DEVICE_GATED` |
| GIT-001 | git | `data/git/JGitService.kt` | backend Git local avançado | PARCIAL/AMPLA | full unit tests + BUILD PASS | remotes/conflitos reais pendentes | `IMPLEMENTED_ADVANCED` |
| GIT-002 | git | `JGitService.kt` | clone shallow | SIM/INDIRETA | BUILD PASS | remote fixture pendente | `SOURCE_FUNCTIONAL` |
| GIT-003 | git | `JGitService.kt` | clone single branch | SIM/INDIRETA | BUILD PASS | remote fixture pendente | `SOURCE_FUNCTIONAL` |
| GIT-004 | git | `JGitService.kt` | clone com submodules | SIM/INDIRETA | BUILD PASS | remote fixture pendente | `SOURCE_FUNCTIONAL` |
| GIT-005 | git | `JGitService.kt` | amend / pull-rebase / stash / reflog / blame / config | SIM/PARCIAL | BUILD PASS | matriz E2E pendente | `SOURCE_FUNCTIONAL` |
| GIT-006 | git | `JGitService.kt` | force-push-with-lease fail-closed | SIM/INDIRETA | BUILD PASS | private remote fixture pendente | `IMPLEMENTED / RUNTIME_GATED` |
| GIT-007 | git | `InteractiveStagingService.kt` | stage/unstage por hunk, index-only, stale gates | SIM | full unit tests PASS | Android smoke pendente | `IMPLEMENTED / TESTED / DEVICE_GATED` |
| API-001 | github | `GithubDataRepository.kt` | usuário/repos e núcleo REST | SIM | BUILD PASS | E2E real parcial | `PARTIAL_ADVANCED` |
| API-002 | github | camada GitHub Retrofit/OkHttp | issues, PRs, releases, notifications, search, reactions, starring, SSH keys | SIM/PARCIAL | BUILD PASS | matriz E2E pendente | `PARTIAL_ADVANCED` |
| PLATFORM-001 | providers | `platform/MultiPlatformManager.kt` | GitLab adapter | INDIRETA | full unit tests PASS | credencial/endpoint real pendente | `IMPLEMENTED / FIXTURE_GATED` |
| PLATFORM-002 | providers | `platform/MultiPlatformManager.kt` | Bitbucket adapter | INDIRETA | full unit tests PASS | fixture real pendente | `IMPLEMENTED / FIXTURE_GATED` |
| PLATFORM-003 | providers | `platform/MultiPlatformManager.kt` | Gitea/Forgejo adapter | INDIRETA | full unit tests PASS | fixture real pendente | `IMPLEMENTED / FIXTURE_GATED` |
| PLATFORM-004 | providers | `platform/MultiPlatformManager.kt` | Azure DevOps adapter | INDIRETA | full unit tests PASS | fixture real pendente | `IMPLEMENTED / FIXTURE_GATED` |
| OFFLINE-001 | offline | `offline/OfflineQueue.kt` | fila offline | INDIRETA | BUILD PASS | restart/recovery device pendente | `IMPLEMENTED / DEVICE_GATED` |
| OFFLINE-002 | offline | `offline/RoomOfflineQueueStorage.kt` | persistência Room | N/A | BUILD PASS | recovery físico pendente | `IMPLEMENTED / DEVICE_GATED` |
| OFFLINE-003 | offline | `offline/AtomicFileQueueStorage.kt` | storage atômico com `fsync`/publish | N/A | BUILD PASS | filesystem device pendente | `IMPLEMENTED / DEVICE_GATED` |
| OFFLINE-004 | offline | `offline/SyncWorker.kt` | worker de sincronização | INDIRETA | BUILD PASS | scheduling/restart pendente | `IMPLEMENTED / DEVICE_GATED` |
| TERM-001 | terminal | `terminal/TerminalEmulator.kt` | bounded executor com allowlist/restrições | SIM | full unit suite PASS | device smoke pendente | `IMPLEMENTED_BOUNDED_EXECUTOR` |
| TERM-002 | terminal | roadmap/Termux strategy | PTY/VT100 completo | NÃO | não provado | não provado | `TOKEN_VAZIO_PTY` |
| LFS-001 | git-lfs | LFS service/UI (`LfsScreen`) | install/track/list/fetch/pull/env adapters/UI | SIM | BUILD PASS | `git-lfs` + remote fixture pendentes | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` |
| EXT-001 | git external | worktree adapter | worktree operations | PARCIAL | BUILD PASS | filesystem matrix pendente | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` |
| EXT-002 | git external | bisect adapter | bisect operations | PARCIAL | BUILD PASS | regression fixture pendente | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` |
| EXT-003 | signing | GPG adapter | interface/processo GPG | PARCIAL | BUILD PASS | binário + fixture pendentes | `ADAPTER_IMPLEMENTED` |
| NATIVE-001 | native | `kernel/RafaeliaCore.kt` + CMake/JNI | bridge Kotlin → JNI | INDIRETA | APK dual-ABI BUILD PASS | chamada física pendente | `BRIDGE_IMPLEMENTED / BUILD_VERIFIED` |
| NATIVE-002 | native | `app/src/main/cpp/` / native libs | bibliotecas Android ARM | N/A | `armeabi-v7a` + `arm64-v8a` presentes no APK | device pendente | `BUILD_VERIFIED` |
| LLM-001 | local AI | `bridge/RafBridgeService.java` | HTTP loopback foreground bridge | SIM/INDIRETA | app BUILD PASS | Android/Kiwi/modelo real pendentes | `IMPLEMENTED / RUNTIME_GATED` |
| LLM-002 | local AI | `bridge/RafBridgeContract.java` | policy gate do bridge | INDIRETA | BUILD PASS | integração real pendente | `IMPLEMENTED` |
| LLM-003 | local AI | `bridge/RafModelClient.java` | cliente local chat completion | INDIRETA | BUILD PASS | GGUF/model server real pendente | `IMPLEMENTED / TOKEN_VAZIO_MODEL_RUNTIME` |
| KIWI-001 | extension | `kiwi-extension/` | extensão MV3 para loopback | SIM | fonte presente; fora do APK gate | Kiwi unpacked pendente | `IMPLEMENTED / TOKEN_VAZIO_KIWI_RUNTIME` |
| LLAMA-001 | native AI | bridge JNI LLaMA | ponte de integração | N/A | fonte/bridge parcial | `llama.h`/runtime externo não pinado | `BRIDGE_IMPLEMENTED / BLOCKED_EXTERNAL` |
| BUILD-001 | build | `.github/workflows/android-client-build.yml` | test + lint + assemble + verify + receipt | N/A | run `31821491676` PASS | N/A | `VERIFIED` |
| BUILD-002 | artifact | GitHub Actions artifact | `app-dev-debug.apk` 24,672,130 bytes | N/A | SHA/CRC/dual ABI PASS | install/start pendente | `BUILD_VERIFIED` |
| DEVICE-001 | device | runtime receipt | instalação + launch do APK exato | N/A | não executado neste checkpoint | `TOKEN_VAZIO` | `DEVICE_REQUIRED` |

## P33

A antiga lista de 33 itens L1 não representa mais 33 itens não implementados. O estado de fonte é:

```text
P33 source-functional = 33/33
P33 source coverage    = 100%
P33 device verified    = NÃO INFERIR
```

Ver `PENDING_33_ITEMS.md` para a genealogia item a item.

## Divergências eliminadas neste corte

1. `MultiPlatformManager` não é mais tratado como stub global.
2. `OfflineQueue` não é mais tratada como fila sem storage/workers.
3. unit tests do candidato não são mais `TOKEN_VAZIO`: foram executados e passaram.
4. APK não é mais `TOKEN_VAZIO` para o BUILD `bbdb556...`.
5. ARM32/ARM64 deixam de ser apenas intenção no BUILD observado: ambas ABIs estão presentes no APK.
6. `DEVICE_VERIFIED` continua vazio; build não é confundido com aparelho.

## Relação build ↔ head documental

A comparação `bbdb556...8ee151e` contém somente alterações em documentação/dados (`docs/`, `data/evidence/`, `ECOSYSTEM_RUNTIME_STATE.json`). Não houve alteração de `app/` nesse intervalo. Isso sustenta **equivalência da árvore de aplicação**, mas não converte um receipt commit-bound no receipt do head documental.

## Fontes complementares

- `RAFGITTOOLS_CURRENT_STATE.md`
- `STATUS_REPORT.md`
- `PENDING_33_ITEMS.md`
- `FIRST_COMPILE_RUN_TRIANGLE.md`
- `../ECOSYSTEM_RUNTIME_STATE.json`
- `canonical/2026-08-14/RAFGITTOOLS_SOURCE_BUILD_EVIDENCE_V1.md`
- `../data/evidence/github/rafgittools_android_build_31821491676.v1.json`

## Próxima ação

Fechar `DEVICE-001` com o APK hash-bound correspondente ao commit escolhido. Depois validar remotes/credenciais/provider/LFS/worktree/bisect em fixtures independentes, sem promover tudo por associação.
