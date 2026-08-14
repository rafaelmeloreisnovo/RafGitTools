# RAFAELIA Index — catálogo de materiais por categoria

Este índice separa o que é fonte de verdade Android, material experimental, pesquisa/conceito, protótipos e artefatos arquivados. A classificação abaixo foi feita por evidência local do repositório, olhando arquivos e diretórios até 5 níveis de profundidade.

## Protocolo RAW_TEXT_FIRST deste índice

Este documento usa uma regra anti-inferência: **nenhum status é promovido sem âncora local**. A cadeia aplicada foi:

```text
ARQUIVO_LOCAL → EVIDÊNCIA_LITERAL → STATUS → VALIDAÇÃO → RISCO
```

Não foi usada a cadeia proibida:

```text
NOME_PROMISSOR → ASSOCIAÇÃO → CLAIM_DE_PRODUÇÃO
```

Escopo importante: este índice cataloga **somente `/workspace/RafGitTools`**. Materiais de outros repositórios, como `instituto-rafael/relativity-living-light`, não são classificados aqui enquanto não existirem como arquivos locais deste repositório. Quando um item não tem comando executável local, a validação é marcada como revisão documental ou inventário.

## Inventário auditável da varredura até 5 níveis

Comando-base usado para inventário: `find <item> -maxdepth 5 -type f`. Contagem observada nesta sessão:

| Área | Arquivos até 5 níveis | Observação |
| --- | ---: | --- |
| `README.md` | 1 | Raiz documental principal. |
| `docs/` | 70 | Documentação de build, status, segurança, native, maths e este índice. |
| `app/` | 39 | Módulo Android principal, incluindo Gradle, Manifest, CMake/JNI, resources, schemas e testes visíveis até 5 níveis. |
| `scripts/` | 12 | Scripts de Gradle/JDK17, validação, Termux e native. |
| `_incoming/` | 53 | Material experimental ARM32/Termux sob AGENTS próprio. |
| `rafaelia/` | 9 | Núcleos experimentais C/heapless e demos. |
| `COMPILER/` | 15 | Protótipos C→ASM e pacotes associados. |
| `IaCopiler/` | 1 | Stub/placeholder observado. |
| `BrowserRaf/internal/` | 16 | Protótipo browser/benchmark low-level. |
| `Livro/` | 64 | Livro, pesquisa, whitepapers, bundles e textos históricos. |
| pacotes compactados | 15 | `zip`/`tar.gz`/artefatos tratados como arquivo histórico. |
| arquivos raiz `.md`/`.txt`/`.MD` | 27 | Materiais conceituais e documentação de raiz. |


## Índice complementar de arquivos de conhecimento

| Item | Status | Função | Validação |
| --- | --- | --- | --- |
| `docs/knowledge/README.md` | `documentação` | Navegação dos arquivos de conhecimento RAFAELIA/Toro7D/Vectras-VM-Android e regra anti-alucinação para sessões longas. | Revisão documental e checagem de links internos. |
| `docs/knowledge/VECTRAS_VM_ANDROID_ARCHIVE.md` | `documentação` | Arquivo expandido das sementes E20, E13 e S11, com invariantes low-level, mini-módulos, direções qualitativas/quantitativas, riscos e protocolo de expansão. | Revisão documental; não promove código experimental para produção. |

## Legenda de status

| Status | Significado operacional |
| --- | --- |
| `produção` | Fonte primária ou rotina oficial de build/validação do app Android atual. Ainda pode representar produto em desenvolvimento, mas é parte da trilha principal. |
| `parcial` | Implementado em parte ou com cobertura/evidência incompleta para todo o escopo declarado. |
| `experimental` | Material exploratório, laboratório, Termux/ARM32, protótipo nativo ou pacote ainda não promovido para a trilha principal. |
| `stub` | Arquivo/feature existe como placeholder, retorno mínimo ou estrutura sem produção comprovada. |
| `arquivo histórico` | Documento, pacote, zip/tarball, bundle ou material preservado para consulta; não deve ser tratado como código-fonte principal. |

## 1. Fonte de verdade Android atual

