# Auditoria corretiva — escopo de billing e exceção operacional do RLL

## Motivo

A auditoria anterior registrou o sentido oposto ao informado pelo responsável. Ela dizia que o problema de pagamento afetava somente `instituto-Rafael/relativity-living-light`.

A correção é:

```text
instituto-Rafael/relativity-living-light = único repositório que continua executando CI
demais repositórios observados = bloqueados antes dos steps
mensagem exibida = pagamento/billing, segundo observação direta do responsável
```

## Evidência técnica observada

```text
rafaelmeloreisnovo/Mapa
  run 29600507830 / job 87951195310 -> failure, 0 steps, no logs
  run 29600507825 / job 87951195363 -> failure, 0 steps, no logs

rafaelmeloreisnovo/RafGitTools
  run 29586438868 / job 87904483057 -> failure, 0 steps, no logs
  run 29586438868 / job 87904483088 -> failure, 0 steps, no logs

instituto-Rafael/relativity-living-light
  run 29566816023 / job 87841176605
  conclusion = success
  observed steps = 14
  classification = WORKFLOW_PASS
```

## Cronologia relatada pelo responsável

```text
1. pagamento do GitHub realizado via Google Play;
2. Actions inicialmente executando nas instalações pessoal e institucional;
3. interrupção posterior dos CIs, exceto no RLL institucional;
4. GitHub passou a exibir bloqueio relacionado a pagamento nos demais repositórios;
5. aproximadamente cinco dias depois ocorreu devolução unilateral;
6. fluxo relatado da devolução: GitHub -> Google -> conta do responsável.
```

Estado da cronologia:

```text
source = owner_observation
state = DECLARED
claim_allowed = false
```

A auditoria não publica recibos, IDs de transação ou dados financeiros.

## Decisão

```text
Mapa execution observation = ZERO_STEP_NO_LOGS
RafGitTools execution observation = ZERO_STEP_NO_LOGS
RLL execution observation = WORKFLOW_PASS
billing block outside RLL = DECLARED_BY_OWNER
billing message artifact captured = false
refund chronology artifact captured = false
root cause of RLL exception = TOKEN_VAZIO
```

## Invariante corrigida

```text
não inverter o controle positivo:
RLL é a exceção que roda, não o repositório bloqueado.
```

## Próximas evidências válidas

1. capturar privadamente a mensagem de billing em um repositório bloqueado;
2. preservar privadamente o comprovante do pagamento e da devolução;
3. correlacionar datas de pagamento, interrupção, mensagem e estorno;
4. comparar configurações de Actions/billing entre as duas instalações;
5. manter o RLL como controle positivo para isolar a assimetria.

## Rollback

Reverter os commits desta correção restaura a auditoria anterior, que contém o escopo invertido e, portanto, não deve ser considerada canônica.
