# RAFAELIA — Invariante Federada de Runtime v1

**Estado:** `IMPLEMENTED_CONTRACT_LOCAL_TESTED`  
**Claim global:** `false`

## Invariante

```text
autorização
→ job tipado
→ execução limitada no Termux
→ recibo endereçado por conteúdo
→ compilação de evidência no RafPolimata
→ decisão
→ rollback
→ Vectras somente quando vm_required=true
```

Nenhum corpo substitui o outro:

- **RafGitTools:** autoriza, roteia e registra; não executa shell irrestrito.
- **Termux RAFCODE-Φ:** executa somente efeitos permitidos; não inventa autorização.
- **RafPolimata:** valida recibos e compila evidência; não transforma dispatch em execução.
- **Vectras VM Android:** controla a VM; permanece parada sem gate e recibo.

## Android IPC v2

O contrato elimina as falsas equivalências:

```text
caminho privado descoberto != caminho executável por outro aplicativo
dispatch aceito != comando executado
exit code zero != guest boot comprovado
```

A descoberta retorna nomes e capacidades sanitizados. A execução usa o
`RunCommandService` protegido pela permissão
`com.termux.rafacodephi.permission.RUN_COMMAND`.

## Gate local

```bash
python3 scripts/federation/validate_federated_runtime_v1.py \
  contracts/rafaelia-federated-runtime-v1.json

python3 -m unittest tests.test_federated_runtime_v1
```

Resultado local limitado deste corte:

```yaml
validator: PASS
unit_tests: 4/4 PASS
canonical_sha256: 815e4e01eecf668867c8c2dc9358e373f6daef1ac81c2f592f5980b68f06a1b9
android_build: TOKEN_VAZIO
device_dispatch: TOKEN_VAZIO
qemu_execution: TOKEN_VAZIO
guest_boot: TOKEN_VAZIO
claim_allowed: false
```

## R3

```text
F_ok   = contrato central, validador e testes negativos
F_gap  = build Android, concessão de permissão, dispatch e receipt em device
F_next = integrar os endpoints v2 nos dois APKs e capturar o primeiro recibo ARM
```