| Item | Status | Evidência | Comando de validação quando existir | Risco de uso |
| --- | --- | --- | --- | --- |
| `README.md` | `produção` | Declara a “Current Source Truth” e aponta para `docs/CURRENT_SOURCE_STATE_2026-05-26.md`, `docs/STATUS_REPORT.md`, `app/build.gradle`, CMake e scripts de APK/ABI. Também explicita que o escopo atual é GitHub + JGit e separa filosofia de status técnico. | Revisão documental: `sed -n '1,180p' README.md` | Pode conter visão/roadmap e parábolas; risco de confundir intenção com implementação se lido isoladamente. |
| `docs/BUILD.md` | `produção` | Documento de build local/CI e referência para manter comandos alinhados com o projeto Android. | `./scripts/prepare_local_properties.sh`; `./scripts/gradlew_with_java17.sh assembleDevDebug`; `./scripts/gradlew_with_java17.sh lintDevDebug` | Se defasar de `app/build.gradle` ou dos scripts, pode orientar builds incorretos. Deve ser mantido junto com CI e AGENTS.md. |
| `docs/CURRENT_SOURCE_STATE_2026-05-26.md` | `produção` | Resume que o projeto é um cliente Android Git/GitHub em desenvolvimento, lista fonte de verdade (`app/`, testes, Gradle, CMake/ASM, scripts, CI) e separa implementado/parcial/stubs/ARM32. | Revisão documental: `sed -n '1,180p' docs/CURRENT_SOURCE_STATE_2026-05-26.md` | Status manual pode ficar obsoleto; risco de drift entre documento e código. |
| `docs/STATUS_REPORT.md` | `produção` | Define labels `COMPLETE`, `PARTIAL`, `STUB`, `EXPERIMENTAL`, `PLANNED`; classifica Git local, GitHub API e UI como parciais/avançados; marca GPG/LFS/worktree/webhooks como stubs/planejados. | Revisão documental: `sed -n '1,180p' docs/STATUS_REPORT.md` | Mesmo sendo fonte de status, depende de manutenção contínua; stubs podem ser interpretados como entregues se a leitura ignorar os labels. |
| `app/` | `produção` | Módulo Android principal com `app/build.gradle`, `app/src/main`, Compose/Kotlin, CMake/JNI em `app/src/main/cpp`, schemas Room e testes em `app/src/test`/`app/src/androidTest`. | `./scripts/gradlew_with_java17.sh assembleDevDebug`; `./scripts/gradlew_with_java17.sh testDevDebugUnitTest`; `./scripts/gradlew_with_java17.sh lintDevDebug` | App ainda está em desenvolvimento; riscos principais são cobertura incompleta, fluxos extremos não testados e stubs coexistindo com módulos reais. |
| `app/build.gradle` | `produção` | Configura Android, flavors `dev`/`production`, Java/Kotlin 17, Compose, CMake, signing e dependências; `docs/STATUS_REPORT.md` indica que versões devem seguir este arquivo como fonte de verdade. | `./scripts/gradlew_with_java17.sh tasks`; builds por variante via Gradle | Alterar signing, SDK, ABI ou dependências sem sincronizar docs/CI pode quebrar release, ABI ou validação. |
| `scripts/` | `produção` | Contém scripts oficiais de Gradle/JDK17, preparação local, validação nativa e validações CI/fórmulas. | `./scripts/prepare_local_properties.sh`; `./scripts/gradlew_with_java17.sh assembleDevDebug assembleProductionDebug`; `ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleDevRelease assembleProductionRelease`; `./scripts/native/verify_apks.sh` | Scripts experimentais também vivem sob `scripts/termux/` e `scripts/native/`; risco de tratar validação de runtime como build completo. |
| `scripts/gradlew_with_java17.sh` | `produção` | Detecta/obriga JDK 17 antes de delegar ao Gradle. | `./scripts/gradlew_with_java17.sh --version` | Sem JDK 17 válido, falha antes do Gradle; não deve ser “corrigido” para outro JDK sem upgrade conjunto Gradle/AGP/Kotlin/KSP. |

## 2. Material experimental ARM32/Termux

