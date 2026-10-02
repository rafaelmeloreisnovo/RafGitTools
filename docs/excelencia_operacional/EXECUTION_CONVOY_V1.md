# Execution Convoy V1 — WorkGroup durável e auditável

Estado inicial: `IMPLEMENTED_UNTESTED` até a execução CI do commit exato.

## Objetivo

Transformar uma intenção autorizada em um grupo de etapas tipadas sem exigir que o operador acione cada etapa manualmente. A interface pode expor um único **portão de execução** e um syslog por etapas, enquanto a cadeia de custódia permanece granular.

```text
INTENT
-> WORKGROUP
-> GOVERNANCE
-> READY SET
-> FRONTIER SELECTION
-> STAGE
-> RECEIPT
-> CHECKPOINT
-> NEXT READY SET
-> RESULT / R3
```

`RECEIPT_EVERY_STAGE` e `COMMIT_ON_MATERIAL_DELTA_ONLY` são regras diferentes. Testes e medições não criam commits vazios; ainda assim produzem receipt.

## Invariantes herdados

- `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`
- `TOKEN_VAZIO != 0`
- `IMPLEMENTED_UNTESTED != PASS`
- falha de integridade => `FAIL_CLOSED`
- predecessor de receipt é obrigatório para retomada
- retomada exige mesma identidade de plano e `source_sha`
- estágio dependente não executa após predecessor `FAIL/BLOCKED`
- estágio independente continua elegível
- V1 não aceita shell arbitrário vindo do plano
- mutações V1: somente `READ_ONLY|TEST`
- `claim_allowed=false`

## Seleção de próxima etapa

Entre estágios prontos, V1 reutiliza a heurística operacional:

```text
FLS = ((U + R + D + A) * E) / C
```

Ela ordena trabalho; não mede verdade, qualidade científica ou valor econômico.

## Operações V1

- `ASSERT_FILE`
- `ASSERT_JSON`
- `HASH_FILE`
- `UNITTEST_DISCOVERY`

`UNITTEST_DISCOVERY` usa `subprocess` com `shell=False` e argv construído pelo executor. Não existe campo de comando livre.

## Receipt e checkpoint

Cada receipt liga:

```text
source_sha
plan_sha256
stage_id
operation
state
artifact/evidence
previous_receipt_sha256
receipt_sha256
```

SHA-256 é obrigatório. BLAKE3 é calculado somente quando uma implementação verificável está disponível no runtime; caso contrário o campo permanece `TOKEN_VAZIO_UNAVAILABLE`. Hash não promove claim.

A retomada verifica toda a cadeia antes de confiar no checkpoint. Mudança em qualquer receipt provoca `IntegrityError` e bloqueia resume.

## Proveniência de design ZIPRAF fornecida em 2026-10-01

Os arquivos fornecidos fora do repositório foram usados como **fonte de invariantes**, não como evidência de execução do RafGitTools:

- `zipraf03.zip` — SHA-256 `fe0f8ad71fcb2232165117aa807c79583848a2690c0a326f6b24a5c5101cf672`: matriz de transição, erro sticky e recovery/reinit.
- `zipraf04.zip` — SHA-256 `de7280a85a655728fd9c0aba4a70bcb6a3bc407ed78482edc7cb15d6bc13b46a`: replay monotônico, corrupção de manifesto e watchdog/failover.
- `zipraf05.zip` — SHA-256 `125c8ed3ac1174bd61a9061b539d79b243d1a3d8082990d7d949d08e055dc656`: anti-rollback e cadeia de receipts com propagação de tamper.
- `zipraf06.zip` — SHA-256 `73d834f1a48d0ee920aefaff6ef7f5cabbc6e5baf3abb4222e9b4704d4f02b97`: preservação da baseline anterior ao adicionar novos gates.
- `ZIPRAF_OMEGA_NEXT_G13_G15.tar.gz` — SHA-256 `f6a23207fb8cf52b6149fac12e529fabb4c807437f0f1c557bab0752d7a5e8b0`: envelope autônomo, corpus adversarial e reprodutibilidade.

BLAKE3 desses arquivos permanece `TOKEN_VAZIO_UNAVAILABLE` neste runtime de preparação; não foi inventado ou substituído por outro algoritmo.

## Uso

```bash
python3 scripts/execution_convoy.py \
  --plan configs/execution-convoy.v1.json \
  --root . \
  --output artifacts/execution-convoy/run-001 \
  --source-sha "$(git rev-parse HEAD)"
```

Retomar somente após verificar receipts:

```bash
python3 scripts/execution_convoy.py \
  --plan configs/execution-convoy.v1.json \
  --root . \
  --output artifacts/execution-convoy/run-001 \
  --source-sha "$(git rev-parse HEAD)" \
  --resume
```

## R3 do delta

- `F_ok`: contrato do executor, cadeia SHA-256, checkpoint, resume, seleção por fronteira e fail-closed estão implementados no source.
- `F_gap`: CI do commit exato e integração de UI/botão ainda precisam de evidência; BLAKE3 depende de runtime verificado.
- `F_next`: observar CI exato; depois ligar a UI ao `WorkGroup` sem criar segundo workflow raiz e ampliar operações somente por adaptadores tipados.
