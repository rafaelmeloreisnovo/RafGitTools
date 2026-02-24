# Issue: Implementação real de GPG

## Contexto
Atualmente `GpgKeyManager` retorna `Result.failure(NotImplementedError(...))` para todas as operações.

## Escopo
- Implementar geração de chave.
- Implementar import/export de chave.
- Implementar assinatura de dados.
- Integrar com UX de erro/sucesso real.

## Critérios de aceite
- Nenhum fluxo deve retornar sucesso sem implementação real.
- Erros devem ser propagados para UI com mensagem útil.
- Cobertura de testes para fluxos principais.
