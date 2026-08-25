# RAFAELIA Navigator Private — Bootstrap V1.2

Camada local-first para indexar os JSONs estruturais já existentes no `NOVOexport` sem reler o corpus inteiro a cada pergunta.

## Invariantes

- fontes originais são somente leitura e nunca são movidas, renomeadas ou sobrescritas;
- cada arquivo-fonte fecha uma transação e um checkpoint append-only;
- preserva todos os nós do `mapping`, inclusive nós sem mensagem;
- preserva `conversation_id`, `message_id`, `node_id`, parent edges, papéis, datas, content types, assets e erros;
- para Codex preserva tarefa, repositório, branch, commit, PR, path e hash do diff quando presentes;
- fonte previamente indexada com hash diferente é bloqueada como alteração da origem imutável;
- FTS é populado em lote por shard, não por mensagem;
- a publicação no Drive é reconstruída somente das tabelas SQLite commitadas, em diretório novo e vazio;
- todos os artefatos são `PRIVATE_DEFAULT_DENY` e `claim_allowed=false`;
- não há treino, gradiente, atualização de peso ou checkpoint de modelo.

## Auto testes sintéticos

```sh
python tools/rafaelia_navigator/rafaelia_navigator.py selftest
cd tools/rafaelia_navigator
python rafaelia_navigator_integrity_v1.py selftest
python rafaelia_navigator_bulk_v1.py selftest
python rafaelia_navigator_publish_v1.py selftest
```

## Execução canônica no Termux

Use a rota V1.2 com grafo integral e FTS em lote:

```sh
cd "$HOME/RafGitTools/tools/rafaelia_navigator"
python rafaelia_navigator_bulk_v1.py build \
  "$HOME/storage/shared/NOVOexport" \
  "$HOME/rafaelia-navigator-output"
```

Gate pequeno:

```sh
python rafaelia_navigator_bulk_v1.py build SOURCE OUTPUT --max-files 3
```

## Publicação privada e determinística

Nunca publique diretamente os segmentos provisórios produzidos durante a ingestão. Reconstrua uma fotografia limpa a partir do banco commitado:

```sh
python rafaelia_navigator_publish_v1.py publish \
  "$HOME/rafaelia-navigator-output/RAFAELIA_NAVIGATOR.sqlite3" \
  "$HOME/rafaelia-navigator-publication-v1"
```

O destino precisa estar vazio. A saída inclui `SOURCES`, `CONVERSATIONS`, `NODES`, `MESSAGES`, `CODEX`, `ASSETS`, manifesto, hashes e Merkle root.

## Consulta local

```sh
python rafaelia_navigator_bulk_v1.py query \
  OUTPUT/RAFAELIA_NAVIGATOR.sqlite3 \
  '"termux" OR "rafcodephi"'
```

## Saídas privadas

- `NAVIGATOR_ROOT.md`
- `MANIFEST.json`
- `COVERAGE.csv`
- `RAFAELIA_NAVIGATOR.sqlite3`
- `DRIVE_SEARCH_INDEX/MESSAGES-*.jsonl.txt`
- `DRIVE_SEARCH_INDEX/CODEX-*.jsonl.txt`
- `DRIVE_SEARCH_INDEX/SOURCES-*.jsonl.txt`
- `RECEIPTS/CHECKPOINTS.jsonl`
- publicação limpa: `SOURCES`, `CONVERSATIONS`, `NODES`, `MESSAGES`, `CODEX`, `ASSETS` e `PUBLICATION_MANIFEST.json`.

Os segmentos `*.jsonl.txt` são a ponte de navegação rápida pelo Drive. O SQLite é o índice veloz para Termux, GAIA_phi e Rafaelia_Private. O Drive recebe somente saídas privadas e compactas; o GitHub recebe código, testes e receipts sem corpos privados.
