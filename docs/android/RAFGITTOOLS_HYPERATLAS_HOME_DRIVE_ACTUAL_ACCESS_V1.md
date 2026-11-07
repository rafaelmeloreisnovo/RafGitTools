# RafGitTools Ω HyperAtlas — função real na aba Drive (V1)

**Produto:** app Android RafGitTools, não um script avulso e não um aplicativo concorrente.
**Rota:** Home → Drive → HyperAtlas → seletor Android/Google Drive → pasta autorizada → prévia → escolher destino privado → salvar índice → leitura de volta SHA-256.

## Executável já ligado ao fluxo do aplicativo

- `app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeScreen.kt` exibe `DriveFirstRunIndexCard()` na aba Drive.
- `ui/screens/setupwizard/DriveFirstRunIndexCard.kt` usa `ActivityResultContracts.OpenDocumentTree` para selecionar uma árvore acessível pelo provedor SAF e `CreateDocument` para salvar o mapa, sem receber ou expor senha/token Google.
- `navigator/NovoexportSafInventory.kt` faz a leitura recursiva **só de metadados** dos diretórios e arquivos da árvore selecionada. Reutilizado, não substituído.
- `setupwizard/DriveMetadataIndexGate.kt` classifica nomes `.zip`, `.zipraf`, `.zip.001`, `.zip.t0.zip.004`, `.z01`, `.tgz/.tar.gz`, preserva tamanhos `Long?`, paths e relações `CONTAINS`, e nunca declara conteúdo interno verificado. O mapa mantém `archive_candidates`, quantidades e estado explícito `TOKEN_VAZIO_ARCHIVE_MEMBERS_NOT_READ`.
- O índice é gravado no destino **explicitamente escolhido**, não nos diretórios de código GitHub. A leitura de volta recalcula SHA-256. Os bytes dos arquivos grandes originais não passam pelo cache local do aparelho nessa função.

## Organização GitHub — acesso já disponível

- Home usa `GithubOwnerPicker` com conta e organização separadas.
- `GithubApiService` tem `GET orgs/{organization}/repos` com o PAT autenticado pelo próprio app.
- `GithubOrganizationInventory.requireScopedOwner` rejeita repositórios fora do proprietário selecionado.
- Para `instituto-Rafael` (Organization), escolher no seletor de proprietário ou consultar a organização explicitamente. Isso mostra apenas repositórios permitidos ao PAT; não importa credenciais do ChatGPT e não alega acesso aos privados não autorizados.

## Limites reais — não declarar implementação inexistente

- Acesso SAF depende do provedor Google Drive visível no seletor Android e da concessão feita pelo usuário. **A autorização do ChatGPT ao Drive não é herdada pelo APK.**
- Scan metadata da tela HyperAtlas usa `DriveMetadataIndexGate.MAX_DOCUMENTS=5000` e índice `MAX_INDEX_BYTES=2 MiB`. Acima disso, a operação bloqueia com estado tipado em vez de afirmar inventário completo; paginação real e fragmentação serão necessárias.
- Para ZIPRAF de 4–20 GB, a função identifica path/tamanho por metadados sem download. Ela **não** lê diretório central ZIP64, não reconstitui partes e não verifica CRC. Isso demanda adaptador de range autenticado ou FD comprovadamente seekable, sem spool integral no telefone.
- Mapear a seleção SAF não é a mesma coisa que inventariar o Google Drive inteiro; um único diretório selecionado define o escopo da autorização.
- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`, `claim_allowed=false`; build APK e teste físico no Motorola continuam pendentes até resultado terminal.

## Gates / próximos incrementos

1. Executar e inspecionar testes `DriveMetadataIndexGateTest` e compilação Gradle da PR exact-head; impedir regressões do SetupWizard e da Home.
2. Testar no Android real o seletor do Drive, pastas com ZIPs de muitos gigabytes e o readback SHA-256 de um índice pequeno, sem abrir os arquivos originais.
3. Adicionar paginação/particionamento por volume de metadados, checkpoints de destino e reconciliação de fontes sob permissão, mantendo o celular como thin client.
4. Só depois integrar leitura parcial ZIP64/ZIPRAF e mapas internos, respeitando formato/seekability/ACL e recusando downloads integrais involuntários.
5. Backup GitHub → Drive é um fluxo separado, sob aprovação e restauração verificável, não um efeito colateral do mapeamento.

Rollback: PR em Draft/HOLD, somente arquivos aditivos e alterações localizadas de UI/gate; nenhuma mudança em ZIPs, Drive original ou branch principal.