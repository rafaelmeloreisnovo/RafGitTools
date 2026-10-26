# RAFAELIA P0 — integração controlada do L0 existente e Actions autoral

Observação em 2026-10-26. Branch `ci/p0-main-queue-owned-action-composite-20261026`. Base: RafGitTools `main@c6a66a1ea681e62a6b16962e8925d6e9abe08ebe`.

## Objetivo e fronteira
Unificar **no mesmo HEAD testável** duas alterações previamente isoladas:
1. O núcleo de fila em C freestanding `scripts/freestanding/session_queue_core_v1.c` e seus testes que já existem em `main` via merge real do PR #707.
2. Os Actions autorais `verify-source@47e0ad95703009bfaaa902f605fd3bf69aae4b48` e `evidence-receipt@885b6f4c4356d5e1cea352653cbe69a4e141d198` do PR #709, com falsificador de README e material documental preservado por exato blob SHA.

Não introduzir `src/freestanding/session_queue_preflight_core_v1.c` do PR #710: esse candidato permanece **isolado** até prova diferencial de ABI, JSON, SHA, segurança, falsificadores e stack; seu sucesso de CI não é herdado.
O Python de referência pré-existente é usado somente no job hosted de testes, **não** como núcleo. Não há novo serviço de despacho, PAT, bridge privado, secret, criptografia, armazenamento de chaves ou publicação.

## Mudança controlada
- Adição das ações autorais no job `topology` e testes do L0 já no main no job `coherence` de **um único** START.yml.
- Compile dos objetos nativos ARMv7 e AArch64; link relocatável `-nostdlib`; verificação de todos os símbolos ELF indefinidos nomeados `UND`; autoteste C host descartável e testes Python sintéticos preexistentes.
- Saídas tipadas `PASS_SCOPED` não declaram prova física. Não gerar artefato público contendo dados particulares.
- Falsificador e YAML histórico do PR #709 copiados como arquivos auxiliares; arquivo de workflow legado permanece fora de `.github/workflows`.
- Permissões de GitHub Actions, regras de branch, segredos e cron não modificados.

## Aceitação
Somente a **CI executada para o HEAD exato deste PR**, com verificação independente dos jobs `01 · Workflow topology`, dos dois passos RAFCODE, `02 · Coherence`, passos do L0 e validação do recibo final, pode atestar este escopo de integração. A comparação das duas implementações L0 (#707 e #710), execução física ARM, CodeQL independente e private E2E permanecem NOT_RUN/HOLD.

## Risco e reversibilidade
Se um passo falhar: `PROMOTION=HOLD`; não mesclar ou herdar PASS. Este PR deve permanecer Draft; rollback = fechar o PR sem merge, mantendo objetos Git e autoria. Se no futuro for mesclado com autorização, revert por commit revisado limitado a este patch.
`claim_allowed=false`. `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`. `TOKEN_VAZIO != 0`.
