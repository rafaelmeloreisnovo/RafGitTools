# RAFGITTOOLS_TERMINAL_STRATEGY

- Status: ATIVO
- Última atualização: 2026-05-27

## Estado atual

Terminal interno controlado por allowlist de comandos e timeout. Não é PTY completo e não substitui Termux.

## Estratégia de risco

Classes sugeridas para próxima iteração:
- SAFE_READ
- SAFE_GIT_READ
- SAFE_GIT_WRITE
- DANGEROUS_GIT_WRITE
- BLOCKED

Comandos DANGEROUS devem exigir confirmação explícita antes da execução.
