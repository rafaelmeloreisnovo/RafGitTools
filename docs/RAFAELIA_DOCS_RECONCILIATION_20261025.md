# RAFAELIA — Reconciliação documental de RafGitTools — 2026-10-25

> **Censo completo da raiz:** [arquivos soltos e todos os diretórios de primeiro nível](RAFAELIA_ROOT_CENSUS_20261025.md) — contagem por pasta, SHA Git e locais sem índice. Este mapa complementa o inventário recursivo `tools/` ou `scripts/`; não autoriza exclusões.


> **DRAFT / HOLD · Não promove claims, releases, versões nem direitos.** Documento adicional, não substitui predecessores. A única verdade do inventário de arquivos aqui é a árvore Git no HEAD exato descrito abaixo; o conteúdo operacional exige revisão específica.

## Corte e escopo

- Projeto: `rafaelmeloreisnovo/RafGitTools`.
- HEAD ancestral: `93711b96e70d22881991178edff59d84a2dc983f`.
- Árvore Git completa: **1.961 arquivos, 468 diretórios**, `truncated=false`.
- Documentos/textos por extensão: **474**; isto inclui documentação potencial, não necessariamente páginas publicadas.
- Diretórios raiz: **48**, dos quais **43 sem README/INDEX/FILES_MAP local direto** (critério heurístico, não prova de orfandade).
- Escopo de interesse `tools/`: **63 arquivos, 24 subdiretórios** — são arquivos, não aplicativos únicos.
- Função técnica: controle de repositórios, Android, proveniência, automações e corpus.

## Divergência já observada

README apresenta corte documental 2026-09-28 e HEAD de referência anterior ao snapshot atual; não transferir status de sucesso entre commits.

O anterior não será apagado, renomeado ou sobrescrito silenciosamente. Quando houver conflito, prevalecem **fonte versionada → teste do SHA → recibo verificável**, não um selo de "completo" herdado de documento antigo.

## Roteamento atualizado

| Referência existente (verificada no snapshot) | Papel |
| --- | --- |
| [`README.md`](../README.md) | guia ou índice histórico |
| [`docs/INDEX.md`](../docs/INDEX.md) | guia ou índice histórico |
| [`docs/CODE_TO_DOC_MAP.md`](../docs/CODE_TO_DOC_MAP.md) | fonte ou contrato em versão Git |
| [`docs/RAFGITTOOLS_CURRENT_STATE.md`](../docs/RAFGITTOOLS_CURRENT_STATE.md) | fonte ou contrato em versão Git |
| [`tools/rafandroid/README.md`](../tools/rafandroid/README.md) | guia ou índice histórico |
| [`tools/rafymlc/rafymlc.py`](../tools/rafymlc/rafymlc.py) | fonte ou contrato em versão Git |
| [`tools/termuxforge/forge.sh`](../tools/termuxforge/forge.sh) | fonte ou contrato em versão Git |
| [`tools/rafaelia_navigator/README.md`](../tools/rafaelia_navigator/README.md) | guia ou índice histórico |
| [`tools/forensic_git_provenance/forensic_git.py`](../tools/forensic_git_provenance/forensic_git.py) | fonte ou contrato em versão Git |
| [`tools/corpus_logistics/conversation_relation_vector_lowlevel_v1.py`](../tools/corpus_logistics/conversation_relation_vector_lowlevel_v1.py) | fonte ou contrato em versão Git |

**Áreas prioritárias de classificação e dono:** `BrowserRaf/`, `COMPILER/`, `freestanding/`, `native/`, `contracts/`, `tools/`, `scripts/`, `federation/`, `receipts/`, `audit/`. Estes caminhos são agrupamentos propostos, sem mudança física na árvore.

## Pastas sem índice local (amostra para triagem)

`COMPILER/`, `IaCopiler/`, `contracts/`, `artifacts/`, `audit/`, `federation/`, `freestanding/`, `native/`, `scripts/`, `tools/termuxforge/`, `tools/rafymlc/`.

Um diretório listado não é automaticamente redundante, abandonado ou livre para remoção. Identificar dono, consumidores, licença e predecessores antes de qualquer reorganização.