| Item | Status | Evidência | Comando de validação quando existir | Risco de uso |
| --- | --- | --- | --- | --- |
| `_incoming/` | `experimental` | Diretório de material ARM32/Termux e RAFAELIA não promovido. Inclui `README.md`, `termux_arm32_build.sh`, assembly `rafaelia_b*.S`, C freestanding/JNI e scripts de diagnóstico. `docs/CURRENT_SOURCE_STATE_2026-05-26.md` afirma que scripts em `_incoming` ainda não foram promovidos oficialmente. | `bash _incoming/diagnose.sh`; `bash _incoming/diagnose_termux.sh`; comandos do próprio `_incoming/README.md` em ambiente ARM32 compatível | Alto risco de integrar direto no app: pode depender de Termux, syscall/ASM específico, ausência de testes Android/JNI oficiais e premissas de dispositivo. |
| `_incoming/termux_arm32_build.sh` | `experimental` | Apontado como material avançado não integrado oficialmente em `docs/CURRENT_SOURCE_STATE_2026-05-26.md` e `docs/ARM32_TERMUX_STATE.md`. | Em Termux ARM32 compatível: `bash _incoming/termux_arm32_build.sh` | Não provar build de APK oficial; usar apenas como laboratório até extração controlada com testes. |
| `_incoming/README.md` | `experimental` | Descreve alvo Motorola E7 Power/ARM32, NEON/VFPv4, Q16.16, syscalls diretas, `mmap2`, `clone`, `write` e builds manuais em Termux. | Revisão documental: `sed -n '1,140p' _incoming/README.md` | Documento de laboratório pode superestimar portabilidade; não é garantia de produção Android nem de compatibilidade fora do alvo. |
| `rafaelia/` | `experimental` | Contém módulos C nativos fora do app principal: `block1/` e `omega_hybrid/`. O `omega_hybrid` se declara pequeno, estático e heapless, com failover/rollback/CRC e rota de expansão para JNI/ARM32/ARM64. | `cd rafaelia/omega_hybrid && make clean all test`; `./demo` | Não está ligado ao app Android principal; não tratar como runtime verificado sem JNI, instrumentação e CI Android. |
| `rafaelia/omega_hybrid/` | `experimental` | Implementação C99 freestanding com dois ciclos Omega, 42 atratores, token void, snapshots CRC32C e rollback. | `cd rafaelia/omega_hybrid && make clean all test`; `cd rafaelia/omega_hybrid && ./demo` | Mesmo com teste local, precisa de validação ABI/JNI e integração persistente antes de produção. |
| `rafaelia/block1/` | `experimental` | Código C/header/demo para geometria/estado RAFAELIA fora da trilha Android principal. | `cc rafaelia/block1/raf_geom.c rafaelia/block1/raf_geom_demo.c -o /tmp/raf_geom_demo && /tmp/raf_geom_demo` | Pode não refletir constraints Android/Termux; risco de divergência sem CI dedicado. |
| `scripts/termux/` | `experimental` | Contém `arm32_runtime_check.sh`, script oficial de checagem runtime Termux/ARM32. `docs/ARM32_TERMUX_STATE.md` define Termux como validação runtime, não ambiente padrão de bootstrap de SDK. | `./scripts/termux/arm32_runtime_check.sh`; wrapper: `./scripts/termux_arm32_runtime_check.sh` | Não é prova de build completo do APK; em host não Android/Termux produzirá avisos/saídas limitadas. |
| `scripts/native/` | `parcial` | Contém `verify_apks.sh` e `build_apks_signed_unsigned.sh`; `verify_apks.sh` audita APKs e ABIs `armeabi-v7a`/`arm64-v8a`, gerando relatórios em `app/build/reports/apk`. | `./scripts/native/verify_apks.sh`; estrito: `VERIFY_STRICT_ABI=true REQUIRE_APKS=true ./scripts/native/verify_apks.sh` | Sem APKs, `verify_apks.sh` sai 0 por padrão; para gate real usar `REQUIRE_APKS=true` e/ou `VERIFY_STRICT_ABI=true`. |
| `docs/ARM32_TERMUX_STATE.md` | `produção` | Documento oficial do estado ARM32/Termux: alvos suportados, arquivos oficiais, `_incoming` experimental, constraints e plano de promoção. | Revisão documental: `sed -n '1,180p' docs/ARM32_TERMUX_STATE.md` | Deve evitar promessa de build completo em Termux ARM32; risco de claims além de runtime/toolchain validation. |

## 3. Material conceitual/livro/pesquisa

