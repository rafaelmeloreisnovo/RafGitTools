# RAFGITTOOLS_CURRENT_STATE

- Status: **ATIVO — source-functional avançado / BUILD anchor comprovado / PR #347 em gate CI**
- Estado observado: **2026-08-14**
- Branch operacional: `hardening/first-compile-run-triangle-20260814`
- PR atual: **#347 — draft, mergeable=true na última observação**
- BUILD anchor comprovado: `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`
- `claim_allowed=false`
- `release_allowed=false`

## Regra canônica

```text
SOURCE != TESTED != BUILD_VERIFIED != DEVICE_VERIFIED != RELEASE
```

### Regra de auto-referência

Este documento mutável **não congela o SHA do próprio head da branch**. Qualquer atualização deste arquivo cria um novo commit e tornaria esse SHA imediatamente obsoleto.

Para o head corrente do PR #347, a fonte de verdade é a metadata do próprio PR/GitHub Actions. SHA exato só é congelado aqui quando funciona como **âncora histórica/evidence checkpoint**, como o BUILD `bbdb556...`.

## Genealogia recente

1. PR #345 integrou o contrato `source -> build -> device`.
2. PR #346 integrou correções pós-merge, reconciliação documental e o primeiro BUILD atual comprovado.
3. PR #346 foi mesclado em `f3396cdea5fbf43ac302de19e7d1e9064ecfa122`.
4. Deltas produzidos depois do merge foram separados no PR sucessor **#347**.
5. A branch do #347 foi reconciliada com `main` sem force-push por merge commit não destrutivo.
6. O #347 permanece draft e deve ser promovido somente após os gates do seu **head exato no momento da execução**.

## BUILD anchor comprovado

Commit:

`bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`

Workflow `Android Client Build` run `31821491676`, job `94835531838`: **SUCCESS**.

PASS observado:

- custody/structural tests;
- authentication unit tests;
- full dev unit tests;
- Android lint;
- `assembleDevDebug`;
- APK verification;
- build receipt;
- artifact upload.

Artifact:

- `RafGitTools-devDebug` id `9227343409`;
- APK `app-dev-debug.apk`;
- size `24,672,130` bytes;
- SHA-256 `115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6`;
- ZIP CRC PASS;
- `armeabi-v7a` PRESENT;
- `arm64-v8a` PRESENT;
- build receipt SHA-256 `f124ac18a9f1e158aa764a12b49a25dbf54cc870cca8359e0355416bee5219a5`.

Esse BUILD continua válido **somente como evidência commit-bound a `bbdb556...`**.

## Delta sucessor — PR #347

O #347 contém:

- remoção do alvo histórico hardcoded `issues/236/comments` de `.github/workflows/ci.yml`;
- comentário de CI ligado a `github.event.pull_request.number`;
- validação explícita de `PR_NUMBER`;
- permissão `pull-requests: write` explícita;
- `tests/test_workflow_pr_binding.py` para rejeitar regressão a destino numérico hardcoded;
- inclusão desse teste no gate estrutural do `Android Client Build`;
- `RAFGITTOOLS_ROADMAP_TRUE.md` atualizado;
- `RAFGITTOOLS_CURRENT_REPO_MAP_V1.md` append-only;
- `docs/INDEX.md` reorganizado;
- `ECOSYSTEM_RUNTIME_STATE.json` separado entre BUILD anchor e fronteira atual.

Como workflow/teste mudaram depois do BUILD anchor, o estado do **head corrente do #347** deve ser lido assim até existir execução conclusiva:

```text
SOURCE          = PRESENT
TESTED          = consultar workflow do head exato
BUILD_VERIFIED  = somente se houver APK + receipt do head exato
DEVICE_VERIFIED = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
RELEASE         = BLOCKED
```

## Núcleo de fonte atual

