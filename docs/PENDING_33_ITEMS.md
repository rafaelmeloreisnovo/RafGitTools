# PENDING 33 ITEMS — Baseline revisado + status 2026-08-12

## Origem dos "33 lugares"

A origem adotada para os **33 lugares** é o recorte dos **primeiros 33 itens marcados como `🔴 L1`** no roadmap oficial de desenvolvimento.

- Arquivo-fonte: `docs/ROADMAP.md`
- Critério: linhas de tabela no formato `| <id> | <feature> | 🔴 L1 | ... |`
- Objetivo: transformar lacunas de implementação em backlog rastreável com ID único.
- Regra de evidência: `source-functional != tests executed != APK built != device verified`.

## Legenda de status

| Símbolo | Significado |
|---|---|
| ✅ | Implementado em fonte/integrado ao fluxo correspondente |
| 🔶 | Parcial em fonte |
| 🔴 | Não iniciado |
| 🆕 | Marcador histórico de item implementado na execução de 2026-08-03; não é categoria aditiva |

## Lista rastreável

| Item ID | Feature ID | Feature (ROADMAP) | Status | Evidência / limite |
|---|---:|---|---|---|
| P33-01 | 20 | Git clone (shallow) | ✅ | `JGitService.cloneShallow()` |
| P33-02 | 21 | Git clone (single branch) | ✅ | `JGitService.cloneSingleBranch()` |
| P33-03 | 22 | Git clone (with submodules) | ✅ | `setCloneSubmodules(true)`; transporte herdado pelo JGit child command |
| P33-04 | 24 | Git commit (amend) | ✅ | `JGitService.commitAmend()` + interface + impl |
| P33-05 | 25 | Interactive staging | ✅ | `InteractiveStagingService` + DiffViewer: stage/unstage por hunk, index-only, stale-check, DirCache lock/atomic commit; runtime Android ainda é evidência separada |
| P33-06 | 29 | Force push with lease | ✅ | destination-ref lease + authenticated `lsRemote` + fail-closed; fixture GitHub privada real ainda é runtime evidence |
| P33-07 | 30 | Pull with rebase | ✅ | `JGitService.pullWithRebase()` integrado ao fluxo pull |
| P33-08 | 33 | Branch rename | ✅ | implementação JGit integrada |
| P33-09 | 36 | Merge strategies | ✅ | `JGitService.mergeWithStrategy()`; sem promover estratégia não testada a runtime PASS |
| P33-10 | 40 | Stash operations | ✅ | operações stash integradas |
| P33-11 | 42 | Git config management | ✅ | `getGitConfig`, `setGitConfig`, `listGitConfig` |
| P33-12 | 46 | Syntax highlighting | 🆕✅ | `SyntaxHighlighter.kt` + FileViewer |
| P33-13 | 47 | Line numbers | 🆕✅ | gutter no FileViewer |
| P33-14 | 48 | File search | ✅ | `JGitService.searchFiles()` |
| P33-15 | 50 | Breadcrumb navigation | 🆕✅ | `BreadcrumbBar` em FileBrowserScreen |
| P33-16 | 51 | File type icons | 🆕✅ | `getFileIcon()` + `getFileIconColor()` |
| P33-17 | 52 | File size display | ✅ | `GitFile.size` + formatter |
| P33-18 | 53 | Last modified date | ✅ | `GitFile.lastModified`, `getFileLastModified()` |
| P33-19 | 54 | Commit info display | ✅ | `getCommits()` retorna autor + data |
| P33-20 | 55 | Branch selector | 🆕✅ | branch selector em FileBrowserScreen |
| P33-21 | 56 | Tag selector | 🆕✅ | tags no selector do FileBrowserScreen |
| P33-22 | 57 | Repository metadata | 🆕✅ | HomeScreen mostra private/public, stars, forks, language |
| P33-23 | 59 | Device authorization flow | ✅ | `OAuthDeviceFlowManager`; Client ID real continua CONFIG_REQUIRED |
| P33-24 | 61 | Fine-grained PAT support | ✅ | `PATScopeInspector` + validação remota |
| P33-25 | 63 | Token refresh mechanism | ✅ | contrato real = lifecycle/invalidation: `AuthInterceptor` + `TokenRefreshManager` invalidam sessão em 401, distinguem rate-limit 403 por headers e exigem reautenticação; nenhum refresh-token/client-secret é falsamente alegado para PAT/OAuth App atual |
| P33-26 | 64 | SSH key generation | ✅ | `SshKeyManager.generateKeyPair()` |
| P33-27 | 65 | SSH key management | ✅ | `SshKeyManager` |
| P33-28 | 66 | SSH agent integration | ✅ | transporte SSH JGit integrado; servidor/chave real = runtime evidence |
| P33-29 | 67 | Biometric authentication | ✅ | `BiometricAuthManager` |
| P33-30 | 68 | Multi-account support | ✅ | `MultiAccountManager` |
| P33-31 | 69 | Account switching | ✅ | `MultiAccountManager.switchAccount()` |
| P33-32 | 70 | Session management | ✅ | estado persistido + 401/403 lifecycle integrado |
| P33-33 | 71 | Secure logout | ✅ | `AuthRepository.clearAuthState()` / `logout()` |

