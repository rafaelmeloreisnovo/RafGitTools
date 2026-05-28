# Benchmark Mirror — BrowserRaf/internal

Subdiretório dedicado a benchmark e espelho lógico de GitHub Actions CI, sem alterar o código de produção.

## Objetivos
- Validar consistência de build para `armeabi-v7a` e `arm64-v8a`.
- Gerar baseline comparável (latência p95/p99 + throughput).
- Preservar trilha oficial signed e permitir trilha unsigned de validação interna.

## Estrutura
- `scripts/run_benchmark.sh`: rotina local determinística de benchmark.
- `.github/workflows/benchmark-mirror.yml`: espelho de CI com upload de artefatos.
- `results/`: saídas geradas localmente.

## Execução local
```bash
bash benchmark/scripts/run_benchmark.sh
```

## Saídas esperadas
- `benchmark/results/benchmark_summary.md`
- `benchmark/results/metrics.csv`

## Contratos de metodologia
1. Sem declarar ganho sem medir.
2. Comparar sempre baseline vs atual.
3. Tratar falha como dado de correção.
4. Não usar benchmark como substituto de testes funcionais.
