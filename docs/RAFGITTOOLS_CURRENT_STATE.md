# RAFGITTOOLS_CURRENT_STATE

- Status: **ATIVO — código integrado, validação local pendente**
- Estado observado: **2026-07-21** (atualizado)
- Fonte de verdade: código em `app/src/`, testes, contratos e `ECOSYSTEM_RUNTIME_STATE.json`
- GitHub Actions: **OUT_OF_SCOPE** — workflows falham por `actions/checkout@v6` inexistente (baseline pré-existente); ausência de run não é PASS nem FAIL de código

## Núcleo Android/Git/GitHub

| Componente | Estado | Limite atual |
|---|---|---|
| Android / Compose / Hilt / Room v4 | `IMPLEMENTED` | APK e device smoke não executados nesta atividade |
| PAT + armazenamento seguro | `IMPLEMENTED` | Resultado end-to-end depende de execução local/device |
| OAuth Device Flow (RFC 8628) | `IMPLEMENTED / CONFIG_REQUIRED` | Exige Client ID público configurado |
| SSH key rotation (Ed25519) | `IMPLEMENTED` | Upload/delete via GitHub API — `SshKeyRotationManager` |
| PAT expiry detection | `IMPLEMENTED` | `TokenRefreshManager.checkPATExpiry()` via `GitHub-Authentication-Token-Expiry` header |
| API GitHub (50+ endpoints) | `PARTIAL_ADVANCED` | Falta matriz end-to-end completa |
| Git local via JGit (25+ ops) | `PARTIAL_ADVANCED` | Rede, credenciais e conflitos precisam de regressão real |
| SSH auth (key use) | `PARTIAL` | Depende de ambiente/chaves e teste em device |
| GPG | `ADAPTER_IMPLEMENTED / RUNTIME_TOKEN_VAZIO` | Wrapper exige binário `gpg` acessível pelo processo autorizado |
| Git LFS (UI completo) | `IMPLEMENTED / RUNTIME_TOKEN_VAZIO` | `LfsScreen` + `LfsViewModel` + `LfsManager`; requer `git-lfs` e repositório real para validar |
| Worktree | `ADAPTER_IMPLEMENTED / RUNTIME_TOKEN_VAZIO` | Falta matriz de filesystem/device |
| Bisect | `ADAPTER_IMPLEMENTED / RUNTIME_TOKEN_VAZIO` | Falta cenário regressivo controlado |
| Webhooks (GitHub API) | `IMPLEMENTED` | `WebhooksScreen` + `WebhooksViewModel`; list/create/delete/ping via `GithubApiService` |
| Worktree (UI) | `IMPLEMENTED` | `WorktreeScreen` + `WorktreeViewModel`; add/list/remove/lock/unlock/prune via `WorktreeManager` |
| Bisect (UI) | `IMPLEMENTED` | `BisectScreen` + `BisectViewModel`; start/good/bad/skip/reset workflow via `BisectManager` |
| Terminal | `BOUNDED_EXECUTOR` | Não é PTY/VT100 e não aceita Git gravável (ProcessBuilder allowlist) |
| Multi-provider (5 providers) | `IMPLEMENTED` | GitHub, GitLab, Bitbucket, Gitea/Forgejo, Azure DevOps — todos via `MultiPlatformManager` |
| Offline queue (Room) | `IMPLEMENTED` | `RoomOfflineQueueStorage` + `OfflineOperationDao` (DB v3); `SyncWorker` ainda usa AtomicFile |
| Repository sync state | `IMPLEMENTED` | `RepositorySyncWorker` (15 min) — `BranchTrackingStatus` → `SyncState` (DB v4) |
| rafaelia JNI bridge | `IMPLEMENTED / BUILD_WIRED` | CMakeLists.txt produz `librafaelia.so`; integração EMA/prefetch pendente |
| LLaMA kernel JNI | `BRIDGE_IMPLEMENTED / BLOCKED` | `raf_kernel_jni.c` existe; bloqueado em `llama.h` externo |

## Correções estruturais deste corte

### Executor limitado

`TerminalEmulator.kt` agora:

