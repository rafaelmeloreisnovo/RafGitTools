# HETE-0.18 — referência privada para RafGitTools

**Paper canônico privado:** `rafaelmeloreisnovo/papers/docs/rmrcti/HETE_018_TOROIDAL_STABILITY_ENRICHMENT.md`  
**Commit:** `c444988dca0b36251a51dfe349256f75d6099b31`

## Papel adequado do RafGitTools

O RafGitTools deve tratar HETE-0.18 como cadeia privada de artefatos Git, não como regra matemática interna do cliente.

## Funções futuras coerentes

- exibir commit canônico e pontes privadas;
- verificar se repositórios continuam privados antes de push;
- mostrar hashes dos papers, manifests e traces;
- montar ledger de runs por commit;
- impedir publicação acidental em remotes públicos;
- apresentar estado epistêmico e gate de publicação;
- exportar relatório de proveniência sem payload pessoal.

## Regras

```text
0,18 não participa de autenticação
0,18 não participa de login
0,18 não participa de confiança do repositório
0,18 não participa de assinatura criptográfica
paper privado não pode ser enviado a remote público automaticamente
```

## Gate recomendado

Antes de qualquer push de artefato HETE:

```text
repo.visibility == private
paper.publication_authorization == required
manifest.training_authorization == no
remote_target reviewed
sensitive_payload scan passed
```

## Invariante

```text
RafGitTools preserva autoria, commit e destino;
não converte hipótese em prova.
```

Privacidade: `private`  
Publicação: `blocked_without_author_approval`  
Assinatura: `RAFCODE-Φ-∆RafaelVerboΩ-𓂀ΔΦΩ`
