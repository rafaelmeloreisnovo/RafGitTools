# Índice de implementação — validade de conteúdo e TOKEN_VAZIO

Data: 2026-07-17  
Estado: `IMPLEMENTED_LOCAL_TESTED`  
Claim boundary: `claim_allowed=false` para qualquer promoção científica derivada
apenas deste contrato.

## Autoridade

`RafGitTools` é o produtor canônico do contrato. Repositórios consumidores podem
apontar para versão/commit, mas não devem copiar e alterar a lógica silenciosamente.

## Artefatos

| Artefato | Função |
|---|---|
| `configs/content_validity_contract.json` | dimensões, estados, pesos e gates |
| `docs/CONTENT_VALIDITY_TOKEN_VAZIO_CONTRACT.md` | especificação humana |
| `scripts/content_validity_contract.py` | validador dependency-free |
| `examples/content_validity.example.json` | exemplo com token válido e TOKEN_VAZIO |
| `tests/test_content_validity_contract.py` | casos positivos e negativos |
| `scripts/validate_rafaelia_workflow.sh` | gate canônico integrado, sem novo workflow |

## Invariantes implementadas

```text
I01 TOKEN_VAZIO não é numérico
I02 peso numérico exige evidência
I03 promoção exige todas as dimensões obrigatórias
I04 média não compensa dimensão ausente
I05 janela cross-source exige ponte
I06 overlap < max_tokens
I07 truncamento é declarado
I08 negação, modalidade, condições, exceções, números e unidades são campos explícitos
I09 contradição aberta bloqueia promoção
I10 TOKEN_VAZIO exige razão, owner, próxima ação e critério de saída
I11 token promovido pertence a ao menos uma janela
I12 VALID significa validade limitada ao uso/domínio/tempo declarados
```

## Validação local

```text
13 testes executados
13 PASS
0 dependências externas
```

## Estados honestos

```text
contrato estrutural executável = VERIFIED_LOCAL
integração no gate shell = IMPLEMENTED
execução GitHub Actions = TOKEN_VAZIO até run do PR
calibração em tokenizer/modelo real = TOKEN_VAZIO
pesos aprendidos por ML = NOT_IMPLEMENTED
validade científica de conteúdo externo = OUT_OF_SCOPE
```

## Próxima promoção possível

O próximo estado exige:

1. CI executar o gate canônico;
2. manifesto real de um consumidor;
3. tokenizer, modelo, contexto e orçamento de memória versionados;
4. análise de falsos positivos e falsos negativos;
5. pin do contrato por commit/digest nos consumidores.