## Formato mínimo para cada arquivo / aplicação

`PATH | GIT_BLOB_SHA | PRODUCER | CLASS | CAPABILITY | INPUT | OUTPUT | SIDE_EFFECT | DEPENDENCIES | LICENSE | TEST_SHA | EVIDENCE | OWNER_ROLE | GAP | ROLLBACK`.

Estados: `OBSERVED_SOURCE`, `DOCUMENTED`, `TESTED_SCOPED`, `RUNTIME_PROVEN`, `TOKEN_VAZIO_NOT_TESTED`, `SUPERSEDED`. Ausência não é zero; script listado não equivale a execução.

## Refatoração sem regressão

1. Inventário recursivo pinado ao HEAD e com preservação do blob SHA (não copiar dados privados na documentação pública).
2. Verificar se referências relativas apontam para caminhos realmente existentes; documentos históricos com links quebrados recebem registro de divergência.
3. Priorizar pastas sem índice local de acordo com impacto, não apenas ordem alfabética.
4. Criar `README` por diretório funcional somente após leitura de fontes e consumidores; não inventar interfaces ou capacidades.
5. Separar o que é código próprio, upstream e wrapper, preservando licenças e direitos de terceiros.
6. Atualizar índice de entrada por ponteiro para o documento sucessor; nunca apagar históricos, artefatos autorais ou branches.
7. Exigir revisão humana, CI exata e gate do provedor para autorizar merge; `claim_allowed=false` até a evidência aplicável.

## Falsificadores e fechamento R3

- **F_ok**: árvore recursiva consultada em HEAD exato; alvos da tabela de links checados na árvore base.
- **F_gap**: auditoria semântica total, teste de build/ABI/hardware, checagem exaustiva de links históricos, inventário jurídico e proteção de merge ainda NÃO realizados.
- **F_next**: link-checker negativo (alvo faltante), manifesto canônico por arquivo, revisão de licenças e prioridade P0 das pastas operacionais.
- **Rollback**: reverter somente commits de documentação do PR; preservar todos os predecessores.

## Sucessor S2 — capacidade operacional e índice local (2026-10-25)

- [Índice funcional confirmado](../tools/rafymlc/README.md): funções, entradas/saídas documentadas e riscos, sem execução presumida.
- [Verificador determinístico](../tools/doc_ref_integrity_gate.py), [testes negativos](../tools/test_doc_ref_integrity_gate.py) e [workflow de CI](../.github/workflows-legacy-20260907/rafaelia-docs-integrity-v1.yml).
- O gate fornece estado JSON (PASS_SCOPED/FAIL_SCOPED) para links Markdown locais e censo de diretórios raiz sem índice direto. É **heurístico**, não prova de abandono, segurança, autoria ou certificação; referências HTML/anchors fora do escopo.
- Verificação depende de execução hospedada no SHA exato; proteção do provedor e revisão humana continuam obrigatórias. `claim_allowed=false` e `PROMOTION=HOLD`.


## Sucessor S7 — integração no orquestrador (2026-10-27)

- O fluxo S2 acima foi preservado como snapshot histórico de SHA Git `6a1bffe03ed4cd0f5075ec94f6cd59bd994e8cfe`; seu caminho original não é mais ativo.
- A execução atual dos mesmos testes e do link-checker está no job `05Z · Docs Integrity` de [START.yml](../.github/workflows/START.yml), com SHA exato, recibo final e modo `docs` manual.
- Resultado de S7: somente os checks de CI observados em SHA exato autorizam um estado `EVIDENCED_SCOPED`; até lá, `TOKEN_VAZIO_NOT_RUN` / `HOLD`. Não herdar automaticamente o PASS do S2.

## Sucessor: contrato e esquema — 27/10/2026

> **DRAFT/HOLD · Fonte pinada:** [Inventário de contratos, JSON Schema, autoridade e consumidores](../contracts/README.md). Superfícies contratuais não são provas de execução, certificação, compatibilidade sem adaptador ou autorização humana. Documentos de 25/10 preservados.