- drena stdout/stderr enquanto o processo executa, evitando bloqueio por pipe cheio;
- rejeita aspas abertas, escapes incompletos, NUL e múltiplas linhas;
- permite apenas subcomandos Git de leitura;
- rejeita ações `find` que executem comandos ou removam/escrevam arquivos;
- continua explicitamente fora do escopo de PTY/VT100.

Operações graváveis devem seguir:

```text
RafGitTools
→ job.v1 tipado
→ GovernanceGate
→ runtime Termux autorizado
→ resultado estruturado
```

### Fila offline

`OfflineQueue` recebeu fronteira de armazenamento durável. A implementação
`AtomicFileQueueStorage` usa:

- registros binários length-prefixed;
- limites de quantidade e tamanho;
- `fsync` antes da publicação;
- arquivo temporário no mesmo diretório;
- rollback da mutação quando a persistência falha.

Ainda falta conectar um codec de `OfflineOperation` e WorkManager. Portanto,
**fila durável disponível** não significa **sincronização de produção concluída**.

### Multi-plataforma

Todos os cinco providers estão implementados em `MultiPlatformManager`:

| Provider | Adapter | Autenticação |
|---|---|---|
| GitHub | `GithubApiService` | PAT / OAuth |
| GitLab | `GitLabApiService` — `GET /api/v4/projects?membership=true` | `Authorization: Bearer token` |
| Bitbucket | `BitbucketApiService` — `GET /2.0/repositories/{workspace}` | Basic Auth |
| Gitea/Forgejo | `GiteaApiService` — `GET /api/v1/user/repos` | `Authorization: token token` |
| Azure DevOps | `AzureDevOpsApiService` — `GET /{org}/{project}/_apis/git/repositories?api-version=7.0` | PAT Basic (`Base64(":token")`) |

As consultas tipadas distinguem `Success`, `NotConfigured`, `AuthenticationError`, `NetworkError`.

## Diretório `fazer/`

Os 17 arquivos de `fazer/` são rascunhos históricos **não compilados**. A auditoria
do PR #267 concluiu que foram superados pelas implementações mais completas em
`app/src/`. Eles **não estão pendentes de integração** e não podem ser usados como
fonte de verdade.

Próxima limpeza dedicada:

```text
fazer/
→ archive/legacy-drafts/fazer-2026-07/
ou remoção após comparação final de hashes/diffs
```

Nenhuma funcionalidade deve ser contabilizada duas vezes por existir em
`app/src/` e em `fazer/`.

## Contratos locais

- `contracts/job-v1.schema.json`: handoff tipado e limitado para o runtime;
- `contracts/ecosystem-runtime-state.schema.json`: estados e evidências;
- `ECOSYSTEM_RUNTIME_STATE.json`: matriz material desta revisão;
- `scripts/validate_runtime_truth.py`: validação stdlib, sem GitHub Actions.

## Evidência e limitações

- Mudanças de código e testes estão presentes na branch de auditoria.
- GitHub Actions não foi executado por falta momentânea de créditos.
- APK, device smoke ARM32/ARM64 e integração Termux permanecem `TOKEN_VAZIO` até
  existirem comando, stdout/stderr, hash e resultado de aparelho.

## Retroalimentar[4] — 2026-07-21

- **F_ok:** inconsistências foram transformadas em código (multi-platform 5 providers, LFS UI, SSH rotation, PAT expiry, Room offline queue + sync worker, rafaelia build wiring, HomeViewModel local repos, **Webhooks UI**, **Worktree UI**, **Bisect UI** — todos os TOKEN_VAZIO de UI agora têm Screen + ViewModel com navegação completa). Documentação alinhada com código.
- **F_gap:** build Android em device físico, WorkManager scheduling em device real, ponte Termux PTY e `llama.h` para LLaMA kernel não comprovados. GPG exige binário `gpg` acessível.
- **F_next:** executar `./gradlew assembleDevDebug` em ambiente com SDK; resolver `actions/checkout@v6` nos workflows para desbloquear CI; adicionar `llama.cpp` como submódulo para desbloquear RafKernelBridge.
