# Issue: Implementação real de Webhook

## Contexto
Atualmente `WebhookHandler.handle` retorna `Result.failure(NotImplementedError(...))` após log de recebimento.

## Escopo
- Validar assinatura do webhook.
- Parsear payload por tipo de evento.
- Rotear ações de domínio para cada evento suportado.
- Adicionar telemetria e tratamento de erro.

## Critérios de aceite
- Somente eventos válidos geram processamento.
- Retorno de sucesso apenas em fluxo implementado.
- Cobertura de testes para assinatura/payload inválido.
