# RAFAELIA Epistemic Provenance Interop v1

**Estado:** `STRUCTURAL_MAPPING + TESTED_LOCAL_PENDING`  
**Claim gate:** `claim_allowed=false`  
**Objetivo:** tornar o vocabulário RAFAELIA exportável para formatos públicos sem fingir conformidade, assinatura, runtime remoto ou validação científica.

## 1. Invariante

```text
expressão
→ claim atômico
→ autoridade local
→ implementação
→ teste
→ evidência
→ decisão
→ rollback
→ memória longitudinal
```

A interoperabilidade preserva quatro eixos independentes:

```text
source_status
!= epistemic_status
!= operational_status
!= claim_gate
```

Uma fonte pode estar identificada, a interpretação continuar hipotética, o runtime permanecer bloqueado e o claim continuar não autorizado.

## 2. O que foi criado

```text
configs/epistemic-provenance-interop.json
schemas/epistemic-provenance-interop.schema.json
scripts/federation/epistemic_interop.py
tests/federation/test_epistemic_interop.py
```

O perfil contém quinze termos canônicos:

```text
SESSION
CLAIM
SOURCE
IMPLEMENTATION
TEST
EVIDENCE
DECISION
ROLLBACK
TOKEN_VAZIO
CONTRADICTION
FALSIFIER
GAP
AUTHORITY
ARTIFACT
RUNTIME_CONTEXT
```

## 3. Projeções geradas

O comando:

```bash
python3 scripts/federation/epistemic_interop.py \
  --profile configs/epistemic-provenance-interop.json \
  --index configs/workflow-master-index.json \
  --output-dir artifacts \
  --report artifacts/epistemic-interop-report.json
```

produz:

```text
artifacts/epistemic-provenance.prov.json
artifacts/epistemic-lineage.openlineage.json
artifacts/epistemic-provenance.slsa.json
artifacts/epistemic-inventory.spdx.json
artifacts/epistemic-risk-crosswalk.nist-ai-rmf.json
artifacts/epistemic-interop-report.json
```

### W3C PROV

Repositórios e artefatos são projetados como entidades; relações federadas tornam-se atividades; RafGitTools aparece como agente de projeção. A estrutura usa relações de uso, derivação, associação e atribuição.

```text
PROV-shaped projection = STRUCTURAL_MAPPING
formal PROV conformance = TOKEN_VAZIO
```

### OpenLineage

A malha é projetada como um `RunEvent` determinístico com inputs, outputs e facets customizadas contendo os quatro eixos de estado.

```text
OpenLineage-shaped projection = STRUCTURAL_MAPPING
formal OpenLineage conformance = TOKEN_VAZIO
```

### SLSA / in-toto

O índice mestre torna-se sujeito de uma declaração in-toto com predicado em forma de SLSA Provenance. Dependências, builder e limites são serializados, mas a declaração não é assinada nem promovida a attestation.

```text
signed = false
verified_builder = TOKEN_VAZIO
formal_slsa_conformance = TOKEN_VAZIO
attestation = TOKEN_VAZIO
```

Um hash confirma identidade de bytes. Não confirma autoria, builder, cadeia de custódia ou verdade científica.

### SPDX

Cada repositório vira um pacote documental; dependências e contratos cross-repo viram relações `DEPENDS_ON`. `filesAnalyzed=false` permanece explícito.

```text
inventory projection = STRUCTURAL_MAPPING
legal conclusion = TOKEN_VAZIO
formal SPDX conformance = TOKEN_VAZIO
```

### NIST AI RMF

O vocabulário é cruzado com:

```text
GOVERN
MAP
MEASURE
MANAGE
```

Esse cruzamento é conceitual. Não representa certificação, compliance ou aceitação de risco.

## 4. TOKEN_VAZIO

```text
TOKEN_VAZIO != 0
TOKEN_VAZIO != null
TOKEN_VAZIO != false
TOKEN_VAZIO != PASS
TOKEN_VAZIO != FAIL
```

Ele representa uma posição conhecida cuja evidência ainda não foi obtida. Uma entrada vazia válida deve permanecer navegável e possuir, no contrato de origem, responsável, próxima ação e critério de saída.

## 5. Relações semânticas

O perfil impede atalhos como:

```text
código CLOSES gap
```

A cadeia correta é:

```text
CODE IMPLEMENTS EXPERIMENT
EXPERIMENT PRODUCES RESULT
RESULT INFORMS DECISION
DECISION CLOSES_OR_PRESERVES GAP
```

Também existe a relação de fronteira:

```text
PARABLE_OR_SYMBOL NOT_EVIDENCE_FOR PHYSICAL_OR_SCIENTIFIC_CLAIM
```

A metáfora pode gerar hipótese. Não pode promover sozinha um resultado físico, jurídico, biológico, cosmológico ou de runtime.

## 6. Determinismo

Com perfil e índice idênticos:

```text
SHA256(output_run_1) == SHA256(output_run_2)
```

O identificador do run OpenLineage e os namespaces SPDX são derivados deterministicamente do conteúdo canônico. Nenhum timestamp atual é criado durante a execução; o tempo vem do perfil versionado.

## 7. O que o PASS prova

Um relatório `PASS` prova somente:

- JSON parseável;
- vocabulário obrigatório completo e sem duplicação;
- mapeamento explícito para cinco famílias públicas;
- quatro eixos preservados;
- `TOKEN_VAZIO` não promovido;
- attestation SLSA não inventada;
- projeções determinísticas;
- relações do índice mestre com nós conhecidos.

Não prova:

- conformidade formal com os padrões externos;
- assinatura ou identidade autenticada do builder;
- execução de repositórios remotos;
- validade científica;
- novidade mundial;
- permissão para promover claims;
- certificação NIST, SLSA, SPDX, PROV ou OpenLineage.

## 8. Próxima promoção permitida

A evolução segura é:

```text
estrutura interna validada
→ testes locais
→ CI com steps e artefatos reais
→ validadores oficiais externos
→ assinatura/attestation
→ reprodução independente
```

Até esses gates existirem:

```text
formal_conformance = TOKEN_VAZIO
claim_allowed = false
automatic_cross_repository_write = false
automatic_merge = false
```

## R3

```text
F_ok   = vocabulário e projeções públicas estruturados
F_gap  = conformidade oficial, assinatura e reprodução independente
F_next = executar gate canônico e validar as projeções com ferramentas externas
```
