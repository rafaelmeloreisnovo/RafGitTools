# RAFGITTOOLS_CURRENT_STATE

- Status: **ATIVO — source-functional avançado / runtime ainda gated**
- Estado observado: **2026-08-12**
- Fonte de verdade: `app/src/`, testes, contratos, `docs/PENDING_33_ITEMS.md`, `ECOSYSTEM_RUNTIME_STATE.json` e receipts executáveis.
- GitHub Actions: **BLOCKED_INFRA_BILLING** — o GitHub não está alocando runner; checks recentes registram `steps=[]`, `runner_id=0` e annotation de pagamento/spending limit. Ausência de execução não é PASS nem FAIL_CODE.
- Claim boundary: **`claim_allowed=false`** até build/test executado + APK/SHA-256 + device smoke do head revisado.

## Núcleo Android/Git/GitHub

| Componente | Estado de fonte | Limite atual |
|---|---|---|
| Android / Compose / Hilt / Room | `IMPLEMENTED_ADVANCED` | build/test do head e device smoke não comprovados |
| P33 roadmap L1 | `33/33 SOURCE_FUNCTIONAL` | não equivale a runtime PASS |
| PAT + armazenamento seguro | `IMPLEMENTED` | device regression ainda necessária |
| Token lifecycle P33-25 | `IMPLEMENTED / INTEGRATED` | 401 invalida sessão; 403 rate-limit/forbidden preserva credencial; não existe refresh-token fictício |
| OAuth Device Flow | `IMPLEMENTED / CONFIG_REQUIRED` | exige Client ID público real |
| GitHub privado HTTPS | `IMPLEMENTED / RUNTIME_GATED` | PAT/OAuth token usado como password, nunca como URL/username |
| Force-with-lease | `IMPLEMENTED / RUNTIME_GATED` | destination-ref lease + authenticated `lsRemote`; fixture GitHub privada real pendente |
| Interactive hunk staging | `IMPLEMENTED / RUNTIME_GATED` | index-only stage/unstage; Android smoke pendente |
| API GitHub | `PARTIAL_ADVANCED / RUNTIME_GATED` | falta matriz end-to-end completa |
| Git local via JGit | `IMPLEMENTED_ADVANCED / RUNTIME_GATED` | rede, credenciais e conflitos exigem regressão real |
| SSH auth | `PARTIAL / RUNTIME_GATED` | depende de chaves/servidor real |
| GPG | `ADAPTER_IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | exige `gpg` autorizado |
| Git LFS | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | exige `git-lfs` + repositório fixture |
| Worktree | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | falta matriz filesystem/device |
| Bisect | `IMPLEMENTED / TOKEN_VAZIO_RUNTIME` | falta cenário regressivo controlado |
| Terminal | `BOUNDED_EXECUTOR` | não é PTY/VT100 e mantém Git gravável fora da allowlist genérica |
| Multi-provider | `IMPLEMENTED` | GitLab/Bitbucket/Gitea-AForgejo/Azure DevOps ainda requerem credenciais/fixtures reais |
| Offline queue | `IMPLEMENTED` | produção/device recovery ainda requer evidence |
| LLaMA kernel JNI | `BRIDGE_IMPLEMENTED / BLOCKED_EXTERNAL` | `llama.h`/runtime externo não está pinado e provado |

## Autenticação — contrato canônico

### Entrada e persistência

- PAT é validado contra GitHub `/user` antes de criar sessão.
- Credencial persistida é protegida por Android Keystore/AES-GCM.
- `AuthTokenCache` mantém somente a cópia em memória usada pelo interceptor.
- `gh` import é opcional; PAT continua caminho primário quando `gh` não existe no processo Android.
- OAuth Device Flow exige somente o Client ID público configurado para o fluxo atual; nenhum client secret deve ser embutido no APK.

### Ciclo de vida

```text
request autenticado
  -> 2xx/3xx: sessão permanece
  -> 401: TokenRefreshManager tenta clearAuthState
          + AuthInterceptor zera AuthTokenCache incondicionalmente
          + usuário precisa reautenticar
  -> 403 + X-RateLimit-Remaining=0: RateLimited; sessão preservada
  -> outro 403: Forbidden; sessão preservada
```

O antigo `refreshOAuthToken(clientId, clientSecret, refreshToken)` era um stub sem caller de produção e foi removido. O app atual não recebe/modela refresh token no OAuth App Device Flow, então não reivindica refresh automático.

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

P33-05 está implementado para a representação lossless atual:

- tracked `MODIFY`;
- UTF-8 <= 2 MiB;
- newline final;
- stage/unstage de um hunk;
- working tree nunca é reescrito;
- hunk/HEAD/index são revalidados;
- `lockDirCache` + `DirCacheEditor.commit()`;
- unlock idempotente em `finally`;
- binary/non-UTF8/no-final-newline/unmerged falham fechado.

Esses limites são contrato de segurança, não `TOKEN_VAZIO`.

## Executor e fila

`TerminalEmulator` continua bounded executor: drena saída concorrente, rejeita entrada malformada e aceita apenas Git de leitura na superfície genérica. Operações graváveis devem passar por ações tipadas/governadas.

A fila offline tem armazenamento durável e workers, mas recovery/scheduling em device real permanecem evidência separada.

## Multi-provider

Fonte presente para GitHub, GitLab, Bitbucket, Gitea/Forgejo e Azure DevOps. A existência dos adapters não é substituto para regressões com credenciais e endpoints reais.

## Diretório `fazer/`

`fazer/` permanece histórico/não compilado e **não é fonte de verdade**. Implementações compiladas vivem em `app/src/`. Não contar funcionalidade duas vezes nem reintroduzir rascunhos por engano.

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
OAuth Client ID real flow      = CONFIG_REQUIRED / TOKEN_VAZIO_RUNTIME
GPG/LFS/worktree/bisect        = TOKEN_VAZIO_RUNTIME
claim_allowed                  = false
```

## Retroalimentar[4] — 2026-08-12

- **F_ok:** P33 33/33 em fonte; Privacy Manager, Readiness Gate, HTTPS privado, force-with-lease, hunk staging e token lifecycle possuem contratos fail-closed e regressões de fonte.
- **F_gap:** execução Gradle/APK/device não existe porque hosted runner está billing-blocked e o gate local ainda não foi executado no aparelho; fixtures externas permanecem separadas.
- **F_next:** continuar eliminando contradições de fonte; em paralelo, o fechamento de runtime é `readiness gate -> tests -> APK/SHA -> device smoke -> fixtures privadas`.
