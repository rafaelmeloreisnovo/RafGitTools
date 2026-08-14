# Documentação RafGitTools

## Fonte de verdade — ordem de leitura

1. [`RAFGITTOOLS_CURRENT_STATE.md`](RAFGITTOOLS_CURRENT_STATE.md) — estado operacional mutável mais recente.
2. [`STATUS_REPORT.md`](STATUS_REPORT.md) — classificação técnica consolidada e gaps.
3. [`../ECOSYSTEM_RUNTIME_STATE.json`](../ECOSYSTEM_RUNTIME_STATE.json) — matriz de estado legível por máquina.
4. [`PENDING_33_ITEMS.md`](PENDING_33_ITEMS.md) — P33 `33/33 SOURCE_FUNCTIONAL`, separado de runtime.
5. [`FIRST_COMPILE_RUN_TRIANGLE.md`](FIRST_COMPILE_RUN_TRIANGLE.md) — contrato source → build → device.
6. [`canonical/2026-08-14/RAFGITTOOLS_SOURCE_BUILD_EVIDENCE_V1.md`](canonical/2026-08-14/RAFGITTOOLS_SOURCE_BUILD_EVIDENCE_V1.md) — checkpoint append-only do BUILD observado.
7. [`../data/evidence/github/rafgittools_android_build_31821491676.v1.json`](../data/evidence/github/rafgittools_android_build_31821491676.v1.json) — evidence JSON do run GitHub Actions.

### Invariante de leitura

```text
roadmap != source state != tests executed != build evidence != physical runtime != release
```

Quando houver conflito, prevalece a evidência ligada ao **commit exato**. Documentos históricos continuam preservados, mas não podem sobrepor o estado observável mais novo.

## Estado real e auditoria

1. RAFGITTOOLS_CURRENT_STATE.md
2. STATUS_REPORT.md
3. PENDING_33_ITEMS.md
4. FIRST_COMPILE_RUN_TRIANGLE.md
5. RAFGITTOOLS_CODE_REALITY_MATRIX.md
6. RAFGITTOOLS_UI_NAVIGATION_MAP.md
7. RAFGITTOOLS_GIT_OPERATIONS_MATRIX.md
8. RAFGITTOOLS_GITHUB_API_MATRIX.md
9. RAFGITTOOLS_SECURITY_AUTH_MAP.md
10. RAFGITTOOLS_TERMINAL_STRATEGY.md
11. RAFGITTOOLS_TERMUX_ARM32_STATUS.md
12. RAFGITTOOLS_ROADMAP_TRUE.md
13. RAFGITTOOLS_TEST_PLAN.md
14. RAFGITTOOLS_RELEASE_CHECKLIST.md
15. RAFGITTOOLS_PIPELINE_DEADLINES.md
16. RAFGITTOOLS_MANUAL_USER.md
17. RAFGITTOOLS_MANUAL_DEVELOPER.md
18. TERMUX_AUTH.md
19. TERMUX_HEALTH_CONTRACT.md
20. RAFGITTOOLS_POSSIBILITIES_CATALOG.md
21. knowledge/README.md
22. knowledge/VECTRAS_VM_ANDROID_ARCHIVE.md

## Checkpoint atual — 2026-08-14

```text
candidate commit = bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa
Actions run       = 31821491676 PASS
unit tests        = PASS
Android lint      = PASS
assembleDevDebug  = PASS
APK SHA-256       = 115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6
armeabi-v7a       = PRESENT
arm64-v8a         = PRESENT
DEVICE            = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
claim_allowed     = false
release_allowed   = false
```

> Documentos de roadmap/planejamento e estados anteriores permanecem no repositório como contexto/proveniência. Para decisões operacionais, começar sempre pela seção **Fonte de verdade — ordem de leitura**.
