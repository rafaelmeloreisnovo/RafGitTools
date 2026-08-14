# Documentação RafGitTools

## Fonte de verdade — ordem de leitura

1. [`RAFGITTOOLS_CURRENT_STATE.md`](RAFGITTOOLS_CURRENT_STATE.md) — estado operacional mutável mais recente.
2. [`RAFGITTOOLS_CODE_REALITY_MATRIX.md`](RAFGITTOOLS_CODE_REALITY_MATRIX.md) — capacidades reais em fonte, teste, build e runtime.
3. [`STATUS_REPORT.md`](STATUS_REPORT.md) — classificação técnica consolidada e gaps.
4. [`../ECOSYSTEM_RUNTIME_STATE.json`](../ECOSYSTEM_RUNTIME_STATE.json) — matriz de estado legível por máquina.
5. [`RAFGITTOOLS_ROADMAP_TRUE.md`](RAFGITTOOLS_ROADMAP_TRUE.md) — sequência operacional atual; roadmap histórico não substitui source truth.
6. [`canonical/2026-08-14/RAFGITTOOLS_CURRENT_REPO_MAP_V1.md`](canonical/2026-08-14/RAFGITTOOLS_CURRENT_REPO_MAP_V1.md) — mapa canônico atual da árvore relevante.
7. [`PENDING_33_ITEMS.md`](PENDING_33_ITEMS.md) — P33 `33/33 SOURCE_FUNCTIONAL`, separado de runtime.
8. [`FIRST_COMPILE_RUN_TRIANGLE.md`](FIRST_COMPILE_RUN_TRIANGLE.md) — contrato source → build → device.
9. [`canonical/2026-08-14/RAFGITTOOLS_SOURCE_BUILD_EVIDENCE_V1.md`](canonical/2026-08-14/RAFGITTOOLS_SOURCE_BUILD_EVIDENCE_V1.md) — checkpoint append-only do BUILD observado.
10. [`../data/evidence/github/rafgittools_android_build_31821491676.v1.json`](../data/evidence/github/rafgittools_android_build_31821491676.v1.json) — evidence JSON do run GitHub Actions.

### Invariante de leitura

```text
roadmap != source state != tests executed != build evidence != physical runtime != release
```

Quando houver conflito, prevalece a evidência ligada ao **commit exato**. Documentos históricos continuam preservados, mas não podem sobrepor o estado observável mais novo.

## Estado real e auditoria

1. RAFGITTOOLS_CURRENT_STATE.md
2. RAFGITTOOLS_CODE_REALITY_MATRIX.md
3. STATUS_REPORT.md
4. ECOSYSTEM_RUNTIME_STATE.json
5. RAFGITTOOLS_ROADMAP_TRUE.md
6. RAFGITTOOLS_CURRENT_REPO_MAP_V1.md
7. PENDING_33_ITEMS.md
8. FIRST_COMPILE_RUN_TRIANGLE.md
9. RAFGITTOOLS_UI_NAVIGATION_MAP.md
10. RAFGITTOOLS_GIT_OPERATIONS_MATRIX.md
11. RAFGITTOOLS_GITHUB_API_MATRIX.md
12. RAFGITTOOLS_SECURITY_AUTH_MAP.md
13. RAFGITTOOLS_TERMINAL_STRATEGY.md
14. RAFGITTOOLS_TERMUX_ARM32_STATUS.md
15. RAFGITTOOLS_TEST_PLAN.md
16. RAFGITTOOLS_RELEASE_CHECKLIST.md
17. RAFGITTOOLS_PIPELINE_DEADLINES.md
18. RAFGITTOOLS_MANUAL_USER.md
19. RAFGITTOOLS_MANUAL_DEVELOPER.md
20. TERMUX_AUTH.md
21. TERMUX_HEALTH_CONTRACT.md
22. RAFGITTOOLS_POSSIBILITIES_CATALOG.md
23. knowledge/README.md
24. knowledge/VECTRAS_VM_ANDROID_ARCHIVE.md

## BUILD anchor comprovado — 2026-08-14

```text
build-bound commit = bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa
Actions run        = 31821491676 PASS
unit tests         = PASS
Android lint       = PASS
assembleDevDebug   = PASS
APK SHA-256        = 115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6
armeabi-v7a        = PRESENT
arm64-v8a          = PRESENT
DEVICE             = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
claim_allowed      = false
release_allowed    = false
```

## Delta posterior ao BUILD anchor

A branch avançou depois de `bbdb556...` para:

- reconciliar documentação/dados;
- remover o alvo hardcoded `issues/236/comments` de `.github/workflows/ci.yml`;
- ligar o comentário de CI ao `github.event.pull_request.number`;
- adicionar `tests/test_workflow_pr_binding.py`;
- incluir esse teste no gate estrutural do `Android Client Build`.

Como workflows/testes mudaram, o BUILD anchor anterior continua válido **somente para `bbdb556...`**. O head posterior exige execução própria antes de receber `BUILD_VERIFIED` commit-bound.

> Documentos de roadmap/planejamento e estados anteriores permanecem no repositório como contexto/proveniência. Para decisões operacionais, começar sempre pela seção **Fonte de verdade — ordem de leitura**.