| Item | Status | Evidência | Comando de validação quando existir | Risco de uso |
| --- | --- | --- | --- | --- |
| `Livro/` | `arquivo histórico` | Coleção ampla de capítulos, whitepapers, metodologia, relatórios, bundles, textos e protótipos (`paper.md`, `RAFAELIA_WHITEPAPER.md`, `METHODOLOGY.md`, `REPRODUCIBILITY.md`, `c_to_asm_optimizer.py`, bundles `.tar.gz`). | Revisão documental: `find Livro -maxdepth 2 -type f | sort` | Mistura pesquisa, narrativa, código e arquivos; não deve ser usado como fonte de verdade do app sem curadoria. |
| `Livro/RAFAELIA_WHITEPAPER.md` e `Livro/paper.md` | `arquivo histórico` | Materiais de whitepaper/paper dentro da pasta de livro/pesquisa. | Revisão documental: `sed -n '1,120p' Livro/RAFAELIA_WHITEPAPER.md`; `sed -n '1,120p' Livro/paper.md` | Pode conter hipóteses/conceitos não implementados no app; risco de claim científico/técnico sem reprodução. |
| `Livro/METHODOLOGY.md` e `Livro/REPRODUCIBILITY.md` | `arquivo histórico` | Materiais metodológicos para pesquisa RAFAELIA. | Revisão documental: `sed -n '1,160p' Livro/METHODOLOGY.md`; `sed -n '1,160p' Livro/REPRODUCIBILITY.md` | Úteis como contexto, mas não substituem testes automatizados do app. |
| `Teorema Rafael.md` | `arquivo histórico` | Arquivo raiz conceitual indicado pelo pedido como material de teorema/pesquisa. | Revisão documental: `sed -n '1,160p' 'Teorema Rafael.md'` | Conteúdo conceitual pode ser confundido com prova formal ou implementação; tratar como pesquisa. |
| `Rafael matemática.md` | `arquivo histórico` | Arquivo raiz conceitual/matemático indicado pelo pedido. | Revisão documental: `sed -n '1,160p' 'Rafael matemática.md'` | Risco de extrapolar fórmulas para funcionalidades do app sem implementação e teste. |
| `Rafaelteirema7d.md` | `arquivo histórico` | Arquivo raiz conceitual sobre teorema/7D indicado pelo pedido. | Revisão documental: `sed -n '1,160p' Rafaelteirema7d.md` | Pode orientar pesquisa, mas não valida produção Android nem RAFAELIA runtime. |
| Outros arquivos raiz conceituais (`Rafaelmatematica3.md`, `Razões e índices adequados rafaelreis.md`, `rafaelmreis.md`, `uniao.txt`, `prompt.md`) | `arquivo histórico` | Materiais de ideia, prompt, matemática e consolidação fora do módulo Android. | Revisão documental: `find . -maxdepth 1 -type f \( -name '*.md' -o -name '*.txt' \) -print` | Podem conter propostas não reconciliadas com o estado atual; exigir triagem antes de promover. |

## 4. Compiladores/protótipos

| Item | Status | Evidência | Comando de validação quando existir | Risco de uso |
| --- | --- | --- | --- | --- |
| `COMPILER/` | `experimental` | Protótipo RAFAELIA Root C→ASM Optimizer. O README declara explicitamente que não é compilador C completo e opera em subconjunto C-like seguro, com backends ARM64/x86-64 e auditoria JSON. | `python3 COMPILER/raf_c_to_asm_root_optimizer.py --target arm64 COMPILER/raiz_example.c --audit /tmp/raiz_audit_arm64.json > /tmp/raiz_output_arm64.s`; `python3 COMPILER/raf_c_to_asm_root_optimizer.py --target x86_64 COMPILER/raiz_example.c --audit /tmp/raiz_audit_x86_64.json > /tmp/raiz_output_x86_64.asm` | Risco de tratar como compilador C geral; deve permanecer protótipo até testes de linguagem, ABI e segurança. |
| `COMPILER/2/` | `arquivo histórico` | Subpasta com `RED.MD` e zips de pacotes do otimizador. | Revisão/arquivo: `find COMPILER/2 -maxdepth 2 -type f | sort` | Zips e notas antigas podem divergir da versão em `COMPILER/`; não usar como código principal sem comparação. |
| `IaCopiler/` | `stub` | Diretório contém apenas `.new` no levantamento até 5 níveis, sem código/documentação funcional identificável. | `find IaCopiler -maxdepth 5 -type f -print` | Nome sugere compilador, mas evidência de implementação é insuficiente; risco alto de falsa expectativa. |
| `BrowserRaf/internal/` | `experimental` | Protótipo de browser low-level com `Makefile`, C/headers e start assembly. O Makefile compila alvo Android AArch64 e tenta ARM32 com toolchains disponíveis. | `make -C BrowserRaf/internal`; `make -C BrowserRaf/internal x86_64`; `make -C BrowserRaf/internal run URL=http://example.com/` | `-nostdlib`, `_start`, targets e toolchains específicos podem falhar fora do ambiente; não integra o app Android principal. |
| `BrowserRaf/internal/benchmark/` | `experimental` | Benchmark mirror com README, script local, resultados e workflow espelho; contrato afirma não declarar ganho sem medir e não substituir testes funcionais. | `bash BrowserRaf/internal/benchmark/scripts/run_benchmark.sh` | Benchmarks não provam funcionalidade nem segurança; risco de usar métrica como validação de produto. |
| `genesis_bench.c` | `experimental` | Arquivo C raiz de benchmark/protótipo fora do app. | `cc genesis_bench.c -o /tmp/genesis_bench && /tmp/genesis_bench` | Pode depender de premissas não Android; não é parte do build Gradle. |

