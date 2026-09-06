# Documentação RafGitTools

## Fonte de verdade — ordem de leitura

1. [`RAFGITTOOLS_CURRENT_STATE.md`](RAFGITTOOLS_CURRENT_STATE.md) — estado editorial corrente, observado contra `main@56f4ce95158e6b8a1dbfa4fd8c029937aea20224`.
2. [`STATUS_REPORT.md`](STATUS_REPORT.md) — classificação técnica/evidencial corrente.
3. [`RAFGITTOOLS_ROADMAP_TRUE.md`](RAFGITTOOLS_ROADMAP_TRUE.md) — sequência operacional atual.
4. [`CODE_TO_DOC_MAP.md`](CODE_TO_DOC_MAP.md) — roteamento semântico código → documentação.
5. [`URGENCY_GATE_GAP_20260906.md`](URGENCY_GATE_GAP_20260906.md) — snapshot append-only de urgências/gates/gaps do seu source revision.
6. [`RAFGITTOOLS_CODE_REALITY_MATRIX.md`](RAFGITTOOLS_CODE_REALITY_MATRIX.md) — matriz de capacidades; interpretar sempre pelo revision/evidence boundary indicado no próprio documento.
7. [`FIRST_COMPILE_RUN_TRIANGLE.md`](FIRST_COMPILE_RUN_TRIANGLE.md) — contrato source → build → device.
8. [`PENDING_33_ITEMS.md`](PENDING_33_ITEMS.md) — backlog histórico/source-functional; não equivale a runtime.
9. [`canonical/2026-08-14/RAFGITTOOLS_SOURCE_BUILD_EVIDENCE_V1.md`](canonical/2026-08-14/RAFGITTOOLS_SOURCE_BUILD_EVIDENCE_V1.md) — checkpoint BUILD append-only de 2026-08-14.
10. [`DOCUMENTATION_AUDIT_RECEIPT_2026-09-06.md`](DOCUMENTATION_AUDIT_RECEIPT_2026-09-06.md) — proveniência desta reconciliação docs-only.

### Invariante

```text
source != test != build != runtime != device != release
TOKEN_VAZIO != FAIL != PASS
historical receipt != current-head receipt
structured file != current truth when its observed revision is stale
```

Quando houver conflito, prevalece a evidência ligada ao **commit/artefato exato**.

## Estado de `ECOSYSTEM_RUNTIME_STATE.json`

O arquivo raiz `ECOSYSTEM_RUNTIME_STATE.json` foi observado com `observed_at=2026-08-14` e ainda contém a narrativa PR #346/#347. Como esta revisão é estritamente documental, o JSON não foi alterado.

Até regeneração pelo mecanismo apropriado:

```text
ECOSYSTEM_RUNTIME_STATE.json = HISTORICAL_MACHINE_STATE
regeneration on current main = TOKEN_VAZIO_REGEN_REQUIRED
```

Ele não deve ultrapassar `RAFGITTOOLS_CURRENT_STATE.md` ou provider metadata atual na ordem de decisão.

## Núcleos de estado e auditoria

- `RAFGITTOOLS_CURRENT_STATE.md`
- `STATUS_REPORT.md`
- `RAFGITTOOLS_ROADMAP_TRUE.md`
- `CODE_TO_DOC_MAP.md`
- `URGENCY_GATE_GAP_20260906.md`
- `RAFGITTOOLS_CODE_REALITY_MATRIX.md`
- `FIRST_COMPILE_RUN_TRIANGLE.md`
- `PENDING_33_ITEMS.md`
- `RAFGITTOOLS_TEST_PLAN.md`
- `RAFGITTOOLS_RELEASE_CHECKLIST.md`
- `RAFGITTOOLS_SECURITY_AUTH_MAP.md`
- `RAFGITTOOLS_GIT_OPERATIONS_MATRIX.md`
- `RAFGITTOOLS_GITHUB_API_MATRIX.md`
- `RAFGITTOOLS_UI_NAVIGATION_MAP.md`
- `RAFGITTOOLS_TERMINAL_STRATEGY.md`
- `RAFGITTOOLS_TERMUX_ARM32_STATUS.md`
- `RAFGITTOOLS_MANUAL_USER.md`
- `RAFGITTOOLS_MANUAL_DEVELOPER.md`
- `TERMUX_AUTH.md`
- `TERMUX_HEALTH_CONTRACT.md`
- `knowledge/README.md`
- `knowledge/VECTRAS_VM_ANDROID_ARCHIVE.md`

## Novas superfícies que a documentação corrente deve reconhecer

A revisão de 2026-09-06 confirmou em fonte rotas de Repository Governance:

- `RepositoryGovernanceApiService.kt`;
- `RepositoryGovernanceAudit.kt`;
- `RepositoryGovernanceReceiptStore.kt`;
- `RepositoryGovernanceMutationPlan.kt`;
- `RepositoryGovernanceViewModel.kt`;
- `RepositoryGovernanceScreen.kt`;
- testes correspondentes.

Também confirmou o validador fail-closed `scripts/rafaelia_receipt_validator.py` para receipts FNEXT. Essas superfícies estão agora roteadas em `CODE_TO_DOC_MAP.md` e no estado/roadmap atual.

## Checkpoint histórico BUILD — 2026-08-14

```text
commit              = bbdb556a59c06a23cc2f6df6ba0ae7c98466a4fa
Actions run         = 31821491676 PASS
APK SHA-256         = 115b9cb1e71f53f16b2648924a09549b8e5e0b9e453280cab2e7f183a411ebf6
armeabi-v7a         = PRESENT
arm64-v8a           = PRESENT
physical DEVICE     = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
```

Esse bloco é histórico e commit-bound.

## Política editorial

- documentos em `docs/canonical/<date>/` e receipts históricos são preservados como checkpoints;
- documentos mutáveis de estado devem refletir o source/evidence cut corrente;
- métricas antigas de 288 features são baseline de planejamento, não percentual runtime corrente;
- contagens exatas correntes não auditadas ficam `TOKEN_VAZIO_RECOUNT_REQUIRED`;
- arquivo gerado/machine-readable stale não é corrigido manualmente por prosa; requer regeneração própria;
- nenhuma revisão documental autoriza claim, release ou provider mutation.

## R3

- **F_ok:** índice atual separa estado vivo, snapshots e receipts históricos e roteia as superfícies novas.
- **F_gap:** machine-state regeneration e contagens integrais revision-bound permanecem abertas.
- **F_next:** manter a ordem de leitura sincronizada a cada mudança material e preservar `TOKEN_VAZIO` onde a prova não existe.