# Vocabulários Semânticos RAFCODE-Φ

Esta camada amplia o diretório `Livro` com vocabulário operacional, clusters semânticos, varredura de corpus local, seleção de métodos e scheduler de próxima ação.

Ela não substitui o `falas.sh`; ela fica acima dele como camada de decisão:

```text
FALA → FONEMA → TOKEN → AST → BYTECODE → ASM → VM → OUTPUT
```

## O que foi adicionado

| Arquivo | O que faz |
| --- | --- |
| `semantic_vocab.py` | Motor de vocabulário, cluster, score `phi`, scan do `Livro`, decisão de scheduler e hints RVM. |
| `falas_vocab.sh` | Wrapper que roda tudo: gera monólito, manifesto, vocabulário, smoke-test, schedule e explicação dos métodos. |
| `VOCABULARIOS_SEMANTICOS_RAFCODE.md` | Documentação do sistema semântico. |

## O que o `semantic_vocab.py` faz

Ele recebe texto e transforma em um quadro operacional:

1. normaliza Unicode;
2. tokeniza por script;
3. identifica clusters semânticos;
4. mede cobertura;
5. calcula `phi`;
6. lista gaps;
7. escolhe métodos;
8. gera bytecode/hints RVM;
9. decide o próximo passo pelo scheduler.

## Clusters fixos

| Cluster | Domínio | Uso |
| --- | --- | --- |
| `CRIAR` | ação | gerar, emitir, produzir, criar forma operacional. |
| `FALA` | entrada | voz, áudio, fonema, fala. |
| `TOKEN` | representação | lexema, símbolo, chunk, unidade mínima. |
| `AST` | estrutura | árvore, parse, sintaxe, dependência. |
| `BYTECODE` | execução | opcode, VM, RVM, instrução. |
| `ASM` | baixo_nível | assembly, ARM64, AArch64, NEON, syscall. |
| `COERENCIA` | verificação | phi, estabilidade, integridade, consistência. |
| `RUIDO` | diagnóstico | gap, erro, entropia, ambiguidade. |
| `SCHEDULER` | operação | cache, latência, reuse, thread, pipeline. |
| `BIBLIA_CORPUS` | corpus | hebraico, grego, latim, logos, gênesis. |
| `AGENTE` | controle | plano, ação, observação, feedback. |
| `TERMUX_ANDROID` | operação | Termux, Android, AndroidX, Gradle, NDK/JNI, Vectras. |
| `QEMU_TCG` | execução | QEMU, TCG, JIT, tradução guest→host. |
| `MEMORIA_CACHE` | baixo_nível | memória, cache, L1/L2, prefetch, warm/cold/ghost state. |

## Métodos operacionais

| Método | Estágio | O que faz |
| --- | --- | --- |
| `tokenize` | FALA→FONEMA→TOKEN | Separa entrada em unidades rastreáveis. |
| `parse_ast` | TOKEN→AST | Organiza tokens em forma estrutural. |
| `semantic_expand` | TOKEN→CLUSTER→CONTEXTO | Amplia cobertura semântica sem inventar. |
| `compile_rvm` | AST→BYTECODE→VM | Emite forma executável compacta. |
| `audit_phi` | OUTPUT→VERIFICAÇÃO | Bloqueia saída fraca e expõe lacunas. |
| `agent_loop` | PLAN→ACT→OBSERVE→VERIFY | Decide próximo método operacional. |
| `scan_livro` | CORPUS→VOCAB→CLUSTER | Aprende termos em uso no próprio diretório `Livro`. |

## Scheduler

O scheduler retorna sempre um destes estados:

| Estado | Significado |
| --- | --- |
| `F_DE_RESOLVIDO` | Cobertura e `phi` suficientes para seguir. |
| `F_DE_GAP` | Há termos sem cluster; precisa expandir ou varrer corpus. |
| `F_DE_NEXT` | Não há gap crítico, mas ainda existe próximo método necessário. |

## Uso completo

```bash
cd Livro
chmod +x falas.sh falas_vocab.sh
./falas_vocab.sh
```

Isso gera:

```text
compiladorlowFala.txt
compiladorlowFala.manifest.json
semantic_vocab.export.json
semantic_vocab.export.smoke.json
semantic_vocab.export.schedule.json
semantic_vocab.export.methods.json
```

## Uso direto do vocabulário

### Analisar frase

```bash
python3 Livro/semantic_vocab.py --pretty "compilar fala em bytecode com coerência phi"
```

### Varrer o diretório Livro e ampliar vocabulário

```bash
python3 Livro/semantic_vocab.py --root Livro --scan-livro --pretty "QEMU TCG VM cache scheduler"
```

### Ver só a decisão do scheduler

```bash
python3 Livro/semantic_vocab.py --root Livro --scan-livro --schedule --pretty "Termux Android QEMU bytecode cache"
```

### Explicar métodos

```bash
python3 Livro/semantic_vocab.py --explain-methods --pretty
```

### Exportar vocabulário com termos aprendidos do Livro

```bash
python3 Livro/semantic_vocab.py --root Livro --scan-livro --export-vocab --pretty > Livro/semantic_vocab.export.json
```

## Regra de integridade

- Termo conhecido entra em cluster.
- Termo desconhecido vira `gap`.
- Gap não é preenchido por invenção.
- `scan_livro` só cria clusters derivados de termos realmente encontrados nos arquivos locais.
- `compile_rvm` só deve vir depois de `phi` e cobertura mínimos.

## Para que serve na prática

Essa camada permite transformar o `Livro` em um sistema mais usável:

- saber o que o texto está tentando fazer;
- localizar vocabulário em uso no próprio repo;
- decidir se deve tokenizar, expandir, compilar ou auditar;
- produzir pistas de bytecode para a VM;
- reduzir resposta ornamental e forçar rastreabilidade.

SEAL: 0xFF · RAFCODE-Φ
