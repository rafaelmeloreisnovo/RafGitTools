# RafGit Tools — Private Sandbox Exchange V1

## Papel

RafGit Tools é o operador do protocolo, não o cofre de dados privados.

Ele:
- valida envelope;
- bloqueia segredo/credencial;
- calcula SHA-256 canônico;
- resolve somente test-plan IDs allowlisted;
- registra cada observação no ledger SHA-256 append-only já existente;
- mantém commit-base/rollback separado de resultado/claim.

## Autoridade privada

O destino privado concreto é fornecido por configuração/usuário autorizado. O código público usa `custodyTargetId` opaco e não precisa embutir o nome de um repositório privado.

## Fluxo

```text
select source
-> pin commit
-> classify artifacts
-> opaque refs + hashes
-> authorization receipt
-> validate
-> stage
-> live runner re-probe
-> execute allowlisted test plan
-> sanitize/hash result
-> append governance receipt
-> optional handoff
```

## Não executa JSON

`testPlanId` não é shell.

Apenas IDs presentes em `SandboxExchangePlanner` podem ser resolvidos para executores internos versionados.

## Cadeia de custódia

O novo bridge reutiliza `RepositoryGovernanceReceiptStore`, que já mantém:
- sequence;
- previous_hash;
- record_hash;
- before/after snapshots;
- gaps;
- append-only JSONL.

O receipt do sandbox adiciona:
- exchange_id;
- envelope_sha256;
- sandbox_receipt_hash;
- route;
- test plan;
- evidence state;
- claim_allowed=false.

## Privacidade

Envelope é metadata-only.

Bloqueado:
- tokens GitHub;
- bearer token;
- private key;
- OpenSSH private key;
- password/senha inline;
- seed phrase/mnemonic;
- artifact com `containsSecretMaterial=true`.

## Rollback

O protocolo exige `rollbackRef`. A execução remota deve continuar usando o branch writer existente, que já exige base SHA e confirmação explícita para rollback.

## Estado

```text
PROTOCOL_SOURCE = IMPLEMENTED
UNIT_TESTS = CI_PENDING
PRIVATE_AUTHORITY = EXTERNAL_CONFIGURED
PHYSICAL_VECTRA_EXECUTION = TOKEN_VAZIO
PHYSICAL_PCR_EXECUTION = TOKEN_VAZIO
RAFPOLIMATA_HANDOFF = TOKEN_VAZIO
claim_allowed = false
```


## Ledger-head binding

Before appending a sandbox receipt, the bridge:
1. verifies the existing governance SHA-256 chain;
2. reads its current head hash;
3. requires `envelope.previousReceiptHash` to equal that head (or `GENESIS` for an empty chained ledger);
4. rejects the exchange on mismatch.

Observation metadata (`outcome`, runner commit and gaps) is independently validated for malformed commits and credential markers before receipt persistence.
