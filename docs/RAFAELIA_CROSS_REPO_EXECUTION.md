# RAFAELIA · Cross-repository execution plan

Plano para aplicar o scanner RAFAELIA nos repositórios críticos sem misturar hipótese, metáfora e prova.

## Repositórios alvo

| Ordem | Repositório | Por quê | Primeiro relatório a olhar |
|---:|---|---|---|
| 1 | `termux-app-rafacodephi` | app instala mas sessão terminal falha; SDK/runtime é urgente | `RAFAELIA_HOTFIX_CANDIDATES.md` |
| 2 | `UserLAnd2` | fluxo Linux/Android/bootstrap e sessão | `RAFAELIA_PRIORITY_REPORT.md` |
| 3 | `BLAKE3` | KAT/bench/correção criptográfica | `RAFAELIA_HOTFIX_CANDIDATES.md` |
| 4 | `llamaRafaelia` | CTI/modelos/scripts/datasets | `RAFAELIA_REPO_INVENTORY.md` |
| 5 | `relativity-living-light` | hipótese vs definição vs simulação | `RAFAELIA_PRIORITY_REPORT.md` |
| 6 | `RafGitTools` | ferramenta-mãe de varredura | todos |

## Comando local recomendado

Rodar manualmente, dentro de cada repositório alvo ou apontando para cada clone local:

```text
bash RafGitTools/tools/raf_scan_repo.sh termux-app-rafacodephi
bash RafGitTools/tools/raf_scan_repo.sh UserLAnd2
bash RafGitTools/tools/raf_scan_repo.sh BLAKE3
bash RafGitTools/tools/raf_scan_repo.sh llamaRafaelia
bash RafGitTools/tools/raf_scan_repo.sh relativity-living-light
bash RafGitTools/tools/raf_scan_repo.sh RafGitTools
```

## Fluxo de decisão depois do scan

1. Abrir `RAFAELIA_SCAN_SUMMARY.md`.
2. Abrir os 20 primeiros itens de `RAFAELIA_PRIORITY_REPORT.md`.
3. Conferir se há erro real em `RAFAELIA_HOTFIX_CANDIDATES.md`.
4. Se for código, build ou runtime: criar patch pequeno.
5. Se for documentação: criar definição curta e comando reproduzível.
6. Se for fórmula ou hipótese: marcar como `[HIPOTESE]`, `[SIMULACAO]`, `[VALIDADO]` ou `[REFUTADO]`.
7. Nunca apagar órfãos automaticamente; primeiro classificar.

## Critério de hotfix

Um hotfix só deve entrar se cumprir pelo menos um:

```text
- corrige erro reproduzível;
- melhora log/diagnóstico sem mudar comportamento;
- adiciona comando de validação;
- separa hipótese de prova;
- remove ambiguidade de build/runtime;
- reduz risco de regressão.
```

## Saída desejada por repositório

Cada repo crítico deve terminar com:

```text
.rafaelia/reports/RAFAELIA_SCAN_SUMMARY.md
.rafaelia/reports/RAFAELIA_PRIORITY_REPORT.md
.rafaelia/reports/RAFAELIA_HOTFIX_CANDIDATES.md
docs/RAFAELIA_PRIORITY_MATRIX.md
```

## Retroalimentar[3]

- F_ok: existe plano de execução cruzada com ordem e critério.
- F_gap: os relatórios precisam ser gerados em clones locais ou CI futura.
- F_next: rodar scanner nos alvos, selecionar top 10 por repo e transformar em patches mínimos.
