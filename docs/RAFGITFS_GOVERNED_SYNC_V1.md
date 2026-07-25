# RafGitFS — Governed Sync V1

Status: `IMPLEMENTED_SOURCE / CLAIM_ALLOWED=false`

## Pipeline

```text
SCAN → DIFF → PLAN → DRY_RUN → APPROVE → EXECUTE → RECEIPT
```

## Regras

- todo plano recebe hash SHA-256 canônico;
- aprovação vale somente para `requestId + planHash` exatos;
- confirmação exigida: `APPROVE <12 primeiros caracteres do planHash>`;
- conflitos bloqueiam execução;
- jobs usam Room e transições condicionais;
- pausa, retomada, cancelamento e retry limitado são persistentes;
- recibos são append-only e incluem `F_ok`, `F_gap` e `F_next`;
- logs são limitados e sanitizam tokens, segredos e cabeçalhos de autorização;
- `claimAllowed=false` permanece invariável.

## Capacidades Prompt 6

Executáveis:

```text
NO_OP
CACHE_DOWNLOAD
PIN_OFFLINE
```

Planejáveis, porém bloqueadas até o Prompt 7:

```text
CREATE_WORKSPACE
WRITE_WORKSPACE_FILE
CREATE_BRANCH
CREATE_COMMIT
PUSH_BRANCH
OPEN_PULL_REQUEST
```

Permanentemente bloqueada nesta arquitetura:

```text
DELETE_REMOTE
```

## Conflitos

`BOTH_CHANGED` e evidência incompleta produzem registro em `sync_conflicts`. Nenhum conflito é resolvido silenciosamente.

## Recibos

O recibo contém hash da requisição, hash do recibo, estado de evidência, resultado, alvo, SHA observado e vetores `F_ok/F_gap/F_next`.

## Gates

```bash
python3 scripts/validate_rafgitfs_governed_sync.py
python3 -m unittest tests/test_validate_rafgitfs_governed_sync.py -v
./gradlew --no-daemon :app:testDevDebugUnitTest --tests '*RafGitFsGovernedSyncTest*'
./gradlew --no-daemon :app:compileDevDebugKotlin
```

## Limites

```yaml
remote_write_capability: BLOCKED_PROMPT_6
protected_branch_write: false
destructive_remote: false
android_execution: TOKEN_VAZIO
remote_ci_pass: TOKEN_VAZIO
claim_allowed: false
```
