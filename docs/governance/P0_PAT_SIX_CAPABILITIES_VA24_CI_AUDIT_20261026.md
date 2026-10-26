# P0 · Vetorização de duas iterações · RafGitTools / Actions / RLL

**Data de corte:** 2026-10-26. **Autoridade:** RafGitTools executor limitado; Mapa roteador; RLL superfície pública de revisão.
**Estado:** IMPLEMENTED_SOURCE_ONLY, claim_allowed=false, PROMOTION=HOLD. Esta tabela reúne as duas iterações precedentes e seus estados observados, sem reivindicar validação global.

## Evidência-base — sem herança de PASS

- **Iteração A (reconciliação):** PR #707 **MERGED** em `main@a6155333ad9c319ad9e1860c1d4ac21a8cf45726`; contém núcleo L0 freestanding e um adaptador Python de referência fora do L0. PR #709 é integração candidata de Actions autoral; PR #710 é **alternativa** de core L0, sem prova diferencial/ABI física. RLL PR #1131 foi posteriormente merged sem passar pelo fechamento humano esperado; a cópia pública mantém o estado histórico anterior, com correção submetida via PR #1135.
- **Iteração B (CI composta):** RafGitTools PR #719 em Draft, `HEAD=9ce3a92a610cbc8ccdabc05be47503ae7ac3b21d`, run [40302617138](https://github.com/rafaelmeloreisnovo/RafGitTools/actions/runs/40302617138) **SUCCESS no SHA exato**. Steps de Actions autoral, compilação L0 ARMv7/AArch64, regressão do JSON e recibo final SUCCESS, apenas no escopo sintético; testes físicos/CodeQL/bridge privado pulados. Tentativas anteriores no mesmo PR detectaram símbolo `memcpy` no autoteste ARMv7 e foram corrigidas sem alterar o núcleo em main. RLL PR #1135 permanece correção Draft. A reconciliação financeira privada é uma rota separada, NUNCA vai para GitHub/Pages.
- Inventário do conector em 26/10: **97 repositórios acessíveis por afiliação de proprietário**, sem atestação de cobertura/exaustividade de todos os repositórios organizacionais. `RafGitTools` é público e a conexão informa `permissions.admin=true`, **não** a permissão dos PATs. GET de proteção de `main` retornou **403** pela identidade do conector; GET de rulesets retornou lista vazia. Admin da conta, conexão GitHub, PAT, GitHub App e `GITHUB_TOKEN` têm identidades e poderes diferentes.

## VA — vetor de observações e aceitação (24 eixos, duas iterações)

| ID | Eixo | Situação/risco | Falsificador/aceitação antes de promover |
|---|---|---|---|
| VA01 | SOURCE / autoria | merged #707, #719 candidato | conferir origem, licença e HEAD exato |
| VA02 | ARTIFACT | SHA de objeto ≠ funcionalidade | bytes do objeto e hash reobtidos no provedor |
| VA03 | EXECUTION | run #40302617138 success limitado | steps e SHA exato; nenhum PASS herdado |
| VA04 | EVIDENCE | recibo do Actions autoral | run ID + attempt + source SHA + hash + teste negativo |
| VA05 | CLAIM | `claim_allowed=false` | revisão humana independente obrigatória |
| VA06 | ROLLBACK | Draft, sem merge | plano específico e ensaio no escopo |
| VA07 | PROVIDER / branch protection | GET 403 nesta identidade | readback autorizado e negação de bypass |
| VA08 | RULESETS | lista vazia na leitura atual | obter regras efetivamente aplicadas e enforcement |
| VA09 | CODEQL / code scanning | job anterior skipped | relatório e status required em HEAD exato |
| VA10 | DEPENDABOT | PAT registrado, escopo desconhecido | GET de alertas com credencial limitada, sem escrita |
| VA11 | ACTIONS AUTORAL | actions/pins no #719 | imutabilidade dos SHAs e recibo hospedado verificado |
| VA12 | SEIS PATs | nenhum valor lido; escopos diferentes | provedores e endpoints por capacidade; sem fallback |
| VA13 | ENVIRONMENT | `PAT_ENV` != `PAT_ENVIRONMENTS` | nome/scope/issuer/approval verificados em readback |
| VA14 | AGENTS | `PAT_AGENTS` sem permissão provada | caso de uso + endpoint + escopo mínimo |
| VA15 | CODESPACES | `PAT_CODESPACES` sem prova | probe read-only separado |
| VA16 | PRIVATE CI | bridge anterior skipped | isolamento, artefato hash-only, autorização separada |
| VA17 | L0 ARM32 | compilação cruzada passou | execução física + stack/ABI medidos |
| VA18 | L0 ARM64 | compilação cruzada passou | execução física + stack/ABI medidos |
| VA19 | JSON / idempotência | hash de correlação ≠ assinatura | vetores diferenciais #707 × #710; replay |
| VA20 | PAGES / RLL | PR #1131 merged; página mudou fora da revisão | PR #1135 revisado e prova de deploy, não autoaprovação |
| VA21 | PRIVACY / infantil | não transportar corpus para CI pública | inspeção dos caminhos e política fail-closed |
| VA22 | CRYPTO / ZIPRAF | AES-GCM/ChaCha são opções, ZIPRAF não AEAD | protocolo padronizado, nonce, custódia, AEAD e testes |
| VA23 | FINANCE PRIVADA | documento de reembolso não é extrato | prova privada bancária, sem GitHub, sem inferir crime |
| VA24 | UX / CHECKBOX | Wizard pode captar intenção | clique ≠ autorização; mostrar plano, alvo, impacto e rollback |

