# RAFGITTOOLS_GIT_OPERATIONS_MATRIX

- Status: ATIVO (auditoria de operações)
- Última atualização: 2026-05-27
- Fonte: `domain/repository/GitRepository.kt`, `data/repository/GitRepositoryImpl.kt`, `data/git/JGitService.kt`.

## Cobertura principal

As operações listadas no plano (clone/status/commits/branches/stash/tags/diff/rebase/cherry-pick/reset/revert/clean/reflog/blame etc.) estão implementadas em `GitRepositoryImpl` e encaminhadas para `JGitService`.

## Operações destrutivas (exigir confirmação UX)

- `forcePushWithLease`
- `deleteBranch`
- `stashClear`
- `deleteTag`
- `reset`
- `clean`
- `rebaseAbort`
- `cherryPickAbort`

## Status

- **CODE_REAL**: alto.
- **UI_EXPOSTA**: parcial para operações avançadas/destrutivas.
- **Ação**: criar diálogos de confirmação para operações de risco e trilha de auditoria por ação.