## 5. Pacotes e zips arquivados

Estes artefatos devem ser tratados como snapshots/arquivos históricos, não como código-fonte principal. Para validação mínima de integridade, preferir checksums existentes (`*.sha256.txt`) quando houver, ou listar conteúdo sem extrair no workspace principal.

| Artefato | Status | Evidência | Comando de validação quando existir | Risco de uso |
| --- | --- | --- | --- | --- |
| `RAFAELIA_10_CODE_MODELS.zip` | `arquivo histórico` | Zip raiz listado no levantamento até 5 níveis. | `unzip -t RAFAELIA_10_CODE_MODELS.zip`; `unzip -l RAFAELIA_10_CODE_MODELS.zip` | Conteúdo pode estar desatualizado; extrair pode sobrescrever/duplicar materiais. |
| `RAFAELIA_20_BAREMETAL_C_MODELS.zip` | `arquivo histórico` | Zip raiz de modelos C/baremetal. | `unzip -t RAFAELIA_20_BAREMETAL_C_MODELS.zip`; `unzip -l RAFAELIA_20_BAREMETAL_C_MODELS.zip` | Pode conter protótipos não integrados e claims sem CI atual. |
| `RAFAELIA_BARE_METAL_LAB_50_TOPS.zip` | `arquivo histórico` | Zip raiz de laboratório bare metal. | `unzip -t RAFAELIA_BARE_METAL_LAB_50_TOPS.zip`; `unzip -l RAFAELIA_BARE_METAL_LAB_50_TOPS.zip` | Alto risco de confundir laboratório com produção Android. |
| `RAF_96_ITEMS_56_C_METHODS_BENCH_PREFIXED.zip` | `arquivo histórico` | Zip raiz de benchmarks/métodos C. | `unzip -t RAF_96_ITEMS_56_C_METHODS_BENCH_PREFIXED.zip`; `unzip -l RAF_96_ITEMS_56_C_METHODS_BENCH_PREFIXED.zip` | Benchmark arquivado não substitui teste funcional ou validação ABI. |
| `formula_ci.zip` | `arquivo histórico` | Zip raiz com script relacionado `scripts/apply_formula_ci_zip.sh`. | `unzip -t formula_ci.zip`; `unzip -l formula_ci.zip`; aplicação controlada: `bash scripts/apply_formula_ci_zip.sh` | Aplicar zip pode alterar CI/arquivos; deve ser feito só com diff revisado e rollback. |
| `COMPILER/rafaelia_factory_with_project.zip` | `arquivo histórico` | Pacote arquivado dentro de `COMPILER/`. | `unzip -t COMPILER/rafaelia_factory_with_project.zip`; comparar checksum: `sha256sum -c COMPILER/rafaelia_factory.sha256.txt` quando aplicável | Pode divergir dos arquivos atuais do protótipo. |
| `COMPILER/rafaelia_multilang_root_optimizer.zip` | `arquivo histórico` | Pacote arquivado com checksum `COMPILER/rafaelia_multilang_root_optimizer.sha256.txt`. | `sha256sum -c COMPILER/rafaelia_multilang_root_optimizer.sha256.txt`; `unzip -t COMPILER/rafaelia_multilang_root_optimizer.zip` | Não usar como fonte ativa sem comparar com `COMPILER/raf_c_to_asm_root_optimizer.py`. |
| `COMPILER/2/rafaelia_multilang_root_optimizer.zip` e `COMPILER/2/rafaelia_root_optimizer_package.zip` | `arquivo histórico` | Zips em subpasta de versão/arquivo. | `unzip -t COMPILER/2/rafaelia_multilang_root_optimizer.zip`; `unzip -t COMPILER/2/rafaelia_root_optimizer_package.zip` | Pode representar versão antiga ou alternativa; risco de drift. |
| `Livro/RAFAELIA_SESSION_COMPLETE.zip` | `arquivo histórico` | Zip dentro de materiais de livro/pesquisa. | `unzip -t Livro/RAFAELIA_SESSION_COMPLETE.zip`; `unzip -l Livro/RAFAELIA_SESSION_COMPLETE.zip` | Snapshot de sessão; não fonte principal. |
| `Livro/rafaelia_bundle_v4.tar.gz`, `Livro/rafaelia_bundle_v5.tar.gz`, `Livro/rafaelia_bundle_v6.tar.gz` | `arquivo histórico` | Bundles tar.gz dentro de `Livro/`. | `tar -tzf Livro/rafaelia_bundle_v4.tar.gz`; `tar -tzf Livro/rafaelia_bundle_v5.tar.gz`; `tar -tzf Livro/rafaelia_bundle_v6.tar.gz` | Arquivos compactados podem conter duplicatas/versões antigas; extrair apenas em diretório temporário. |
| `_upcoming/RafGitTools-main_fixed_build (1).zip` e `_upcoming/RafGitTools-main_patched (1).zip` | `arquivo histórico` | Zips em `_upcoming/`, fora da trilha principal. | `unzip -t '_upcoming/RafGitTools-main_fixed_build (1).zip'`; `unzip -t '_upcoming/RafGitTools-main_patched (1).zip'` | Podem representar patches alternativos; aplicar sem diff pode sobrescrever código atual. |