Os identificadores VA são **categorias de revisão**, não unidades de mérito, certificados, números de testes, nem autorização de treinamento.

## Seis identidades tipadas (sem valores)

| ID | Segredo nominal | Evidência de rota atualmente documentada | Permissão efetiva |
|---|---|---|---|
| 0 | `PAT_ACTIONS` | repository, leitura/cross-repo bounded main manual | parcialmente documentada, NÃO provada globalmente |
| 1 | `PAT_AGENTS` | repository, preflight | TOKEN_VAZIO_NOT_PROBED |
| 2 | `PAT_CODESPACES` | repository, preflight | TOKEN_VAZIO_NOT_PROBED |
| 3 | `PAT_DEPENDABOT` | repository, preflight | TOKEN_VAZIO_NOT_PROBED |
| 4 | `PAT_ENV` | environment PAT_ENVIRONMENTS, manual | só rota conhecida, não atesta admin |
| 5 | `PAT_ENVIRONMENTS` | identidade humana reportada distinta do nome de environment | TOKEN_VAZIO_PROVIDER_SCOPE |

**Patronagem operacional:** `GITHUB_TOKEN` com `contents:read` por padrão; PATs só na lane manual separada já existente, nunca no job de PR. `PAT_ENV` não substitui `PAT_ENVIRONMENTS`; seis IDs diferentes não significam seis acessos efetivos nem um supertoken intercambiável.

## P0 nativo (sem driver privilegiado)

O módulo `native/pat_controlplane_gate_v1/` expõe apenas um **planejador freestanding** C11: no máximo propõe leitura de metadados públicos quando o campo repository e o commit SHA estão sintaticamente presos. Toda operação privada, visibilidade/private, PR mutation, merge, segredo, regras, environment, alert update ou despacho resulta em HOLD; `operator_approved=1` também não autoriza execução remota.

A suíte exerce 6 PAT IDs × 10 classes de operações e múltiplos vetores negativos; build x86-64, ARMv7 e AArch64 usa `-ffreestanding -fno-builtin -nostdlib` nos objetos e checa símbolos ELF globais ou fracos `UND`. Entry-point com libc é **somente runner de autoteste hospedado**. O código fonte não lê PAT, rede, Drive, GitHub, nem apresenta UI falsa de sucesso. `PLAN_ONLY != PERMISSION != EXECUTION`.

## Próxima rota de integração (não executada por esta CI)

1. Capturar inventário *autorizado* por repo, com nome, ID, owner, visibilidade, branch HEAD, proteções e permissões por ator; registrar `403` ou `TOKEN_VAZIO` sem substituir token.
2. Mostrar interface humana em modo seleção/checkbox **somente para intenção**; classificar visibilidade como HIGH_IMPACT e colocar um segundo gate específico por repositório.
3. Para cada mudança, exigir alvo/repo ID exato, commit HEAD, endpoint, auth actor least-privilege, simulação prestate, impacto de visibilidade/publicação/CI e rollback.
4. Usar uma action hospedada **separada**, só em workflow_dispatch a partir de main protegido com environment reviewer, e registrar pós-estado e falsificador. **Este patch NÃO implementa essa rota de escrita**.
5. Na revisão pública Pages/RLL, publicar só metadados sanitizados após aprovação humana. Dados financeiros, PAT, corpus privado, pessoas e crianças nunca entram na página.
6. Guardar recibos privados append-only com provider run ID, SHA exato, teste negativo e estados F_ok/F_gap/F_next. Negar promoção em qualquer lacuna crítica.

## Rollback e fechamento

O patch adiciona somente os três arquivos freestanding e este documento mais um passo na CI existente. Se recusado, fechar o PR Draft sem merge; se no futuro autorizado e mesclado, reverter apenas esses arquivos/passo com um commit revisado. Não alterar secrets, branch rules, visibilidade, repositórios alheios, workflows privados, credenciais, Pages, dados financeiros ou agendamentos neste P0.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`; `TOKEN_VAZIO != 0`; `claim_allowed=false`; `PROMOTION=HOLD`.
