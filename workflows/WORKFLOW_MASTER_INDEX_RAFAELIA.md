# WORKFLOW MASTER INDEX — RAFAELIA v2

## Estado

`structural-contract / temporal / owner-bound / runtime-evidence-gated / rollback-drilled / public-interop-mapped`

Este índice é a tábua de navegação do ecossistema. Ele não substitui o README, o código, o paper, o teste ou o ledger de cada repositório.

```text
expressão
→ definição
→ fonte canônica
→ implementação
→ teste
→ evidência
→ estado temporal
→ decisão
→ rollback
→ próxima ação
```

## Fontes normativas

- `configs/rafaelia-federation.json`: topologia e failover federado v1.
- `configs/workflow-master-index.json`: responsabilidade, tempo, runtime, supply chain e recuperação v2.
- `schemas/workflow-master-index.schema.json`: contrato estrutural.
- `scripts/federation/validate_master_index.py`: gate sem inferência remota.
- `scripts/federation/recovery_drill.py`: restauração local real e simulação limitada dos outros nós.
- `.github/actions-lock.json`: actions fixadas por commit SHA.
- `configs/epistemic-provenance-interop.json`: vocabulário e fronteiras de interoperabilidade v1.
- `schemas/epistemic-provenance-interop.schema.json`: schema público do perfil.
- `scripts/federation/epistemic_interop.py`: projeções determinísticas PROV, OpenLineage, SLSA, SPDX e NIST AI RMF.
- `docs/federation/RAFAELIA_EPISTEMIC_PROVENANCE_INTEROP_V1.md`: interpretação humana e limites.

## Condições estruturais implantadas

### 1. Índice mestre canônico

Cada nó liga sessão lógica, repositório, fonte canônica, responsável, dependências, estado, evidência, rollback e critério de fechamento.

### 2. Ontologia completa

O contrato reconhece explicitamente:

```text
REPOSITORY CLAIM TEST DATASET ARTIFACT METHOD SOFTWARE
DEVICE ABI ENVIRONMENT PARAMETER_SET
```

O perfil de interoperabilidade acrescenta, sem alterar a autoridade local:

```text
SESSION SOURCE IMPLEMENTATION EVIDENCE DECISION ROLLBACK
TOKEN_VAZIO CONTRADICTION FALSIFIER GAP AUTHORITY RUNTIME_CONTEXT
```

### 3. Tempo como dado

Cada nó possui:

```text
observed_at
valid_from
valid_until
superseded_at
event_sequence
```

`observed_at` não é tratado como o tempo em que o fato necessariamente ocorreu.

### 4. Ausência com responsável

`TOKEN_VAZIO`, `BLOCKED` ou `PARTIAL` exigem:

```text
owner
next_action
exit_criteria
```

A ausência deixa de ser um vazio solto e vira obrigação verificável.

### 5. Prova de runtime

Build, CI, documentação e hash não bastam. A evidência de runtime separa:

```text
artifact
device
ABI
environment
status
```

### 6. Supply chain

O workflow usa Actions por SHA completo, dependência Python por versão exata e produz SBOM estrutural. Assinatura/attestation externa continua `TOKEN_VAZIO` até configuração própria.

### 7. Recuperação

O drill executa uma restauração real de arquivo local:

```text
estado conhecido
→ falha injetada
→ mutação detectada
→ restauração
→ SHA-256 original recuperado
```

Para dispositivo, VM, modelo e repositórios remotos, o drill valida apenas contrato e safe-state. A recuperação real continua local a cada projeto.

### 8. Interoperabilidade pública sem promoção silenciosa

O perfil gera cinco projeções:

```text
PROV-JSON-shaped
OpenLineage RunEvent-shaped
in-toto/SLSA provenance-shaped
SPDX JSON-shaped
NIST AI RMF conceptual crosswalk
```

Essas projeções registram estrutura e limites. Elas não afirmam certificação ou conformidade formal:

```text
formal_conformance = TOKEN_VAZIO
signed_attestation = TOKEN_VAZIO
claim_allowed = false
```

## Regra central

```text
presente ≠ executado
executado ≠ correto
hash igual ≠ verdade científica
CI verde ≠ dispositivo real validado
failover ≠ promoção de claim
mapeamento estrutural ≠ conformidade com padrão
```

## Caminho humano mínimo

```bash
python3 scripts/federation/validate_master_index.py \
  --index configs/workflow-master-index.json

python3 scripts/federation/epistemic_interop.py \
  --profile configs/epistemic-provenance-interop.json \
  --index configs/workflow-master-index.json \
  --output-dir artifacts \
  --report artifacts/epistemic-interop-report.json

python3 scripts/federation/recovery_drill.py \
  --index configs/workflow-master-index.json \
  --node rafgittools
```

A saída deve dizer: um diagnóstico, um estado, uma evidência e uma próxima ação.

## Rollout

1. Validar o control plane.
2. Gerar e conferir as projeções públicas sem promover conformidade.
3. Produzir o primeiro runtime evidence do ChipQuantum.
4. Fechar a proveniência real do Termux.
5. Produzir boot smoke do Vectras.
6. Gerar receipt GAIA → RLL.
7. Executar CAMB/RECFAST por ponto no RLL.
8. Somente depois testar validadores oficiais e assinatura/attestation.

## Limite

Este índice prova estrutura, responsabilidade e mapeamento interno. Não prova automaticamente execução Android, boot de VM, inferência de modelo, conteúdo privado, verdade física, conformidade PROV/OpenLineage/SLSA/SPDX/NIST ou attestation assinada.
