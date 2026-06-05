# Fluxo de promoção de `_incoming/`

Este documento define o fluxo obrigatório para avaliar, evidenciar e promover arquivos recebidos em `_incoming/` para áreas versionadas de produção ou documentação do RafGitTools.

## 1. Escopo e princípio de segurança

Arquivos em `_incoming/` são considerados material não promovido: podem conter documentação preliminar, protótipos, scripts, fórmulas, ASM, JNI ou benchmarks ainda sem prova suficiente. Nenhum arquivo deve ser movido para `app/`, `scripts/`, `rafaelia/` ou `docs/` sem inventário, classificação, evidência mínima e validação registrada.

A promoção não pode remover funcionalidades existentes, reduzir suporte a ABI, alterar assinatura de release pública ou declarar como pronto qualquer stub conhecido (`GPG`, `LFS`, `worktree`, `webhook`) sem implementação e testes.

## 2. Inventário obrigatório

Antes de mover qualquer item de `_incoming/`, registre um inventário com:

1. **Arquivo de origem**: caminho completo dentro de `_incoming/`.
2. **Destino proposto**: `app/`, `scripts/`, `rafaelia/`, `docs/` ou outro destino justificado.
3. **Objetivo**: problema que o arquivo resolve e usuário/fluxo afetado.
4. **Dependências**: bibliotecas, ferramentas, ABIs, SDK/NDK, JDK, shell, permissões, rede, dados externos e arquivos relacionados.
5. **Estado atual**: rascunho, protótipo, validado parcialmente, validado para runtime, validado para build ou apenas referência.
6. **Risco de integração**: impacto em build Android, Termux ARM32, CI, release, segurança, privacidade e compatibilidade.
7. **Plano de rollback**: como reverter a promoção se build, runtime ou validação falhar.

## 3. Classificação do conteúdo

Classifique cada arquivo em uma categoria principal antes da promoção:

| Categoria | Destino provável | Critério de classificação |
| --- | --- | --- |
| Documentação | `docs/` | Texto explicativo, guia, matriz, checklist, relatório ou referência sem código executável obrigatório. |
| Protótipo | `rafaelia/`, `docs/` ou área experimental explícita | Código/conceito não integrado ao app, sem garantia de produção. |
| Script | `scripts/` | Automação shell/Python/etc. usada por build, validação, empacotamento ou auditoria. |
| ASM | `app/`, `scripts/native/`, `rafaelia/` ou documentação nativa | Código assembly, rotinas SIMD/NEON, syscall, bare-metal ou micro-otimização dependente de arquitetura. |
| JNI | `app/` | Ponte nativa chamada por Kotlin/Java/Android. |
| Benchmark | `docs/`, `scripts/` ou área de benchmark | Medição de desempenho, harness, resultado ou metodologia reproduzível. |
| Fórmula | `docs/` ou `rafaelia/` | Modelo matemático, métrica, equação, heurística ou especificação algorítmica ainda não necessariamente executável. |

Se um arquivo se encaixar em mais de uma categoria, registre a categoria primária e as secundárias. Exemplo: `ASM + benchmark` exige evidência de correção nativa e evidência de medição.

## 4. Evidência mínima por destino

### 4.1 Promoção para `docs/`

Exige, no mínimo:

- Objetivo claro e escopo explícito.
- Declaração do que é comprovado, experimental ou apenas proposta.
- Ausência de alegações de produção sem prova.
- Compatibilidade com o estado real do projeto e com `docs/BUILD.md` quando mencionar build.
- Links ou referências internas quando depender de outros documentos.

### 4.2 Promoção para `scripts/`

Exige, no mínimo:

- Script com modo de falha explícito (`set -euo pipefail` para shell, quando aplicável).
- Entradas, saídas e variáveis de ambiente documentadas.
- Execução local registrada com comando exato.
- Verificação de que não altera SDK, assinatura, cache ou artefatos críticos sem intenção explícita.
- Plano de rollback para arquivos gerados ou alterados pelo script.

### 4.3 Promoção para `app/`

Exige, no mínimo:

- Integração compatível com Kotlin, Gradle, Jetpack Compose, JGit, Retrofit/OkHttp, Room e stack Android existente.
- Build dev executado ou justificativa objetiva se o ambiente impedir.
- Testes unitários, lint ou validação equivalente registrados.
- Evidência de que `compileSdk/targetSdk 34`, `minSdk 24`, JDK 17 e assinatura de release não foram alterados indevidamente.
- Para JNI/ASM, validação ABI conforme a seção ARM32/Termux/ASM deste documento.

### 4.4 Promoção para `rafaelia/`

Exige, no mínimo:

- Separação clara entre experimento, fórmula, protótipo e módulo integrável.
- Entradas/saídas descritas sem alegar capacidade não testada.
- Critério de validação ou benchmark quando houver algoritmo executável.
- Indicação de dependências matemáticas, runtime, shell, linguagem ou arquitetura.
- Rota de rollback para retirar o módulo sem quebrar o app Android.

## 5. Regras especiais para ASM, Termux e ARM32

Promoções envolvendo ASM, JNI nativo, Termux ou ARM32 devem cumprir todos os itens abaixo:

1. **Preservar `armeabi-v7a`**: não remover a ABI ARM32 nem degradar sua validação.
2. **Preservar `arm64-v8a`**: alterações ARM32 não devem quebrar a ABI ARM64 existente.
3. **Não substituir ARM32 por ARM64-only**: otimizações ARM64 podem coexistir, mas não podem apagar a rota ARM32.
4. **Não afirmar build completo em Termux ARM32 sem prova**: Termux ARM32 é ambiente de validação de runtime/toolchain, não host canônico de build de APK, salvo evidência explícita de SDK compatível via `ANDROID_SDK_ROOT` ou `ANDROID_HOME`.
5. **Não bootstrapar Android SDK command-line tools em Android/Termux ARM32** se `ANDROID_SDK_ROOT` ou `ANDROID_HOME` não apontar para SDK compatível já existente.
6. **Registrar fallback genérico** quando houver caminho otimizado por arquitetura.
7. **Registrar evidência de runtime** para rotinas bare-metal, syscall, NEON/SIMD ou JNI antes de chamar a mudança de produção.

## 6. Validação esperada

Escolha os comandos conforme a categoria e o destino. Registre comando, ambiente, resultado e motivo de qualquer `SKIPPED`.

### 6.1 Validação desktop/CI recomendada

```bash
./scripts/prepare_local_properties.sh
./scripts/gradlew_with_java17.sh assembleDevDebug assembleProductionDebug
ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleDevRelease assembleProductionRelease
./scripts/gradlew_with_java17.sh testDevDebugUnitTest
./scripts/gradlew_with_java17.sh lintDevDebug
./scripts/native/verify_apks.sh
```

### 6.2 Validação Termux/ARM32 quando aplicável

Use quando a promoção envolver ASM, JNI, Termux, ARM32, ABI nativa, runtime nativo ou alegações sobre execução em Android/Termux:

```bash
./scripts/termux_arm32_runtime_check.sh
```

Se o comando não puder ser executado no ambiente atual, registre como `SKIPPED` com a limitação objetiva. Não substitua essa evidência por afirmações genéricas.

### 6.3 Validação mínima por tipo

| Tipo promovido | Comandos mínimos esperados |
| --- | --- |
| Documentação | Revisão do arquivo, checagem de links/referências internas quando aplicável, `git diff --check`. |
| Protótipo | Comando de execução do protótipo ou registro de que é referência não executável, `git diff --check`. |
| Script | Execução direta do script em modo seguro ou dry-run, mais `git diff --check`. |
| ASM | Build/validação nativa, `./scripts/native/verify_apks.sh` quando gerar APK, e `./scripts/termux_arm32_runtime_check.sh` quando aplicável. |
| JNI | Build Android, testes unitários/lint quando aplicável, verificação ABI e runtime Termux/ARM32 quando aplicável. |
| Benchmark | Comando que reproduz a medição ou explicação de limitação, além de registrar hardware/ambiente. |
| Fórmula | Revisão de consistência, escopo de hipótese, ausência de alegação de prova além do demonstrado, `git diff --check`. |

## 7. Registro de promoção

Cada promoção deve deixar registro em PR, commit ou relatório com:

- Arquivos criados, modificados, movidos ou removidos.
- Categoria atribuída a cada arquivo.
- Evidências coletadas e comandos executados.
- Resultados `PASS`, `FAIL` ou `SKIPPED`.
- Riscos conhecidos e mitigação.
- Rollback planejado.
- Próximos passos recomendados.

Se houver teste falhando, não faça promoção silenciosa: explique a falha, o impacto e se a mudança deve ser revertida, mantida como documentação ou bloqueada até correção.

## 8. Checklist rápido

Antes de promover de `_incoming/`, confirme:

- [ ] O arquivo foi inventariado com objetivo e dependências.
- [ ] A categoria foi registrada: documentação, protótipo, script, ASM, JNI, benchmark ou fórmula.
- [ ] O destino foi justificado.
- [ ] A evidência mínima do destino foi coletada.
- [ ] Os comandos de validação esperados foram executados ou marcados como `SKIPPED` com motivo.
- [ ] Para ASM/JNI/Termux/ARM32, `armeabi-v7a` foi preservado e ARM32 não foi substituído por ARM64-only.
- [ ] Não há alegação de build completo em Termux ARM32 sem prova.
- [ ] Riscos, mitigação e rollback foram registrados.
