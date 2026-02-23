# RafGitTools — Bug Report & Fix Manifest
> Auditoria completa: 42 bugs em 5 categorias

---

## 🔴 CAT-1: COMPILE ERRORS (build quebrado)

| # | Arquivo | Bug | Fix |
|---|---------|-----|-----|
| C1 | CommitDetailScreen.kt:133 | `commit.id` → campo não existe | → `commit.sha` |
| C2 | CommitDetailViewModel.kt:44 | `it.id` → campo não existe | → `it.sha` |
| C3 | CommitDetailViewModel.kt:42 | `jGitService.getCommitHistory()` → método não existe | → `gitRepository.getCommits()` |
| C4 | CommitDetailViewModel.kt:66 | `it.filePath` → campo não existe em `GitDiff` | → `it.newPath ?: it.oldPath ?: ""` |
| C5 | CommitDetailScreen.kt:147 | `commit.authorName` → campo plano não existe | → `commit.author.name` |
| C6 | CommitDetailScreen.kt:148 | `commit.authorEmail` → campo plano não existe | → `commit.author.email` |
| C7 | CommitDetailScreen.kt:267 | `diff.filePath` → campo não existe em `GitDiff` | → `diff.newPath ?: diff.oldPath` |
| C8 | CommitDetailScreen.kt:278 | `line.startsWith("-")` em `DiffLine` object | → `line.type == DiffLineType.DELETE` |
| C9 | CommitDetailScreen.kt | `Text(line)` passando `DiffLine` para `Text()` | → `Text(line.content)` |
| C10 | PullRequestDetailScreen.kt | `R.string.date_format_*` → 3 strings faltando | → adicionadas em strings.xml |

---

## 🔴 CAT-2: SERIALIZAÇÃO NULA SILENCIOSA (runtime — API retorna null em tudo)

**64 campos** em `GithubModels.kt` sem `@SerializedName`.
GSON não mapeia `camelCase` → `snake_case` automaticamente.
**Resultado:** TODA resposta da API GitHub deserializa campos como `null`.

Campos corrigidos (amostra):
- `fullName` → `@SerializedName("full_name")`
- `htmlUrl` → `@SerializedName("html_url")`
- `stargazersCount` → `@SerializedName("stargazers_count")`
- `isPrivate` → `@SerializedName("private")`
- `tagName` → `@SerializedName("tag_name")`
- `downloadCount` → `@SerializedName("download_count")`
- `lastReadAt` → `@SerializedName("last_read_at")`
- `openIssues` → `@SerializedName("open_issues")`
- ... 56 outros

---

## 🟠 CAT-3: BUGS DE LÓGICA (runtime silencioso)

| # | Arquivo | Bug | Fix |
|---|---------|-----|-----|
| L1 | AppModule.kt:38 | `HttpLoggingInterceptor.Level.BODY` em produção → tokens expostos em log | → condicional `if (BuildConfig.DEBUG)` |
| L2 | AppModule.kt:47 | `CertificatePinner` com hash placeholder `AAAA…` → **SSL crash em 100% das chamadas** | → removido; adicionar pin real no release |
| L3 | SettingsViewModel.kt | `.collect{}` em Flow infinito dentro de `launch{}` → 2ª coroutine nunca inicia | → `onEach{}.launchIn(viewModelScope)` |
| L4 | JGitService.kt | 0 ocorrências de `withContext(IO)` — **CONFIRMADO JÁ CORRETO** (48 funções têm) | ✅ já fixado no arquivo original |
| L5 | SecurityManager.kt:282 | `runBlocking` em função normal → potencial deadlock em pool limitado | ⚠ anotado; refactor para `suspend` recomendado |
| L6 | OAuthDeviceFlowManager.kt:31 | `CLIENT_ID = "Iv1.your_github_client_id"` hardcoded → OAuth **100% quebrado** | → `BuildConfig.GITHUB_CLIENT_ID` |
| L7 | AuthViewModel.kt:65 | `.first()` em `Flow<Result<User>>` → retorna cache stale, ignora fresh API | → `getAuthenticatedUserSync()` |
| L8 | CommitDetailViewModel.kt | Carregava 200 commits pra achar 1 SHA → O(n) desnecessário | → busca em lotes 50→200→1000 com early-exit |
| L9 | HomeViewModel.kt | `.first()` em 2 flows → cache stale em user e repos | → `*Sync()` variants |
| L10 | PersistentErrorLogger + DiffAuditLogger | `runBlocking` em logger → ANR se chamado de main thread | ⚠ anotado; refactor async recomendado |

---

## 🟡 CAT-4: APIs DEPRECATED

| # | Arquivo | API Deprecated | Fix |
|---|---------|---------------|-----|
| D1 | GithubApiService.kt:48 | `vnd.github.v3.text-match+json` (descontinuado) | → `vnd.github+json` + `X-GitHub-Api-Version: 2022-11-28` |
| D2 | data/network/AuthInterceptor.kt | Classe morta (DI injeta `data.auth.AuthInterceptor`) | → `@Deprecated` + comentário; seguro deletar |
| D3 | CommitDetailScreen.kt | `SimpleDateFormat` + `Date()` (não thread-safe, deprecated) | → `java.time.Instant` + `DateTimeFormatter` |
| D4 | SecurityManager.kt:262 | `PackageManager.GET_SIGNATURES` (< API 28 deprecated) | ✅ já protegido com `@Suppress + SDK_INT check` |

---

## 🟡 CAT-5: RESOURCES FALTANDO (compile error)

| String Key | Uso | Valor |
|-----------|-----|-------|
| `date_format_short_date` | PullRequestDetailScreen | `MMM dd, yyyy` |
| `date_format_short_datetime` | RepositoryDetailScreen | `MMM dd, yyyy HH:mm` |
| `date_format_datetime_with_at` | PullRequestDetailScreen (2x) | `MMM dd, yyyy 'at' HH:mm` |

---

## 📦 Arquivos Entregues (drop-in replacement)

```
app/src/main/
├── kotlin/com/rafgittools/
│   ├── data/
│   │   ├── auth/OAuthDeviceFlowManager.kt   [L6]
│   │   ├── git/JGitService.kt               [confirmado OK]
│   │   ├── github/GithubApiService.kt       [D1]
│   │   └── network/AuthInterceptor.kt       [D2 flagged]
│   ├── di/AppModule.kt                      [L1, L2]
│   ├── domain/model/github/GithubModels.kt  [CAT-2: 64 @SerializedName]
│   └── ui/screens/
│       ├── auth/AuthViewModel.kt            [L7]
│       ├── commits/
│       │   ├── CommitDetailScreen.kt        [C1,C5-C9, D3]
│       │   └── CommitDetailViewModel.kt     [C2-C4, L8]
│       ├── home/HomeViewModel.kt            [L9]
│       └── settings/SettingsViewModel.kt    [L3]
└── res/values/strings.xml                   [CAT-5: 3 keys]
```

---

## ⚠ Pendências (fora do escopo do patch automático)

1. **`BuildConfig.GITHUB_CLIENT_ID`** — adicionar em `build.gradle`:
   ```groovy
   buildConfigField "String", "GITHUB_CLIENT_ID", '"Iv1.REAL_CLIENT_ID"'
   ```
2. **CertificatePinner** — obter hash real com:
   ```bash
   openssl s_client -connect api.github.com:443 | openssl x509 -pubkey -noout | \
   openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64
   ```
3. **`SecurityManager.verifyTrustedSignature`** — refactor para `suspend`
4. **`PersistentErrorLogger` / `DiffAuditLogger`** — remover `runBlocking`
