# RafGitTools · HyperAtlas V3 — índice compacto e navegável

**Produto produtor:** [RafGitTools](https://github.com/rafaelmeloreisnovo/RafGitTools).  
**Entrada Android:** Home → Drive → HyperAtlas, usando o provedor SAF/Google Drive **já presente no aparelho**.  
**Custódia e navegação longitudinal:** Drive privado \`001_PONTEIRO_RAFGITTOOLS_ATLAS_V1\`.  
**GitHub:** fontes e código; \`instituto-Rafael\` é **Organization**, não \`User\`. Seus repositórios continuam sob a autenticação GitHub já implementada no app.

## Mudança funcional

A tela mantém o botão legado **Salvar mapa** (JSON v2), mas ganha a ação principal **Salvar HyperAtlas otimizado no Drive (índice fragmentado)**. Uma seleção de pasta de destino cria um novo diretório \`HYPERATLAS_V3_<uuid>\` no provedor Android autorizado, sem mover ou copiar as fontes:

\`\`\`text
HYPERATLAS_V3_<run-id>/
├── index-00001.jsonl
├── index-00002.jsonl
├── …
└── MANIFEST.json       # commit lógico: publicado somente após SHA-256 de cada shard
\`\`\`

As entradas JSONL são **um objeto por caminho**, ordenado, com \`type\` (\`FILE\`/\`DIRECTORY\`), \`path\`, \`parent\`, \`mime\`, \`size_bytes\` (null se desconhecido), \`archive_kind\`, tokens/máscaras semânticos e estado do pai. A relação \`CONTAINS\` decorre de \`parent → path\`, sem duplicar caminhos em múltiplos arrays.

O manifesto inclui contagens de arquivos/pastas/ZIP candidatos, totais de bytes conhecidos, contagem de tamanhos desconhecidos, SHA-256 por shard, SHA-256 do índice concatenado, bytes escritos, \`source_bytes_read=false\`, \`archive_members_read=false\`, \`claim_allowed=false\` e cobertura estritamente \`USER_SELECTED_SAF_TREE_ONLY\`.

**Reconstrução:** verificar cada shard pelo digest do manifesto; concatenar \`index-00001.jsonl\`, \`index-00002.jsonl\` … por ordinal, verificar SHA-256 global, então criar árvore lógica/gráfico por \`parent\`. Não inferir bytes ausentes nem conteúdo interno do ZIP. \`MANIFEST.json\` ausente = execução interrompida/sem commit, não \`PASS\`.

## Reuso e limites

- Fonte: \`NovoexportSafInventory.scan\` já acessa o provedor pelo SAF com autorização humana. **Não baixa ZIPs** para criar o mapa.
- Índice legado \`DriveMetadataIndexGate.build\` permanece intacto como fallback de compatibilidade. \`classifyArchive\` usa regex e MIME sets pré-compilados.
- V3: shard padrão **256 KiB**, máximo **512 KiB**; máximo 256 shards e 32 MiB totais de índice, em memória apenas o shard de saída atual além do inventário já existente.
- **Limite de varredura atual: 5.000 documentos** na tela de setup. A indexação fragmentada elimina o teto de 2 MiB do *formato de saída*, mas **não** autoriza declarar uma árvore maior que o scanner atual consiga enumerar. Paginação da fonte ainda exige nova implementação.
- Documento fonte, IDs de capacidade e URIs de acesso não são incluídos no índice. Metadados de nomes de arquivos ainda podem ser sensíveis: escolher pasta **privada** no destino.
- Em falha de escrita/readback, um diretório parcial pode permanecer. Ele **não** recebe \`MANIFEST.json\`, não é sobrescrito nem apagado automaticamente.
- Para ZIP64 de 4–20 GB, o indexador V3 registra apenas nomes/tamanhos dos arquivos **contêineres**. Entradas internas exigem o leitor ZIP64 posicionável e transporte Drive HTTP Range autenticado, ainda separado e não validado em E2E.
- Os repositórios privados da organização são mapeados via \`GithubOwnerPicker\` / GitHub API autenticada; um índice Drive não substitui a autorização GitHub.

## Gates verificáveis

1. Testes: \`DriveMetadataIndexGateTest\`, \`DriveAtlasCompactIndexV3Test\` e \`ZipCentralDirectoryRangeV1Test\`.
2. CI de compilação Android no SHA exato, não apenas compilação de commit anterior.
3. E2E no Motorola: selecionar pasta pequena, publicar shards, readback SHA-256, repetir com destino sem escrita e com conexão interrompida. Não alegar PASS até executar.
4. Depois: scanner paginado/cursor independente, Range 206/ETag para ZIPs remotos e cópia GitHub → Drive com restore test.

**Rollback:** PR DRAFT/HOLD sem merge; código anterior e arquivo v2 preservados. Não apagar ZIPs nem repositórios.

\`SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM\`  
\`TOKEN_VAZIO ≠ 0\` · \`claim_allowed=false\`
