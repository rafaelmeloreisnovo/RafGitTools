# PENDING 33 ITEMS (Baseline revisado)

## Origem dos “33 lugares”

A origem adotada para os **33 lugares** é o recorte dos **primeiros 33 itens marcados como `🔴 L1`** no roadmap oficial de desenvolvimento.

- Arquivo-fonte: `docs/ROADMAP.md`
- Critério: linhas de tabela no formato `| <id> | <feature> | 🔴 L1 | ... |`
- Objetivo: transformar lacunas de implementação em backlog rastreável com ID único para commits futuros.

## Nota de manutenção (critério desta revisão)

Esta revisão reclassifica cada item com base em evidências explícitas nos arquivos:

- `app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt`
- `app/src/main/kotlin/com/rafgittools/core/security/SshKeyManager.kt`
- `docs/STATUS_REPORT.md`

Critério aplicado por item:

- **Implementado**: há função/classe funcional direta cobrindo o requisito principal.
- **Parcial**: existe base funcional relevante, porém com escopo incompleto, integração ausente ou sem garantia de fluxo fim a fim.
- **Não iniciado**: não foi encontrada evidência direta nesses arquivos para o requisito.

## Lista rastreável (reclassificada com evidências)

| Item ID | Feature ID | Feature (ROADMAP) | Origem (arquivo:linha) | Classificação | Lacuna atualizada | Evidência (arquivo + função/classe) |
|---|---:|---|---|---|---|---|
| P33-01 | 20 | Git clone (shallow) | `docs/ROADMAP.md:131` | **Implementado** | Sem lacuna funcional principal identificada neste recorte. | `JGitService.cloneShallow(...)` |
| P33-02 | 21 | Git clone (single branch) | `docs/ROADMAP.md:132` | **Implementado** | Sem lacuna funcional principal identificada neste recorte. | `JGitService.cloneSingleBranch(...)` |
| P33-03 | 22 | Git clone (with submodules) | `docs/ROADMAP.md:133` | **Implementado** | Sem lacuna funcional principal identificada neste recorte. | `JGitService.cloneWithSubmodules(...)` |
| P33-04 | 24 | Git commit (amend) | `docs/ROADMAP.md:135` | **Parcial** | Commit padrão está implementado, mas não há evidência explícita de `--amend` no serviço analisado. | `JGitService.commit(...)` |
| P33-05 | 25 | Interactive staging | `docs/ROADMAP.md:136` | **Parcial** | Há staging/unstaging por arquivo; seleção interativa por hunk/linha não aparece nestes arquivos. | `JGitService.stageFiles(...)`, `JGitService.unstageFiles(...)` |
| P33-06 | 29 | Force push with lease | `docs/ROADMAP.md:140` | **Implementado** | Sem lacuna funcional principal identificada neste recorte. | `JGitService.forcePushWithLease(...)` |
| P33-07 | 30 | Pull with rebase | `docs/ROADMAP.md:141` | **Parcial** | Pull e rebase existem separadamente; falta evidência de fluxo integrado de pull com rebase em uma única operação. | `JGitService.pull(...)`, `JGitService.rebase(...)` |
| P33-08 | 33 | Branch rename | `docs/ROADMAP.md:144` | **Implementado** | Sem lacuna funcional principal identificada neste recorte. | `JGitService.renameBranch(...)` |
| P33-09 | 36 | Merge strategies | `docs/ROADMAP.md:147` | **Parcial** | Merge básico implementado; estratégias avançadas/configuráveis não são expostas no serviço analisado. | `JGitService.merge(...)` |
| P33-10 | 40 | Stash operations | `docs/ROADMAP.md:151` | **Implementado** | Sem lacuna funcional principal identificada neste recorte. | `JGitService.listStashes(...)`, `stash(...)`, `stashApply(...)`, `stashPop(...)`, `stashDrop(...)`, `stashClear(...)` |
| P33-11 | 42 | Git config management | `docs/ROADMAP.md:153` | **Parcial** | Há manipulação pontual de config para remotes; gestão ampla de config Git não está evidenciada. | `JGitService.addRemote(...)` |
| P33-12 | 46 | Syntax highlighting | `docs/ROADMAP.md:187` | **Parcial** | Há detecção de linguagem como base para highlight; renderização completa de highlight não está comprovada por estes arquivos. | `JGitService.getFileContent(...)`, `detectLanguage(...)`; `STATUS_REPORT.md` (File Browser/Diff Viewer) |
| P33-13 | 47 | Line numbers | `docs/ROADMAP.md:188` | **Parcial** | Existem bases de dados por linha em diff/blame; ausência de evidência explícita de exibição consistente em visualização de arquivo. | `JGitService.getDiff(...)`, `parseDiffHunks(...)`, `blame(...)` |
| P33-14 | 48 | File search | `docs/ROADMAP.md:189` | **Não iniciado** | Navegação/listagem de arquivos existe, mas não foi encontrada busca textual/estrutural de arquivos neste recorte. | `JGitService.listFiles(...)` (sem rotina de search dedicada) |
| P33-15 | 50 | Breadcrumb navigation | `docs/ROADMAP.md:191` | **Parcial** | Estrutura de path em browser existe; construção de breadcrumb de UI não aparece explicitamente nestes arquivos. | `JGitService.listFiles(...)`; `STATUS_REPORT.md` (File Browser) |
| P33-16 | 51 | File type icons | `docs/ROADMAP.md:192` | **Parcial** | Há base de classificação por linguagem/extensão, útil para ícones; mapeamento visual de ícones não está neste recorte. | `JGitService.detectLanguage(...)` |
| P33-17 | 52 | File size display | `docs/ROADMAP.md:193` | **Implementado** | Sem lacuna funcional principal no backend de metadados de tamanho. | `JGitService.listFiles(...)` (campo `size`) |
| P33-18 | 53 | Last modified date | `docs/ROADMAP.md:194` | **Não iniciado** | Não há campo direto de última modificação por arquivo na listagem analisada. | `JGitService.listFiles(...)` (sem `lastModified`) |
| P33-19 | 54 | Commit info display | `docs/ROADMAP.md:195` | **Implementado** | Sem lacuna funcional principal identificada para obtenção de histórico/autor. | `JGitService.getCommits(...)`; `STATUS_REPORT.md` (Commit List) |
| P33-20 | 55 | Branch selector | `docs/ROADMAP.md:196` | **Parcial** | Dados de branches e tela de listagem existem; controle específico de seletor de referência não está explícito neste recorte. | `JGitService.getBranches(...)`; `STATUS_REPORT.md` (Branch List) |
| P33-21 | 56 | Tag selector | `docs/ROADMAP.md:197` | **Parcial** | Listagem/gestão de tags existe; seletor dedicado de tag não está explícito neste recorte. | `JGitService.listTags(...)`; `STATUS_REPORT.md` (Tag List) |
| P33-22 | 57 | Repository metadata | `docs/ROADMAP.md:198` | **Parcial** | Metadados relevantes existem (nome, path, branch, remote), mas cobertura de metadados “completa” pode exigir ampliação de campos. | `JGitService.cloneRepository(...)`, `getRemotes(...)`, `getStatus(...)` |
| P33-23 | 59 | Device authorization flow | `docs/ROADMAP.md:227` | **Parcial** | Há autenticação OAuth/Token no status global, porém fluxo Device Code não é evidenciado diretamente nos arquivos comparados. | `STATUS_REPORT.md` (Auth: Login OAuth/Token) |
| P33-24 | 61 | Fine-grained PAT support | `docs/ROADMAP.md:229` | **Parcial** | Existe validação/uso de credenciais PAT; não há evidência explícita de fluxos específicos para PAT fine-grained. | `STATUS_REPORT.md` (Credential validation PAT/username); `JGitService` (Credentials.Token) |
| P33-25 | 63 | Token refresh mechanism | `docs/ROADMAP.md:231` | **Não iniciado** | Não foi encontrada evidência direta de mecanismo de refresh de token nos arquivos comparados. | Sem função/classe específica encontrada neste recorte |
| P33-26 | 64 | SSH key generation | `docs/ROADMAP.md:232` | **Implementado** | Sem lacuna funcional principal identificada neste recorte. | `SshKeyManager.generateKeyPair(...)` |
| P33-27 | 65 | SSH key management | `docs/ROADMAP.md:233` | **Implementado** | Sem lacuna funcional principal identificada neste recorte. | `SshKeyManager.listKeys(...)`, `importKey(...)`, `exportPublicKey(...)`, `deleteKey(...)` |
| P33-28 | 66 | SSH agent integration | `docs/ROADMAP.md:234` | **Implementado** | Sem lacuna funcional principal identificada neste recorte para integração nas operações Git suportadas. | `JGitService.createSshTransportCallback(...)`; `STATUS_REPORT.md` (SSH Agent) |
| P33-29 | 67 | Biometric authentication | `docs/ROADMAP.md:235` | **Implementado** | Sem lacuna funcional principal identificada neste recorte documental. | `STATUS_REPORT.md` (Biometric authentication completo) |
| P33-30 | 68 | Multi-account support | `docs/ROADMAP.md:236` | **Não iniciado** | Não foi encontrada evidência direta de suporte multi-conta nesses três arquivos. | Sem função/classe específica encontrada neste recorte |
| P33-31 | 69 | Account switching | `docs/ROADMAP.md:237` | **Não iniciado** | Não foi encontrada evidência direta de troca de conta nesses três arquivos. | Sem função/classe específica encontrada neste recorte |
| P33-32 | 70 | Session management | `docs/ROADMAP.md:238` | **Não iniciado** | Não foi encontrada evidência direta de gestão de sessão nesses três arquivos. | Sem função/classe específica encontrada neste recorte |
| P33-33 | 71 | Secure logout | `docs/ROADMAP.md:239` | **Não iniciado** | Não foi encontrada evidência direta de logout seguro nesses três arquivos. | Sem função/classe específica encontrada neste recorte |

## Regra para próximos commits

A partir deste baseline revisado, cada commit de preenchimento deve referenciar explicitamente um ID desta lista no formato:

- `feat: implementa <descrição> (P33-XX)`
- `fix: corrige <descrição> (P33-XX)`
