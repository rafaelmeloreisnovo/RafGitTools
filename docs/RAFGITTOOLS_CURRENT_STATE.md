# RAFGITTOOLS_CURRENT_STATE

- Status: **ATIVO — source-functional avançado / BUILD anchor comprovado / current head em novo gate CI**
- Estado observado: **2026-08-14**
- Branch: `hardening/first-compile-run-triangle-20260814`
- PR atual: **#347 — draft, mergeable=true na observação**
- Head atual: `654ef8bb921ce47db7c415b4a726d6082a2ef9ea`
- Base `main`: `f3396cdea5fbf43ac302de19e7d1e9064ecfa122`
- BUILD anchor anterior: `bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa`
- `claim_allowed=false`
- `release_allowed=false`

## Regra canônica

```text
SOURCE != TESTED != BUILD_VERIFIED != DEVICE_VERIFIED != RELEASE
```

O estado atual deve ser lido por commit. Um BUILD PASS de um commit não é automaticamente herdado por outro commit, mesmo quando a árvore de aplicação é equivalente.

## Genealogia recente

1. PR #345 integrou o contrato source → build → device.
2. PR #346 integrou correções pós-merge, reconciliação documental e o primeiro BUILD atual comprovado.
3. PR #346 foi mesclado em `f3396cdea5fbf43ac302de19e7d1e9064ecfa122`.
4. Deltas produzidos depois do merge foram separados no PR sucessor **#347**.
5. A branch do #347 foi reconciliada com `main` sem force-push pelo merge commit `654ef8bb921ce47db7c415b4a726d6082a2ef9ea`; o PR passou a `mergeable=true`.

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

Esse BUILD continua válido como evidência **commit-bound a `bbdb556...`**.

## Current head — #347

O head `654ef8bb...` contém, além da genealogia já integrada, o seguinte delta sucessor:

- `.github/workflows/ci.yml`: remove alvo hardcoded `issues/236/comments`;
- comentário de CI ligado a `github.event.pull_request.number`;
- validação explícita de `PR_NUMBER`;
- permissão `pull-requests: write` explícita;
- `tests/test_workflow_pr_binding.py`: regressão contra alvo numérico hardcoded;
- `android-client-build.yml`: novo teste incluído no gate estrutural;
- `RAFGITTOOLS_ROADMAP_TRUE.md`: atualizado para a fronteira real;
- `RAFGITTOOLS_CURRENT_REPO_MAP_V1.md`: mapa canônico append-only;
- `docs/INDEX.md`: ordem de leitura operacional atualizada.

Como **workflow/teste** mudaram depois do BUILD anchor, o head `654ef8bb...` permanece:

```text
SOURCE          = PRESENT / REVIEWED_DELTA
TESTED          = CI_QUEUED_OR_IN_PROGRESS
BUILD_VERIFIED  = TOKEN_VAZIO_FOR_EXACT_HEAD
DEVICE_VERIFIED = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
RELEASE         = BLOCKED
```

Nenhum PASS é inferido enquanto os workflows do head não concluírem.

## Núcleo de fonte atual

| Componente | Estado de fonte | Limite de evidência |
|---|---|---|
| Android / Compose / Hilt / Room | `IMPLEMENTED_ADVANCED` | current-head build em gate |
| P33 | `33/33 SOURCE_FUNCTIONAL` | não equivale a 33/33 runtime |
| Git/JGit | `IMPLEMENTED_ADVANCED` | remotes/conflitos reais ainda granulares |
| Interactive staging | `IMPLEMENTED` | unit regression já comprovada no anchor; current-head gate em curso |
| PAT / auth lifecycle | `IMPLEMENTED / CAPABILITY_AWARE` | real device/Auth fixtures pendentes |
| OAuth Device Flow | `IMPLEMENTED / CONFIG_REQUIRED` | Client ID/fixture real pendentes |
| SSH | `PARTIAL_RUNTIME` | chave/servidor real pendentes |
| GitHub API | `PARTIAL_ADVANCED` | matriz E2E real pendente |
| Multi-provider | `IMPLEMENTED / FIXTURE_GATED` | GitLab/Bitbucket/Gitea-Forgejo/Azure DevOps reais pendentes |
| Offline queue/storage/workers | `IMPLEMENTED / DEVICE_GATED` | restart/recovery físico pendente |
| Terminal | `IMPLEMENTED_BOUNDED_EXECUTOR` | PTY/VT100 = `TOKEN_VAZIO_PTY` |
| LFS/worktree/bisect/GPG | `IMPLEMENTED_OR_ADAPTER / RUNTIME_GATED` | fixtures externas pendentes |
| RAFAELIA JNI/native | `BRIDGE_IMPLEMENTED` | BUILD anchor dual-ABI; physical invocation pendente |
| Local LLM/Kiwi bridge | `SOURCE_PRESENT / RUNTIME_GATED` | modelo/Kiwi real pendentes |

## TOKEN_VAZIO atual

```text
current-head BUILD receipt           = TOKEN_VAZIO_UNTIL_CI_PASS
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
#347 head CI PASS
  -> current-head APK + SHA + BUILD_RECEIPT
  -> selecionar exatamente esse artifact
  -> physical ARM install + launch
  -> runtime receipt commit+APK-bound
  -> triangle_closure PASS
  -> Git/Auth/Offline fixtures reais
  -> providers/external runtimes
  -> release gate
```

Não usar o APK do BUILD anchor `bbdb556...` como receipt do head `654ef8bb...`.

## Fonte de verdade ordenada

1. commit/branch/PR exatos;
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

- **F_ok:** #346 integrado; build anchor real preservado; #347 separa corretamente o delta pós-merge e corrigiu o acoplamento ao PR 236.
- **F_gap:** current-head CI ainda não concluiu; DEVICE permanece físico e commit-bound.
- **F_next:** promover `BUILD_VERIFIED` somente quando o head do #347 produzir seu próprio APK/receipt PASS; então executar o aparelho físico com esses bytes.
