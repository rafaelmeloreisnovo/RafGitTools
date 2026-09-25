# Receipt — ZIPRAF Bit Layer Inspector Phase A V1

Date: 2026-09-25  
Producer authority: `rafaelmeloreisnovo/RafPolimata@d52afbc38acf6d9580b32cbf9f7f259fa4afdf4b`  
Consumer: `rafaelmeloreisnovo/RafGitTools`  
claim_allowed=false

## Delta

Materializes a read-only stdlib inspector for an explicitly supplied ZIPRAF Phase-A vector JSON.

The adapter pins upstream repository, merge, path, Git blob SHA and contract identifiers. It validates bit-plane witnesses and q reconstruction, hashes the supplied source bytes, and fails closed if geometry gaps are promoted or a mutating mode is requested.

It performs no network fetch and no mutation.

## Boundary

```text
RafPolimata = FORMAT/VECTOR AUTHORITY
RafGitTools = READ_ONLY INSPECTOR
INSPECTION_PASS != T-BL-010_PASS
INSPECTION_PASS != G(M)_DEFINED
SOURCE_HASH != CRYPTOGRAPHIC_PROVENANCE_CHAIN
```

Physical runtime and Vectra/Termux parity remain TOKEN_VAZIO.
