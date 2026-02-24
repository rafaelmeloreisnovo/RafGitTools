# Bug Report — Validated Findings

- Source report: `BUG_REPORT.md`
- Source file inventory command: `rg --files`
- Total file references scanned: **38**
- Actionable (resolvable) references: **15**
- Non-resolvable references: **23**
- Mismatch ratio: **60.53%**
- Threshold: **5.00%**
- Pre-check status: **FAIL**

## Actionable Findings (resolved against current tree)

| Source Line | Original Reference | Resolved Path | Context |
|---:|---|---|---|
| 19 | `PullRequestDetailScreen.kt` | `app/src/main/kotlin/com/rafgittools/ui/screens/pullrequests/PullRequestDetailScreen.kt` | \| C10 \| PullRequestDetailScreen.kt \| `R.string.date_format_*` → 3 strings faltando \| → adicionadas em strings.xml \| |
| 25 | `GithubModels.kt` | `app/src/main/kotlin/com/rafgittools/domain/model/github/GithubModels.kt` | **64 campos** em `GithubModels.kt` sem `@SerializedName`. |
| 46 | `AppModule.kt` | `app/src/main/kotlin/com/rafgittools/di/AppModule.kt` | \| L1 \| AppModule.kt:38 \| `HttpLoggingInterceptor.Level.BODY` em produção → tokens expostos em log \| → condicional `if (BuildConfig.DEBUG)` \| |
| 47 | `AppModule.kt` | `app/src/main/kotlin/com/rafgittools/di/AppModule.kt` | \| L2 \| AppModule.kt:47 \| `CertificatePinner` com hash placeholder `AAAA…` → **SSL crash em 100% das chamadas** \| → removido; adicionar pin real no release \| |
| 48 | `SettingsViewModel.kt` | `app/src/main/kotlin/com/rafgittools/ui/screens/settings/SettingsViewModel.kt` | \| L3 \| SettingsViewModel.kt \| `.collect{}` em Flow infinito dentro de `launch{}` → 2ª coroutine nunca inicia \| → `onEach{}.launchIn(viewModelScope)` \| |
| 49 | `JGitService.kt` | `app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt` | \| L4 \| JGitService.kt \| 0 ocorrências de `withContext(IO)` — **CONFIRMADO JÁ CORRETO** (48 funções têm) \| ✅ já fixado no arquivo original \| |
| 50 | `SecurityManager.kt` | `app/src/main/kotlin/com/rafgittools/core/security/SecurityManager.kt` | \| L5 \| SecurityManager.kt:282 \| `runBlocking` em função normal → potencial deadlock em pool limitado \| ⚠ anotado; refactor para `suspend` recomendado \| |
| 63 | `GithubApiService.kt` | `app/src/main/kotlin/com/rafgittools/data/github/GithubApiService.kt` | \| D1 \| GithubApiService.kt:48 \| `vnd.github.v3.text-match+json` (descontinuado) \| → `vnd.github+json` + `X-GitHub-Api-Version: 2022-11-28` \| |
| 66 | `SecurityManager.kt` | `app/src/main/kotlin/com/rafgittools/core/security/SecurityManager.kt` | \| D4 \| SecurityManager.kt:262 \| `PackageManager.GET_SIGNATURES` (< API 28 deprecated) \| ✅ já protegido com `@Suppress + SDK_INT check` \| |
| 87 | `git/JGitService.kt` | `app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt` | │   │   ├── git/JGitService.kt               [confirmado OK] |
| 88 | `github/GithubApiService.kt` | `app/src/main/kotlin/com/rafgittools/data/github/GithubApiService.kt` | │   │   ├── github/GithubApiService.kt       [D1] |
| 90 | `di/AppModule.kt` | `app/src/main/kotlin/com/rafgittools/di/AppModule.kt` | │   ├── di/AppModule.kt                      [L1, L2] |
| 91 | `domain/model/github/GithubModels.kt` | `app/src/main/kotlin/com/rafgittools/domain/model/github/GithubModels.kt` | │   ├── domain/model/github/GithubModels.kt  [CAT-2: 64 @SerializedName] |
| 98 | `settings/SettingsViewModel.kt` | `app/src/main/kotlin/com/rafgittools/ui/screens/settings/SettingsViewModel.kt` | │       └── settings/SettingsViewModel.kt    [L3] |
| 106 | `build.gradle` | `build.gradle` | 1. **`BuildConfig.GITHUB_CLIENT_ID`** — adicionar em `build.gradle`: |

## Out-of-Scope Snapshot Mismatches

| Source Line | Original Reference | Status | Reason |
|---:|---|---|---|
| 10 | `CommitDetailScreen.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 11 | `CommitDetailViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 12 | `CommitDetailViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 13 | `CommitDetailViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 14 | `CommitDetailScreen.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 15 | `CommitDetailScreen.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 16 | `CommitDetailScreen.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 17 | `CommitDetailScreen.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 18 | `CommitDetailScreen.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 19 | `strings.xml` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 51 | `OAuthDeviceFlowManager.kt` | out-of-scope snapshot mismatch | ambiguous basename (2 matches) |
| 52 | `AuthViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (2 matches) |
| 53 | `CommitDetailViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 54 | `HomeViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (2 matches) |
| 64 | `data/network/AuthInterceptor.kt` | out-of-scope snapshot mismatch | ambiguous basename (2 matches) |
| 65 | `CommitDetailScreen.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 86 | `auth/OAuthDeviceFlowManager.kt` | out-of-scope snapshot mismatch | ambiguous basename (2 matches) |
| 89 | `network/AuthInterceptor.kt` | out-of-scope snapshot mismatch | ambiguous basename (2 matches) |
| 93 | `auth/AuthViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (2 matches) |
| 95 | `CommitDetailScreen.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 96 | `CommitDetailViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |
| 97 | `home/HomeViewModel.kt` | out-of-scope snapshot mismatch | ambiguous basename (2 matches) |
| 99 | `res/values/strings.xml` | out-of-scope snapshot mismatch | ambiguous basename (3 matches) |

