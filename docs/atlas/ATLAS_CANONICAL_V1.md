# RAFAELIA Atlas V1 - ponteiros e memória reconstruível
Estado: SOURCE_CANDIDATE / L0_HOST_TESTED / APK_CI_PENDING / DEVICE_TOKEN_VAZIO / claim_allowed=false.
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM; TOKEN_VAZIO != 0.
Autoridade: RafGitTools = código Android/CI; Drive = fontes, apontadores, receipts; Mapa = rotas semânticas federadas.
Overlay em NOVOexport/001_PONTEIRO_RAFGITTOOLS_ATLAS_V1:
00_DISPATCH / 01_HUMAN / 02_AI_METADATA / 03_ROUTES_AND_RELATIONS / 04_RECEIPTS / 05_GAPS.
Apontar para NOVOexport 00_INDEX..09_CONVERSATION_CHUNKS existentes, sem duplicar/mover/apagar corpus.
ID=(provider, access-scope, fileId ou repo/ref/path/blob, observed_revision). Facetas: nome, prefixo, sufixo, MIME, tamanho, assunto, tempo, privacidade, proveniência e estado.
Mesmo tamanho/nome => só POSSIBLE_DUPLICATE; igualdade requer identidade ou hash de conteúdo verificado. Hash tem algoritmo, escopo e fonte. Unknown != zero.
Arestas: part_of, alias_of, routes_to, derived_from, evidences, cites, supersedes, contradicts, co_occurs_with. Coocorrência != causa.
Diretório IA é formato para leitor IA, NÃO controle de acesso. ACL Drive independente. Não publicar paths/IDs privados no GitHub.
Packet humano/IA: intent|roots<=3|id|relations|source_refs|privacy|evidence|gaps|next|rollback.
L0 C -> L1 JNI -> L2 SAF local -> L3 CI debug -> L4 provider Drive/GitHub FUTURO autorizado.
Conteúdo de arquivos, ZIP/DAT, dedup de bytes, leitura Drive/GitHub remota, treinamento e publicação = TOKEN_VAZIO_NOT_RUN.
R3: F_ok=arquitetura+L0 local; F_gap=CI/Android/Drive; F_next=exact-head CI, piloto SAF inocente, revisão e readback.


## S4 — contraexemplos e cobertura epistemicamente limitada
- Um provedor SAF pode devolver muitas linhas inválidas ou IDs repetidos antes de completar 5.000 registros válidos. Foram introduzidos dois limites independentes: até 6.000 linhas lidas e até 5.000 registros aceitos; nomes, IDs, MIME, caminho e profundidade também são limitados. Exceder limite registra `truncated=true` e `PARTIAL_OR_AMBIGUOUS`.
- IDs documentais repetidos entram como `duplicate_document_ids`, **não** como igualdade de bytes nem como deduplicação comprovada.
- Cada registro contém `parent_document_id`; o recibo inclui `root_document_id` e autoridade do provedor. Esses campos privados só devem sair do aparelho mediante exportação explícita. Não são identidade global do Google Drive.
- `complete=false` é intencional: mesmo concluir a enumeração observável não garante cobertura integral do provedor/corpus. `enumeration_finished` descreve apenas término sem erro reportado. `coverage_state=PROVIDER_ENUMERATED_UNVERIFIED` jamais equivale a prova de completude.
- `entries_sha256` refere-se somente às linhas UTF-8 JSONL anteriores ao receipt, conforme `hash_scope`. Não comprova SHA dos arquivos de origem ou autenticidade de assinatura.
- Gate host: 21 testes negativos Java sem dependências/Android; CI job 10Z compila o helper, roda fixtures e compila o APK. Contenção de provedor travado, execução física Android, restauração semântica e revisão independente permanecem `TOKEN_VAZIO_NOT_RUN`.
- Esta revisão preserva o diretório NOVOexport original e não autoriza publicação/treinamento/merge. Rollback = regressar à revisão anterior no PR ou fechar a branch DRAFT.
