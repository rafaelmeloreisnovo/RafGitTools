# RafGitFS — Motor GitHub e Indexação V1

Estado: `IMPLEMENTED_SOURCE / READ_ONLY / CLAIM_ALLOWED=false`

## 1. Fluxo

```text
perfil local
→ API GitHub somente leitura
→ paginação e rate limit
→ repositórios
→ branches/tags
→ commit SHA e tree SHA
→ árvore virtual Room
→ blob por SHA
```

GitHub permanece a autoridade. Room é índice reconstruível.

## 2. API isolada

`RafGitFsGithubApiService` contém somente `GET`:

- `user/repos`;
- branches e tags;
- resolução de commit;
- Git Trees;
- Git Blobs;
- busca de código.

Não existem `POST`, `PUT`, `PATCH` ou `DELETE`. A API GitHub geral do aplicativo não é injetada no indexador RafGitFS.

## 3. Estados

```text
Observed       resposta completa
NotModified    snapshot completo do mesmo commit já indexado
TokenVazio     evidência parcial/ausente, com razão
RateLimited    orçamento remoto esgotado
Failure        erro HTTP/rede classificado
```

Um `TokenVazio` pode transportar dados parciais, mas nunca recebe `complete=true`.

## 4. Paginação e rate limit

- até 100 itens por página;
- cabeçalho `Link` com `rel="next"`;
- orçamento máximo de páginas;
- `PAGE_BUDGET_EXHAUSTED` quando o limite local é alcançado;
- leitura de `X-RateLimit-*`, `Retry-After`, `X-GitHub-Request-Id` e `ETag`;
- HTTP 403 com `remaining=0` e HTTP 429 tornam-se `RateLimited`.

Nenhum loop remoto é ilimitado.

## 5. Repositórios e refs

```text
refreshRepositories → repository_name_cache
refreshRefs          → repository_refs
```

Branches e tags são combinadas. A remoção de refs antigas só ocorre quando ambas as listas são completas e é delimitada por `profileId + repositoryFullName`.

## 6. Snapshot completo da árvore

`repository_refs.gitSha` representa a cabeça remota observada. Ele não prova, sozinho, que a árvore daquele commit foi indexada completamente.

Para separar esses fatos, o Room mantém uma entrada interna escondida:

```text
virtual_tree_entries.path = ""
gitSha = commit completamente indexado
mimeType = application/x-rafgitfs-index-snapshot
```

Consultas de navegação e busca excluem `path=''`.

Fluxo incremental:

```text
resolver commit remoto
→ ler getIndexedCommitSha()
→ se igual: NotModified
→ se diferente: buscar tree SHA
→ mapear árvore
→ se completa:
     gravar snapshot interno
     remover registros antigos
     atualizar ref
→ se truncada:
     preservar registros anteriores
     não atualizar snapshot
     não limpar dados ausentes
     TOKEN_VAZIO
```

Isso impede que uma árvore parcial ou uma simples atualização de refs gere falso `NotModified`.

## 7. Tipos

| Git | RafGitFS |
|---|---|
| `tree` / `040000` | `DIRECTORY` |
| `blob` | `FILE` |
| `120000` | `SYMLINK` |
| `commit` / `160000` | `SUBMODULE` |

Favoritos são preservados durante reindexação.

## 8. Navegação e busca

- filhos por `profile/repo/ref/parentPath`;
- entrada exata por caminho;
- contagem de entradas reais, excluindo o marcador interno;
- busca local por nome/caminho;
- busca remota limitada a um repositório;
- `incomplete_results=true` vira `TOKEN_VAZIO` parcial.

## 9. Conteúdo

```text
arquivo indexado
→ blob SHA
→ Git Blob API
→ base64
→ limite de memória
→ verificação SHA e tamanho
→ snapshot em memória
```

Limite padrão: **5 MiB**. Limite máximo desta camada: **50 MiB**. Download físico, retomada e arquivos grandes ficam para o Prompt 5.

## 10. Invariantes

```text
GitHub = autoridade remota
Room = índice reconstruível
remote write = false
main write = false
truncated tree != complete tree
remote ref head != indexed tree snapshot
missing SHA = TOKEN_VAZIO
blob over budget = TOKEN_VAZIO
claim_allowed=false
```

## 11. Gates

```bash
python3 scripts/validate_rafgitfs_github_engine.py
python3 -m unittest tests/test_validate_rafgitfs_github_engine.py -v
./gradlew testDevDebugUnitTest --tests '*RafGitFsGithubEngineTest*'
./gradlew assembleDevDebug
```

## 12. Limites

Ainda não existem telas Compose, download físico, pin offline funcional, worker de sincronização, edição, branch, commit, push ou Pull Request pelo RafGitFS. Execução Android permanece `TOKEN_VAZIO` até recibo observável.

## 13. Estado

```yaml
prompt: 3/8
read_only_api: IMPLEMENTED_SOURCE
repository_pagination: IMPLEMENTED_SOURCE
branch_tag_index: IMPLEMENTED_SOURCE
complete_snapshot_sha: IMPLEMENTED_SOURCE
truncated_tree_fail_closed: IMPLEMENTED_SOURCE
bounded_blob_read: IMPLEMENTED_SOURCE
remote_write_enabled: false
claim_allowed: false
android_execution: TOKEN_VAZIO
```

## 14. Próximo passo

Prompt 4:

```text
StorageProfilesScreen
→ RepositoryStorageScreen
→ VirtualFileBrowserScreen
→ VirtualFileViewerScreen
→ StorageSettingsScreen
```
