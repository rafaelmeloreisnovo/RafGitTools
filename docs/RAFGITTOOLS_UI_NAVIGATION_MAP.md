# RAFGITTOOLS_UI_NAVIGATION_MAP

- Status: ATIVO
- Última atualização: 2026-05-27

## Mapa de navegação real (preenchido)

| Rota | Tela | Origem principal | Dependência auth | Estado |
|---|---|---|---|---|
| `auth` | AuthScreen | boot quando não autenticado | não | REAL_ATIVO |
| `home` | HomeScreen | pós-auth/splash | sim (ou offline) | REAL_ATIVO |
| `repository_list` | RepositoryListScreen | home | sim | REAL_ATIVO |
| `repository_detail/{repoPath}` | RepositoryDetailScreen | repository_list | sim | REAL_ATIVO |
| `commit_list/{repoPath}` | CommitListScreen | repository_detail | sim | REAL_ATIVO |
| `commit_detail/{repoPath}/{commitSha}` | CommitDetailScreen | commit_list | sim | REAL_ATIVO |
| `branch_list/{repoPath}` | BranchListScreen | repository_detail | sim | REAL_ATIVO |
| `settings` | SettingsScreen | home/menu | não obrigatório | REAL_ATIVO |
| `notifications` | NotificationsScreen | home/menu | sim | REAL_ATIVO |
| `terminal/{repoPath}` | TerminalScreen | repository_detail | não obrigatório | PARCIAL (hardening pendente) |
| `create_issue/{owner}/{repo}` | CreateIssueScreen | issue_list | sim | REAL_ATIVO |
| `create_pr/{owner}/{repo}` | CreatePullRequestScreen | pr_list | sim | REAL_ATIVO |

## Fluxos críticos

- Auth -> Home: liberado por `AuthUiState.Success` ou `AuthUiState.Offline`.
- Offline -> Home: permitido, carregando estado local sem chamada obrigatória à API.
- Logout -> Auth: limpa cache/token e retorna para fluxo de autenticação.
