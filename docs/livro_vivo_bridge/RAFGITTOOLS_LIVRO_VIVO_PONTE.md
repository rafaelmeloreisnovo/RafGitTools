# Ponte Livro Vivo — RafGitTools, Automação Git e Rastreabilidade

> Modo: ponte operacional entre `RafGitTools` e o Livro Vivo RAFAELIA  
> Status inicial: `FORMALIZACAO_READY` + `DADO_SENSIVEL` quando houver tokens ou automação privilegiada  
> Regra: ferramenta Git deve registrar intenção, escopo, credencial, ação e reversão

## Parábola do martelo automático

O ferreiro criou um martelo que batia sozinho.

No primeiro dia, fez cem pregos.

No segundo, quebrou uma porta.

O discípulo perguntou:

— O martelo ficou mau?

O mestre respondeu:

— Não. Ele apenas não sabia onde parar.

Assim é automação Git: poderosa quando tem trilho; perigosa quando não tem escopo.

## Invariante

```text
intenção → escopo → ação Git → log → reversão
```

Forma compacta:

```math
Inv(RafGitTools)=Intent\rightarrow Scope\rightarrow GitAction\rightarrow AuditLog\rightarrow Rollback
```

## Risco principal

| Risco | Correção |
|---|---|
| automação com token exposto | usar segredo protegido e varredura |
| ação sem dry-run | exigir modo simulação |
| commit/PR em repo errado | declarar escopo e allowlist |
| falta de rollback | registrar plano de reversão |
| logs com dado sensível | depurar logs antes de publicar |

## Próximos passos

1. Criar `RAFGITTOOLS_OPERATIONAL_SAFETY.md`.
2. Definir allowlist de repositórios e ações.
3. Exigir modo `--dry-run` para operações destrutivas.
4. Registrar logs sem tokens.
5. Criar checklist de rollback.

## Ficha Livro Vivo

```yaml
repo: rafaelmeloreisnovo/RafGitTools
familia: Git/Automacao
invariante: "intenção → escopo → ação Git → log → reversão"
selo: FORMALIZACAO_READY
risco: "automação com token, ação fora do escopo, ausência de dry-run ou rollback"
proximo_passo: "criar RAFGITTOOLS_OPERATIONAL_SAFETY.md"
```

## Retroalimentar[3]

- **F_ok:** RafGitTools recebe ponte para automação com escopo, log e reversão.
- **F_gap:** falta inventário real das ferramentas, permissões e comandos destrutivos.
- **F_next:** criar `RAFGITTOOLS_OPERATIONAL_SAFETY.md` com dry-run, allowlist e rollback.


## Ponte canônica — Rotas Humanas Lúdicas

Fonte autoral em revisão humana:

- repositório: `rafaelmeloreisnovo/templo-vivo-arcs`
- branch: `docs/livro-vivo-rotas-humanas-v1-20260927`
- head observado: `2f2cb9f44eafa373b515aecae1987d5f7afe1d4e`
- arquivo: `docs/livro_vivo/ROTAS_HUMANAS_LUDICAS_V1.md`
- PR: `#41` (draft no momento desta ponte)

RafGitTools **não duplica** o caderno. Ele registra a rota e traduz seus princípios para controles operacionais.

### Tradução para controle

| Rota humana | Controle operacional |
|---|---|
| Sete Lanternas | múltiplas fontes/rotas não são promovidas automaticamente a uma única verdade |
| Árvore | registrar impacto e custo futuro antes de mutação irreversível |
| Pedra Imperfeita | `TOKEN_VAZIO != 0` |
| Rio Reversível | before/after + replay + rollback |
| Cartógrafo | oferecer alternativas e riscos; preservar decisão humana |
| Três Espelhos | BODY/SOUL/SPIRIT como classes independentes |
| Porta Pequena | maior dever de cuidado em contexto infantil/vulnerável |
| Jardim | pluralismo e não discriminação sem apagar diferenças |

### Gate de uso

```text
PARABLE -> EXPLANATION
EXPLANATION -> ROUTE
ROUTE -> SOURCE/EVIDENCE
SOURCE/EVIDENCE -> DECISION
```

Nunca:

```text
PARABLE -> CLAIM
TRADITION -> TECHNICAL_PROOF
PERSPECTIVE -> LAW
```

### Auditabilidade e reconstrução

Toda automação que materialize um princípio desta ponte deve registrar, quando aplicável:

```yaml
route_id: string
source_ref: string
owner: string
authority: string
before_state: ref
action: bounded_action
after_state: ref
evidence_ref: ref
receipt_ref: ref
replay_recipe: string
rollback_procedure: string
rollback_test: string
privacy_class: string
human_review_required: boolean
claim_allowed: boolean
```

Se a rota não puder ser reconstruída por outra pessoa/IA autorizada a partir das referências registradas, usar `TOKEN_VAZIO_RECONSTRUCTION`.

### Fronteira jurídica e normativa

RafGitTools pode **mapear** normas, requisitos e controles; não deve declarar certificação ou conformidade integral por simples presença de documentação. Uma referência normativa precisa de identidade, versão/data, escopo/aplicabilidade e evidência correspondente.

Material envolvendo dados pessoais, crença, crianças, saúde ou conteúdo cultural sensível deve permanecer fail-closed para publicação quando a base de autorização, finalidade, privacidade ou direitos de terceiros estiver incompleta.

`SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`
