# Issue: Implementação real de Git Worktree

## Contexto
Atualmente `WorktreeManager` retorna `Result.failure(NotImplementedError(...))` para `add`, `list` e `remove`.

## Escopo
- Implementar add worktree.
- Implementar list worktrees.
- Implementar remove worktree.
- Validar path/branch e erros de estado.

## Critérios de aceite
- Operações funcionais com Git real.
- Erros explícitos para branch/path inválidos.
- Testes cobrindo operações básicas.
