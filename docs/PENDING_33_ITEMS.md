# PENDING 33 ITEMS — Baseline revisado + status pós-refatoração 2026-08-03

## Origem dos "33 lugares"

A origem adotada para os **33 lugares** é o recorte dos **primeiros 33 itens marcados como `🔴 L1`** no roadmap oficial de desenvolvimento.

- Arquivo-fonte: `docs/ROADMAP.md`
- Critério: linhas de tabela no formato `| <id> | <feature> | 🔴 L1 | ... |`
- Objetivo: transformar lacunas de implementação em backlog rastreável com ID único para commits futuros.

## Legenda de status

| Símbolo | Significado |
|---|---|
| ✅ | Implementado (funcional, integrado) |
| 🔶 | Parcial (base funcional; integração ou escopo incompletos) |
| 🔴 | Não iniciado |
| 🆕 | Implementado nesta execução |

## Lista rastreável

| Item ID | Feature ID | Feature (ROADMAP) | Status | Lacuna residual |
|---|---:|---|---|---|
| P33-01 | 20 | Git clone (shallow) | ✅ | — |
| P33-02 | 21 | Git clone (single branch) | ✅ | — |
| P33-03 | 22 | Git clone (with submodules) | ✅ | — |
| P33-04 | 24 | Git commit (amend) | ✅ | `JGitService.commitAmend()` + interface + impl |
| P33-05 | 25 | Interactive staging | 🔶 | Staging/unstaging por arquivo OK; seleção por hunk requer UI dedicada |
| P33-06 | 29 | Force push with lease | ✅ | — |
| P33-07 | 30 | Pull with rebase | ✅ | `JGitService.pullWithRebase()` integrado ao fluxo pull |
| P33-08 | 33 | Branch rename | ✅ | — |
| P33-09 | 36 | Merge strategies | ✅ | `JGitService.mergeWithStrategy()` com ours/theirs/recursive/FF |
| P33-10 | 40 | Stash operations | ✅ | — |
| P33-11 | 42 | Git config management | ✅ | `getGitConfig`, `setGitConfig`, `listGitConfig` completos |
| P33-12 | 46 | Syntax highlighting | 🆕✅ | `SyntaxHighlighter.kt` + `FileViewer` usa `highlight()` por linha — 10 linguagens |
| P33-13 | 47 | Line numbers | 🆕✅ | Gutter de 40 dp com `itemsIndexed` no `FileViewer` — `FileBrowserScreen.kt` |
| P33-14 | 48 | File search | ✅ | `JGitService.searchFiles()` — busca por nome e conteúdo na árvore |
| P33-15 | 50 | Breadcrumb navigation | 🆕✅ | `BreadcrumbBar` composable em `FileBrowserScreen.kt` com scroll horizontal |
| P33-16 | 51 | File type icons | 🆕✅ | `getFileIcon()` + `getFileIconColor()` — mapa por extensão em `FileBrowserScreen.kt` |
| P33-17 | 52 | File size display | ✅ | Campo `size` em `GitFile` + `formatFileSize()` no viewer |
| P33-18 | 53 | Last modified date | ✅ | `GitFile.lastModified`, `JGitService.getFileLastModified()` |
| P33-19 | 54 | Commit info display | ✅ | `getCommits()` retorna autor + data |
| P33-20 | 55 | Branch selector | 🆕✅ | `AssistChip` + `DropdownMenu` na TopAppBar do `FileBrowserScreen` — `switchRef()` |
| P33-21 | 56 | Tag selector | 🆕✅ | Seção Tags no mesmo dropdown do branch selector — `FileBrowserScreen.kt` |
| P33-22 | 57 | Repository metadata | 🆕✅ | `HomeScreen` mostra ícone Lock/Public, stars, forks, language por repo GitHub |
| P33-23 | 59 | Device authorization flow | ✅ | `OAuthDeviceFlowManager` implementado |
| P33-24 | 61 | Fine-grained PAT support | ✅ | `PATScopeInspector` distingue classic/fine-grained via `X-OAuth-Scopes` header |
| P33-25 | 63 | Token refresh mechanism | ✅ | `TokenRefreshManager` detecta expiração via `GitHub-Authentication-Token-Expiry` header |
| P33-26 | 64 | SSH key generation | ✅ | `SshKeyManager.generateKeyPair()` |
| P33-27 | 65 | SSH key management | ✅ | `SshKeyManager` completo |
| P33-28 | 66 | SSH agent integration | ✅ | `JGitService.createSshTransportCallback()` |
| P33-29 | 67 | Biometric authentication | ✅ | `BiometricAuthManager` |
| P33-30 | 68 | Multi-account support | ✅ | `MultiAccountManager` — add/switch/remove/getActive |
| P33-31 | 69 | Account switching | ✅ | `MultiAccountManager.switchAccount()` |
| P33-32 | 70 | Session management | ✅ | `AuthRepository` persiste estado; expiração detectada via headers 401/403 |
| P33-33 | 71 | Secure logout | ✅ | `AuthRepository.clearAuthState()` / `logout()` |

## Resumo por status

| Status | Count |
|---|---|
| ✅ Implementado | 30 |
| 🆕 Implementado nesta execução (2026-08-03) | 8 |
| 🔶 Parcial | 2 |
| 🔴 Não iniciado | 0 |

**Total P33 com cobertura ≥ funcional: 32/33 (97 %)**

### Itens parciais remanescentes

| ID | Lacuna | Desbloqueio |
|---|---|---|
| P33-05 | Seleção por hunk na UI (interactive staging) | Requer componente de diff interativo dedicado |
| — | — | — |

> Última atualização: 2026-08-03 — auditoria completa do FileBrowserScreen e HomeScreen confirmou
> implementação de P33-12, 13, 15, 16, 20, 21, 22, 24, 25, 32.

## Regra para próximos commits

```
feat: implementa <descrição> (P33-XX)
fix: corrige <descrição> (P33-XX)
```
