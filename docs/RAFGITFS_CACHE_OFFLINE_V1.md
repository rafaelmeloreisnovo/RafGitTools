# RafGitFS — Cache e Offline V1

Status: `IMPLEMENTED_SOURCE / CLAIM_ALLOWED=false`

## Objetivo

Disponibilizar conteúdo GitHub sob demanda no armazenamento privado do APK, com integridade verificável, limite por perfil, fixação offline e retomada persistente.

```text
metadado Room
→ blob GitHub read-only
→ hash do objeto Git
→ arquivo .part privado
→ fsync
→ rename atômico
→ SHA-256 local
→ registro content_cache
→ projeção de estado na árvore virtual
```

## Estados

```text
REMOTE_ONLY
METADATA_CACHED
PARTIAL
CONTENT_CACHED
PINNED_OFFLINE
STALE
CORRUPTED
```

`PARTIAL`, `STALE` e `CORRUPTED` nunca são servidos como cópia offline completa.

## Armazenamento

O conteúdo fica exclusivamente em:

```text
context.filesDir/rafgitfs-cache-v1/
```

A chave local é SHA-256 da identidade canônica:

```text
profileId + repository + ref + path + blobSha
```

O caminho remoto não é usado diretamente como caminho físico. O resolvedor usa `canonicalPath` e bloqueia escape da raiz.

## Integridade dupla

Antes da persistência:

1. tamanho observado deve coincidir com os bytes;
2. o hash do objeto Git é recalculado como `digest("blob <size>\0" + conteúdo)`;
3. o arquivo final recebe SHA-256 local;
4. toda leitura offline recalcula SHA-256 e o hash Git.

Divergência implica `CORRUPTED` e `TOKEN_VAZIO`.

## Escrita atômica

```text
arquivo.part
→ write
→ flush
→ fd.sync
→ rename para arquivo final
```

Uma cópia parcial não recebe `CONTENT_CACHED` nem `PINNED_OFFLINE`.

## LRU e orçamento

- `maxCacheBytes` vem do perfil Room;
- arquivo individual: máximo de 50 MiB nesta V1;
- itens não fixados podem expirar após sete dias;
- LRU remove os mais antigos não fixados;
- `pinned=true` nunca é candidato de expulsão;
- se os itens fixados ocuparem todo o orçamento, o novo download retorna `CACHE_BUDGET_EXHAUSTED`.

## Fixação offline

```text
CONTENT_CACHED → PINNED_OFFLINE
```

Fixar remove a expiração. Para apagar uma cópia fixada é obrigatório:

```text
unpin → remove local
```

## Gerações e staleness

Se o mesmo caminho passa a apontar para outro blob SHA, as gerações anteriores são marcadas `STALE`. A identidade atual continua vinculada ao SHA observado na árvore.

## Fila e retomada

A fila usa a tabela existente `transfer_jobs`:

```text
CACHE_DOWNLOAD | PIN_OFFLINE
QUEUED_OFFLINE
PAUSED
EXECUTING
COMPLETE | FAILED
```

A retomada é manual e persistente. Ela pode ser chamada após o retorno da rede ou após reiniciar o aplicativo.

### Limite explícito

A Git Blob API usada nesta camada entrega o blob integral em base64. Portanto, a V1 **não declara retomada HTTP Range**. Uma tentativa interrompida é repetida integralmente sobre arquivo temporário e só é promovida após verificação completa.

## Interface

O visualizador oferece:

- `Cache now`;
- `Read offline`;
- `Pin offline` / `Unpin`;
- `Queue pin`;
- `Resume queue`;
- `Remove local` somente quando não fixado.

Essas operações modificam apenas o cache local. Não há upload, commit, push, PR ou exclusão remota.

## Gates

```bash
python3 scripts/validate_rafgitfs_cache_offline.py
python3 -m unittest tests/test_validate_rafgitfs_cache_offline.py -v
./gradlew --no-daemon :app:testDevDebugUnitTest --tests '*RafGitFsCacheCoreTest*'
./gradlew --no-daemon :app:compileDevDebugKotlin
```

## Limites honestos

```yaml
background_worker: TOKEN_VAZIO
http_range_resume: false
android_device_execution: TOKEN_VAZIO
remote_ci_pass: TOKEN_VAZIO
remote_write_enabled: false
claim_allowed: false
```

O Prompt 6 será responsável pela engine governada de jobs, fases, aprovação, cancelamento e recibos. Esta onda implementa apenas a fila local segura de cache/offline.
