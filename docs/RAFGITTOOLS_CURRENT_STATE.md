# RAFGITTOOLS_CURRENT_STATE

- Status: **ATIVO — source-functional avançado / runtime ainda gated**
- Estado observado: **2026-08-12**
- Fonte de verdade: `app/src/`, testes, contratos, `docs/PENDING_33_ITEMS.md`, `ECOSYSTEM_RUNTIME_STATE.json` e receipts executáveis.
- GitHub Actions: **BLOCKED_INFRA_BILLING** — checks recentes registram `steps=[]`, `runner_id=0` e annotation de pagamento/spending limit. Ausência de execução não é PASS nem FAIL_CODE.
- Claim boundary: **`claim_allowed=false`** até build/test executado + APK/SHA-256 + device smoke do head revisado.

## Núcleo Android/Git/GitHub

| Componente | Estado de fonte | Limite atual |
|---|---|---|
| Android / Compose / Hilt / Room | `IMPLEMENTED_ADVANCED` | build/test do head e device smoke não comprovados |
| P33 roadmap L1 | `33/33 SOURCE_FUNCTIONAL` | não equivale a runtime PASS |
| PAT + armazenamento seguro | `IMPLEMENTED` | device regression pendente |
| Token lifecycle P33-25 | `IMPLEMENTED / INTEGRATED / CAPABILITY_AWARE` | PAT/OAuth App reautenticam; GitHub App Device Flow pode rotacionar refresh token quando a capacidade existe; runtime real pendente |
| OAuth Device Flow | `IMPLEMENTED / CONFIG_REQUIRED` | exige Client ID público real |
| GitHub privado HTTPS | `IMPLEMENTED / RUNTIME_GATED` | token usado como password, nunca como URL/username |
| Force-with-lease | `IMPLEMENTED / RUNTIME_GATED` | destination-ref lease + authenticated `lsRemote`; fixture privada real pendente |
| Interactive hunk staging | `IMPLEMENTED / RUNTIME_GATED` | index-only stage/unstage; Android smoke pendente |
| API GitHub | `PARTIAL_ADVANCED / RUNTIME_GATED` | falta matriz end-to-end completa |
| Git local via JGit | `IMPLEMENTED_ADVANCED / RUNTIME_GATED` | rede, credenciais e conflitos exigem regressão real |
| SSH auth | `PARTIAL / RUNTIME_GATED` | depende de chave/servidor real |
| GPG | `ADAPTER_IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | exige `gpg` autorizado |
| Git LFS | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | exige `git-lfs` + fixture |
| Worktree | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | falta matriz filesystem/device |
| Bisect | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | falta cenário regressivo controlado |
| Terminal | `BOUNDED_EXECUTOR` | não é PTY/VT100; Git gravável fica fora da allowlist genérica |
| Multi-provider | `IMPLEMENTED` | GitLab/Bitbucket/Gitea-Forgejo/Azure DevOps requerem credenciais/fixtures reais |
| Offline queue | `IMPLEMENTED` | recovery/scheduling em device ainda requer evidence |
| LLaMA kernel JNI | `BRIDGE_IMPLEMENTED / BLOCKED_EXTERNAL` | `llama.h`/runtime externo não pinado/provado |

## Autenticação — contrato canônico

### Persistência

- Access token é cifrado com Android Keystore/AES-GCM.
- Refresh token, quando o Device Flow realmente o fornece, usa alias Keystore separado.
- `savePat()` remove refresh token e expiries anteriores; troca de credencial não herda capacidade de outra sessão.
- `saveOAuthSession()` grava access + refresh opcional + expiries numa única edição de DataStore.
- `clearAuthState()` remove access, refresh, expiries, username e método.
- `AuthTokenCache` mantém somente a cópia em memória usada pelo interceptor.
- nenhum client secret faz parte do contrato Android.

### Ciclo de vida capability-aware

```text
request autenticado
  -> 2xx/3xx: sessão permanece

  -> 401 com PAT/OAuth App sem refresh_token:
       refresh capability ausente
       -> clearAuthState
       -> AuthTokenCache = null
       -> reautenticação

  -> 401 com GitHub App Device Flow + refresh_token:
       recoveryMutex
       -> refreshMutex
       -> se outro request já rotacionou o token rejeitado: reutiliza token novo
       -> senão refresh(client_id, grant_type=refresh_token, refresh_token)
       -> NÃO envia client_secret
       -> persiste access+refresh rotacionados
       -> atualiza cache
       -> fecha resposta 401 anterior
       -> retry exatamente uma vez
       -> segundo 401: invalida, sem segundo refresh

  -> 403 + X-RateLimit-Remaining=0: RateLimited; sessão preservada
  -> outro 403: Forbidden; sessão preservada
