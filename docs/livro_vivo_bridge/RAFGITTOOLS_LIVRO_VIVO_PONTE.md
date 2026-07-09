# Ponte Livro Vivo — RafGitTools, Automação Git e Rastreabilidade

> Modo: ponte operacional entre `RafGitTools` e o Livro Vivo RAFAELIA  
> Status inicial: `FORMALIZACAO_READY` + `DADO_SENSIVEL` quando houver tokens ou automação privilegiada  
> Regra: ferramenta Git deve registrar intenção, escopo, credencial, ação e reversão

## Parábola do martelo automático

O ferreiro criou um martelo que batia sozinho.

No primeiro dia, fez cem pregos.

No segundo, quebrou uma porta.

O discípulo perguntou:

— O martelo ficou mau?

O mestre respondeu:

— Não. Ele apenas não sabia onde parar.

Assim é automação Git: poderosa quando tem trilho; perigosa quando não tem escopo.

## Invariante

```text
intenção → escopo → ação Git → log → reversão
```

Forma compacta:

```math
Inv(RafGitTools)=Intent\rightarrow Scope\rightarrow GitAction\rightarrow AuditLog\rightarrow Rollback
```

## Risco principal

| Risco | Correção |
|---|---|
| automação com token exposto | usar segredo protegido e varredura |
| ação sem dry-run | exigir modo simulação |
| commit/PR em repo errado | declarar escopo e allowlist |
| falta de rollback | registrar plano de reversão |
| logs com dado sensível | depurar logs antes de publicar |

## Próximos passos

1. Criar `RAFGITTOOLS_OPERATIONAL_SAFETY.md`.
2. Definir allowlist de repositórios e ações.
3. Exigir modo `--dry-run` para operações destrutivas.
4. Registrar logs sem tokens.
5. Criar checklist de rollback.

## Ficha Livro Vivo

```yaml
repo: rafaelmeloreisnovo/RafGitTools
familia: Git/Automacao
invariante: "intenção → escopo → ação Git → log → reversão"
selo: FORMALIZACAO_READY
risco: "automação com token, ação fora do escopo, ausência de dry-run ou rollback"
proximo_passo: "criar RAFGITTOOLS_OPERATIONAL_SAFETY.md"
```

## Retroalimentar[3]

- **F_ok:** RafGitTools recebe ponte para automação com escopo, log e reversão.
- **F_gap:** falta inventário real das ferramentas, permissões e comandos destrutivos.
- **F_next:** criar `RAFGITTOOLS_OPERATIONAL_SAFETY.md` com dry-run, allowlist e rollback.
