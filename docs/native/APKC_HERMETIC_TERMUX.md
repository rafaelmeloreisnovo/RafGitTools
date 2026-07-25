# RafGitTools — APKC hermético no Termux

`scripts/termux/build_apkc_hermetic.sh` é a rota local de contingência para
produzir um APK nativo mínimo para `armeabi-v7a`, `arm64-v8a` ou ambas as ABIs.
Ela usa um checkout local de RafPolimata como fornecedor do ApkC, sem clone,
download, Gradle, Android SDK, Android Studio, Maven, JDK, `aapt` ou `d8`.

## Pré-condição local

Os dois repositórios devem estar lado a lado ou o caminho de RafPolimata deve
ser fornecido explicitamente. Nenhum deles é baixado pelo script.

```text
work/
  RafGitTools/
  RafPolimata/
```

## Uso

```sh
cd RafGitTools
sh scripts/termux/build_apkc_hermetic.sh --abi both
```

Com um caminho diferente:

```sh
sh scripts/termux/build_apkc_hermetic.sh \
  --rafpolimata-root /caminho/RafPolimata \
  --abi armeabi-v7a
```

Para assinar com uma chave local já existente:

```sh
APKSIGNER_KEYSTORE=/caminho/chave.jks \
APKSIGNER_ALIAS=meu_alias \
APKSIGNER_KS_PASS_FILE=/caminho/senha-keystore.txt \
APKSIGNER_KEY_PASS_FILE=/caminho/senha-chave.txt \
sh scripts/termux/build_apkc_hermetic.sh --abi both --sign
```

Não há geração de chave nem senha em arquivo versionado. `apktool` não assina
APK; a assinatura opcional usa `apksigner`.

## O que este modo entrega

| Item | Estado |
|---|---|
| Artefato | APK NativeActivity mínimo |
| Fonte | `tools/termuxforge/rafgittools_bootstrap.s` |
| Formatos | AXML, DEX mínimo, ELF ARM32/ARM64 e ZIP produzidos pelo ApkC |
| Evidência | `dist/apkc-hermetic/receipt.env`, `build.log` e `rafgittools-orchestrator.env` |
| Rede/Gradle/SDK/JDK/Maven | não usados durante a execução |

## Limite obrigatório

O APK hermético é um bootstrap de ABI e cadeia de compilação, não o aplicativo
completo. O RafGitTools de produção continua sendo Kotlin/Compose/JGit/Room e
exige seus artefatos já resolvidos. Não promover este modo como compilação da
interface completa, nem como prova de instalação, runtime ou compatibilidade
com páginas de 16 KiB: esses estados permanecem `TOKEN_VAZIO` até execução
documentada no mesmo commit.
