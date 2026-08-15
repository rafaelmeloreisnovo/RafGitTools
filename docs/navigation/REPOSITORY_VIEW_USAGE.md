# RAFAELIA Repository View V1

Status: `IMPLEMENTED_LOCAL / STRUCTURE_ONLY / claim_allowed=false`

## Problema que resolve

A documentação manual tende a ficar atrás do código e arquivos podem permanecer tecnicamente presentes, porém difíceis de descobrir. O Repository View cria uma camada navegável e verificável entre a árvore física do repositório e a documentação humana.

Ele não substitui README, arquitetura, status ou documentação de domínio. Ele responde primeiro à pergunta estrutural: **o que existe, onde está, em qual diretório/subdiretório e quais regiões ainda não têm âncora local de navegação?**

## Saídas

Por padrão:

```text
docs/repository-map/
  INDEX.yml
  TREE.md
  RECEIPT.json
  DIRECTORIES_0001.yml
  FILES_0001_<grupo>_001.yml
  FILES_0002_<grupo>_001.yml
  ...
```

- `INDEX.yml`: entrada principal, estatísticas, grupos top-level, lista de shards e dívida documental estrutural.
- `TREE.md`: visão humana compacta.
- `DIRECTORIES_*.yml`: todos os diretórios/subdiretórios observados, shardados.
- `FILES_*.yml`: arquivos observados, shardados por primeiro componente do caminho.
- `RECEIPT.json`: recibo mínimo da geração.

## Execução

```bash
python3 scripts/navigation/repository_view.py \
  --root . \
  --output-dir docs/repository-map \
  --max-records-per-shard 5000
```

O gerador usa apenas Python stdlib. Se estiver dentro de um checkout Git, usa `git ls-files`, portanto o escopo normal é de **arquivos rastreados**. Fora de Git, cai para uma varredura recursiva de filesystem com exclusões mínimas de metadados/ferramentas.

## Semântica de dívida documental

Um diretório é marcado como `source_without_local_index=true` quando contém diretamente código/testes e não possui uma âncora local com nome semelhante a README/INDEX/MANIFEST/NAVIGATION/MAP/CATALOG/STATUS.

Isso é um **sinal de navegabilidade**, não uma condenação arquitetural. Um diretório pode estar corretamente documentado pelo pai; por isso o gerador não promove esse sinal a claim de erro.

Arquivos desconhecidos recebem:

```yaml
role_guess: unknown
semantic_role_state: TOKEN_VAZIO
```

A ausência de entendimento semântico nunca é convertida em conclusão automática.

## Repositórios muito grandes

O formato é shardado para evitar um único YAML monolítico. Ajuste:

```bash
--max-records-per-shard 10000
```

ou reduza para mapas menores. Cada shard permanece endereçável a partir de `INDEX.yml`.

## Fronteira de evidência

O mapa prova apenas presença estrutural observada no checkout e metadados locais básicos. Ele **não prova**:

- que um arquivo é usado em runtime;
- que uma função está correta;
- que um documento está semanticamente atualizado;
- que um artefato foi reproduzido;
- que um claim científico ou de performance é válido.

Esses pontos exigem relações código→docs→teste→execução→evidência separadas.

## Relação com o streaming inventory existente

`scripts/federation/streaming_inventory.py` continua sendo o scanner de custódia/bytes, com SHA-256 em streaming e checkpoints. O Repository View não o substitui: ele acrescenta **navegação por árvore, shards e candidatos de dívida documental**.

A separação é intencional:

```text
streaming_inventory = identidade/bytes/custódia
repository_view      = topologia/navegação/dívida documental estrutural
```

## Próximo gate

1. gerar mapa no próprio RafGitTools;
2. revisar falsos positivos de `source_without_local_index`;
3. adotar o mesmo contrato nos repositórios prioritários;
4. somente depois criar gate de CI que impeça documentação estrutural obsoleta.

F_gap atual: cobertura semântica arquivo→feature→documento ainda é `TOKEN_VAZIO` para os caminhos que não possuem relação explícita.
