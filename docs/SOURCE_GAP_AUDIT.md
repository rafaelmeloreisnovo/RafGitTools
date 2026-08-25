# RafGitTools — Source Gap Audit V1

Status: **SOURCE_GATE_IMPLEMENTED / TOKEN_VAZIO_SEMANTICS_SEPARATED / RUNTIME_EXECUTION_SEPARATE**

## Objetivo

`audit_source_gaps.py` transforma busca manual por lacunas em um contrato determinístico sobre **somente a fonte compilada/runtime** em `app/src/main`.

Não varre `fazer/`, docs históricos ou testes para decidir maturidade de produção.

## Bloqueadores

Sem allowlist válida, o scanner retorna exit `1` para ocorrências executáveis que demonstram diretamente ausência de implementação, como:

- `TODO(...)` Kotlin;
- `NotImplementedError` / `NotImplementedException`;
- `UnsupportedOperationException("not implemented|todo|stub...")`;
- `error/check/require("not implemented|todo|stub...")`.

TODO/FIXME/stub/placeholder textuais são inventariados como `WARNING` quando não constituem diretamente um blocker executável.

### `TOKEN_VAZIO` não é, sozinho, um blocker de implementação

`TOKEN_VAZIO` é um estado epistemológico/runtime válido e fail-closed. Ele pode aparecer legitimamente como:

- membro de enum/closed vocabulary;
- comparação ou transição de estado;
- retorno quando evidência de runtime não está disponível;
- resultado de uma medição incompleta;
- estado explícito de `NOT_ASSESSED`/incerteza preservada.

Por isso, uma ocorrência lexical de `TOKEN_VAZIO` é registrada como `WARNING/TOKEN_VAZIO_SOURCE`, e **não** como prova automática de que uma implementação está faltando.

Se a mesma linha também contiver um marcador executável independente (`TODO(...)`, `NotImplementedError`, etc.), esse marcador continua `BLOCKER`.

A lacuna concreta representada por um `TOKEN_VAZIO` continua aberta no ledger/evidence gate correspondente. Reclassificar a palavra como atenção lexical **não converte a incerteza em PASS**.

Essa separação implementa a reconciliação já documentada em `docs/audits/SOURCE_GAP_RECONCILIATION_2026-08-14.md`:

```text
sinal lexical
!= caminho executável
!= runtime observado
!= claim
```

## Privacidade / proveniência

O receipt **não copia a linha de código**. Para cada finding grava apenas:

```text
severity
marker
relative path
line number
SHA-256(normalized line)
allowlisted?
allow reason (quando aplicável)
```

Também calcula SHA-256 de cada arquivo e um `source_tree_sha256` determinístico sobre `path + file hash` ordenados.

Receipts locais ficam em `.rafgittools/receipts/` e recebem SHA-256 próprio.

## Allowlist fail-closed

`contracts/source-gap-allowlist.v1.json` mantém somente exceções semanticamente justificadas.

Uma exceção só é aceita com:

```json
{
  "marker": "KOTLIN_TODO_CALL",
  "path": "app/src/main/.../Example.kt",
  "line_sha256": "<64 hex>",
  "reason": "<justificativa verificável>"
}
```

A chave é `marker + path + line_sha256`. Portanto, se a linha mudar, a exceção deixa de casar automaticamente. Duplicatas, hash inválido, razão vazia ou schema errado fazem o scanner encerrar com erro de configuração (`exit 2`).

Allowlist reduz somente o gate daquele finding; **não transforma runtime não executado em PASS**.

## Execução

Self-test puro Python:

```bash
python3 scripts/audit_source_gaps.py --self-test
```

Varredura + receipt:

```bash
python3 scripts/audit_source_gaps.py
```

Varredura sem receipt:

```bash
python3 scripts/audit_source_gaps.py --no-receipt
```

Gate completo recomendado:

```bash
chmod +x scripts/rafgittools_full_readiness_gate.sh
./scripts/rafgittools_full_readiness_gate.sh
```

Para o smoke físico já explicitamente autorizado pelo operador:

```bash
RAFGITTOOLS_DEVICE_SMOKE=1 ./scripts/rafgittools_full_readiness_gate.sh
```

O wrapper primeiro executa self-test + source-gap scan. Só então chama `rafgittools_readiness_gate.sh`.

## CI

`.github/workflows/source-gap-audit.yml` executa apenas Python stdlib e não depende de Android SDK/Gradle.

O gate falha por **blocker executável**, não por mera existência de um estado epistemológico `TOKEN_VAZIO` no vocabulário/código. Findings `TOKEN_VAZIO_SOURCE` permanecem visíveis no receipt e devem ser ligados aos evidence ledgers apropriados.

## Invariante

```text
no executable source gap
!= no uncertainty
!= unit tests passed
!= APK built
!= device smoke passed
!= production claim
```

O Source Gap Audit fecha a fronteira de implementação ausente detectável por marcador executável. Incerteza/evidência `TOKEN_VAZIO`, testes, build e runtime continuam exigindo seus próprios receipts e gates.
