# RafGitTools — Mapa de Correções (GAPS → FIXES)

Gerado em 2026-02-19. Todos os arquivos desta pasta são correções prontas para merge.

---

## 🔴 CRÍTICOS — Erros de compilação

| Arquivo corrigido | Gap | Problema | Solução |
|---|---|---|---|
| `ui/screens/search/SearchViewModel.kt` | Sintaxe | `extractRepoFromUrl` declarada dentro de `repositoryNameFromUrl` — chave fechante ausente. Arquivo não compilava. | Funções separadas corretamente. |
| `data/network/r` | Fantasma | Arquivo sem extensão, vazio, gerava ruído no build. | **Deletar** do repositório: `git rm app/src/main/kotlin/com/rafgittools/data/network/r` |

---

## 🟠 SHELLS — UI sem lógica / dados

| Arquivo corrigido | Gap P33 | Problema | Solução |
|---|---|---|---|
| `ui/screens/notifications/NotificationsViewModel.kt` | — | 32 linhas, zero lógica, lista nunca populava | Injeta `GithubApiService`, `loadNotifications()`, `markAsRead()`, `markAllAsRead()`, toggle showAll |
| `ui/screens/releases/ReleasesViewModel.kt` | — | 33 linhas, zero chamadas de API | Paginação real, `loadReleases(owner, repo)`, `loadNextPage()` |
| `ui/screens/releases/ReleaseDetailViewModel.kt` | — | Sem injeção de API, dados nunca carregavam | `loadRelease(owner, repo, id)` real via `GithubApiService` |
| `ui/screens/releases/ReleaseDetailScreen.kt` | — | Exibia `"Release details coming soon"` | UI completa: badge pre-release/draft, assets com download, release notes, author |
| `ui/screens/commits/CommitDetailScreen.kt` | — | Exibia `"Commit details planned"` | 3 tabs: Info (SHA, autor, data, parents), Files, Diff com colorização |
| `ui/screens/commits/CommitDetailViewModel.kt` | — | Arquivo inexistente | Criado do zero: carrega commit via JGitService, lista arquivos alterados, extrai diffs |

---

## 🟡 FEATURES AUSENTES — Zero código existente

| Arquivo novo | Gap P33 | Descrição |
|---|---|---|
| `data/auth/OAuthDeviceFlowManager.kt` | P33-23 | OAuth Device Flow completo (RFC 8628): device_code → user_code → polling → token |
| `ui/screens/auth/AuthScreen.kt` | P33-23 | AuthScreen atualizada com 2 tabs: PAT e OAuth Device Flow |
| `ui/screens/auth/AuthViewModel.kt` | P33-23 | AuthViewModel com `startOAuthFlow()`, `cancelOAuth()`, estados OAuth |
| `data/auth/TokenRefreshManager.kt` | P33-25 | Detecção de token expirado/revogado via 401/403. Documenta por que PATs não têm refresh_token e quando usar re-auth |
| `core/security/MultiAccountManager.kt` | P33-30/31/32 | Múltiplas contas GitHub: add, switch, remove, token por conta (encriptado via EncryptionManager) |
| `ui/components/SyntaxHighlighter.kt` | P33-12 | Highlight puro Kotlin/Compose para: Kotlin, Java, Python, JS/TS, XML, JSON, YAML, Shell, Gradle |
| `ui/screens/terminal/TerminalViewModel.kt` | — | Terminal com ProcessBuilder, histórico de comandos, safelist de comandos permitidos |
| `ui/screens/terminal/TerminalScreen.kt` | — | UI de terminal: fundo escuro, output colorido, chips de atalho, navegação no histórico |

---

## 🔧 INCOMPLETOS — Lógica parcial corrigida

| Arquivo corrigido | Problema | Solução |
|---|---|---|
| `ui/screens/home/HomeViewModel.kt` | Só carregava repos remotos do GitHub — repos locais clonados nunca apareciam | Adicionado scan de storage para repos locais via JGitService + tabs REMOTE/LOCAL |
| `ui/screens/diff/DiffViewerScreen.kt` | Side-by-side: painéis opostos ficavam em branco (comentário `// Empty placeholder`) | `buildSideBySidePairs()`: emparelha deleções + adições; slots vazios renderizam background diferente |

---

## Como integrar

```bash
# 1. Copiar cada arquivo para o path correto em app/src/main/kotlin/com/rafgittools/
# 2. Deletar arquivo fantasma:
git rm app/src/main/kotlin/com/rafgittools/data/network/r

# 3. Para o OAuth funcionar, adicionar CLIENT_ID no build.gradle:
#    buildConfigField "String", "GITHUB_CLIENT_ID", '"Iv1.your_client_id"'
#    E substituir a constante em OAuthDeviceFlowManager.kt

# 4. Para MultiAccountManager, EncryptionManager já existe — apenas injetar no AppModule

# 5. Para TerminalScreen, adicionar à navegação em MainActivity.kt:
#    composable(Screen.Terminal.route) { ... }
#    E adicionar Screen.Terminal ao sealed class Screen
```

---

## Gaps restantes não cobertos nesta iteração

| Gap P33 | Feature | Razão do adiamento |
|---|---|---|
| P33-05 | Interactive staging (hunks) | Requer UI complexa de seleção de hunks — sprint separada |
| P33-11 | Git config management | Requer nova tela de configuração Git |
| P33-26/27/28 | SSH gen/management/agent | SshKeyManager.kt já existe mas UI de gestão está incompleta |
| P33-29 | Biometric authentication | BiometricAuthManager.kt existe mas não está wired na tela de settings |
