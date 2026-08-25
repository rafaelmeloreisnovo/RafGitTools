# CI Private Billing Boundary V1 — Control Plane

**Evento:** `RAFAELIA-CI-PRIVATE-BILLING-BOUNDARY-V1-20260730T054800Z`  
**Predecessor:** RafGitTools `393d80bd30ea07dfd4ff5f7d3b5329636098100c` (PR #315)  
**Tempo:** 2026-07-30 05:48 UTC / 02:48 BRT  
**Política:** `APPEND_ONLY · NON_DESTRUCTIVE · CLAIM_ALLOWED=false · NO_AUTO_MERGE`

## Regra de controle

Quando GitHub Actions não é disponibilizado para este repositório privado por ausência de cobertura de pagamento, o plano de controle deve emitir:

```text
CI_UNAVAILABLE_PRIVATE_BILLING
```

Não emitir `CODE_FAILURE`, `TEST_FAILURE`, `REGRESSION` ou `ROOT_CAUSE` apenas porque um run não expôs steps nem logs.

## Contrato de decisão

| Sinal | Estado correto | Próxima ação |
|---|---|---|
| run sem steps/logs | `CI_UNAVAILABLE_PRIVATE_BILLING` | registrar restrição e usar receipt local |
| receipt local com hashes e exit code | `EVIDENCIADO_LOCAL_LIMITADO` | revisão humana e replay |
| execução Android não observada | `TOKEN_VAZIO_RUNTIME_NOT_EXECUTED` | executar no dispositivo alvo |
| claim de produção/segurança | `CLAIM_ALLOWED=false` | teste específico e autoridade competente |

## Limite

Esta regra é um classificador operacional informado pelo titular; não afirma que GitHub Actions falhou tecnicamente, nem altera configurações de faturamento, runners ou workflows. Nenhum rerun ou merge foi executado por este registro.

## F_next

O Control Plane deve aceitar receipts locais como entrada primária quando a CI privada estiver indisponível, preservando `head_sha`, comando, hashes, ambiente, exit code e rollback.
