# PENDING 33 ITEMS — Baseline revisado + status 2026-08-14

## Origem dos "33 lugares"

A origem adotada para os **33 lugares** é o recorte dos **primeiros 33 itens marcados como `🔴 L1`** no roadmap oficial de desenvolvimento.

- Arquivo-fonte: `docs/ROADMAP.md`
- Critério: linhas de tabela no formato `| <id> | <feature> | 🔴 L1 | ... |`
- Regra de evidência: `source-functional != tests executed != APK built != device verified`.

## Legenda

| Símbolo | Significado |
|---|---|
| ✅ | Implementado em fonte/integrado |
| 🔶 | Parcial em fonte |
| 🔴 | Não iniciado |
| 🆕 | Marcador histórico de 2026-08-03; subconjunto, não categoria aditiva |

## Lista rastreável

| Item ID | Feature ID | Feature | Status | Evidência / limite |
|---|---:|---|---|---|
| P33-01 | 20 | Git clone (shallow) | ✅ | `JGitService.cloneShallow()` |
| P33-02 | 21 | Git clone (single branch) | ✅ | `JGitService.cloneSingleBranch()` |
| P33-03 | 22 | Git clone (with submodules) | ✅ | `setCloneSubmodules(true)`; transporte herdado pelo child command JGit |
| P33-04 | 24 | Git commit (amend) | ✅ | `JGitService.commitAmend()` |
| P33-05 | 25 | Interactive staging | ✅ | index-only stage/unstage por hunk + stale/HEAD/index gates |
| P33-06 | 29 | Force push with lease | ✅ | destination-ref lease + authenticated `lsRemote` + fail-closed |
| P33-07 | 30 | Pull with rebase | ✅ | `JGitService.pullWithRebase()` |
| P33-08 | 33 | Branch rename | ✅ | JGit integrado |
| P33-09 | 36 | Merge strategies | ✅ | `JGitService.mergeWithStrategy()`; contrato semântico ainda auditado separadamente |
| P33-10 | 40 | Stash operations | ✅ | operações stash integradas |
| P33-11 | 42 | Git config management | ✅ | get/set/list config |
| P33-12 | 46 | Syntax highlighting | 🆕✅ | FileViewer |
| P33-13 | 47 | Line numbers | 🆕✅ | FileViewer gutter |
| P33-14 | 48 | File search | ✅ | `JGitService.searchFiles()` |
| P33-15 | 50 | Breadcrumb navigation | 🆕✅ | FileBrowser |
| P33-16 | 51 | File type icons | 🆕✅ | FileBrowser |
| P33-17 | 52 | File size display | ✅ | `GitFile.size` |
| P33-18 | 53 | Last modified date | ✅ | `getFileLastModified()` |
| P33-19 | 54 | Commit info display | ✅ | autor/data em commits |
| P33-20 | 55 | Branch selector | 🆕✅ | FileBrowser |
| P33-21 | 56 | Tag selector | 🆕✅ | FileBrowser |
| P33-22 | 57 | Repository metadata | 🆕✅ | HomeScreen |
| P33-23 | 59 | Device authorization flow | ✅ | `OAuthDeviceFlowManager`; Client ID real = CONFIG_REQUIRED |
| P33-24 | 61 | Fine-grained PAT support | ✅ | validação remota / scope inspection |
| P33-25 | 63 | Token refresh mechanism | ✅ | capability-aware: PAT/OAuth App sem refresh -> reauth; GitHub App Device Flow com `refresh_token` -> rotação cifrada, concorrência serializada, retry único, zero client secret |
| P33-26 | 64 | SSH key generation | ✅ | `SshKeyManager.generateKeyPair()` |
| P33-27 | 65 | SSH key management | ✅ | `SshKeyManager` |
| P33-28 | 66 | SSH agent integration | ✅ | transporte SSH JGit; fixture real = runtime evidence |
| P33-29 | 67 | Biometric authentication | ✅ | `BiometricAuthManager` |
| P33-30 | 68 | Multi-account support | ✅ | `MultiAccountManager` |
| P33-31 | 69 | Account switching | ✅ | `switchAccount()` |
| P33-32 | 70 | Session management | ✅ | persistência + lifecycle 401/403 + refresh capability |
| P33-33 | 71 | Secure logout | ✅ | `clearAuthState()` remove access/refresh/expiry |

## Resumo não sobreposto

