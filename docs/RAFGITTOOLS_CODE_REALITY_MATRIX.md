# RAFGITTOOLS_CODE_REALITY_MATRIX

Status: ATIVO (parcial)
Última atualização: 2026-05-27

| ID | Área | Arquivo | Classe/Função | Tipo | Finalidade | Chamado por | Rota/Tela | UI exposta? | Auth? | Offline? | GitHub API? | JGit? | Termux/ARM32 risco | Status | Evidência | Ação necessária |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| AUTH-001 | auth | app/src/main/kotlin/com/rafgittools/ui/screens/auth/AuthScreen.kt | AuthScreen | screen | Login | MainActivity NavHost | Screen.Auth | SIM | PAT/OAuth/GH/SSH/OFFLINE | PARCIAL | SIM | NÃO | médio | PARCIAL | composable(Screen.Auth.route) | expandir UX e testes |
| AUTH-002 | auth | app/src/main/kotlin/com/rafgittools/ui/screens/auth/AuthViewModel.kt | startDeviceCodeLogin | viewmodel | Device Flow | AuthScreen | Screen.Auth | SIM | OAuth | NÃO | SIM | NÃO | médio | REAL_ATIVO | collect(DeviceFlowState) | testar client id ausente |
| AUTH-003 | auth | app/src/main/kotlin/com/rafgittools/data/network/AuthInterceptor.kt | AuthInterceptor | interceptor | legado | ninguém | n/a | NÃO | n/a | n/a | n/a | n/a | baixo | DEAD_CODE | @Deprecated | remover com segurança |
| GIT-001 | git | app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt | JGitService | service | Git local | GitRepositoryImpl | várias | PARCIAL | None | SIM | NÃO | SIM | alto | NECESSITA_TESTE | classe ativa | completar matriz de operações |
| DOC-001 | docs | docs/FEATURE_MATRIX.md | documento | doc | visão de features | equipe | n/a | n/a | n/a | n/a | n/a | n/a | baixo | ROADMAP | conteúdo declarativo | reconciliar com matriz real |
