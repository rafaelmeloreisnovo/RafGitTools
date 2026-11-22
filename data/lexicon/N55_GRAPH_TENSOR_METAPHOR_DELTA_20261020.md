# Δ N55 | léxico, proveniência e relações

Estado: `PROPOSED_REVIEW_NOT_COMMITTED`, `claim_allowed=false`.

Autoridade histórica: N55 como política de recuperação informacional, diferente de designações homônimas em outras áreas. Os 121 relacionamentos lexicais existentes NÃO são substituídos. As 18 famílias C01–C18 foram vinculadas a uma fonte de cálculo; não equiparar sem evidência à expressão histórica "18 heurísticas".

Contrato candidato para aresta relacional:
- `source_pointer`: objeto canônico e revisão/hash conhecido
- `source_surface`: expressão original preservada
- `relation_type`: ANALOGY, FORMAL_IDENTITY, HYPOTHESIS, EVIDENCE, CONTRADICTION, FALSIFIER
- `embedding_model`: nome, versão, dimensão, tokenizer ou `TOKEN_VAZIO_MODEL`
- `evidence_state`: SOURCE_ONLY, NOT_RUN, PASS_SCOPED, FAIL, TOKEN_VAZIO
- `angle_coordinate`: opcional, com unidade e derivação verificável
- `physical_unit`: somente quando tiver medições físicas; não atribuir tesla a vetores semânticos
- `negative_control`: teste capaz de refutar a interpretação
- `claim_allowed`: false por padrão

Invariantes: `SURFACE_WORD != MODEL_TOKEN`; `TRANSLATION != SEMANTIC_IDENTITY`; `TOKEN_VAZIO != OBSERVED_ZERO`; `SIMILARITY != EVIDENCE`; `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`.

Rota: N55 session → Lexicon 121 → C01..C18 → Mathematics angular checker → papers preregistration → receipt → index. Nenhum pipeline lexical ou treinamento modificado.
