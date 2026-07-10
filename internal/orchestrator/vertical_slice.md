# Vertical slice v1 (read-only)

Fluxo implementado:

1. Entrada `intent_ir` tipada em `rafaelia.intent.v1`.
2. Validação estrutural mínima do intent.
3. Governance gate com allowlist de capability e default deny.
4. Compilação de plano read-only explícito com:
   - `git status`
   - `git diff --stat`
5. Execução local read-only e captura auditável de stdout/stderr/exit code.
6. Cálculo de SHA-256 completo de stdout e stderr.
7. Emissão de `execution_result.json` para trilha de auditoria.

Lacunas mapeadas no corte v1: `TOKEN_VAZIO` para commits/hashes ausentes no runtime lock e integrações externas.
