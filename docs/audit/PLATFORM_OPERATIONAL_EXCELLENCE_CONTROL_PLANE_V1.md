# RAFAELIA Platform Assurance Control Plane v1

## Estado

```text
authority       = rafaelmeloreisnovo/RafGitTools
claim_allowed   = false
promotion_ready = 0
work_items      = 12
```

Este artefato não substitui o `Mapa`, os contratos científicos do RLL, o
runtime Termux ou o consumidor de memória. Ele coordena a **decisão
operacional** entre essas autoridades.

## Problema resolvido

A plataforma já possuía mapas, contratos e adaptadores, mas ainda precisava de
uma visão transversal que respondesse, para cada entrega:

1. qual repositório é responsável;
2. qual evidência foi realmente observada;
3. qual dimensão está bloqueando;
4. quais dependências vêm antes;
5. qual é o critério objetivo de saída;
6. como reverter sem destruir a cadeia anterior.

A regra central é:

```text
arquivo presente
!= código integrado
!= teste executado
!= artefato produzido
!= runtime no aparelho
!= certificação
```

## Dimensões obrigatórias

Todo item possui exatamente onze dimensões:

| Dimensão | Pergunta |
|---|---|
| `code` | O código foi materializado e revisado? |
| `tests` | Existem testes positivos e adversariais executados? |
| `ci` | Houve steps e logs observáveis? |
| `artifact` | O binário/JSON/APK foi produzido e selado? |
| `runtime` | Rodou no ambiente-alvo? |
| `security` | A superfície de ataque foi delimitada? |
| `rights` | Licença, autoria e uso estão comprovados? |
| `documentation` | O estado real está documentado? |
| `authority` | O repositório correto é dono da decisão? |
| `rollback` | Há retorno seguro ao estado anterior? |
| `provenance` | Ref, hash, fonte e cadeia de custódia estão fixados? |

Não existe média compensatória. `security=BLOCKED` não pode ser escondido por
`documentation=PASS`.

## Prioridades atuais

### P0 — urgentes

1. **Termux CI #289** — merge estrutural realizado; execução pós-merge ainda
   precisa produzir steps e artefato.
2. **Termux loader #290** — bloqueado por autorização, URL/destino controlados
   pelo chamador, ausência de limites de ZIP e handoff entre pacotes não
   definido.
3. **RLL FASE29 #582** — bloqueado por contagem inconsistente, ΔAIC que não
   fecha, licenças não comprovadas e ledger histórico sem cadeia de hash.
4. **Device receipt** — o coletor existe, mas falta
   `DEVICE_RECEIPT_COMPLETE`.
5. **Execução de Actions** — jobs sem steps/logs permanecem
   `ZERO_STEP_NO_LOGS`.

### P1 — fechamento técnico

- TLS BrowserRaf;
- emissão/verificação/execução ELF;
- writer/verificador/ART DEX;
- QEMU → Vectras → guest boot;
- avaliação local do `llamaRafaelia`.

### P2 — sustentação

- primeiro patch semântico revisado do intake de documentos;
- benchmarks e ABI atuais para AndroidX RmR e Gradle Vectra.

## Achados críticos desta auditoria

### Termux PR #290

A Activity exportada recebe `source_url` e `target_dir`. Antes de merge, o
loader precisa de:

```text
permission signature
+ origem HTTPS permitida
+ destino imutável/staging privado
+ limites de download e expansão
+ transação atômica
+ resultado explícito ao host
+ teste de chamador não autorizado
```

### RLL PR #582

O bundle contém três `pass_ids`, mas declara `pass=2`. Além disso:

```text
ΔAIC = Δχ² + 2Δk
```

Com os próprios valores materializados:

```text
Δχ² = 710.613 - 710.808 = -0.195
Δk  = 6 - 3 = 3
ΔAIC = 5.805
```

O arquivo registra `3.805`. A aritmética deve ser derivada do artefato, não
digitada manualmente.

Repositório ou arquivo público também não equivale a domínio público. DESI
possui CC BY 4.0 explícita; Pantheon+ e Planck exigem verificação específica
antes de `license_verified=true`.

## Estados

### Estado do item

```text
MERGED_LIMITED
PARTIAL
BLOCKED
TOKEN_VAZIO
CLOSED
```

### Estado da dimensão

```text
PASS
PASS_LIMITED
PARTIAL
BLOCKED
TOKEN_VAZIO
ZERO_STEP_NO_LOGS
NOT_APPLICABLE
```

`MERGED_LIMITED` significa que a mudança entrou, mas ainda não encerrou todas
as provas posteriores.

## Ordem de execução

```text
P0 segurança/verdade
→ CI observável
→ artifacts
→ aparelho/runtime
→ integração cross-repository
→ performance
→ certificação externa
```

## Validação

```sh
python3 -m unittest \
  tests/test_platform_assurance_control_plane.py -v

python3 scripts/platform_assurance_control_plane.py \
  configs/platform-assurance/index.json \
  --write-report artifacts/platform-assurance-report.json
```

O gate também é executado por:

```sh
sh scripts/validate_rafaelia_workflow.sh
```

## Limites

O control plane:

- não executa automaticamente escrita em outros repositórios;
- não faz merge automático;
- não converte ausência em falha ou sucesso;
- não certifica TLS, Android, ciência ou licenças;
- não substitui revisão humana;
- não promove `ZERO_STEP_NO_LOGS` a falha de código;
- não permite que documentação compense segurança ou direitos.

## R3

```text
F_ok   = control plane determinístico, 12 itens e gate não compensatório
F_gap  = execução remota observável, aparelho, segurança do loader e direitos
F_next = fechar P0 antes de ampliar capacidade ou performance
```