## Rota segura de uso e promoção

1. **Fonte de verdade:** começar por `README.md`, `docs/BUILD.md`, `docs/CURRENT_SOURCE_STATE_2026-05-26.md`, `docs/STATUS_REPORT.md`, `app/` e `scripts/`.
2. **Experimentos ARM32/Termux:** manter `_incoming/`, `rafaelia/`, `scripts/termux/` e partes nativas como laboratório até haver testes Android/JNI e verificação ABI reproduzível.
3. **Pesquisa/livro:** usar `Livro/` e arquivos raiz conceituais como contexto, não como promessa de entrega.
4. **Protótipos de compilador/browser:** validar por comandos locais específicos e não integrar ao app sem contrato de API, testes, CI e rollback.
5. **Pacotes arquivados:** nunca tratar zip/tarball como fonte principal; validar integridade, listar conteúdo, extrair em `/tmp` e comparar diffs antes de qualquer promoção.

## Comandos recomendados de validação por perfil

### Desktop/CI Android

```bash
./scripts/prepare_local_properties.sh
./scripts/gradlew_with_java17.sh assembleDevDebug assembleProductionDebug
ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleDevRelease assembleProductionRelease
./scripts/native/verify_apks.sh
./scripts/gradlew_with_java17.sh testDevDebugUnitTest
./scripts/gradlew_with_java17.sh lintDevDebug
```

### Termux ARM32 runtime

```bash
./scripts/termux_arm32_runtime_check.sh
```

### RAFAELIA Omega Hybrid experimental

```bash
cd rafaelia/omega_hybrid
make clean all test
./demo
```

### Protótipo de compilador RAFAELIA Root

```bash
python3 COMPILER/raf_c_to_asm_root_optimizer.py --target arm64 COMPILER/raiz_example.c --audit /tmp/raiz_audit_arm64.json > /tmp/raiz_output_arm64.s
python3 COMPILER/raf_c_to_asm_root_optimizer.py --target x86_64 COMPILER/raiz_example.c --audit /tmp/raiz_audit_x86_64.json > /tmp/raiz_output_x86_64.asm
```

### BrowserRaf/internal experimental

```bash
make -C BrowserRaf/internal
bash BrowserRaf/internal/benchmark/scripts/run_benchmark.sh
```
