# Issue: Implementação real de Git LFS

## Contexto
Atualmente `LfsManager` retorna `Result.failure(NotImplementedError(...))` para `install`, `track` e `fetch`.

## Escopo
- Implementar instalação/configuração de LFS.
- Implementar tracking de padrões.
- Implementar fetch de objetos LFS.
- Tratar erros de ambiente/repos remoto.

## Critérios de aceite
- Operações retornam `Result.success` apenas em execução real.
- Falhas propagadas com causa explícita.
- Testes para sucesso/falha.
