# WORKFLOW MASTER INDEX — RAFAELIA v2

## Estado

`structural-contract / temporal / owner-bound / runtime-evidence-gated / rollback-drilled`

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

## Condições estruturais implantadas

### 1. Índice mestre canônico

Cada nó liga sessão lógica, repositório, fonte canônica, responsável, dependências, estado, evidência, rollback e critério de fechamento.

### 2. Ontologia completa

O contrato reconhece explicitamente:

```text
REPOSITORY CLAIM TEST DATASET ARTIFACT METHOD SOFTWARE
DEVICE ABI ENVIRONMENT PARAMETER_SET
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

## Regra central

```text
presente ≠ executado
executado ≠ correto
hash igual ≠ verdade científica
CI verde ≠ dispositivo real validado
failover ≠ promoção de claim
```

## Caminho humano mínimo

```bash
python3 scripts/federation/validate_master_index.py \
  --index configs/workflow-master-index.json

python3 scripts/federation/recovery_drill.py \
  --index configs/workflow-master-index.json \
  --node rafgittools
```

A saída deve dizer: um diagnóstico, um estado, uma evidência e uma próxima ação.

## Rollout

1. Validar o control plane.
2. Produzir o primeiro runtime evidence do ChipQuantum.
3. Fechar a proveniência real do Termux.
4. Produzir boot smoke do Vectras.
5. Gerar receipt GAIA → RLL.
6. Executar CAMB/RECFAST por ponto no RLL.

## Limite

Este índice prova estrutura e responsabilidade. Não prova automaticamente execução Android, boot de VM, inferência de modelo, conteúdo privado ou verdade física.
