# Auditoria Build/Release/CI Android — RafGitTools

**Data:** 2026-05-05  
**Modo:** auditoria, classificação e relatório; sem implementação corretiva.  
**Repositório auditado:** `RafGitTools` (`rootProject.name = "RafGitTools"`).  
**Escopo técnico:** Android, Gradle, CMake, NDK, JNI, SDK, JDK, scripts nativos e GitHub Actions.

## 1. Classificação do repositório selecionado

| Verificação | Resultado | Classificação |
|---|---:|---|
| Repositório selecionado é `Vectras-VM-Android` | Não | Fora da trilha Vectras; auditoria Vectras não aplicada. |
| Repositório selecionado é `termux-app-rafacodephi` | Não | Fora da trilha RAFAELIA/Termux; marcação obrigatória de dependências Vectras não aplicada. |
| Referências textuais a Termux | Sim | `EXTERNAL_REFERENCE`: inspiração/licença/futuro terminal, sem dependência compilável Termux no build Android atual. |
| Referências textuais a Vectras | Não encontradas no escopo auditado | Nenhum item classificado como dependência Vectras. |
| App Android próprio | Sim | `INTERNAL_CODE`. |
| Core nativo C/ASM via CMake | Sim | `INTERNAL_NATIVE_CODE`. |
| Workflows GitHub Actions | Sim | `CI_RELEASE_PIPELINE`. |

## 2. Fonte de verdade observada

| Camada | Fonte atual | Estado |
|---|---|---|
| Gradle root | `settings.gradle`, `build.gradle` | Projeto Android único com módulo `:app`; AGP 8.3.0; Kotlin 1.9.22. |
| App Android | `app/build.gradle` | `compileSdk 34`, `targetSdk 34`, flavors `dev`/`production`, ABIs `armeabi-v7a` e `arm64-v8a`. |
| Nativo | `app/src/main/cpp/CMakeLists.txt` | Biblioteca compartilhada `rafcore`, C + ASM genérico em `app/src/main/cpp/asm/`. |
| JNI | `app/src/main/cpp/native_bridge.c` | Exporta `nativeAsmHealth` para `com.rafgittools.platform.MultiPlatformManager`. |
| Release oficial | `.github/workflows/release.yml` | Assinatura obrigatória via secrets; bloqueia release unsigned. |
| Validação interna | `.github/workflows/internal-validation.yml` | `ALLOW_UNSIGNED_RELEASE=true` para validação interna. |
| Build auxiliar nativo | `scripts/native/build_apks_signed_unsigned.sh` | Primeiro gera release com fallback debug; tenta release assinado apenas se secrets locais existirem. |
| Verificação APK | `scripts/native/verify_apks.sh` | Valida presença de libs `armeabi-v7a` e `arm64-v8a`, tamanho e estado de assinatura. |

## 3. Causas-raiz encontradas

### CR-001 — Conflitos de merge não resolvidos em Kotlin bloqueiam qualquer APK final

**Severidade:** bloqueador.  
**Classificação:** `SOURCE_INTEGRITY_BLOCKER`.

Foram encontrados marcadores de conflito Git (`<<<<<<<`, `=======`, `>>>>>>>`) em código Kotlin compilável:

- `app/src/main/kotlin/com/rafgittools/core/error/PersistentErrorLogger.kt`
- `app/src/main/kotlin/com/rafgittools/core/logging/DiffAuditLogger.kt`

Efeito verificado: `assembleProductionRelease` em modo unsigned interno chega a executar CMake/NDK para `arm64-v8a` e `armeabi-v7a`, mas falha em `compileProductionReleaseKotlin`. Portanto não há APK final, nem assinado nem unsigned.

### CR-002 — Ambiente local inicial sem Android SDK impedia até configuração Gradle

**Severidade:** bloqueador de ambiente local inicial.  
**Classificação:** `ENVIRONMENT_BOOTSTRAP_BLOCKER`.

A execução inicial de Gradle falhou por ausência de `ANDROID_SDK_ROOT`/`ANDROID_HOME` e ausência de `local.properties`. Foi instalado SDK local em `/opt/android-sdk` para permitir auditoria mais profunda. Após isso, `scripts/prepare_local_properties.sh` gerou `local.properties` com `sdk.dir=/opt/android-sdk` e `./scripts/gradlew_with_java17.sh tasks --all` passou.

### CR-003 — Release oficial está corretamente protegido contra unsigned, mas documentação/relatório deve deixar claro o bloqueio esperado

**Severidade:** informativa/controle de segurança.  
**Classificação:** `RELEASE_SECURITY_CONTROL_OK`.

`app/build.gradle` exige `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` e `RELEASE_KEY_PASSWORD` para tarefas release, exceto quando `ALLOW_UNSIGNED_RELEASE=true`. O workflow oficial `release.yml` fixa `ALLOW_UNSIGNED_RELEASE=false` e valida secrets antes do build. O comportamento foi confirmado: sem secrets, `assembleProductionRelease` falha na configuração por política de assinatura obrigatória.

### CR-004 — Build nativo arm32/arm64 compila, mas não pode ser validado dentro de APK por falha posterior no Kotlin

**Severidade:** bloqueado por CR-001.  
**Classificação:** `NATIVE_PARTIAL_PASS_APP_BLOCKED`.

Durante `ALLOW_UNSIGNED_RELEASE=true assembleProductionRelease`, as tarefas nativas passaram:

- `configureCMakeRelWithDebInfo[arm64-v8a]`
- `buildCMakeRelWithDebInfo[arm64-v8a]`
- `configureCMakeRelWithDebInfo[armeabi-v7a]`
- `buildCMakeRelWithDebInfo[armeabi-v7a]`
- `mergeProductionReleaseNativeLibs`

Artefatos nativos intermediários encontrados:

- `app/build/intermediates/cxx/RelWithDebInfo/1r4r282h/obj/arm64-v8a/librafcore.so`
- `app/build/intermediates/cxx/RelWithDebInfo/1r4r282h/obj/armeabi-v7a/librafcore.so`

Como o APK não foi empacotado, `scripts/native/verify_apks.sh` não pôde validar libs dentro do APK.

### CR-005 — Referências Termux são externas e não integram dependência compilável atual

**Severidade:** informativa.  
**Classificação:** `EXTERNAL_REFERENCE`.

Termux aparece em README/licenças/documentação/string de descrição e em comentário de ViewModel como inspiração/futuro enhancement. Não foi encontrada dependência Gradle Termux nem submódulo Termux no caminho de build auditado.

## 4. Comandos executados e resultados

| Comando | Resultado | Observação |
|---|---|---|
| `pwd && rg --files -g 'AGENTS.md' -g '!**/.git/**' ...` | Passou | Nenhum `AGENTS.md` encontrado no repositório auditado. |
| `git status --short --branch` | Passou | Branch atual: `work`. |
| `./gradlew --version` | Passou | Gradle 8.4; JVM inicial Java 25, wrapper auxiliar troca para JDK 17. |
| `./scripts/gradlew_with_java17.sh tasks --all` antes do SDK | Falhou | `SDK location not found`. |
| Instalação Android SDK em `/opt/android-sdk` | Passou | Instalados cmdline-tools, platform-tools, platform android-34, build-tools 34.0.0, CMake 3.22.1 e NDK 26.1.10909125. |
| `./scripts/prepare_local_properties.sh` | Passou | Gerou `local.properties` local/ignorado com `sdk.dir=/opt/android-sdk`. |
| `./scripts/gradlew_with_java17.sh tasks --all` após SDK | Passou | Variants e tasks Android resolvidas. |
| `ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleProductionRelease --stacktrace` | Falhou | Falha em compilação Kotlin por conflitos de merge não resolvidos; CMake arm32/arm64 passou antes da falha. |
| `./scripts/gradlew_with_java17.sh assembleProductionRelease --stacktrace` sem signing secrets | Falhou esperado | Política oficial bloqueia release sem secrets e sem `ALLOW_UNSIGNED_RELEASE=true`. |
| `python3 scripts/ci/validate_formulas_graph.py` | Passou | `Validation PASS`; gerou `build/ci-validation/formula_graph_validation.json`. |
| `rg -n "^(<<<<<<<|=======|>>>>>>>)" ...` | Passou como auditoria | Encontrou conflitos em `PersistentErrorLogger.kt` e `DiffAuditLogger.kt`. |

## 5. Resultado de artefatos

| Artefato | Estado | Evidência |
|---|---|---|
| APK production unsigned | Não gerado | Bloqueado por CR-001 em `compileProductionReleaseKotlin`. |
| APK production signed oficial | Não gerado | Sem secrets oficiais, bloqueio correto por política de release; com código atual ainda haveria CR-001. |
| APK dev/debug | Não executado nesta auditoria | Auditoria priorizou cadeia release solicitada. |
| `librafcore.so` arm64-v8a | Gerado como intermediário | `app/build/intermediates/cxx/RelWithDebInfo/.../arm64-v8a/librafcore.so`. |
| `librafcore.so` armeabi-v7a | Gerado como intermediário | `app/build/intermediates/cxx/RelWithDebInfo/.../armeabi-v7a/librafcore.so`. |
| Relatórios fórmula/grafo | Gerados | `build/ci-validation/formula_graph_validation.json`, `build/ci-validation/formula_graph_edges.txt`. |

## 6. Workflows e upload de artefatos

| Workflow | Upload configurado | Estado auditado |
|---|---:|---|
| `.github/workflows/release.yml` | Sim | Upload de APK release e `CHANGELOG.md`; release oficial exige assinatura. |
| `.github/workflows/internal-validation.yml` | Sim | Upload de APK unsigned interno e validação fórmula/grafo. |
| `.github/workflows/android-ci.yml` | Sim | Upload de APKs, relatórios de teste e lint. |
| `.github/workflows/native-asm-ci.yml` | Sim | Upload de APKs gerados por script nativo. |

## 7. Bloqueios restantes

1. Resolver os conflitos de merge em `PersistentErrorLogger.kt` e `DiffAuditLogger.kt` antes de qualquer tentativa de empacotamento final.
2. Reexecutar, após correção, no mínimo:
   - `ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleProductionRelease --stacktrace`
   - `./scripts/native/verify_apks.sh`
   - build assinado com `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
3. Só publicar release oficial via `.github/workflows/release.yml` com secrets reais. Não converter o caminho oficial para unsigned.

## 8. Decisão de auditoria

O repositório está estruturalmente próximo de uma cadeia coerente de build/release, porque SDK/JDK/Gradle, CMake/NDK, ABIs e workflows têm uma fonte de verdade reconhecível. Entretanto, o estado atual do código-fonte contém conflitos de merge literais em arquivos Kotlin compiláveis. Essa é a causa-raiz imediata que impede APK com e sem assinatura. Nenhuma implementação corretiva foi aplicada nesta etapa porque a instrução operacional inicial foi auditar, classificar e gerar relatório primeiro.
