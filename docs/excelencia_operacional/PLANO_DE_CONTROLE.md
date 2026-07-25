# Plano de Controle RafGitTools

## 1. Topologia

```text
Humano / interface
  -> IntentIR
  -> GovernanceGate
  -> CapabilityResolver
  -> JobEnvelope
  -> executor local/remoto
  -> EventEnvelope
  -> EvidenceEnvelope
  -> decisão e próximo passo
```

## 2. Registro de capacidade

Cada capacidade deve declarar:

```yaml
capability_id: string
module_id: string
version: semver
state: DECLARED|SIMULATED|AVAILABLE|VERIFIED|BLOCKED|TOKEN_VAZIO
operations: []
input_schema: reference
output_schema: reference
privacy_classes: []
mutation_level: READ_ONLY|REVERSIBLE|DESTRUCTIVE
health_probe: string
executor: string
timeout_seconds: integer
rollback: string
evidence_contract: reference
```

`DECLARED` não equivale a `AVAILABLE`; `AVAILABLE` não equivale a `VERIFIED`.

## 3. Gates

1. identidade do solicitante;
2. capacidade observada;
3. política de privacidade;
4. dependências disponíveis;
5. orçamento de recursos;
6. mutabilidade e reversibilidade;
7. revisão humana quando exigida;
8. executor saudável;
9. contrato de evidência;
10. safe state.

## 4. Priorização

A fila operacional deve considerar:

\[
P=f(seguranca, privacidade, bloqueio, impacto, risco\ do\ atraso, evidencia, reversibilidade)
\]

Ordem canônica:

```text
P0_CRITICAL > P1_URGENT > P2_NECESSARY > P3_IMPORTANT > P4_BACKLOG
```

Velocidade e estética nunca superam segurança humana, privacidade ou preservação de evidência.

## 5. Observabilidade

Todo job deve ser rastreável por:

```text
trace_id -> run_id -> job_id -> stage -> event -> artifact -> evidence
```

Status e severidade são campos diferentes. Uma advertência pode ocorrer em job bem-sucedido; um job bloqueado pode não conter erro de código.

## 6. Interface honesta

A UI deve mostrar:

- o que será feito;
- quem executará;
- quais dados serão lidos;
- se haverá mutação;
- limites e tempo estimado;
- estado da capacidade;
- evidência já disponível;
- lacunas;
- rollback;
- resultado real.

Não usar “completo”, “seguro”, “conectado” ou “verificado” sem o estado e a evidência correspondentes.
