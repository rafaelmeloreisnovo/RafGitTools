# Adaptador local de despacho de sessão IA — V1

## Autoridade e limite

O catálogo federado continua em `rafaelmeloreisnovo/Mapa`, no merge commit
`ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09`, arquivo
`data/control-plane/SESSION_AI_WORK_DISPATCH_V1.json`, blob
`c1d1d8f1692c74957f8b70968fa863f855d93cb0`. Esse pin preserva a identidade do
merge corrigido que contém o mesmo blob validado; ele não afirma que o `main`
móvel do Mapa deva permanecer nesse commit. RafGitTools fornece somente um
resolvedor local e somente leitura. O adaptador confere o `HEAD`, o blob Git e o
schema antes de expor uma rota. Uma divergência resulta em `ROUTE_STATE_BLOCKED`.

## Uso

Com o checkout Mapa exatamente no commit pinado:

```bash
python3 scripts/resolve_session_ai_work_packet.py \
  --mapa-checkout /caminho/para/Mapa \
  --packet SP08_SESSION_TO_WORK_PACKETS
```

Para resolver um papel individual, use `--agent AI02_CONTROL_EXECUTOR` no lugar
de `--packet`. A saída inclui autoridade, proprietário, missão, `source_min`,
alvo e regra de evidência diretamente do manifesto federado.

## Evidência e promoção

`ROUTE_RESOLVED` comprova apenas a leitura estrutural do manifesto pinado.
`assignment_is_execution=false`, `evidence_state=STRUCTURAL_ROUTE_ONLY` e
`claim_allowed=false` permanecem invariantes. Despachar ou listar um papel não
executa agentes, gates, benchmark, stress concorrente, build Android ou runtime
físico. Para atualizar a origem, primeiro atualize e audite o pin do Mapa com
testes correspondentes; não copie o catálogo para este repositório.

## Testes

```bash
python3 -m unittest discover -s tests -p 'test_resolve_session_ai_work_packet.py' -v
```

Os testes usam manifesto sintético e checkout Git temporário. Eles verificam a
política de validação e bloqueio; não são evidência de execução dos papéis
federados nem substituem a leitura do Mapa autoritativo.
