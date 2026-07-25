# Contrato RafGitTools ↔ RafPolimata

## Separação de responsabilidade

| RafGitTools | RafPolimata |
|---|---|
| autentica e autoriza | lê e processa |
| seleciona fonte | valida formato |
| cria job | executa streaming |
| aplica política | normaliza e indexa |
| acompanha eventos | produz checkpoints |
| apresenta resultado | emite evidência |
| pede decisão humana | nunca decide publicação sozinho |

## JobEnvelope mínimo

```json
{
  "schema": "rafaelia.job.v1",
  "job_id": "uuid",
  "trace_id": "uuid",
  "capability_id": "rafpolimata.index_source.v1",
  "source": {
    "provider": "filesystem|drive|github",
    "opaque_ref": "...",
    "content_id": "TOKEN_VAZIO_UNTIL_PREFLIGHT",
    "read_only": true
  },
  "policy": {
    "privacy_class": "P2_PRIVATE",
    "max_memory_bytes": 268435456,
    "max_output_bytes": 1073741824,
    "timeout_seconds": 3600,
    "network": false,
    "original_mutation": false
  },
  "requested_outputs": ["manifest", "index", "audit", "checkpoint"]
}
```

## EventEnvelope mínimo

```json
{
  "schema": "rafaelia.event.v1",
  "event_id": "uuid",
  "trace_id": "uuid",
  "run_id": "uuid",
  "job_id": "uuid",
  "stage": "parse",
  "status": "RUNNING",
  "severity": "INFO",
  "bytes_read": 0,
  "records_emitted": 0,
  "evidence_refs": []
}
```

## ResultEnvelope mínimo

```json
{
  "schema": "rafaelia.result.v1",
  "run_id": "uuid",
  "state": "VERIFIED|PASS_LIMITED|FAILED|BLOCKED|CONTRADICTED|TOKEN_VAZIO",
  "artifacts": [],
  "evidence": [],
  "limits": [],
  "contradictions": [],
  "F_ok": [],
  "F_gap": [],
  "F_next": []
}
```

## Regras de compatibilidade

- schemas versionados;
- campos desconhecidos ignorados somente quando opcionais;
- mudança incompatível exige nova versão principal;
- referências privadas permanecem opacas;
- credenciais nunca atravessam o envelope;
- logs não contêm texto bruto privado por padrão;
- o modelo interpretativo recebe segmentos governados, não a fonte inteira.

## Invariante compartilhada

\[
I = identidade \land proveniencia \land politica \land limite \land evidencia \land retorno
\]

Se qualquer termo obrigatório estiver ausente, o job deve ser bloqueado ou rebaixado para `TOKEN_VAZIO`/`PASS_LIMITED`.
