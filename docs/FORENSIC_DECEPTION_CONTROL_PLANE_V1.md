# Forensic Deception Control Plane V1

Estado: `REFERENCE`  
Implementação Android/runtime: `TOKEN_VAZIO`  
Claim boundary: `claim_allowed=false`

## 1. Responsabilidade

`RafGitTools` não é a autoridade dos dados empresariais nem o detector científico. Ele coordena:

```text
pedido
→ política
→ aprovação
→ job tipado
→ execução no produtor
→ evidência
→ gate
→ decisão
→ rollback/arquivo
```

Autoridades federadas:

| Corpo | Autoridade |
|---|---|
| `Mapa` | ontologia, contratos, dependências e estado |
| `papers` | claims, referências, falsificadores e limites |
| `RafPolimata` | laboratório e evidence runs |
| runtime/banco | transações reais e métricas |
| `RafGitTools` | controle, aprovação, acompanhamento e recibos |

## 2. Tipos de job

```text
FD_INVENTORY
FD_VALIDATE_SCHEMA
FD_GENERATE_SYNTHETIC_DATASET
FD_BUILD_PROJECTION
FD_VALIDATE_BUSINESS_INVARIANTS
FD_VALIDATE_DECOY_ISOLATION
FD_SIMULATE_SUBSET_ATTACK
FD_SIMULATE_STRIP_ATTACK
FD_SIMULATE_CORRELATION_ATTACK
FD_SIMULATE_COLLUSION
FD_ROTATE_EPOCH
FD_DETECT_SOURCE_CANDIDATES
FD_ARCHIVE_EVIDENCE
FD_ROLLBACK_EPOCH
```

Jobs que contenham `SIMULATE_*` operam somente em fixtures/dados sintéticos no V1.

## 3. Máquina de estados

```text
DRAFT
→ POLICY_CHECKED
→ APPROVED
→ QUEUED
→ RUNNING
→ PASS | FAIL | TOKEN_VAZIO
→ ARCHIVED
```

Transições adicionais:

```text
RUNNING → CANCELLED
PASS/FAIL → ROLLED_BACK
qualquer estado → BLOCKED_POLICY
```

`PASS` exige:

- commit/ref imutável;
- ambiente;
- comando;
- início e fim;
- exit code;
- stdout/stderr preservados ou resumidos com hash;
- hashes dos artefatos;
- gate e limite explícitos.

## 4. Defaults seguros

```text
dry_run = true
destructive = false
production_data = false
synthetic_only = true
claim_allowed = false
automatic_attribution = false
automatic_retaliation = false
```

Nenhuma dessas opções pode ser invertida apenas por sugestão de modelo.

## 5. Aprovações

A execução que tocar banco real exige, no mínimo:

```text
DATA_OWNER_APPROVAL
SECURITY_APPROVAL
PRIVACY_OR_LEGAL_REVIEW
ROLLBACK_VERIFIED
SNAPSHOT_VERIFIED
```

Rotação de época exige ainda:

```text
PREVIOUS_EPOCH_MANIFEST
NEW_EPOCH_PLAN
MAPPING_CUSTODY
BUSINESS_QUERY_BASELINE
```

## 6. Gates

| Gate | Condição |
|---|---|
| `FD-G0` | envelope e schema válidos |
| `FD-G1` | fixture/dataset autorizado |
| `FD-G2` | isolamento canonical/deception |
| `FD-G3` | invariantes empresariais preservadas |
| `FD-G4` | projeção sem colisões/relações órfãs |
| `FD-G5` | manifesto autenticado |
| `FD-G6` | teste adversarial executado |
| `FD-G7` | falso positivo/falso negativo medidos |
| `FD-G8` | p50/p95/p99 e armazenamento medidos |
| `FD-G9` | rollback executado |
| `FD-G10` | evidência arquivada e claims atualizados |

## 7. Eventos

Cada evento precisa conter:

```text
event_id
job_id
job_type
repository
commit_sha
actor
station_id
epoch_id
policy_version
state_from
state_to
input_root
output_root
previous_event_hash
timestamp
result
limitations
```

## 8. Relação com Vectra/modelos

Vectra ou modelo gerativo podem:

- recuperar documentos e evidências;
- sugerir job;
- priorizar revisão;
- apontar anomalias;
- produzir explicação.

Não podem isoladamente:

- autorizar acesso;
- mudar época;
- gravar decoy em produção;
- atribuir vazamento a uma pessoa;
- revogar usuário;
- promover claim para `PASS`.

```text
modelo sugere
política autoriza
executor realiza
verificador decide o gate
ledger registra
```

## 9. UI futura

Superfície Android proposta:

```text
Forensic Deception
├── Overview
├── Epochs
├── Jobs
├── Gates
├── Evidence
├── Source Candidates
├── Decoy Events
├── Metrics
└── Rollback
```

Cores ou badges devem representar estado epistemológico, nunca certeza jurídica.

## 10. Falhas obrigatórias

- dataset real sem aprovação: `BLOCKED_POLICY`;
- falta de snapshot: `BLOCKED_POLICY`;
- ausência de run: `TOKEN_VAZIO`;
- divergência empresarial: `FAIL`;
- decoy alcança caminho canônico: `FAIL_CRITICAL`;
- manifesto inválido: `FAIL`;
- atribuição sem evidência independente: `BLOCKED_OVERCLAIM`;
- rollback não testado: produção bloqueada.

## 11. Próxima implementação

1. validar `configs/forensic-deception-control-contract.v1.json`;
2. criar modelos Kotlin somente após confirmar package/Room atuais;
3. conectar primeiro ao relatório JSON do laboratório;
4. manter toda ação como read-only/dry-run;
5. adicionar UI depois dos testes de domínio.

---

`F_ok`: plano de controle delimitado e não destrutivo.  
`F_gap`: modelos Kotlin, Room, workers e UI permanecem `TOKEN_VAZIO`.  
`F_next`: parser read-only do evidence run do `RafPolimata`.
