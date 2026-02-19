# PENDING 33 ITEMS (Baseline)

## Origem dos “33 lugares”

A origem adotada para os **33 lugares** é o recorte dos **primeiros 33 itens marcados como `🔴 L1`** no roadmap oficial de desenvolvimento.

- Arquivo-fonte: `docs/ROADMAP.md`
- Critério: linhas de tabela no formato `| <id> | <feature> | 🔴 L1 | ... |`
- Objetivo: transformar lacunas de implementação em backlog rastreável com ID único para commits futuros.

## Lista rastreável (arquivo + linha + tipo de lacuna)

| Item ID | Feature ID | Feature (ROADMAP) | Origem (arquivo:linha) | Tipo de lacuna |
|---|---:|---|---|---|
| P33-01 | 20 | Git clone (shallow) | `docs/ROADMAP.md:131` | Implementação funcional ausente (Core Git) |
| P33-02 | 21 | Git clone (single branch) | `docs/ROADMAP.md:132` | Implementação funcional ausente (Core Git) |
| P33-03 | 22 | Git clone (with submodules) | `docs/ROADMAP.md:133` | Implementação funcional ausente (Core Git) |
| P33-04 | 24 | Git commit (amend) | `docs/ROADMAP.md:135` | Implementação funcional ausente (Core Git) |
| P33-05 | 25 | Interactive staging | `docs/ROADMAP.md:136` | Implementação funcional ausente (Core Git) |
| P33-06 | 29 | Force push with lease | `docs/ROADMAP.md:140` | Regra de segurança/fluxo de push não implementada |
| P33-07 | 30 | Pull with rebase | `docs/ROADMAP.md:141` | Implementação funcional ausente (Core Git) |
| P33-08 | 33 | Branch rename | `docs/ROADMAP.md:144` | Implementação funcional ausente (Core Git) |
| P33-09 | 36 | Merge strategies | `docs/ROADMAP.md:147` | Estratégias avançadas de merge ausentes |
| P33-10 | 40 | Stash operations | `docs/ROADMAP.md:151` | Implementação funcional ausente (Core Git) |
| P33-11 | 42 | Git config management | `docs/ROADMAP.md:153` | Gestão de configuração Git incompleta |
| P33-12 | 46 | Syntax highlighting | `docs/ROADMAP.md:187` | Lacuna de UX/renderização de conteúdo |
| P33-13 | 47 | Line numbers | `docs/ROADMAP.md:188` | Lacuna de UX em visualização de arquivo |
| P33-14 | 48 | File search | `docs/ROADMAP.md:189` | Busca local em repositório não implementada |
| P33-15 | 50 | Breadcrumb navigation | `docs/ROADMAP.md:191` | Navegação contextual ausente |
| P33-16 | 51 | File type icons | `docs/ROADMAP.md:192` | Sinalização visual de tipo de arquivo ausente |
| P33-17 | 52 | File size display | `docs/ROADMAP.md:193` | Metadado de arquivo não exibido |
| P33-18 | 53 | Last modified date | `docs/ROADMAP.md:194` | Metadado temporal de arquivo ausente |
| P33-19 | 54 | Commit info display | `docs/ROADMAP.md:195` | Contexto de histórico/autor não exibido |
| P33-20 | 55 | Branch selector | `docs/ROADMAP.md:196` | Seletor de referência Git ausente |
| P33-21 | 56 | Tag selector | `docs/ROADMAP.md:197` | Seletor de tags ausente |
| P33-22 | 57 | Repository metadata | `docs/ROADMAP.md:198` | Exposição de metadados do repositório incompleta |
| P33-23 | 59 | Device authorization flow | `docs/ROADMAP.md:227` | Fluxo OAuth Device Code incompleto |
| P33-24 | 61 | Fine-grained PAT support | `docs/ROADMAP.md:229` | Suporte a token granular ausente |
| P33-25 | 63 | Token refresh mechanism | `docs/ROADMAP.md:231` | Renovação segura de token não implementada |
| P33-26 | 64 | SSH key generation | `docs/ROADMAP.md:232` | Geração de chave SSH pendente no roadmap |
| P33-27 | 65 | SSH key management | `docs/ROADMAP.md:233` | Gestão de chave SSH pendente no roadmap |
| P33-28 | 66 | SSH agent integration | `docs/ROADMAP.md:234` | Integração com agente SSH ausente |
| P33-29 | 67 | Biometric authentication | `docs/ROADMAP.md:235` | Camada de autenticação biométrica ausente |
| P33-30 | 68 | Multi-account support | `docs/ROADMAP.md:236` | Suporte multi-conta incompleto |
| P33-31 | 69 | Account switching | `docs/ROADMAP.md:237` | Troca de conta não implementada |
| P33-32 | 70 | Session management | `docs/ROADMAP.md:238` | Gestão de sessão incompleta |
| P33-33 | 71 | Secure logout | `docs/ROADMAP.md:239` | Encerramento seguro de sessão incompleto |

## Regra para próximos commits

A partir deste baseline, cada commit de preenchimento deve referenciar explicitamente um ID desta lista no formato:

- `feat: implementa <descrição> (P33-XX)`
- `fix: corrige <descrição> (P33-XX)`

