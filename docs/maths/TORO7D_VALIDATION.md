# Toro7D Validation and Governance Metrics

Este módulo transforma o modelo T^7 em rotina executável, auditável e falsificável.

## Rotina única (baixo overhead)
Comando:

```bash
python3 scripts/ci/toro7d_benchmark.py --iterations 420
```

Saída em JSON com métricas:
- `pi_mean`, `pi_stdev`
- razão de regimes `Ω`, `Δ`, `ρ`
- checks de falsificabilidade (`ratios_sum_to_1`, `pi_non_negative`)

## Base formal e verificabilidade
- Estado: `s in [0,1)^7`
- Atualização: `C,H` com `alpha = 0.25`
- Classificação: `Pi = Ein / D` em três regimes

## Governança e auditoria
- Execução em CI (`.github/workflows/toro7d_ci.yml`)
- Resultado reproduzível e serializado em JSON
- Evidência quantitativa para revisão técnica pública

## Limites
- Não é prova jurídica automática.
- É instrumento técnico de diligência, rastreabilidade e consistência operacional.
