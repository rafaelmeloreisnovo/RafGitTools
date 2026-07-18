# ZIPRAF direct runtime — federação v1

A autoridade é distribuída sem copiar o corpo:

```text
ZIPRAF_CORE          → ABI binária, janelas e bits fixos
ZIPRAF_OMEGA_FULL    → contêiner STORE, mmap e manifesto
llamaRafaelia        → consumo pelo loader/modelo
Vectras-VM-Android   → execução mapeada no Android
Rafaelia_Private     → política privada deny-by-default
GAIA_phi             → recibo pointer-only
RafGitTools          → índice, gates e auditoria federada
```

Invariantes comuns:

- `storage_method=STORE`;
- `decompression_required=false`;
- `BUFFER → L1_HOT → L2_SHARED`;
- no máximo oito núcleos lógicos;
- bits fixos obrigatoriamente preservados;
- nenhum controle físico de cache é alegado;
- nenhum corpo é copiado entre autoridades;
- `claim_allowed=false`.

O validador recusa autoridade ausente, descompressão escondida, mais de oito núcleos, mutação da política de bits fixos ou promoção de claim.