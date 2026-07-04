# Vocabulários Semânticos RAFCODE-Φ

Este arquivo documenta a camada `semantic_vocab.py`, criada para ampliar o diretório `Livro` com vocabulários, clusters semânticos e seleção de métodos operacionais.

A ideia é simples e operacional: antes de compilar ou responder, o sistema identifica termos, scripts, domínio semântico, método provável e possíveis lacunas. Isso reduz preenchimento falso e melhora a passagem:

```text
FALA → FONEMA → TOKEN → AST → BYTECODE → ASM → VM → OUTPUT
```

## Arquivo principal

```text
Livro/semantic_vocab.py
```

## Funções centrais

| Função | Papel |
| --- | --- |
| `normalize(text)` | Normaliza Unicode em NFKC e reduz espaços. |
| `detect_script(char)` | Detecta HE, AR, EL, CN, JP, RU, latin, numérico ou separador. |
| `tokenize(text)` | Gera tokens com `raw`, `norm`, `script`, `cluster` e `phi`. |
| `expand_semantics(tokens)` | Agrupa tokens em clusters e identifica lacunas. |
| `choose_methods(text)` | Escolhe os três métodos mais coerentes para o estado atual. |
| `rvm_hints(tokens)` | Emite uma sequência sugerida de opcodes RVM. |
| `context_frame(text)` | Produz o quadro completo: tokens, semântica, métodos, gaps e próximo passo. |
| `export_vocab()` | Exporta o vocabulário completo em JSON. |

## Métodos disponíveis

| Método | Estágio | Uso |
| --- | --- | --- |
| `tokenize` | FALA→FONEMA→TOKEN | Separar, normalizar, detectar script e preparar lexemas. |
| `parse_ast` | TOKEN→AST | Montar estrutura sintática mínima. |
| `semantic_expand` | TOKEN→CLUSTER→CONTEXTO | Expandir sentido, aliases, glossas e clusters. |
| `compile_rvm` | AST→BYTECODE→VM | Sugerir opcodes e ponte com runtime. |
| `audit_phi` | OUTPUT→VERIFICAÇÃO | Medir coerência, cobertura e lacunas. |
| `agent_loop` | PLAN→ACT→OBSERVE→VERIFY | Escolher próximo método por estado. |

## Clusters semânticos iniciais

| Cluster | Domínio | Finalidade |
| --- | --- | --- |
| `CRIAR` | ação | gerar, emitir, produzir, criar forma operacional. |
| `FALA` | entrada | voz, áudio, fonema, utterance. |
| `TOKEN` | representação | lexema, símbolo, chunk, unidade mínima. |
| `AST` | estrutura | árvore, parse, sintaxe, dependência. |
| `BYTECODE` | execução | opcode, VM, RVM, instrução. |
| `ASM` | baixo_nível | assembly, ARM64, AArch64, NEON, syscall. |
| `COERENCIA` | verificação | phi, estabilidade, integridade, consistência. |
| `RUIDO` | diagnóstico | gap, erro, entropia, ambiguidade. |
| `SCHEDULER` | operação | cache, latência, reuse, thread, pipeline. |
| `BIBLIA_CORPUS` | corpus | hebraico, grego, latim, logos, gênesis. |
| `AGENTE` | controle | plano, ação, observação, feedback. |

## Uso rápido

### Analisar uma frase

```bash
python3 Livro/semantic_vocab.py --pretty "compilar fala em bytecode com coerência phi"
```

Saída esperada:

```json
{
  "semantic": {
    "dominant_domain": "execução",
    "phi": 0.78
  },
  "methods": [
    {
      "method": "compile_rvm",
      "stage": "AST→BYTECODE→VM"
    }
  ],
  "next": "compile_rvm"
}
```

### Exportar vocabulário completo

```bash
python3 Livro/semantic_vocab.py --export-vocab --pretty > Livro/semantic_vocab.export.json
```

## Integração recomendada com `falas.sh`

Depois de gerar `compiladorlowFala.txt`, rode:

```bash
python3 Livro/semantic_vocab.py --pretty "FALA TOKEN AST BYTECODE VM COERENCIA"
```

Isso cria uma leitura rápida do estado semântico do pipeline e aponta o próximo método com base em domínio e cobertura.

## Regras de coerência

1. Se o termo entra em cluster conhecido, ele recebe maior `phi`.
2. Se o termo não entra em cluster, ele aparece em `gap`.
3. Se o domínio dominante for execução, os métodos `compile_rvm`, `audit_phi` e `agent_loop` ganham prioridade.
4. Se o domínio dominante for diagnóstico, `audit_phi` vem antes de gerar mais código.
5. Se o vocabulário não cobre o termo, o sistema não inventa: marca lacuna.

## Próxima expansão natural

- Adicionar termos vindos de `bibliaCorpus.txt` como glossário multilíngue.
- Conectar `vm_runtime.txt` para validar opcodes aceitos pelo runtime real.
- Ligar `agent_loop.txt` ao campo `next`, transformando método sugerido em ação executável.
- Acrescentar exportação automática ao `falas.sh`.

SEAL: 0xFF · RAFCODE-Φ
