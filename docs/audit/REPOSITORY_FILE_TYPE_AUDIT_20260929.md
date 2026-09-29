# Auditoria de tipos de arquivo do RafGitTools

**Data:** 2026-09-29  
**Repositório:** `rafaelmeloreisnovo/RafGitTools`  
**Commit de referência observado:** `862b83547532d47e60eadf7810e0571b12ca8804` (merge do PR #584)  
**Estado:** `PARTIAL_METADATA_SCAN_WITH_CONTENT_VERIFIED_TEXT_CLASSIFICATIONS`  
**claim_allowed:** `false`

## Escopo e método

A leitura percorreu a raiz, as 45 pastas de primeiro nível, e aprofundou 82 das 93 subpastas de segundo nível descobertas. Foram catalogados 1.065 caminhos de arquivos distintos após remover prefixos de pasta duplicados devolvidos pelo leitor. O recorte contém 53 arquivos na raiz, 700 arquivos diretamente nas pastas de primeiro nível e 312 arquivos diretamente nas 82 pastas de segundo nível lidas.

A janela de leitura do repositório limitou a varredura a 150 chamadas por hora. Duas pastas restantes foram tentadas depois do limite e não retornaram conteúdo: `receipts/t7` e `scripts/t7`. Nove outras subpastas de segundo nível ficaram para uma próxima passagem: `examples/context-bundle-v2`, `examples/human-ai-middleware`, `examples/rafgitfs`, `examples/rafyml`, `examples/semantic-context-exam`, `fixtures/coherence_ruler_gate`, `fixtures/cross_repo_impact`, `fixtures/memory_epoch` e `tests/fixtures`. As 82 pastas lidas revelaram ainda 52 pastas de terceiro nível; elas não foram percorridas. Portanto, estes números são cobertura observada, não uma contagem completa do repositório.

A inspeção foi de nomes, tamanhos, primeiras linhas e conteúdo textual exposto pelo leitor. Arquivos binários não foram baixados nem verificados por assinatura/magic bytes. Nenhum arquivo foi renomeado ou alterado.

## Inventário observado por sufixo

| Sufixo / classe de nome | Arquivos |
|---|---:|
| `.md` | 342 |
| `.json` | 224 |
| `.py` | 206 |
| `.yml` | 53 |
| `.txt` | 47 |
| `.sh` | 38 |
| `.h` | 29 |
| `.c` | 28 |
| `.kt` | 17 |
| Sem sufixo | 16 |
| `.s` | 15 |
| `.zip` | 14 |
| Dotfiles sem sufixo | 3 |
| `.gradle`, `.html`, `.gz`, `.mk` | 3 cada |
| `.yaml`, `.properties`, `.asm`, `.js` | 2 cada |
| `.bat`, `.mfd`, `.cff`, `.bib`, `.csv`, `.client`, `.java`, `.pro`, `.tsv`, `.css`, `.lock`, `.jar`, `.manifest` | 1 cada |

## Arquivos sem sufixo identificados

Os nomes abaixo foram verificados como texto (licença, script ou Makefile), exceto `_upcoming/1`, que contém somente uma quebra de linha e é um placeholder vazio. “Texto” descreve o conteúdo observado; a auditoria não altera nomes nem acrescenta `.txt`.

- Raiz: `COPYING`, `LICENSE`, `Makefile`, `gradlew`.
- `Livro/LICENSE`.
- Makefiles: `BrowserRaf/internal/Makefile`, `native/energy_cascade_l0_v1/Makefile`, `native/knowledge_campus_l0_v1/Makefile`, `native/rafcode_federation_v1/Makefile`, `native/rafcode_route_exec_v1/Makefile`, `native/rafcode_route_federation_bridge_v1/Makefile`, `native/rafcode_route_v1/Makefile`, `native/silicon_light_v1/Makefile`, `rafaelia/block1/Makefile`, `rafaelia/omega_hybrid/Makefile`.
- Placeholder: `_upcoming/1` (1 byte, newline).

## Sufixos não usuais com conteúdo textual confirmado

| Sufixo | Caminho observado | Classificação pelo conteúdo |
|---|---|---|
| `.mfd` | `sss.mfd` | Texto UTF-8 com prosa e trecho de script shell; não é tratado como binário pelo conteúdo observado. |
| `.client` | `_incoming/Makefile.client` | Texto de Makefile/build. |
| `.manifest` | `native/rafaelia_omega_v32/rafaelia_omega_v32.reference.manifest` | Texto de metadados chave/valor. |
| `.pro` | `app/proguard-rules.pro` | Texto de regras de configuração ProGuard. |
| `.cff`, `.bib`, `.tsv` | `Livro/CITATION.cff`, `Livro/paper.bib`, `configs/lineage-stage-2.tsv` | Texto de metadados bibliográficos/tabelares. |
| `.mk` | 3 arquivos observados | Texto de regras de build Make. |
| `.s`, `.asm` | 17 arquivos observados | Texto de código Assembly. |
| `.bat` | 1 arquivo observado | Script de texto para Windows. |

Os três arquivos ocultos sem sufixo observados, `IaCopiler/.new`, `_incoming/.keep` e o dotfile de raiz `.gitignore`, são marcadores/configuração em texto; `.new` e `.keep` contêm apenas uma quebra de linha. `Livro/.zenodo.json` é JSON textual e foi contado em `.json`.

Sufixos de pacote observados: `.zip` (14), `.gz` (3) e `.jar` (1). Foram classificados apenas pelo nome nesta passagem; sua assinatura e conteúdo binário não foram verificados.

## Próxima passagem necessária

1. Repetir a leitura após a janela de limite para `receipts/t7` e `scripts/t7`.
2. Ler as nove subpastas listadas no escopo e, em seguida, os 52 diretórios de terceiro nível.
3. Gerar inventário recursivo no próprio RafGitTools/Android para as árvores de usuário, com classificação por nome, MIME informado pelo SAF e assinatura/conteúdo somente quando o usuário autorizar a leitura daquele arquivo.
4. Atualizar este recibo com cobertura e hashes reproduzíveis antes de declarar o inventário completo.

O processamento NOVOexport do telefone é independente desta auditoria do repositório. PR #584 foi mesclado em `862b835`; na captura desta auditoria, o workflow exato do head `65c2323` ainda aparecia `queued` (run `36567876002`). Merge não é evidência de CI aprovado. Não há receipt de instalação/canário no telefone nem de processamento do corpus.
