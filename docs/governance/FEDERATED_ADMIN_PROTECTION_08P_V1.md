# RAFAELIA — Admin Federation 08P V1 (candidato P0)

**Status: DRAFT_SOURCE_ONLY / PROVIDER_MUTATION_NOT_RUN / PROMOTION=HOLD / claim_allowed=false.**

## Objetivo
Permitir que RafGitTools seja ponto de comando do provedor GitHub para operações administrativas de escopo estrito, reaproveitando **o único `.github/workflows/START.yml`**, a lane 08P e o ambiente de credenciais `Pat_environments` já existentes. Não é um switch de administração global: cada execução processa **somente um repositório autorizado**.

## Primeira aplicação permitida pelo código
Repositórios **públicos** explicitamente fixados por nome e ID observado:
- `rafaelmeloreisnovo/Mapa`, ID `1267885874`.
- `rafaelmeloreisnovo/actions`, ID `1083210044`.

Operação `federated_preflight`: metadata GET, branch main exata, proteção e rulesets; receipt sanitizado e nenhum PUT.
Operação `federated_protect`: apenas se HEAD exato, ID, dono, branch, estado do provedor e permissão administrativa forem positivos, sem regras ativas concorrentes nem proteção preexistente. Repetir leitura imediatamente antes de um único PUT de proteção e verificar resposta e estado. Política desta V1: impedir force-push e exclusão de main, enforce_admins=true; **não** alegar review obrigatório, CodeQL obrigatório, automerge bloqueado, runner seguro ou proteção de todos os repositórios. Não desativa rulesets nem sobrescreve proteção existente.

## UI — comando humano a partir de main, após revisão e merge futuro
GitHub > RafGitTools > Actions > START > Run workflow, branch `main`:
1. `mode=provider_environments`
2. `provider_operation=federated_preflight` inicialmente, nunca protect sem olhar recibo
3. `repository_factory_owner=rafaelmeloreisnovo`
4. `repository_factory_name=Mapa` ou `actions`
5. `provider_expected_main_sha=<commit SHA de 40 hex obtido agora para o target>`
6. `federated_protect` só depois de revisão humana do recibo de preflight e decisão expressa para **aquele repo**. O ambiente `Pat_environments` pode ter regras próprias cuja proteção real precisa de readback; a simples declaração do ambiente não prova review independente.

Ações por `schedule`, `push`, `pull_request`, rerun, ator diferente do proprietário, branch diferente de main, SHA inválido/stale, ID divergente, repositório privado, 403/404 ambíguo, ruleset ativo, proteção existente, token ausente e resposta não verificável entram em HOLD. Nenhuma substituição silenciosa de `PAT_ENV` por outro PAT.

## Fronteiras e ressalvas
- Nenhum PAT é escrito/hasheado em logs ou artefatos; cabeçalho Authorization apenas no request de transporte seguro `https://api.github.com` com redirecionamento negado. A API GitHub exige credencial com `Administration: write` para o PUT de branch protection; presença de segredo, admin da conexão ou HTTP 403 não provam escopo.
- A V1 **não tem bulk**, mudança de público/privado, alteração de Secrets, organização, ACL, ambientes, Dependabot, PR ou merge. A camada freestanding C anterior é somente plano, não driver de rede. Python é adaptador hospedado fora do L0; código do kernel preservado.
- Um PUT confirmado pode criar estado irreversível pela mesma credencial se a capacidade administrativa mudar. O rollback operacional consiste em solicitação humana separada com readback e reversão pelo provedor **somente da proteção criada**; não há rollback destrutivo automático de branch, histórico ou conteúdo. `ROLLBACK_NOT_REHEARSED`.
- Receipts só contêm metadados públicos e estado tipado. RLL Pages só pode publicar cópias sanitizadas após revisão. Não divulgar registros de repositórios privados em CI pública.
- Não usar dados nem fotos pessoais, material infantil, chaves, contas financeiras ou propriedade autoral no fluxo.

## Matriz F_ok / F_gap / F_next
`F_ok`: código de rota manual dentro da CI única, executor Python de alvo singular e testes de fake provider; sem alternativa de PAT. Status de CI registrado apenas quando run exato retornar.

`F_gap`: proteção real e escopo de `PAT_ENV` não provados, ambiente com revisão independente não demonstrado, rollback não ensaiado, aplicação real NÃO_EXECUTADA, controles adicionais de CodeQL, aprovação e rulesets pendentes. O escopo dos 97 repositórios não é uma allowlist de mutação.

`F_next`: revisar código e testes, promover conscientemente a camada para `main` com autorização humana, realizar **preflight manual só de um repo**, examinar recibo/proteções do provedor e decidir se aplica a política. Após essa evidência, ampliar allowlist em PRs menores com risco, rollback, pin de ID e SHA.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`; `TOKEN_VAZIO != 0`; `claim_allowed=false`; `PROMOTION=HOLD`.
