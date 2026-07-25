# RafGitFS — Interface Compose V1

Estado: `IMPLEMENTED_SOURCE / STACKED_ON_PR_301 / READ_ONLY / CLAIM_ALLOWED=false`

## Fluxo

```text
RafGitFS launcher
→ StorageProfilesScreen
→ RepositoryStorageScreen
→ VirtualFileBrowserScreen
→ VirtualFileViewerScreen
→ StorageSettingsScreen
```

A interface está no mesmo APK do RafGitTools e também responde ao deep link:

```text
rafgittools://storage
```

Enquanto o Prompt 3 permanece em PR aberta, o Prompt 4 usa uma Activity própria para conservar a pilha auditável e evitar alterações concorrentes no `MainActivity` legado.

## Telas

### Perfis

- cria perfil local canônico quando ausente;
- `provider=GITHUB`;
- `accessMode=READ_ONLY`;
- `writePolicy=BLOCKED`;
- recibos obrigatórios;
- pausa/ativação somente local;
- acesso às configurações.

### Repositórios

- lista o catálogo Room;
- atualiza metadados pelo motor GitHub;
- pesquisa por nome, nome completo e descrição;
- mostra privacidade, branch padrão e linguagem;
- mantém metadados já observados quando não há rede.

### Navegador

- seleciona branch ou tag;
- navega por breadcrumbs;
- busca dentro do diretório atual;
- mostra tipo, tamanho e estado de cache;
- permite favoritos locais;
- observa filhos via `Flow` do Room;
- atualiza refs e árvore pelo indexador do Prompt 3.

### Visualizador

- lê conteúdo por blob SHA;
- texto UTF-8 selecionável quando plausível;
- prévia hexadecimal limitada para binários;
- mostra tamanho e SHA;
- não contém editor, upload, staging, commit ou push.

### Configurações

Permite apenas:

- `METADATA_ONLY`;
- `ON_DEMAND`;
- `SELECTIVE_OFFLINE` como política preparada para o Prompt 5;
- orçamento local entre 16 e 4096 MiB.

Ao salvar, as invariantes são reaplicadas:

```text
READ_ONLY
BLOCKED
receiptRequired=true
protectedBranchWrite=false
deleteEnabled=false
claimAllowed=false
```

## Estados visuais

```text
LOADING
OBSERVED
NOT_MODIFIED
TOKEN_VAZIO
RATE_LIMITED
ERROR
```

`TOKEN_VAZIO` e rate limit são exibidos ao usuário. Um valor parcial nunca recebe aparência de observação completa.

## Gates

```bash
python3 scripts/validate_rafgitfs_ui.py
python3 -m unittest tests/test_validate_rafgitfs_ui.py -v
./gradlew --no-daemon :app:compileDevDebugKotlin
./gradlew --no-daemon :app:testDevDebugUnitTest --tests '*RafGitFsUiPathsTest*'
```

O gate rejeita:

- endpoint ou chamada de mutação na UI;
- `claimAllowed=true`;
- remoção de `TOKEN_VAZIO`;
- ausência de breadcrumbs;
- Activity/deep link não registrado;
- remoção das invariantes do perfil.

## Limites

Ainda não fazem parte desta onda:

- download físico;
- cache de bytes;
- pin offline funcional;
- retomada;
- worker de sincronização;
- workspace;
- edição;
- branch, commit, push ou Pull Request via RafGitFS;
- validação em dispositivo real.

```yaml
prompt: 4/8
screens: 5
view_models: 5
launcher_activity: IMPLEMENTED_SOURCE
deep_link: IMPLEMENTED_SOURCE
remote_write_enabled: false
claim_allowed: false
android_execution: TOKEN_VAZIO
```

## Próximo passo

Prompt 5:

```text
conteúdo observado
→ arquivo temporário
→ checksum
→ catálogo content_cache
→ LRU
→ pin offline
→ retomada
→ remoção segura
```
