# Inventário cruzado `√/sqrt` — 2026-07-21

**Estado:** fotografia conservadora, não exaustiva.  
**Política:** `LICENSES/SQRT_REFERENCES_AND_CHILD_PROTECTION.md`  
**Regra:** localização em repositório próprio, assinatura textual ou nome RAFAELIA são evidências; não bastam isoladamente para declarar autoria exclusiva.

## 1. Promoções realizadas neste ramo

| Origem | Destino em RafGitTools | Decisão | Estado autoral |
|---|---|---|---|
| `rafaelmeloreisnovo/Vectras-VM-Android@6ab34fcfbbaa7fa3536507d9c42d066d2fe94365:docs/rafaelia_reference/sqrt3_2_kernel.md` | `docs/rafaelia_reference/sqrt3_2_kernel.md` | Documento RAFAELIA isolado, importado com origem explícita e limites científicos | `rafaelia_original`; autoria exclusiva ainda não liberada |
| `rafaelmeloreisnovo/Vectras-VM-Android@6ab34fcfbbaa7fa3536507d9c42d066d2fe94365:.ci/matrixbitraf.c` — blob `8fb9960fb821a26eef1538ca9a4270dadd5d3a6d` | `rafaelia/matrix/raf_matrix_q16.h` | Núcleo 10×10×10 promovido, nomes normalizados, API defensiva e teste C independente | `rafaelia_original`; algoritmo geral de raiz é matemática padrão; genealogia pendente |
| `rafaelmeloreisnovo/termux-app-rafacodephi@0940da1c6017910d2ccd7951b63b6480df606b5b:mvp/rafaelia_mvp_puro.s` | `_incoming/repo_mvp_puro.s` | Já presente; preservado em `_incoming` até revisão binária e genealógica | `rafaelia_original`; `claim_allowed=false` |

## 2. Núcleos de alta prioridade — integrar por reconciliação, não por cópia

### RafPolimata / Q16

Arquivos observados no commit `2a3d1534909c687d36fd6a0d3a3986066bb0435c`:

- `Benchmark/raf_types.h` — blob `b61d9e8ae12150f679585208d582a6406ecea0a7`;
- `Benchmark/raf_q16.h` — blob `9c86aef0ee31e8abe45fcbff3e13b30fa83810b8`.

Conteúdo relevante:

- tipos freestanding e constantes Q16;
- `Q16_SQRT3_2`, `q16_spiral`, `q16_phi_ethica` e `q16_fraf_next`;
- filtro IIR e funções auxiliares sem `libm`.

**Decisão:** não duplicar diretamente. O RafGitTools já possui `rafaelia/block1/raf_geom.h/.c` com símbolos sobrepostos. O próximo passo correto é comparar função por função, escolher uma fonte canônica e preservar os commits/blobs de ambas as linhagens.

## 3. Termux — extrair contratos, não carregar o aplicativo

Arquivos observados no commit `0940da1c6017910d2ccd7951b63b6480df606b5b`:

- `app/src/main/java/com/termux/lowlevel/InternalPrograms.java` — blob `8d3501a7bc02a3b7a9a5c67a458d81b3c0001840`;
- `rafaelia/src/main/java/com/termux/rafaelia/RafaeliaUtils.java` — blob `ea9170f3fbe14ea3437793c336a54738d33288ef`;
- `BugOrAdd/final.s`;
- `scripts/rafcodephi_auditor.sh`;
- `mvp/rafaelia_mvp_puro.s` e cópia em `Arme/Add/repo_mvp_puro.s`;
- `SUMMARY.md`, `DOCUMENTACAO.md` e `INVENTARIO.md`.

Os dois arquivos Java expõem raiz, norma vetorial, distância euclidiana e ponte nativa, mas estão acoplados aos pacotes Termux, `BareMetal`, JNI e carregamento de biblioteca.

**Decisão:** não copiar as classes. Extrair futuramente apenas contratos RAFAELIA independentes, após:

1. localizar as implementações JNI/C correspondentes;
2. provar compatibilidade de licença;
3. remover dependência de pacote Termux;
4. adicionar benchmark comparativo antes de afirmar ganho sobre `Math.sqrt`;
5. eliminar duplicações entre `mvp/` e `Arme/Add/`.

## 4. Vectras — separar casca virtualizadora do núcleo autoral

Ocorrências observadas no commit `6ab34fcfbbaa7fa3536507d9c42d066d2fe94365` incluem:

- `docs/rafaelia_reference/sqrt3_2_kernel.md`;
- `.ci/matrixbitraf.c`;
- `_incoming/rafaelia_arm.c`;
- `_incoming/pending/r.S`;
- `docs/for.md` e `docs/33.md`.

**Decisão:** somente os dois primeiros foram promovidos neste ramo. QEMU, telas, firmware, virtualização, Vectras upstream e arquivos `_incoming/pending` não são classificados como núcleo autoral apenas por estarem no fork.

## 5. Corpus privado e CientiEspiritual — quarentena probatória

A busca encontrou diversas ocorrências em `Rafaelia_Private` e `CientiEspiritual-tiEs-`, inclusive:

- `RAFAELIA_TOROIDAL.py`, `src/python/raf_hive_core.py`, `Zipraf/generator_Version2.py`;
- `core a incluir/raf_core_v9.py`, `Anews/mem.py`, `Anews/fisca_aplicada.py`;
- `F222.txt`, `Gggfd.txt`, `Type.txt`, `Grafeno espiritual.txt`, `Fomega.py`.

**Decisão:** nenhuma importação automática. Antes de promoção, cada arquivo exige deduplicação, busca de segredos e dados pessoais, licença, histórico e separação entre código executável, hipótese, parábola e material conversacional.

## 6. Referência acadêmica protegida

A presença de `√/sqrt` **não implica** dependência da Regressão de Júlia. O identificador `[RJ-RPM107-2023]` somente deve ser usado quando houver relação conceitual ou implementacional específica com o trabalho publicado.

A estudante permanece como `ESTUDANTE_MENOR_PROTEGIDA_RJ` no repositório. O professor é registrado bibliograficamente como **Frederico Ferreira de Pinho Tavares**. Paginação, ordem autoral completa e eventual segundo professor permanecem `TOKEN_VAZIO` até verificação direta da RPM 107.

## 7. Ordem de fechamento

1. Executar `scripts/audit_sqrt_provenance.py` em modo informativo e gerar inventário completo.
2. Resolver primeiro os arquivos executáveis sob `rafaelia/` e `_incoming/`.
3. Reconciliar RafPolimata Q16 com `rafaelia/block1`, evitando símbolos concorrentes.
4. Extrair do Termux apenas APIs independentes comprovadas.
5. Promover telas/benchmarks somente quando os arquivos e recursos tiverem origem demonstrável.
6. Manter toda origem incerta em `needs_review`; nunca converter silêncio em autoria.

---

**Resultado deste ciclo:** o RafGitTools recebe núcleos específicos e auditáveis; Termux e Vectras permanecem fontes de proveniência, não recipientes a serem copiados integralmente.
