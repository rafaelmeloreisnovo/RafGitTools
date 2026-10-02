# Federação funcional freestanding

Este patch organiza cinco repositórios como uma federação por contratos concretos, sem
renomear variáveis existentes e sem introduzir uma abstração que substitua as autoridades locais.

Cada repositório recebe `federation/functional-freestanding-leaf.v1.json`.
RafGitTools recebe também o registro de controle e o validador estrutural.

O validador não executa nem substitui os gates locais. Ele verifica:
1. conjunto concreto dos cinco membros;
2. autoridade declarada de cada membro;
3. invariantes contra fusão de autoridade e promoção sem evidência;
4. presença dos paths/gates/evidência freestanding já existentes.

Exemplo, a partir de diretórios irmãos:

python3 scripts/federation/validate_functional_freestanding_federation.py \
  --root rafaelmeloreisnovo/RafGitTools=../RafGitTools \
  --root rafaelmeloreisnovo/GAIA_phi=../GAIA_phi \
  --root rafaelmeloreisnovo/RafPolimata=../RafPolimata \
  --root rafaelmeloreisnovo/termux-app-rafacodephi=../termux-app-rafacodephi \
  --root rafaelmeloreisnovo/termux-packages=../termux-packages

`sumali_classificado_total` permanece como corpus/evidência de entrada e não recebe autoridade
de execução nesta federação.

## Current operational state

The current hotfix/status router is `docs/federation/ECOSYSTEM_OPERATIONAL_HOTFIX_20260930.md` and its machine-readable source is `configs/ecosystem-operational-state.v2.json`.

Validate it with:

```bash
python3 scripts/federation/validate_ecosystem_operational_state.py
```

This current-state pointer supersedes no repository-local authority and does not promote any claim.