## Resumo não sobreposto

| Estado de fonte | Count |
|---|---:|
| ✅ Implementado / source-functional | 33 |
| 🔶 Parcial | 0 |
| 🔴 Não iniciado | 0 |
| **Total** | **33** |

**Cobertura funcional de fonte P33: 33/33 (100%).**

Os 8 itens com marcador `🆕` são um **subconjunto histórico** desses 33, implementados na execução de 2026-08-03; não devem ser somados novamente ao total.

## P33-05 — boundary de segurança

Interactive staging está implementado em fonte para o caso que o modelo de diff representa de forma lossless:

- arquivo rastreado `MODIFY`;
- texto UTF-8 de até 2 MiB;
- newline final preservado;
- stage e unstage de um hunk por vez;
- working tree nunca é reescrito;
- hunk é revalidado contra o diff atual;
- HEAD e object-id do index são revalidados antes do lock/commit;
- conflitos/multi-stage, binário, non-UTF-8 e missing-final-newline falham fechado;
- `DirCacheEditor.commit()` publica a alteração do index sob lock.

Esses limites não são `TOKEN_VAZIO`: são **contrato explícito de representação/safety**. Ampliar para binários, add/delete/rename ou arquivos sem newline exige enriquecer o modelo de patch antes de alterar o índice.

## P33-25 — boundary de autenticação

O nome histórico “Token refresh mechanism” não autoriza inventar um refresh-token que o método de autenticação atual não fornece.

O contrato implementado é:

```text
credential valid      -> sessão permanece
HTTP 401              -> cache em memória invalidado + limpeza persistente tentada + reautenticação necessária
HTTP 403 + remaining=0 -> rate-limit; credencial preservada
HTTP 403 normal       -> forbidden; credencial preservada
PAT/OAuth App atual   -> nenhum client secret ou refresh token armazenado/forjado no APK
```

O antigo método `refreshOAuthToken(clientId, clientSecret, refreshToken)` que sempre falhava era um **stub**, não uma feature. Ele foi removido da fonte compilada. Se futuramente houver migração explícita para um fluxo que entregue refresh tokens, isso deve entrar como novo contrato/versionamento, com armazenamento e testes próprios.

## Evidência ainda não promovida

O fechamento 33/33 é de **fonte**. O seguinte permanece separado:

```text
unit tests do head revisado      = TOKEN_VAZIO / BLOCKED_INFRA_BILLING remoto
APK do head + SHA-256            = TOKEN_VAZIO
physical-device install/start    = TOKEN_VAZIO
private remote fixtures          = TOKEN_VAZIO_RUNTIME
claim_allowed                    = false
```

Nenhuma frase neste documento transforma ausência de execução em PASS.

## Regra para próximos commits

```text
feat: implementa <descrição> (P33-XX)
fix: corrige <descrição> (P33-XX)
```
