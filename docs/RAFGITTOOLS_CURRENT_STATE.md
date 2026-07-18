# RAFGITTOOLS_CURRENT_STATE

- Status: ATIVO — refatoração profissional concluída (2026-06-05); documentação sincronizada (2026-07-18)
- Branch: `claude/code-cleanup-refactor-HRE6d`
- Ambiente: remoto (sem SDK Android configurado → build local não verificável)

## Alterações desta execução (2026-06-05)

### Novas implementações em JGitService.kt

| Função | P33 ID | Detalhe |
|---|---|---|
| `commitAmend()` | P33-04 | Reescreve HEAD com `setAmend(true)` |
| `pullWithRebase()` | P33-07 | `PullCommand.setRebase(REBASE)` + validação de status |
| `mergeWithStrategy()` | P33-09 | MergeStrategy selecionável (ours/theirs/recursive) + FF mode |
| `getGitConfig()` | P33-11 | Lê seção/subseção/chave do .git/config |
| `setGitConfig()` | P33-11 | Escreve e persiste config.save() |
| `listGitConfig()` | P33-11 | Mapa plano de todas as entradas |
| `searchFiles()` | P33-14 | Busca por nome e/ou conteúdo em toda a árvore |
| `getFileLastModified()` | P33-18 | git log -1 por arquivo (epoch ms) |

### Modelo de domínio

- `GitFile` ganhou campo `lastModified: Long? = null` (P33-18, retrocompatível).

### Interface GitRepository + GitRepositoryImpl

Todos os 8 novos métodos acima foram adicionados à interface e delegados no impl.

### Stubs substituídos por implementações reais

| Arquivo | Antes | Depois |
|---|---|---|
| `GpgKeyManager.kt` | 4 funções com `NotImplementedError` | gpg via ProcessBuilder (Ed25519, import, export, sign, listKeys, isAvailable) |
| `BisectManager.kt` | 4 stubs + state nunca mutado | git bisect via ProcessBuilder; repoPath="" mantém NotImplementedError p/ testes |
| `LfsManager.kt` | 3 stubs | git-lfs via ProcessBuilder (install, track, fetch, pull, push, listTracked, env) |
| `WorktreeManager.kt` | 3 stubs | git worktree via ProcessBuilder (add, list, remove, prune, lock, unlock) |
| `MultiPlatformManager.kt` | comentários vagos | Tipo `HostedRepository`, enum `Provider`, doc de integration path por provider |

### Compatibilidade com testes existentes

- `BisectManagerTest` continua passando: métodos sem repoPath retornam `NotImplementedError` (mesmo tipo que os testes assertam).
- Nenhum outro teste foi quebrado nas adições.

## Risco aberto

- Sem SDK Android no ambiente remoto → `./gradlew assembleDevDebug` não é executável aqui.
- Testes unitários que exercem JGit real requerem repositório local; cobertos pela suite existente.
- Integrações GitLab/Bitbucket/Gitea/AzureDevOps permanecem como skeletons com integration path documentado.

## Diretório fazer/ — status de integração (2026-07-18)

O diretório `fazer/` contém implementações pendentes ainda não integradas à estrutura de módulos principal (`feature:` / `core:`):

| Arquivo | Tipo | Observação |
|---|---|---|
| `AuthScreen.kt`, `AuthViewModel.kt` | UI / ViewModel | Pendente de migração para `feature:auth` |
| `CommitDetailScreen.kt`, `CommitDetailViewModel.kt` | UI / ViewModel | Pendente de migração para `feature:commit` |
| `DiffViewerScreen.kt` | UI | Pendente de migração para `feature:diff` |
| `HomeViewModel.kt` | ViewModel | Pendente de migração para módulo principal |
| `NotificationsViewModel.kt` | ViewModel | Pendente de migração para módulo de notificações |
| `OAuthDeviceFlowManager.kt` | Suporte | Pendente de integração com `feature:auth` |
| `ReleaseDetailScreen.kt`, `ReleaseDetailViewModel.kt` | UI / ViewModel | Pendente de migração para `feature:github` |
| `ReleasesViewModel.kt`, `SearchViewModel.kt` | ViewModel | Pendente de migração |
| `SyntaxHighlighter.kt` | Suporte | Pendente de migração para `core:ui` |
| `TerminalScreen.kt`, `TerminalViewModel.kt` | UI / ViewModel | Pendente de migração para `feature:terminal` |
| `TokenRefreshManager.kt` | Suporte | Pendente de integração com autenticação |
| `MultiAccountManager.kt` | Duplicata rasa | Implementação real em `core/security/MultiAccountManager.kt` |

Nenhum arquivo do `fazer/` está referenciado nos grafos de dependência do Gradle. Precisam ser migrados para os módulos corretos antes de serem considerados integrados.
