# Contrato canônico do `runtime-lock.json`

## Finalidade

O `runtime-lock.json` é a fonte de verdade para **proveniência de código-fonte** usada pelo RafGitTools. Ele não deve ser interpretado como prova de que artefatos foram compilados, instalados ou executados.

O contrato separa duas promoções:

1. **SOURCE_LOCKED** — todas as dependências têm branch declarada e commit SHA-1 concreto de 40 caracteres;
2. **ARTIFACT_VERIFIED** — manifests e bundles produzidos pelo build têm SHA-256 concreto e foram verificados por um gate de promoção.

Enquanto os artefatos ainda não foram produzidos, seus hashes permanecem `TOKEN_VAZIO`. Isso é permitido no build de desenvolvimento, mas é rejeitado quando o validador recebe `--require-artifact-hashes`.

## Autoridade por camada

| Campo | Autoridade | Regra |
|---|---|---|
| `integration_repository` | RafGitTools | aparece uma única vez; usa `commit: SELF` para evitar autorreferência impossível |
| `repositories[].commit` | lock de fonte | nunca pode ser branch flutuante ou `TOKEN_VAZIO` |
| `repositories[].branch` | origem humana | precisa corresponder à branch real de desenvolvimento |
| `expected_hashes` | gate de artefato | `TOKEN_VAZIO` antes do build; SHA-256 concreto na promoção |
| `platform.abis` | contrato Android | ordem canônica: `arm64-v8a`, `armeabi-v7a` |

## Repositórios obrigatórios

```text
rafaelmeloreisnovo/termux-app-rafacodephi       @ master
rafaelmeloreisnovo/CONVERSATIONS_CHUNKS_PRIVATE @ main
rafaelmeloreisnovo/llamaRafaelia                 @ master
rafaelmeloreisnovo/RafPolimata                   @ main
```

O próprio RafGitTools fica em `integration_repository` e não pode ser duplicado na lista de dependências. Seu commit é `SELF` no lock; o SHA concreto é capturado no manifesto por `GITHUB_SHA` ou `git rev-parse HEAD`.

## Validação

Build de fonte:

```bash
python3 scripts/runtime_lock_contract.py validate runtime-lock.json
```

Gate de promoção:

```bash
python3 scripts/runtime_lock_contract.py validate \
  runtime-lock.json \
  --require-artifact-hashes
```

Consulta segura de um campo, após validar o contrato inteiro:

```bash
python3 scripts/runtime_lock_contract.py get \
  runtime-lock.json \
  rafaelmeloreisnovo/llamaRafaelia \
  commit
```

## Invariantes

```text
nome único
+ integrador SELF sem recursão
+ branch canônica
+ commit concreto nas dependências
+ parser único
+ manifesto derivado do mesmo lock
= proveniência reproduzível
```

Uma mudança de dependência deve ocorrer por PR explícito e precisa atualizar o commit fixado. Nenhum workflow deve manter um parser paralelo com outro formato de schema.
