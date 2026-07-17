# Supply Chain e Recuperação Federada

## Controles ativos

- GitHub Actions fixadas por SHA completo em `.github/actions-lock.json`.
- `persist-credentials: false` no checkout.
- permissões `contents: read`.
- dependência de teste fixada em `requirements/federation-audit.lock`.
- SBOM SPDX estrutural gerado pelo gate.
- restauração local real validada por SHA-256.
- nenhuma escrita automática cross-repo.
- nenhum merge automático.

## Limites ainda explícitos

```text
assinatura Sigstore/cosign = TOKEN_VAZIO
proveniência SLSA externa  = TOKEN_VAZIO
rollback real Android      = TOKEN_VAZIO
rollback real VM/QEMU      = TOKEN_VAZIO
rollback real modelo       = TOKEN_VAZIO
```

Esses itens não são convertidos em PASS pelo teste estrutural.

## Critério para promover recuperação remota

Um nó só muda de `TOKEN_VAZIO` para `TESTED` quando o artefato local registrar:

- commit e binário/APK/modelo/rootfs;
- hashes antes, durante a falha e após restauração;
- dispositivo, ABI e ambiente;
- comando e códigos de saída;
- tempo de recuperação;
- efeito sobre dependentes;
- confirmação de que claims não foram promovidos.
