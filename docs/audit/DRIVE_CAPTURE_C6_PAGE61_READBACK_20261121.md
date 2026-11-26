# 𓂀ΔΦΩ — RAFAELIA C6 | Página 61, custódia e readback

**Data:** 2026-11-21. **Classificação:** receipt público limitado a métricas; não contém IDs de arquivos/pastas, nomes, conta, cursores, credenciais ou conteúdo privado.

## Autoridade, predecessor e escopo

- Produtor: `rafaelmeloreisnovo/RafGitTools`, branch isolada `rafcode/drive-parent-nullsafe-manifest-20261121-hold`, sob AGENTS.md do próprio repositório.
- Predecessor append-only: `docs/audit/DRIVE_CAPTURE_C5_PAGE60_AND_NULL_CONSUMER_20261121.md`, blob `f7de35ba39b9216db377c85aad424b2a9b380ef9`.
- Autoridade de inventário: AGENTS.md privado → START HERE Ω V2.2 → CURRENT_STATE_B0159, `B0159_COMMITTED_20261115`, status PARTIAL e `claim_complete=false`.
- Custódia privada: `raf_private.drive_folder_capture_pages_v1`; auditor de readback `raf_private.drive_folder_capture_scoped_quality_v1`.
- Escopo: apenas pastas acessíveis pela consulta padronizada do Google Drive; não equivale ao inventário de todos os arquivos.

## C6 / 6Σ-DMAIC: realização

1. **Define / Measure.** Antes da escrita, havia 61 páginas e 28.060 identidades distintas em cada captura (A/B), ambas não terminais. A página 60 tinha cursor de continuação, preservado em Supabase.
2. **Analyze / Falsificadores.** Usando cada cursor diretamente, a próxima página retornou 460 pastas; IDs válidos, MIME de pasta, datas e campos de pai tipados. A/B concordaram integralmente em IDs e metadados selecionados. Novas IDs não coincidiram com IDs armazenados anteriormente; duplicações intrapágina = 0.
3. **Improve / Escrever.** Um único lote append-only invocou `raf_private.drive_folder_capture_append_batch_v1` para as páginas de índice 61 das capturas A/B, protegidas por single-writer lease e fencing token **51**. Resposta SQL: `pages_added=2`; `positions_added=920`. Não foram substituídas observações preexistentes.
4. **Control / Readback.** O SQL confirmou, por página, 460 posições, 460 identidades distintas, hash SHA-256 do manifesto recomputável e `request_cursor` idêntico a `next_cursor` da página 60. O lease foi liberado.
5. **Auditor independente da aplicação.** Visão privada recomputou **62 páginas e 28.520 posições/identidades distintas por captura**, zero duplicações entre páginas, zero falhas de digest e zero rupturas de cursor. Ambos os estados = `CONSISTENT_PREFIX_NONTERMINAL`.
6. **Confronto transversal.** Interseção A∩B: 28.520 IDs; exclusivos de uma captura: 0; diferenças dos metadados guardados: 0. B0154: 2.310 identidades históricas contidas no prefixo atual, nenhuma faltante nesse cruzamento.

## Evidência, incerteza e prevenção de regressão

- **EVIDENCE_SCOPED:** páginas 61 de A/B e auditor privado sob ACL; schema privado não exposto a `anon`; a visão `security_invoker=true`. Não há `claim_allowed=true`.
- **F_gap / TOKEN_VAZIO_UTIL:** nenhuma captura é terminal; contagem total de pastas do provedor desconhecida; instantâneos transacionais e estabilidade temporal global não demonstrados.
- **F_gap / TOKEN_VAZIO(NOT_READ):** SHA-256 de bytes de arquivo não foi calculado; hashes aqui atestam somente manifestos JSON.
- **F_gap / NOT_PROVEN:** testes de CI do produtor/consumidor completo. Testes locais anteriores não constituem CI.
- **B0160:** BLOCKED; não houve atualização do CURRENT_STATE, nem promoção de checkpoint, nem merge.
- **Rollback:** só sucessor append-only documentando correção após evidência; conservar cadeias, pages 0–61, receipts e predecessoress.

## R3

`F_ok` = nova unidade de 920 observações guardada em duas páginas com readback, unicidade por captura e continuidade demonstradas.

`F_gap` = terminal, conteúdo original, estabilidade temporal, CI integral, equivalência com o inventário de todas as modalidades de arquivo.

`F_next` = avançar o próximo lote fechado sob novo lease somente se solicitado e os gates persistirem; encerrar a captura apenas após token de continuação terminal, reconciliação entre todos os IDs e receipt de estado. Manter `CLAIM_ALLOWED=false`.

**SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM.**

`CURRENT_STATE=B0159_COMMITTED` · `B0160=BLOCKED` · `CLAIM_ALLOWED=false`.
