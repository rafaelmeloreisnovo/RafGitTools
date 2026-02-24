# BUG Report (Validated Against Current Snapshot)

- Source report: `BUG_REPORT.md`
- Source file list: `rg --files` from current repository root
- Total findings parsed: **24**
- Actionable findings: **8**
- Non-resolvable findings: **16**
- Missing ratio: **66.67%** (threshold: 5.00%)
- Audit status: **FAIL**

## Actionable findings (resolved to current tree)

| # | Report Path | Resolved Path | Bug | Fix |
|---|---|---|---|---|
| C10 | PullRequestDetailScreen.kt | `app/src/main/kotlin/com/rafgittools/ui/screens/pullrequests/PullRequestDetailScreen.kt` | `R.string.date_format_*` → 3 strings faltando | → adicionadas em strings.xml |
| L1 | AppModule.kt:38 | `app/src/main/kotlin/com/rafgittools/di/AppModule.kt` | `HttpLoggingInterceptor.Level.BODY` em produção → tokens expostos em log | → condicional `if (BuildConfig.DEBUG)` |
| L2 | AppModule.kt:47 | `app/src/main/kotlin/com/rafgittools/di/AppModule.kt` | `CertificatePinner` com hash placeholder `AAAA…` → **SSL crash em 100% das chamadas** | → removido; adicionar pin real no release |
| L3 | SettingsViewModel.kt | `app/src/main/kotlin/com/rafgittools/ui/screens/settings/SettingsViewModel.kt` | `.collect{}` em Flow infinito dentro de `launch{}` → 2ª coroutine nunca inicia | → `onEach{}.launchIn(viewModelScope)` |
| L4 | JGitService.kt | `app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt` | 0 ocorrências de `withContext(IO)` — **CONFIRMADO JÁ CORRETO** (48 funções têm) | ✅ já fixado no arquivo original |
| L5 | SecurityManager.kt:282 | `app/src/main/kotlin/com/rafgittools/core/security/SecurityManager.kt` | `runBlocking` em função normal → potencial deadlock em pool limitado | ⚠ anotado; refactor para `suspend` recomendado |
| D1 | GithubApiService.kt:48 | `app/src/main/kotlin/com/rafgittools/data/github/GithubApiService.kt` | `vnd.github.v3.text-match+json` (descontinuado) | → `vnd.github+json` + `X-GitHub-Api-Version: 2022-11-28` |
| D4 | SecurityManager.kt:262 | `app/src/main/kotlin/com/rafgittools/core/security/SecurityManager.kt` | `PackageManager.GET_SIGNATURES` (< API 28 deprecated) | ✅ já protegido com `@Suppress + SDK_INT check` |

## Out-of-scope snapshot mismatch

| # | Report Path | Status | Reason |
|---|---|---|---|
| C1 | CommitDetailScreen.kt:133 | out-of-scope snapshot mismatch | ambiguous |
| C2 | CommitDetailViewModel.kt:44 | out-of-scope snapshot mismatch | ambiguous |
| C3 | CommitDetailViewModel.kt:42 | out-of-scope snapshot mismatch | ambiguous |
| C4 | CommitDetailViewModel.kt:66 | out-of-scope snapshot mismatch | ambiguous |
| C5 | CommitDetailScreen.kt:147 | out-of-scope snapshot mismatch | ambiguous |
| C6 | CommitDetailScreen.kt:148 | out-of-scope snapshot mismatch | ambiguous |
| C7 | CommitDetailScreen.kt:267 | out-of-scope snapshot mismatch | ambiguous |
| C8 | CommitDetailScreen.kt:278 | out-of-scope snapshot mismatch | ambiguous |
| C9 | CommitDetailScreen.kt | out-of-scope snapshot mismatch | ambiguous |
| L6 | OAuthDeviceFlowManager.kt:31 | out-of-scope snapshot mismatch | ambiguous |
| L7 | AuthViewModel.kt:65 | out-of-scope snapshot mismatch | ambiguous |
| L8 | CommitDetailViewModel.kt | out-of-scope snapshot mismatch | ambiguous |
| L9 | HomeViewModel.kt | out-of-scope snapshot mismatch | ambiguous |
| L10 | PersistentErrorLogger + DiffAuditLogger | out-of-scope snapshot mismatch | missing |
| D2 | data/network/AuthInterceptor.kt | out-of-scope snapshot mismatch | missing |
| D3 | CommitDetailScreen.kt | out-of-scope snapshot mismatch | ambiguous |
