# RafGitTools — Living Book IR Bundle V1

Este adaptador transforma uma IR não executável do `RafPolimata` em um **bundle de transporte somente com descritores**.

```text
IR por digest
→ preflight fail-closed
→ manifesto descriptor-only
→ política de aprovação humana
→ bundle READY_FOR_REVIEW_NOT_DISPATCHED
```

O bundle não incorpora:

- semente ou resumo humano;
- mensagens ou conversa;
- conteúdo privado;
- credenciais, tokens, cookies ou segredos;
- bytes da IR;
- alvo de rede;
- autorização de execução, publicação ou promoção de claim.

Comando de referência:

```bash
python3 tools/living_book_ir_bundle/build_living_book_ir_bundle.py \
  --ir /caminho/INT-MUSIC-0001.ir.json \
  --bundle-id LBB-MUSIC-0001 \
  --producer-repo instituto-Rafael/LivroVivo_ThisBookLives \
  --producer-ref 6e51364d43642cdd65d6d4d50d52c7124394b07a \
  --compiler-repo rafaelmeloreisnovo/RafPolimata \
  --compiler-ref 480dee81b397c9f5a716aed203e67292829d8e82 \
  --out COMPILA/living-book/LBB-MUSIC-0001.bundle.json
```

Estados fixos na V1:

```text
transport_mode=DESCRIPTOR_ONLY
human_approval_state=REQUIRED_BEFORE_DISPATCH
dispatch_allowed=false
execution_allowed=false
publication_allowed=false
claim_allowed=false
network_target=null
```

Ações permitidas na IR:

```text
INDEX_ONLY
PROPOSE_ANALYSIS
PROPOSE_TRANSLATION
PROPOSE_TEST
```

O adaptador usa JSON canônico e digest triplo:

```text
SHA-256
SHA3-256
BLAKE2b-256
```

BLAKE3 permanece perfil futuro quando uma implementação verificada estiver disponível no ambiente. Hash demonstra identidade e alteração, não verdade, autorização ou execução.

Este módulo não substitui o `LivroVivo`, o `Mapa`, o `RafPolimata`, o cockpit Termux nem a custódia privada do Drive.
