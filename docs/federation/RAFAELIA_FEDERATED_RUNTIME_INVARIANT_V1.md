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

```text
caminho privado descoberto != caminho executável por outro aplicativo
dispatch aceito != comando executado
exit code zero != guest boot comprovado
```

A descoberta retorna capacidades sanitizadas. O envelope exige identidade da transação, commits, hashes de entrada e saída, status, efeitos, safe state, rollback e R3. Campo ausente é falha de contrato.

## Gate local

```bash
python3 scripts/federation/validate_federated_runtime_v1.py contracts/rafaelia-federated-runtime-v1.json
python3 -m unittest tests.test_federated_runtime_v1
```

```yaml
validator: PASS
unit_tests: 5/5 PASS
canonical_sha256: fec657b5f22d0cea9b3a17eeceab40955c95f4cff2970a8779fdc76dd7f5cba1
android_build: TOKEN_VAZIO
device_dispatch: TOKEN_VAZIO
qemu_execution: TOKEN_VAZIO
guest_boot: TOKEN_VAZIO
claim_allowed: false
```

## R3

```text
F_ok   = contrato central, envelope completo, validador e testes negativos
F_gap  = build Android, concessão de permissão, dispatch e receipt em device
F_next = integrar os endpoints v2 nos dois APKs e capturar o primeiro recibo ARM
```
