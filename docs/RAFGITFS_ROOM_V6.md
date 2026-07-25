# RafGitFS — Persistência Room V6

Estado: `IMPLEMENTED_SOURCE / EXECUTION_TOKEN_VAZIO / CLAIM_ALLOWED=false`

## 1. Finalidade

O Prompt 2 materializa a persistência local do RafGitFS sem transformar o banco do celular em autoridade remota.

```text
GitHub = autoridade de conteúdo e versão
Room   = índice local reconstruível + catálogo de cache + fila + recibos
```

A migração é aditiva:

```text
CacheDatabase 5 → 6
```

Nenhuma tabela anterior é removida ou renomeada.

## 2. Tabelas

| Tabela | Responsabilidade | Autoridade |
|---|---|---|
| `storage_profiles` | Perfis GitHub e políticas locais | configuração local |
| `repository_refs` | branches/tags e SHA observado | cache reconstruível |
| `virtual_tree_entries` | árvore virtual por repo/ref/path | cache reconstruível |
| `content_cache` | catálogo de conteúdo disponível localmente | cache local |
| `workspaces` | áreas de trabalho governadas | estado local |
| `transfer_jobs` | ciclo SCAN→RECEIPT e progresso | estado operacional |
| `staged_operations` | operações planejadas ainda não executadas | estado local |
| `sync_conflicts` | divergências de SHA e resolução explícita | estado auditável |
| `operation_receipts` | recibos finais append-only | evidência local |

## 3. Relações

Os estados reconstruíveis pertencentes a um perfil usam relacionamento com `storage_profiles` e `ON DELETE CASCADE`:

- refs;
- árvore virtual;
- cache;
- workspaces;
- jobs.

Os recibos não possuem cascade. Excluir um perfil local não apaga o histórico de operações já registrado.

## 4. Invariantes

```text
Room != GitHub
cache hit != SHA remoto atual
DELETE profile != DELETE repository
receipt update/delete API = ausente
requestId de recibo = único
pinned offline != candidato LRU
credenciais/tokens != campos Room
claim_allowed=false
```

### Segurança de credenciais

Nenhuma entidade contém:

- token OAuth;
- PAT;
- senha;
- private key;
- refresh token;
- segredo bruto.

A autenticação continua sob a camada segura já existente do aplicativo.

## 5. Cache delimitado

`RafGitFsCacheMaintenance` implementa limpeza transacional:

```text
remover expirados não-pinned
→ medir bytes atuais
→ selecionar LRU não-pinned
→ remover até o orçamento
→ retornar paths para limpeza física pelo chamador
```

A classe nunca remove uma entrada `pinned=true`.

O banco remove somente o registro. A exclusão do arquivo físico deve ocorrer fora da transação Room e somente para os paths retornados.

## 6. DAOs

A base expõe nove DAOs:

1. `StorageProfileDao`;
2. `RepositoryRefDao`;
3. `VirtualTreeDao`;
4. `ContentCacheDao`;
5. `WorkspaceDao`;
6. `TransferJobDao`;
7. `StagedOperationDao`;
8. `SyncConflictDao`;
9. `OperationReceiptDao`.

Todos são disponibilizados pelo `CacheModule` do Hilt.

`OperationReceiptDao` expõe somente:

- append com conflito `ABORT`;
- leitura por request;
- observação dos recibos recentes.

Não há método de atualização ou exclusão.

## 7. Testes

### Gate estrutural sem dependências

```bash
python3 scripts/validate_rafgitfs_room_v6.py
python3 -m unittest tests/test_validate_rafgitfs_room_v6.py -v
```

O gate verifica:

- versão 6;
- registro da migração no Hilt;
- nove entidades e nove tabelas SQL;
- nove acessores e providers DAO;
- recibos append-only;
- cache pinned não evictável;
- ausência de campos de segredo;
- índices de request único e LRU.

### Instrumentação Android

```bash
./gradlew connectedDevDebugAndroidTest
```

Os testes Android cobrem:

- abertura de banco versão 5;
- migração física para versão 6;
- existência das tabelas e índices;
- cascade de estado reconstruível;
- unicidade de `requestId`;
- comportamento LRU;
- preservação de recibo após exclusão de perfil.

## 8. Limites

Ainda não existem nesta onda:

- ingestão real da API GitHub;
- indexador de árvore;
- download físico;
- tela Compose;
- worker de sincronização;
- workspace funcional;
- commit, push ou pull request;
- execução comprovada em aparelho.

Esses limites permanecem `TOKEN_VAZIO` até execução observável.

## 9. Estado

```yaml
prompt: 2/8
database_version: 6
migration: 5_to_6
tables_added: 9
dao_interfaces: 9
remote_write_enabled: false
receipt_mutation_enabled: false
claim_allowed: false
android_execution: TOKEN_VAZIO
```

## 10. Próximo passo

Prompt 3:

```text
GitHub API
→ paginação
→ refs
→ árvore virtual
→ conteúdo
→ atualização incremental por SHA
→ Room v6
```

Nenhum conteúdo remoto deve ser marcado `OBSERVED` sem resposta GitHub e SHA correspondente.
