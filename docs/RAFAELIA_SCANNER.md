# RAFAELIA Scanner

Ferramenta prática para aplicar o voo vetorial RAFAELIA em qualquer repositório local.

Ela gera inventário, ranking de prioridade, candidatos a hotfix e arquivos órfãos/latentes sem modificar o repositório analisado.

## Uso

```bash
cat <<'EOF' > /tmp/run_rafaelia_scan.sh
#!/usr/bin/env bash
set -euo pipefail
bash tools/raf_scan_repo.sh "${1:-.}"
EOF
chmod +x /tmp/run_rafaelia_scan.sh
/tmp/run_rafaelia_scan.sh .
```

Ou direto:

```bash
bash tools/raf_scan_repo.sh /caminho/do/repositorio
```

## Saídas geradas

Dentro do repositório analisado:

```text
.rafaelia/reports/RAFAELIA_SCAN_SUMMARY.md
.rafaelia/reports/RAFAELIA_REPO_INVENTORY.md
.rafaelia/reports/RAFAELIA_PRIORITY_REPORT.md
.rafaelia/reports/RAFAELIA_HOTFIX_CANDIDATES.md
.rafaelia/reports/RAFAELIA_ORPHAN_FILES.md
```

## O que cada relatório faz

| Relatório | Função |
|---|---|
| `RAFAELIA_SCAN_SUMMARY.md` | resumo numérico da varredura |
| `RAFAELIA_REPO_INVENTORY.md` | lista de arquivos por tipo |
| `RAFAELIA_PRIORITY_REPORT.md` | ranking dos arquivos mais importantes |
| `RAFAELIA_HOTFIX_CANDIDATES.md` | sinais de TODO/FIXME/erro/crash/SDK/permissão |
| `RAFAELIA_ORPHAN_FILES.md` | arquivos sem extensão ou latentes |

## Critério de prioridade

O scanner aproxima esta fórmula:

```text
valor = 0.20*evidencia
      + 0.18*urgencia
      + 0.16*memoria
      + 0.14*transmutacao
      + 0.12*latencia
      + 0.08*obviedade
      + 0.06*intencao
      + 0.04*ruido
      + 0.02*coerencia
```

## Sinais de urgência

O scanner procura sinais como:

```text
TODO FIXME XXX HACK BUG panic crash exception fail failed error
SDK 29 sdk 29 logcat permission denied segfault SIGSEGV
malloc garbage leak overflow undefined UB race deadlock
```

## Repositórios recomendados para rodar primeiro

```text
UserLAnd2
termux-app-rafacodephi
BLAKE3
llamaRafaelia
relativity-living-light
RafGitTools
```

## Interpretação

- Score alto não significa que o arquivo está errado.
- Score alto significa que ele é estruturalmente importante ou contém sinal de risco/oportunidade.
- Arquivo órfão não deve ser apagado automaticamente.
- Hipótese deve virar teste, log, benchmark ou documentação delimitada.

## Próximo passo operacional

Depois de rodar:

1. abrir `RAFAELIA_PRIORITY_REPORT.md`;
2. selecionar os 10 primeiros arquivos;
3. verificar se são build/runtime/test/doc;
4. abrir `RAFAELIA_HOTFIX_CANDIDATES.md`;
5. transformar cada erro real em issue ou patch mínimo;
6. preservar `RAFAELIA_ORPHAN_FILES.md` como mapa de conteúdo latente.

## Retroalimentar[3]

- F_ok: scanner aplica a matriz sem depender de IA em background.
- F_gap: a análise semântica ainda é heurística; precisa revisão humana/técnica.
- F_next: rodar nos repositórios críticos e usar os relatórios para hotfixes verificáveis.
