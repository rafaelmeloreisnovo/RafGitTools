# RafGitTools · Primeira tela Drive/Google e índice de conhecimento V1

**Escopo implementado na PR:** reutiliza o assistente de configuração existente e o scanner `NovoexportSafInventory`; não cria novo APK/repo/login nem solicita permissão global. Preserva as telas GitHub/Drive/Local mostradas pelo usuário.

## O que foi observado nas telas e no código

- `GitHub (111)`: o aplicativo observou 111 repositórios na sessão, com READ e um receipt WRITE para Issue #751. Isso não comprova que os 111 foram indexados em conteúdo.
- `Local (0)`: nenhum repositório local indexado; receipt privado 138 B com SHA-256. Não significa nenhum arquivo local existente.
- `Drive`: seletor SAF do Android, sem login Google próprio; o provedor disponível determina as contas e pastas que pode selecionar.
- `Raf Actions`: tela lista workflows. Botões desabilitados durante a leitura de estado e/ou por gate não são evidência de permissões suficientes.
- O manifest inclui várias Activities com MAIN/LAUNCHER (Raf Setup, Actions, Lens, Benchmark, etc.). **Isso gera diversos ícones do mesmo APK**, além das possíveis aplicações distintas `dev/debug` e `production` (applicationIdSuffix); não concluir "vários APKs" apenas pela quantidade de ícones.

## Entrega em código sem regressão

`SetupWizardScreen` exibe `DriveFirstRunIndexCard` na **primeira página**:
1. Botão Android para selecionar uma árvore SAF, que pode pertencer a uma conta Google Drive se o provedor a expuser. Não representa Google OAuth.
2. Scanner existente lê somente metadados da árvore explicitamente escolhida, com limite de 5.000 documentos. Mostra contagem de arquivos/pastas, candidatos `conversations*.json`/`codex*.json` e tamanhos desconhecidos.
3. O usuário marca que conferiu a privacidade do destino e toca em `Salvar índice no Drive`. O seletor Android cria novo JSON; o app verifica leitura de volta e SHA-256. Não copia conteúdo nem envia à CI, GitHub ou backend.
4. Manifesto contém `path`, `parent`, `mime`, `size_bytes`, candidatos e arestas `CONTAINS`. Omite URI, ID interno do documento, tokens, identidade da conta e conteúdo. Os próprios nomes de arquivo podem ser pessoais; conferir a pasta de destino é obrigatório.
5. Erros ou truncamento são `TOKEN_VAZIO`; um limite atingido não vira inventário total.

**OBS:** a fonte do índice ainda é seleção SAF no celular. A exportação vai ao provedor destino pelo Android e requer interação física do usuário. A execução cloud-only Drive→GitHub→Drive continua sendo outro subsistema, com as fronteiras de autoridade já estabelecidas.

## Evolução desejada, NÃO implementada aqui

### Escolher uma ou várias contas Google por login OAuth

Um login Google verdadeiramente integrado requer Google Identity Services/Credential Manager e Drive API, projeto Google Cloud, identificação Android SHA/appId, consentimento explícito por conta e política de escopos. `drive.metadata.readonly` permite metadados de arquivos, sujeito a verificação/restrições do Google; `drive.file` é mais estreito para arquivos usados pelo app. Não reutilizar PAT GitHub, não inventar `CLIENT_ID`, não solicitar `drive.readonly` para ler tudo sem necessidade/prova. O seletor SAF atual não oferece implicitamente acesso a todos os arquivos das contas.

Para indexar todos os diretórios, com paginação e retomada, seguir por conta: `files.list` paginado, `nextPageToken`, checkpoint por conta/snapshot, IDs protegidos, quota/backoff, controle de alterações, distinção `sharedWithMe`, Team Drives, lixeira, versões, shortcuts e arquivos sem caminho. Dar ao usuário exclusão e revogação de origem. Condições de corrida e permissões podem invalidar a contagem; `TOTAL_UNKNOWN` permanece válido.

### Corpo semântico das conversas / grafo

O repositório já contém `ConversationManifoldProcessor` (JSON streaming), `NovoexportConversationCorpus` e `ConversationManifoldPublication`. Eles exigem autorização separada para abrir o conteúdo. Indexar nomes e relações de pastas não significa ter as mensagens, árvores causais, semântica ou grafos validados. Proposta: separar camadas `L0 metadata → L1 estrutura de JSON → L2 arestas tipadas → L3 pesquisa/claims`, mantendo `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.

**Raf Tools Paint** é melhor tratado como **visualização do grafo com seleção, anotação e rotas**, aproveitando os módulos atuais. Não criar outro editor/repositório antes de provar uma necessidade produtora independente.

## Estados que devem bloquear claims

- `TOKEN_VAZIO_GLOBAL_DRIVE_ACCESS`: nenhuma integração OAuth de conta inteira foi implementada.
- `TOKEN_VAZIO_FULL_COVERAGE`: não representar uma árvore SAF como todos os arquivos do Drive.
- `TOKEN_VAZIO_CONTENT_EDGES`: apenas relações `CONTAINS` de estrutura, nenhuma semântica de conversa.
- `TOKEN_VAZIO_ANDROID_DEVICE`: compilar em CI não prova experiência física nos provedores Google Drive de Android 10/14.
- `TOKEN_VAZIO_LAUNCHER_NORMALIZATION`: manifest ainda cria vários atalhos de launcher; normalização exige escolha para não remover os atalhos que podem ser úteis.
- `TOKEN_VAZIO_PROVIDER_RETENTION`: Google Drive SAF não equivale a armazenamento imutável ou retenção legal.

## Teste terminal e rollback

CI do produtor: compilar Java/Kotlin, executar `DriveMetadataIndexGateTest` (propriedades, não vazamento de URI/documentId, hash determinístico, travessia, cap de documentos) e sem regressões do gate existente. Teste físico necessário: usar o seletor Android com Google Drive real, escolher conta e pasta, exportar ao Drive não compartilhado, comparar bytes/hashes, trocar de conta e verificar isolamento.

Rollback: fechar a PR, preservar `main` e os arquivos anteriores. Fonte SAF não é alterada.

R3 = <primeira tela integrada e índice seguro proposta/testável, OAuth total e aparelho não comprovados, CI exato + revisão de permissão antes de merge>.
