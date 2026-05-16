# Benchmark Summary

- timestamp_utc: 2026-05-16T05:48:14Z
- scope: BrowserRaf/internal
- abi_matrix: armeabi-v7a, arm64-v8a
- methodology: local deterministic mirror

## Metrics

| Metric | Value | Unit |
|---|---:|---|
| latency_p95 | 14.2 | ms |
| latency_p99 | 22.8 | ms |
| throughput | 1820 | req/s |
| error_rate | 0.00 | % |

## Falsification condition

Se latency_p99 > 30ms **ou** error_rate > 0.10%, a hipótese de estabilidade de desempenho é rejeitada.
