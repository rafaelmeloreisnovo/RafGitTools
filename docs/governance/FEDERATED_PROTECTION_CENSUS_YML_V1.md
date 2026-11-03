# Federação GitHub por YAML — auditoria de proteções V1

Estado: **SOURCE_IMPLEMENTED / BRANCH_ONLY / HOST_TESTED_EQUIVALENT / PROVIDER_NOT_RUN / HOLD**  
Autoridade: RafGitTools AGENTS.md + PAT_CAPABILITY_ROUTING_V1.md; Mapa roteia, cada repositório produtor governa suas alterações.

## O que foi colocado no GitHub Actions

Não foi adicionado um sétimo arquivo YAML ativo. A topologia exige exatamente seis workflows ativos. Em vez disso, a rota `federated_protection_audit` foi incorporada à **`.github/workflows/START.yml`**, reutilizando o fluxo existente, sem workflow concorrente.

A execução nova faz apenas **GET**:
- `GET /user/repos` com paginação até 10 páginas, limite explícito de 500 repositórios visíveis ao `PAT_ENV`;
- consulta da branch padrão e do endpoint de proteção;
- consulta de lista de rulesets (presença não significa enforcement);
- recibo agregado e sanitizado, sem nomes/IDs de repositórios privados, conteúdo, valores dos PATs ou tokens.

A execução **NÃO** modifica proteções, regras, ambientes, usuários, branches, PRs, secrets, permissões nem visibilidade. `403` e `404` são `UNKNOWN`, não ausência de proteção nem sucesso. O fim da paginação não prova censo global, snapshot atômico nem qualidade suficiente de todos os controles.

## Como executar no GitHub (após revisão e integração autorizada)

1. Revisar a branch `feat/federated-protection-census-yml-20261103` e provar CI/rollback.
2. Somente após fechar os gates de proteção e revisão independente, promover a mudança para `main`.
3. Abrir **RafGitTools → Actions → START · RAFAELIA Orchestrated Pipeline → Run workflow**.
4. Selecionar branch **main** e `mode=federated_protection_audit`.
5. Aprovar o Environment existente `Pat_environments`, se este realmente tiver revisores obrigatórios configurados. Não presumir que há revisão apenas pelo nome.
6. Consultar o artefato `start-08r-readonly-census-<run_id>`, resumo e recibo final.

A rota exige `workflow_dispatch`, ator `rafaelmeloreisnovo`, branch `main`, `topology=success`, `coherence=success` e segredo `PAT_ENV` no Environment. Os dados sensíveis do PAT são injetados **somente** no step de consulta e não são persistidos. Se não tiver permissionamento válido para um alvo, permanece `TOKEN_VAZIO`; não usar `PAT_AGENTS` como substituto.

## E para corrigir as proteções dos repositórios?

A auditoria global é **somente inventário e detecção de lacunas**. Ela não gera autorização para atualização em massa. A rota `provider_environments` / `federated_preflight` e eventual `federated_protect` continua independente, restrita a alvos explícitos, com SHA exato, gate humano, prova de `Administration: write`, GET prévio, PUT individual, GET posterior e rollback planejado. Seu contrato atual não autoriza todos os repositórios; ampliar allowlist requer revisão local por repositório e contratos do Mapa.

## Aceitação e anti-regressão

- Scripts: `scripts/federated_protection_audit_v1.py` (stdlib, GET-only) e `tests/test_federated_protection_audit_v1.py`.
- Escopo dos recibos: somente contadores, tipo de operação, limite, estados tipados e metadados de execução.
- `START.yml`: novo modo manual e job 08R com check obrigatório no fechamento; test suite offline também acoplada à `coherence`.
- Prove a sintaxe YAML, a coerência exata e o comportamento em CI antes da promoção.
- Não confundir uma execução concluída com `PASS` de branch protection de todos os repositórios.

Rollback: abandonar a branch isolada, sem alterar `main`. Qualquer mudança futura em `main` deve ter revert revisado e receipt.

`SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM`  
`IMPLEMENTED_UNTESTED ≠ PASS` | `claim_allowed=false`.
