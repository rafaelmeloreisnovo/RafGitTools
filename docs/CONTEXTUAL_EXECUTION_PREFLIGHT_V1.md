# Preflight de Execução Contextual v1

## Regra central

```text
função nativa disponível != função autorizada
fonte conectada != fonte observada
hipótese semântica != decisão
PR criado != execução validada
```

O RafGitTools atua como porteiro operacional. Ele recebe o hash do pacote do Mapa, o hash do resultado relacional do RafPolimata, as fontes observadas, o gate semântico, a operação pretendida, revisão humana, rollback e limites.

## Decisão

Um pedido pode ser estruturalmente válido e ainda retornar `BLOCKED`.

Mutação exige simultaneamente:

- gate semântico aberto;
- nenhuma lacuna bloqueante;
- fontes observadas e autorizadas;
- revisão humana aprovada;
- branch isolada e rollback;
- escrita direta na branch padrão negada;
- limites de recurso.

Mesmo `AUTHORIZED` significa apenas **autorizado e ainda não executado**. A execução precisa produzir recibo independente.

## Risco psicológico e confiança

O preflight não tenta decidir se houve intenção de manipulação. Ele remove da resposta e da execução os atalhos que produzem esse efeito:

- confiança sem fonte;
- acolhimento usado como evidência;
- memória alegada tratada como fato;
- capacidade de ferramenta tratada como consentimento.

## Uso

```bash
python3 scripts/validate_contextual_execution_request.py \
  examples/contextual-execution-request.wine-formula.json

python3 -m unittest tests.test_contextual_execution_preflight
```

## R3

```text
F_ok   = contrato válido pode permanecer bloqueado
F_gap  = integração automática com receipts e identidade de commits
F_next = ligar hashes reais do Mapa e RafPolimata ao primeiro pedido read-only
```