```

O antigo `refreshOAuthToken(clientId, clientSecret, refreshToken)` que sempre falhava foi removido. A implementação atual só ativa refresh quando existe uma capacidade real persistida pelo Device Flow.

### Concorrência

Dois 401 simultâneos não podem consumir o mesmo refresh token rotativo duas vezes. O fluxo compara o access token rejeitado com o access token persistido **dentro do mutex**: se já mudou, reutiliza a sessão nova. Além disso, refresh + decisão + eventual `clearAuthState()` passam pelo mesmo `recoveryMutex`, impedindo uma limpeza atrasada apagar uma rotação bem-sucedida.

## Git privado — contrato canônico

```text
UI GitHub HTTPS:
username = login GitHub ou x-access-token
password = PAT/OAuth token

JGit Credentials.Token:
username = x-access-token
password = token
```

Esse mapeamento cobre clone normal/shallow/single-branch/submodules, fetch, pull, pull-rebase, push e force-with-lease.

Force-with-lease usa:

```text
authenticated lsRemote preflight
+ exact expected 40-hex OID
+ RefLeaseSpec(refs/heads/<branch>, expected)
+ no fallback to unconditional force
```

## Interactive staging — contrato canônico

P33-05 está implementado para tracked `MODIFY`, UTF-8 <= 2 MiB e newline final. A operação é index-only, revalida hunk/HEAD/index, usa `lockDirCache + DirCacheEditor.commit()`, libera lock em `finally` e rejeita binário/non-UTF8/no-final-newline/unmerged fail-closed. Esses limites são contrato, não `TOKEN_VAZIO`.

## Executor / fila / providers

`TerminalEmulator` é bounded executor de leitura; operações graváveis passam por ações tipadas. A fila offline possui armazenamento durável/workers, mas device recovery ainda requer evidence. Multi-provider possui fonte para GitHub, GitLab, Bitbucket, Gitea/Forgejo e Azure DevOps; adapters não substituem fixtures reais.

## Diretório `fazer/`

`fazer/` permanece histórico/não compilado e **não é fonte de verdade**. Implementações compiladas vivem em `app/src/`.

## Contratos locais

- `contracts/job-v1.schema.json`
- `contracts/ecosystem-runtime-state.schema.json`
- `ECOSYSTEM_RUNTIME_STATE.json`
- `scripts/validate_runtime_truth.py`
- `scripts/rafgittools_private_auth_check.sh`
- `scripts/rafgittools_readiness_gate.sh`

## Evidência ainda pendente

```text
GitHub Actions execution       = BLOCKED_INFRA_BILLING
local Gradle/Kotlin tests      = TOKEN_VAZIO até executar gate local
APK devDebug + SHA-256         = TOKEN_VAZIO
physical device install/start  = TOKEN_VAZIO
private Git remote fixtures    = TOKEN_VAZIO_RUNTIME
PAT/OAuth reauth device flow   = TOKEN_VAZIO_RUNTIME
GitHub App refresh rotation    = CONFIG_REQUIRED / TOKEN_VAZIO_RUNTIME
GPG/LFS/worktree/bisect        = TOKEN_VAZIO_RUNTIME
claim_allowed                  = false
```

## Retroalimentar[4] — 2026-08-12

- **F_ok:** P33 33/33 em fonte; Privacy Manager, Readiness Gate, HTTPS privado, force-with-lease, hunk staging e token lifecycle possuem contratos fail-closed e regressões de fonte.
- **F_gap:** Gradle/APK/device não foram executados porque hosted runner está billing-blocked e o gate local ainda não foi executado; GitHub App refresh real exige Client ID/configuração e sessão real.
- **F_next:** continuar eliminando contradições de fonte; runtime fecha por `readiness gate -> tests -> APK/SHA -> device smoke -> fixtures privadas/OAuth`.
