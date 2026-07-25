# RafGitFS — Governed Git Write V1

Status: `IMPLEMENTED_SOURCE / CLAIM_ALLOWED=false`

## Fluxo autorizado

```text
workspace privado
→ arquivos staged
→ SHA-base por arquivo
→ comparação de três vias
→ plano canônico
→ DRY_RUN
→ aprovação exata
→ branch rafgitfs/*
→ blobs + tree + commit atômico
→ push sem force
→ pull request DRAFT
→ recibo append-only
```

## Política de branch

A escrita direta em `main`, `master`, `develop`, `development`, `production` e `release` permanece bloqueada. Branches produzidas pela capacidade seguem:

```text
rafgitfs/<base>-<request-id>
```

A API dedicada não contém endpoint de DELETE, merge ou force-push.

## Workspace

- armazenamento privado em `context.filesDir/rafgitfs-workspaces-v1`;
- arquivos temporários com `fsync` e rename;
- caminhos `.git`, `..` e escape da raiz são bloqueados;
- limite individual de 10 MiB nesta V1;
- SHA-256 do payload é conferido novamente antes de publicar;
- `claimAllowed=false` em todo estado.

## Comparação de três vias

Cada arquivo pode carregar:

```text
baseSha
remoteSha
localSha
```

Classificações:

```text
EQUAL
LOCAL_CHANGED
REMOTE_CHANGED
BOTH_CHANGED
LOCAL_ONLY
REMOTE_ONLY
UNKNOWN
```

`BOTH_CHANGED` e `UNKNOWN` bloqueiam publicação até resolução explícita.

## Sequência remota única

Independentemente da quantidade de arquivos, o plano remoto contém exatamente:

```text
1 CREATE_BRANCH
2 CREATE_COMMIT
3 PUSH_BRANCH
4 OPEN_PULL_REQUEST
```

Os arquivos são enviados como blobs e reunidos em uma única tree e um único commit.

## Idempotência

O progresso é preservado em `staged_operations`:

```text
BRANCH_CREATED
COMMIT_READY
PUSHED
PR_OPEN:<number>
ROLLED_BACK
```

Uma repetição segura reutiliza o marco persistido em vez de criar múltiplas PRs.

## Aprovação

A aprovação deve coincidir com:

```text
APPROVE <12 primeiros caracteres do planHash>
```

O `workspaceId` faz parte do hash. Alterar workspace, passos, SHA-base, conteúdo observado ou ação invalida a aprovação.

## Rollback

Rollback não apaga branch nem reescreve histórico. Ele cria um novo commit cuja árvore volta à árvore-base:

```text
ROLLBACK <12 primeiros caracteres do planHash>
```

O push do rollback continua com `force=false`.

## Interface

A tela `WorkspaceEditorScreen` permite:

- criar workspace privado;
- informar caminho, conteúdo e SHA-base;
- stage local e undo local;
- gerar plano e dry-run;
- visualizar conflitos e quatro passos;
- aprovar exatamente;
- abrir PR em draft;
- criar rollback governado.

## Gates

```bash
python3 scripts/validate_rafgitfs_git_write.py
python3 -m unittest tests/test_validate_rafgitfs_git_write.py -v
./gradlew --no-daemon :app:testDevDebugUnitTest --tests '*RafGitFsWriteContractsTest*'
./gradlew --no-daemon :app:compileDevDebugKotlin
```

## Limites honestos

```yaml
direct_main_write: false
force_push: false
remote_delete: false
auto_merge: false
pull_request_default: DRAFT
production_signing: TOKEN_VAZIO
android_device_execution: TOKEN_VAZIO
remote_ci_pass: TOKEN_VAZIO
claim_allowed: false
```
