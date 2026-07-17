# Contrato de evidência de execução do GitHub Actions

## Invariante

```text
zero steps + no logs != billing
zero steps + no logs != workflow failure proven
explicit billing message -> BILLING_BLOCKED
positive steps + failure -> WORKFLOW_EXECUTED_FAILURE
positive steps + success -> WORKFLOW_PASS
```

A ausência de etapas observáveis é evidência de que o job não chegou à execução registrada. Ela não identifica, sozinha, a causa.

## Classificações

| Estado | Critério mínimo |
|---|---|
| `WORKFLOW_PASS` | conclusão `success` e pelo menos um step observado |
| `WORKFLOW_EXECUTED_FAILURE` | conclusão `failure` e pelo menos um step observado |
| `ZERO_STEP_NO_LOGS` | conclusão `failure`, zero steps e logs ausentes |
| `BILLING_BLOCKED` | mensagem explícita de billing/spending/payment |
| `POLICY_BLOCKED` | mensagem explícita de política, runner group ou action proibida |
| `CANCELLED_BEFORE_EXECUTION` | cancelamento com zero steps |
| `TOKEN_VAZIO` | evidência insuficiente para qualquer estado acima |

## Correção do diagnóstico anterior

O rótulo amplo `STARTUP_FAILURE_OR_INFRASTRUCTURE_FAILURE` é mantido apenas como histórico e fica depreciado. Ele mistura observação e hipótese causal.

O estado verificável para os jobs observados no `Mapa` e no `RafGitTools` é:

```text
ZERO_STEP_NO_LOGS
```

A informação de que pagamento afeta somente `instituto-Rafael/relativity-living-light` entra como `DECLARED` até que a mensagem remota seja capturada como artefato. Nenhum outro repositório recebe classificação de pagamento por analogia.

## Limite

Este contrato classifica evidência. Ele não corrige cobrança, política de organização, disponibilidade de runner ou falhas internas do GitHub. Sua função é impedir causalidade inventada e orientar o próximo diagnóstico.
