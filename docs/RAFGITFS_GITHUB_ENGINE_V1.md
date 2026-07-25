# RafGitFS — Motor GitHub e Indexação V1

Estado: `IMPLEMENTED_SOURCE / READ_ONLY / CLAIM_ALLOWED=false`

## 1. Finalidade

O Prompt 3 conecta a fundação e o Room v6 ao GitHub sem transformar o navegador virtual em executor Git.

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

GitHub permanece a autoridade de conteúdo e versão. Room continua sendo índice reconstruível.

## 2. Superfície remota isolada

`RafGitFsGithubApiService` contém somente `GET`:

- `user/repos`;
- `repos/{owner}/{repo}/branches`;
- `repos/{owner}/{repo}/tags`;
- `repos/{owner}/{repo}/commits/{ref}`;
- `repos/{owner}/{repo}/git/trees/{treeSha}`;
- `repos/{owner}/{repo}/git/blobs/{blobSha}`;
- `search/code`.

Não existem `POST`, `PUT`, `PATCH` ou `DELETE` na interface RafGitFS.

A interface GitHub geral do aplicativo continua separada e não é injetada no indexador.

## 3. Estados de resposta

```text
Observed       resposta completa observada
NotModified    SHA remoto igual ao índice local
TokenVazio     evidência parcial ou ausente, com razão explícita
RateLimited    orçamento GitHub esgotado ou Retry-After
Failure        erro HTTP/rede classificado
```

`TokenVazio` pode carregar um valor parcial. Esse valor não pode ser promovido a árvore completa.

## 4. Paginação

A paginação:

- lê o cabeçalho `Link` e a relação `rel="next"`;
- usa até 100 itens por página;
- possui orçamento máximo de páginas;
- encerra normalmente quando não existe próximo link;
- retorna `PAGE_BUDGET_EXHAUSTED` quando a resposta excede o limite configurado.

Nenhum loop remoto é ilimitado.

## 5. Rate limit

São preservados:

- `X-RateLimit-Limit`;
- `X-RateLimit-Remaining`;
- `X-RateLimit-Used`;
- `X-RateLimit-Reset`;
- `X-RateLimit-Resource`;
- `Retry-After`;
- `X-GitHub-Request-Id`;
- `ETag`.

HTTP 403 com `remaining=0` e HTTP 429 viram `RateLimited`, não erro genérico.

## 6. Repositórios e refs

`refreshRepositories(profileId)` pagina os repositórios autenticados e atualiza o catálogo já existente `repository_name_cache`.

`refreshRefs(profileId, repo)` combina:

```text
branches + tags → repository_refs
```

A limpeza de refs antigas é delimitada por:

```text
profileId + repositoryFullName + lastIndexedAt
```

Uma atualização de um repositório não apaga refs de outro.

## 7. Indexação incremental

`refreshTree` executa:

```text
ref
→ resolve commit
→ observar commit SHA
→ comparar com repository_refs.gitSha
→ se igual e árvore local existe: NotModified
→ se diferente: buscar tree SHA recursiva
→ mapear entradas
→ preservar favoritos
→ upsert Room
→ remover somente entradas antigas do mesmo repo/ref
```

Tipos:

| Git | RafGitFS |
|---|---|
| `tree` / modo `040000` | `DIRECTORY` |
| `blob` | `FILE` |
| modo `120000` | `SYMLINK` |
| `commit` / modo `160000` | `SUBMODULE` |

Se a API retornar `truncated=true`, as entradas parciais podem ser indexadas, porém o resultado permanece `TOKEN_VAZIO` e `complete=false`.

## 8. Navegação e busca

A árvore Room permite:

- observar filhos por `profile/repo/ref/parentPath`;
- procurar uma entrada exata;
- contar entradas por ref;
- preservar caminhos favoritos;
- busca local por nome ou caminho;
- busca remota de código limitada ao repositório.

A busca remota respeita `incomplete_results`. Quando verdadeiro, o resultado é parcial e não conclusivo.

## 9. Conteúdo

A leitura usa:

```text
virtual_tree_entries.gitSha
→ Git Blob API
→ base64
→ limite de memória
→ verificação SHA declarado/observado
→ verificação de tamanho
→ snapshot em memória
```

Limite padrão:

```text
5 MiB por leitura em memória
```

O máximo configurável nesta camada é 50 MiB. Downloads físicos, retomada e arquivos grandes pertencem ao Prompt 5.

Conteúdo binário permanece em bytes. Texto UTF-8 só é produzido quando a amostra não apresenta sinais fortes de binário.

## 10. Invariantes

```text
GitHub = autoridade remota
Room = índice reconstruível
remote write = false
main write = false
truncated tree != complete tree
incomplete search != complete search
missing SHA = TOKEN_VAZIO
same commit SHA + indexed tree = NotModified
blob over memory budget = TOKEN_VAZIO
claim_allowed=false
```

## 11. Gates

### Estrutural

```bash
python3 scripts/validate_rafgitfs_github_engine.py
python3 -m unittest tests/test_validate_rafgitfs_github_engine.py -v
```

### JVM

```bash
./gradlew testDevDebugUnitTest --tests '*RafGitFsGithubEngineTest*'
```

### Build

```bash
./gradlew assembleDevDebug
```

## 12. Limites atuais

Ainda não existem:

- telas Compose RafGitFS;
- download físico e cache de bytes;
- pin offline funcional;
- worker de sincronização;
- edição e workspace operacional;
- branch, commit, push ou Pull Request via RafGitFS;
- recibo de execução em dispositivo Android.

## 13. Estado

```yaml
prompt: 3/8
read_only_api: IMPLEMENTED_SOURCE
repository_pagination: IMPLEMENTED_SOURCE
branch_tag_index: IMPLEMENTED_SOURCE
incremental_tree_by_sha: IMPLEMENTED_SOURCE
room_navigation: IMPLEMENTED_SOURCE
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

A interface deverá consumir `Flow` do Room e mostrar claramente `Observed`, `NotModified`, `RateLimited` e `TOKEN_VAZIO`.
