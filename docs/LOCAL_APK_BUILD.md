# RafGitTools — Build local do APK

Use este caminho quando o GitHub Actions não criar runner/job.

## Requisitos

- JDK 17;
- Android SDK com API 34 e Build Tools compatíveis;
- NDK/CMake exigidos pelo módulo nativo;
- repositório clonado localmente;
- acesso à internet na primeira resolução de dependências Gradle.

Configure o SDK por uma das formas:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

ou:

```properties
# local.properties
sdk.dir=/caminho/absoluto/Android/Sdk
```

## Executar

```bash
cd RafGitTools
bash scripts/build_client_apk.sh
```

O script executa:

```text
JDK 17
→ testes de autenticação
→ testes devDebug
→ lintDevDebug
→ assembleDevDebug
→ APK não vazio
→ SHA-256
```

## Saída

```text
dist/rafgittools/
├── RafGitTools-devDebug.apk
├── SHA256SUMS.txt
└── BUILD_INFO.txt
```

Verifique:

```bash
cd dist/rafgittools
sha256sum -c SHA256SUMS.txt
```

## OAuth

Para habilitar o login pelo navegador/device flow no APK:

```bash
export GITHUB_CLIENT_ID_DEV="CLIENT_ID_PUBLICO_DO_OAUTH_APP"
bash scripts/build_client_apk.sh
```

O Client ID é público. Não coloque Client Secret, PAT ou senha em arquivos versionados.

Sem Client ID, o APK ainda pode usar:

- Personal Access Token;
- importação da sessão `gh` quando disponível;
- SSH para operações Git;
- modo local/offline.

## Termux

O script reutiliza `scripts/gradlew_with_java17.sh`, que procura JDK 17 em caminhos comuns de Termux/Linux. O Android SDK/NDK continua sendo obrigatório para compilar o APK completo.

## Diagnóstico

O primeiro comando que falhar é a fonte de verdade. Não marque o APK como compilado se `dist/rafgittools/RafGitTools-devDebug.apk` e `SHA256SUMS.txt` não existirem.