| Componente | Estado de fonte | Limite de evidência |
|---|---|---|
| Android / Compose / Hilt / Room | `IMPLEMENTED_ADVANCED` | build do candidato final deve ser commit-bound |
| P33 | `33/33 SOURCE_FUNCTIONAL` | não equivale a 33/33 runtime |
| Git/JGit | `IMPLEMENTED_ADVANCED` | remotes/conflitos reais continuam granulares |
| Interactive staging | `IMPLEMENTED` | unit regression comprovada no anchor; device pendente |
| PAT / auth lifecycle | `IMPLEMENTED / CAPABILITY_AWARE` | real device/Auth fixtures pendentes |
| OAuth Device Flow | `IMPLEMENTED / CONFIG_REQUIRED` | Client ID/fixture real pendentes |
| SSH | `PARTIAL_RUNTIME` | chave/servidor real pendentes |
| GitHub API | `PARTIAL_ADVANCED` | matriz E2E real pendente |
| Multi-provider | `IMPLEMENTED / FIXTURE_GATED` | credenciais/endpoints reais pendentes |
| Offline queue/storage/workers | `IMPLEMENTED / DEVICE_GATED` | restart/recovery físico pendente |
| Terminal | `IMPLEMENTED_BOUNDED_EXECUTOR` | PTY/VT100 = `TOKEN_VAZIO_PTY` |
| LFS/worktree/bisect/GPG | `IMPLEMENTED_OR_ADAPTER / RUNTIME_GATED` | fixtures externas pendentes |
| RAFAELIA JNI/native | `BRIDGE_IMPLEMENTED` | BUILD anchor dual-ABI; physical invocation pendente |
| Local LLM/Kiwi bridge | `SOURCE_PRESENT / RUNTIME_GATED` | modelo/Kiwi real pendentes |

## TOKEN_VAZIO preservado

```text
final-candidate BUILD receipt        = TOKEN_VAZIO até PASS commit-bound
physical-device install/start        = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
physical runtime receipt             = TOKEN_VAZIO
private Git remote fixtures          = TOKEN_VAZIO_RUNTIME
PAT/OAuth real Android regression    = TOKEN_VAZIO_RUNTIME
GitHub App refresh real              = CONFIG_REQUIRED / TOKEN_VAZIO_RUNTIME
SSH provider matrix                  = TOKEN_VAZIO_RUNTIME
GPG/LFS/worktree/bisect fixtures     = TOKEN_VAZIO_RUNTIME
PTY/VT100                             = TOKEN_VAZIO_PTY
release acceptance/signing           = TOKEN_VAZIO_RELEASE
claim_allowed                         = false
release_allowed                       = false
```

## Próxima ordem operacional

```text
congelar branch do #347
  -> CI do head exato PASS
  -> APK + SHA + BUILD_RECEIPT desse mesmo head
  -> não modificar a branch candidata depois do PASS
  -> physical ARM install + launch com esses bytes
  -> runtime receipt commit+APK-bound
  -> triangle_closure PASS
  -> Git/Auth/Offline fixtures reais
  -> providers/external runtimes
  -> release gate
```

Não usar o APK do BUILD anchor `bbdb556...` como receipt de outro commit.

## Fonte de verdade ordenada

1. metadata do PR/commit exato no GitHub;
2. `app/src/` e build configuration;
3. `app/src/test/` + `tests/`;
4. workflows/gates executados;
5. APK + SHA-256 + build receipt do commit exato;
6. device receipt do mesmo APK;
7. `ECOSYSTEM_RUNTIME_STATE.json`;
8. `RAFGITTOOLS_CODE_REALITY_MATRIX.md`;
9. este documento;
10. `RAFGITTOOLS_ROADMAP_TRUE.md`;
11. roadmaps/README históricos.

## Retroalimentar[3]

- **F_ok:** #346 integrado; BUILD anchor preservado; #347 isola o hardening pós-merge e remove acoplamento ao PR 236.
- **F_gap:** o candidato final precisa de BUILD próprio; DEVICE continua físico e commit-bound.
- **F_next:** congelar a branch, aceitar somente o CI do head exato e então executar o artifact resultante no aparelho físico.
