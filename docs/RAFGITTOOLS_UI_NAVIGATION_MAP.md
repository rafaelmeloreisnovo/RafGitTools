# RAFGITTOOLS_UI_NAVIGATION_MAP

- Status: ATIVO (mapeamento principal)
- Última atualização: 2026-05-27
- Fonte: `ui/navigation/Screen.kt` + `MainActivity.kt`.

## Rotas declaradas

Home, Auth, RepositoryList, RepositoryDetail, CommitList, CommitDetail, BranchList, Settings, AddRepository, IssueList, IssueDetail, PullRequestList, PullRequestDetail, Search, Profile, FileBrowser, DiffViewer, StashList, TagList, Releases, ReleaseDetail, Notifications, Terminal, CreateIssue, CreatePullRequest.

## Mapa real (resumo)

| Screen | route | Montada em MainActivity | Status real |
|---|---|---|---|
| Home | `home` | Sim | REAL_ATIVO |
| Auth | `auth` | Sim | REAL_ATIVO (com chooser) |
| RepositoryList | `repository_list` | Sim | REAL_ATIVO |
| RepositoryDetail | `repository_detail/{repoPath}` | Sim | REAL_ATIVO |
| CommitList | `commit_list/{repoPath}` | Sim | REAL_ATIVO |
| CommitDetail | `commit_detail/{repoPath}/{commitSha}` | Sim | REAL_ATIVO |
| BranchList | `branch_list/{repoPath}` | Sim | REAL_ATIVO |
| Settings | `settings` | Sim | REAL_ATIVO |
| AddRepository | `add_repository` | Sim | REAL_ATIVO |
| IssueList | `issue_list/{owner}/{repo}` | Sim | REAL_ATIVO |
| IssueDetail | `issue_detail/{owner}/{repo}/{number}` | Sim | REAL_ATIVO |
| PullRequestList | `pr_list/{owner}/{repo}` | Sim | REAL_ATIVO |
| PullRequestDetail | `pr_detail/{owner}/{repo}/{number}` | Sim | REAL_ATIVO |
| Search | `search` | Sim | REAL_ATIVO |
| Profile | `profile/{username}` | Sim | REAL_ATIVO |
| FileBrowser | `file_browser/{repoPath}` | Sim | REAL_ATIVO |
| DiffViewer | `diff_viewer/{repoPath}` | Sim | REAL_ATIVO |
| StashList | `stash_list/{repoPath}` | Sim | REAL_ATIVO |
| TagList | `tag_list/{repoPath}` | Sim | REAL_ATIVO |
| Releases | `releases/{owner}/{repo}` | Sim | REAL_ATIVO |
| ReleaseDetail | `release_detail/{owner}/{repo}/{id}` | Sim | REAL_ATIVO |
| Notifications | `notifications` | Sim | REAL_ATIVO |
| Terminal | `terminal/{repoPath}` | Sim | REAL_ATIVO |
| CreateIssue | `create_issue/{owner}/{repo}` | Sim | REAL_ATIVO |
| CreatePullRequest | `create_pr/{owner}/{repo}` | Sim | REAL_ATIVO |

## Gap crítico atual

- A existência da rota/tela não implica paridade funcional total com todas as operações de domínio (ex.: operações avançadas de rebase/cherry-pick/reset ainda requerem UX de confirmação dedicada).
