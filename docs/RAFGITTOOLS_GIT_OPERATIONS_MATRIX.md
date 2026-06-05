# RAFGITTOOLS_GIT_OPERATIONS_MATRIX

- Status: ATIVO
- Última atualização: 2026-05-27

| Operação | Backend (JGitService/GitRepositoryImpl) | UI atual | Risco | Situação |
|---|---|---|---|---|
| clone/fetch/pull/push | implementado | parcial (fluxo principal) | médio | REAL_ATIVO |
| status/log/diff | implementado | exposto | baixo | REAL_ATIVO |
| branches (create/delete/checkout) | implementado | parcial (delete com cuidado) | médio | REAL_ATIVO |
| tags (create/delete/list) | implementado | parcial | médio | REAL_ATIVO |
| stash (save/pop/apply/clear) | implementado | parcial | alto (clear) | REAL_ATIVO |
| reset/revert/clean | implementado | pouco exposto | alto | PARCIAL_UI |
| rebase/cherry-pick | implementado | parcial | alto | PARCIAL_UI |
| reflog/blame | implementado | parcial | baixo | PARCIAL_UI |

## Diretriz

Operações destrutivas devem manter confirmação explícita na UI antes de execução.