| Estado de fonte | Count |
|---|---:|
| ✅ Implementado / source-functional | 33 |
| 🔶 Parcial | 0 |
| 🔴 Não iniciado | 0 |
| **Total** | **33** |

**Cobertura funcional de fonte P33: 33/33 (100%).** Os 8 itens `🆕` são subconjunto histórico e não são somados novamente.

## Evidência executada do candidato 2026-08-14

Para o commit `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`, o workflow `Android Client Build` run `31821491676` executou e passou:

- custody/structural tests;
- authentication unit tests;
- full dev unit tests;
- Android lint;
- `assembleDevDebug`;
- APK verification + build receipt;
- artifact upload.

APK observado:

- `app-dev-debug.apk`;
- `24,672,130` bytes;
- SHA-256 `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`;
- `armeabi-v7a` + `arm64-v8a` presentes;
- build receipt SHA-256 `f124ac18a9f1e158aa764a12b49a25dbf54cc870cca8359e0355416bee5219a5`.

Isso promove **tests/build evidence do candidato**, mas não converte automaticamente cada item P33 em `DEVICE_VERIFIED`.

## P33-05 — boundary de segurança

Interactive staging é lossless somente para tracked `MODIFY`, UTF-8 <= 2 MiB e newline final. Stage/unstage é de um hunk; working tree não é reescrito; hunk/HEAD/index são revalidados; conflitos/multi-stage, binário, non-UTF8 e missing-final-newline falham fechado; publicação usa `DirCacheEditor.commit()` sob lock. Esses limites são contrato, não `TOKEN_VAZIO`.

No candidato atual, a regressão `MissingObjectException` foi corrigida mantendo `scan + format` no mesmo `DiffFormatter`; o full dev unit test gate passou.

## P33-25 — boundary de autenticação

O nome histórico “Token refresh mechanism” não autoriza fingir que toda credencial possui refresh. A fonte implementa por capacidade:

```text
PAT / OAuth App sem refresh_token
  401 -> refresh indisponível -> clearAuthState + cache=null -> reautenticação

GitHub App Device Flow com refresh_token
  -> refresh token cifrado em alias separado
  -> expiries persistidos
  -> 401 entra em recoveryMutex/refreshMutex
  -> request concorrente reutiliza access token já rotacionado se outro 401 venceu
  -> POST refresh envia client_id + grant_type=refresh_token + refresh_token
  -> nunca envia client_secret
  -> access + refresh rotacionados substituem os anteriores
  -> resposta 401 original é fechada
  -> request repetida uma única vez
  -> segundo 401 invalida sessão sem segundo refresh

403 + remaining=0 -> rate-limit, sessão preservada
outro 403         -> forbidden, sessão preservada
```

`savePat()` apaga refresh state de sessão anterior e `clearAuthState()` apaga access/refresh/expiries. O antigo método sempre-falhando `refreshOAuthToken(clientId, clientSecret, refreshToken)` foi removido. Authentication tests do candidato passaram, porém o refresh real de GitHub App continua dependente de configuração/fixture real.

## Evidência ainda não promovida

```text
unit tests do candidato           = PASS
Android lint                      = PASS
APK + SHA-256                     = PASS
armeabi-v7a + arm64-v8a           = PASS
physical-device install/start     = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
private remote fixtures           = TOKEN_VAZIO_RUNTIME
GitHub App refresh real           = CONFIG_REQUIRED / TOKEN_VAZIO_RUNTIME
SSH/provider external fixtures    = TOKEN_VAZIO_RUNTIME
claim_allowed                     = false
release_allowed                   = false
```

O **33/33 continua sendo fonte**. O BUILD provado é uma camada adicional, não equivalência automática com runtime físico por feature.

## Proveniência

- estado atual: `docs/RAFGITTOOLS_CURRENT_STATE.md`;
- matriz executável: `ECOSYSTEM_RUNTIME_STATE.json`;
- checkpoint append-only: `docs/canonical/2026-08-14/RAFGITTOOLS_SOURCE_BUILD_EVIDENCE_V1.md`;
- evidence JSON: `data/evidence/github/rafgittools_android_build_31821491676.v1.json`.

## Regra para próximos commits

```text
feat: implementa <descrição> (P33-XX)
fix: corrige <descrição> (P33-XX)
```

Se o commit/APK mudar, a cadeia de evidência de BUILD deve ser refeita; não reutilizar receipt antigo como prova do novo head.
