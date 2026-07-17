# Auditoria — evidência de execução do GitHub Actions

## Motivo

A classificação anterior `STARTUP_FAILURE_OR_INFRASTRUCTURE_FAILURE` era ampla demais. Após a correção de escopo fornecida pelo responsável, pagamento não pode ser inferido fora de `instituto-Rafael/relativity-living-light` sem mensagem explícita.

## Evidência observada

```text
rafaelmeloreisnovo/Mapa
  run 29600507830 / job 87951195310 -> failure, 0 steps, no logs
  run 29600507825 / job 87951195363 -> failure, 0 steps, no logs

rafaelmeloreisnovo/RafGitTools
  run 29586438868 / job 87904483057 -> failure, 0 steps, no logs
  run 29586438868 / job 87904483088 -> failure, 0 steps, no logs
```

## Decisão

```text
Mapa = ZERO_STEP_NO_LOGS
RafGitTools = ZERO_STEP_NO_LOGS
billing inferred = false
workflow code failure proven = false
remote pass proven = false
```

## Implementação

- contrato JSON machine-readable;
- classificador e validador stdlib-only;
- manifesto com evidências observadas;
- 14 testes positivos e adversariais;
- integração ao gate canônico existente;
- nenhum workflow YAML novo.

## Resultado local

```text
py_compile = PASS
unit tests = 14 PASS
contract validation = PASS
manifest validation = PASS
summary deterministic = true
shell syntax = PASS
external dependencies = 0
```

## Limite epistemológico

A informação de escopo sobre cobrança foi preservada como `DECLARED`, porque ainda não foi anexada a mensagem causal remota do RLL. Isso impede dois erros opostos:

1. ignorar a informação fornecida pelo responsável;
2. promovê-la a prova técnica remota sem artefato.

## Próxima saída válida

1. capturar a mensagem causal explícita do RLL;
2. verificar Actions/repository policy e runner availability nos repositórios pessoais;
3. somente então aplicar hotfix causal;
4. manter materialização do inventário em lotes independentes deste incidente.

## Rollback

Reverter os commits deste hotfix remove integralmente o contrato de classificação sem alterar os contratos longitudinais, o contrato de validade de conteúdo ou o runtime lock.
