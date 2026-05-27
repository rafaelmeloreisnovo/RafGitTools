# RAFGITTOOLS_CODE_REALITY_MATRIX

Status: ATIVO (auditado)
Última atualização: 2026-05-27

| ID | Área | Arquivo | Classe/Função | Tipo | Finalidade | UI exposta? | Auth | Offline | Status real | Evidência | Próxima ação |
|---|---|---|---|---|---|---|---|---|---|---|---|
| AUTH-001 | auth | app/src/main/kotlin/com/rafgittools/ui/screens/auth/AuthViewModel.kt | authenticateWithPat | viewmodel | login com PAT | SIM | PAT | NÃO | REAL_ATIVO | salva token + consulta usuário | ampliar testes de erro de rede |
| AUTH-002 | auth | app/src/main/kotlin/com/rafgittools/ui/screens/auth/AuthViewModel.kt | startDeviceCodeLogin | viewmodel | OAuth Device Flow | SIM | OAUTH_DEVICE | NÃO | REAL_ATIVO | mapeia Requesting/Pending/Polling/Authorized | validar ausência de clientId |
| AUTH-003 | auth | app/src/main/kotlin/com/rafgittools/ui/screens/auth/AuthViewModel.kt | importGhCliToken | viewmodel | importar token do gh CLI | SIM | GH_CLI_IMPORT | NÃO | REAL_ATIVO | chama GhCliAuthImporter.importToken | cobrir casos sem gh |
| AUTH-004 | auth | app/src/main/kotlin/com/rafgittools/ui/screens/auth/AuthViewModel.kt | authenticateWithSshKey | viewmodel | login SSH | PARCIAL | SSH | NÃO | PARCIAL | retorna erro explícito de parcialidade | implementar fluxo chave/agent |
| AUTH-005 | auth | app/src/main/kotlin/com/rafgittools/ui/screens/auth/AuthViewModel.kt | continueOffline | viewmodel | liberar modo local | SIM | OFFLINE | SIM | REAL_ATIVO | setOfflineMode + AuthUiState.Offline | adicionar UX de retomada online |
| HOME-001 | home | app/src/main/kotlin/com/rafgittools/ui/screens/home/HomeViewModel.kt | checkAuthAndLoadData | viewmodel | gate inicial da Home | SIM | PAT/OAuth/Offline | SIM | REAL_ATIVO | considera authRepository.isOfflineMode() | teste unitário de ramo offline |
| GIT-001 | git | app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt | JGitService | service | operações git locais | PARCIAL | N/A | SIM | REAL_ATIVO | backend de GitRepositoryImpl | exposição UX completa |
| API-001 | github | app/src/main/kotlin/com/rafgittools/data/github/GithubDataRepository.kt | getAuthenticatedUserSync | repository | user API | SIM | Token | NÃO | REAL_ATIVO | usado em Auth/Home | fallback offline mais claro |
| API-002 | github | app/src/main/kotlin/com/rafgittools/data/github/GithubDataRepository.kt | getUserRepositoriesSync | repository | lista repositórios | SIM | Token | NÃO | REAL_ATIVO | usado em HomeViewModel | cache local opcional |
| DOC-001 | docs | docs/BUILD.md | build contract | doc | alinhar execução local/CI | N/A | N/A | N/A | REAL_ATIVO | comandos oficiais definidos | manter sincronizado com scripts |
