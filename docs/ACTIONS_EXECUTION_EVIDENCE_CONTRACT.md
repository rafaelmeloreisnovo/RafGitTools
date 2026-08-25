# Contrato de evidência de execução do GitHub Actions

## Invariante

```text
observação do job != causa financeira
zero steps + no logs = ZERO_STEP_NO_LOGS
mensagem de billing observada pelo responsável = escopo causal DECLARED
mensagem causal capturada como artefato = BILLING_BLOCKED verificável
positive steps + failure = WORKFLOW_EXECUTED_FAILURE
positive steps + success = WORKFLOW_PASS
```

A ausência de etapas observáveis mostra que o job não chegou à execução registrada. Ela não identifica, sozinha, a causa. Porém, uma observação direta do responsável sobre a mensagem exibida pelo GitHub também não deve ser descartada: ela entra como declaração causal separada, com origem e critérios de promoção.

## Classificações de execução

| Estado | Critério mínimo |
|---|---|
| `WORKFLOW_PASS` | conclusão `success` e pelo menos um step observado |
| `WORKFLOW_EXECUTED_FAILURE` | conclusão `failure` e pelo menos um step observado |
| `ZERO_STEP_NO_LOGS` | conclusão `failure`, zero steps e logs ausentes |
| `BILLING_BLOCKED` | mensagem explícita de billing/spending/payment capturada como evidência |
| `POLICY_BLOCKED` | mensagem explícita de política, runner group ou action proibida |
| `CANCELLED_BEFORE_EXECUTION` | cancelamento com zero steps |
| `TOKEN_VAZIO` | evidência insuficiente para qualquer estado acima |

## Correção de escopo

A leitura anterior foi invertida.

O responsável informou que:

- Actions ficaram bloqueados com mensagem de pagamento nos repositórios das instalações `rafaelmeloreisnovo` e `instituto-Rafael`;
- `instituto-Rafael/relativity-living-light` permaneceu como a única exceção executando CI;
- o pagamento havia sido feito via Google Play;
- após a interrupção dos CIs, ocorreu devolução unilateral aproximadamente cinco dias depois, primeiro do GitHub ao Google e depois do Google à conta do responsável.

Esses eventos financeiros permanecem `DECLARED` até a preservação privada dos comprovantes e mensagens. Eles não devem ser publicados com identificadores sensíveis.

## Controle positivo verificado

O conector confirmou que `instituto-Rafael/relativity-living-light` executou o workflow `RLL Scientific Validation Pipeline` com `success` e 14 steps observados.

Portanto:

```text
RLL = positive control / WORKFLOW_PASS
Mapa e RafGitTools = ZERO_STEP_NO_LOGS na observação técnica
bloqueio de billing fora do RLL = DECLARED pelo responsável
causa da exceção do RLL = TOKEN_VAZIO
```

## Limite

Este contrato separa quatro coisas:

1. resultado observável do job;
2. mensagem causal vista pelo responsável;
3. artefato técnico capturado;
4. explicação da assimetria entre o RLL e os demais repositórios.

Nenhuma delas deve ser substituída pela outra.
