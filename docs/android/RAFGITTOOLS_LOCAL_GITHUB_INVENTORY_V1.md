# RafGitTools Local — inventário do GitHub para humanos e IAs

**Autoridade de fonte:** GitHub remoto. **Destino:** armazenamento privado do APK Android, sem clone. **Escopo desta mudança:** metadados de repositórios que o usuário já carregou na tela GitHub.

## Por que Local (0) não significa GitHub vazio

A contagem `Local (0)` vinha de repositórios **Git clonados no aparelho**. A contagem `GitHub (111)` correspondia à lista remota exibida naquela sessão. O catálogo local não é um clone e não altera esse cadastro de clones. A aba foi renomeada para `Local`; as duas populações aparecem separadamente.

## Implementação real — sem novo APK nem outro indexador concorrente

- `LocalGithubInventoryStore` cria um snapshot JSON versionado somente depois do toque do usuário. Salva em `Context.filesDir/github-inventory/`, sujeito a `android:allowBackup=false` do manifesto existente.
- Cada arquivo inclui lista observada, proprietário, descrição curta, público/privado, linguagem, branch padrão, fork, estrelas, forks, contagem *informada* de issues e atualização. Não inclui PAT, token, URL de clone/SSH, blobs, mensagens de conversas ou IDs/URIs de Drive.
- `HomeViewModel` marca separadamente a conclusão real da paginação. Se a API interromper a leitura ou atingir o limite de páginas sem saber se terminou, grava `TOKEN_VAZIO_PARTIAL` — nunca afirma cobertura de todas as contas ou organizações.
- O snapshot guarda campos explicitamente não observados: arquivos, workflows, checks, dependências. Também lista facetas por auditar: commits, branches, tags, PRs, conteúdo de issues, releases, segurança, licença, proteção da main e testes independentes.
- Arquivos são append-only sob limite de 2 MiB/snapshot, 5000 repos e 128 snapshots. Sem limpeza/deleção automática. Escreve via arquivo temporário, `fsync`, `rename`, relê e compara SHA-256. O hash detecta corrupção casual, **não cria armazenamento imutável nem fornece certificação independente**.
- A tela `Local` permite salvar, buscar offline por nome/linguagem/descrição, mostrar públicos/privados/recorte de resultados, navegar para o **RafGitFS já existente** (Room: repositórios, refs, árvores, busca).
- O export JSON é **opcional e separado**, exige declaração de destino privado e compara bytes/SHA-256 via provedor Android na leitura de volta. Não há sincronização silenciosa com Google Drive ou GitHub.

### Mapa do que existe e do que falta

| Camada | Fonte/produtor | Estado neste snapshot |
|---|---|---|
| L0 | GitHub API da conta autenticada | LISTA_OBSERVADA, com flag de paginação |
| L1 | GitHub branches, tags, arquivos | Já há indexador RafGitFS; não incluídos no snapshot resumido |
| L2 | GitHub CI, checks, PRs, releases, protection | `TOKEN_VAZIO_NOT_SCANNED` — não declarar PASS |
| L3 | Conversas, semântica, relações tipadas | Requer fontes e autorização independentes |
| L4 | Riscos/custódia/claims | Evidência e aprovações externas não presumidas |

A separação facilita leitura por IA com JSON e navegação humana com busca. O índice é um mapa de fontes e desconhecidos, não substitui inspeção do código exato nem o trabalho do Atlas/Mapa.

## Segurança e riscos

Metadados de repositórios privados também são sensíveis. O salvamento local exige confirmação explícita; na exportação, uma segunda confirmação. O app não cifra este snapshot com uma chave independente do sistema: a proteção atual depende do sandbox Android, política `allowBackup=false` e proteção física do aparelho. Não publicar o índice em repositório público. Android/desinstalação pode apagar os arquivos do sandbox; a cópia externa é opcional e sob sua escolha.

`Complete` significa apenas que a paginação da lista remota terminou no momento observado; **não** significa que todos os repositórios existentes no mundo/organização foram descobertos, nem que seus arquivos ou checks foram auditados.

## Testes e rollback

- `LocalGithubInventoryStoreTest`: nenhuma credencial/clone URL, metadados públicos/privados, duplicados, bloqueio de nomes inseguros, intervalo parcial, SHA-256 determinístico.
- `DriveMetadataIndexGateTest`: etapa SAF opcional preservada.
- CI do produtor: Android devDebug assemble + JVM unit tests + lint e demais gates aplicáveis, no HEAD exato. Compilação não comprova testes físicos, especialmente depois de uma alteração de fonte.
- Aceite físico: abrir APK → GitHub autenticado → aba Local → salvar → pesquisar offline/sem rede → reiniciar → reler hash → exportar pela SAF para pasta privada → verificar sem conteúdo confidencial indevido → testar mudança de identidade.
- Rollback: fechar PR, `main` intacta. Não apagar os snapshots produzidos por usuários após instalação sem permissão específica.

**R3:** F_ok = catálogo local observável em UI, fonte/testes e readback implementados; F_gap = CI exata após mudança, prova física, inventário completo de refs/checks e resistência a adulteração por ator privilegiado; F_next = validar na CI e no Android antes de merge/release.
