# RAFAELIA Navigator Private — Bootstrap V1

Camada local-first para indexar os JSONs estruturais já existentes no `NOVOexport` sem reler o corpus inteiro a cada pergunta.

## Invariantes

- fontes originais são somente leitura e nunca são movidas, renomeadas ou sobrescritas;
- cada arquivo-fonte fecha uma transação e um checkpoint append-only;
- preserva `conversation_id`, `message_id`, `node_id`, parent edges, papéis, datas, content types, assets e erros;
- para Codex preserva tarefa, repositório, branch, commit, PR, path e hash do diff quando presentes;
- todos os artefatos são `PRIVATE_DEFAULT_DENY` e `claim_allowed=false`;
- não há treino, gradiente, atualização de peso ou checkpoint de modelo.

## Auto teste sintético

```sh
python tools/rafaelia_navigator/rafaelia_navigator.py selftest
```

## Execução no Termux

```sh
python tools/rafaelia_navigator/rafaelia_navigator.py build \
  "$HOME/storage/shared/NOVOexport" \
  "$HOME/rafaelia-navigator-output"
```

Gate pequeno:

```sh
python tools/rafaelia_navigator/rafaelia_navigator.py build SOURCE OUTPUT --max-files 3
```

## Consulta local

```sh
python tools/rafaelia_navigator/rafaelia_navigator.py query \
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

Os segmentos `*.jsonl.txt` são a ponte de navegação rápida pelo Drive. O SQLite é o índice veloz para Termux, GAIA_phi e Rafaelia_Private.
