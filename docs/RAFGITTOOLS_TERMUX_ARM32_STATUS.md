# RAFGITTOOLS_TERMUX_ARM32_STATUS

- Status: ATIVO
- Última atualização: 2026-05-27

## Verdade atual

- Termux ARM32 é ambiente de validação runtime/toolchain, não host canônico de build APK.
- Script `scripts/termux/arm32_runtime_check.sh` roda sem quebrar em hosts sem `gh`.
- Não prometer build APK completo em Termux ARM32 sem prova objetiva.

## Validação recomendada

- Desktop/CI para assemble + verify_apks.
- Termux para runtime checks, disponibilidade de git/gh/ssh e storage.
