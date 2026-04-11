# Build Instructions

This document provides detailed instructions for building RafGitTools from source.

## Prerequisites

### Required Software

1. **Android Studio**
   - Version: Hedgehog (2023.1.1) or newer
   - Download: https://developer.android.com/studio

2. **Java Development Kit (JDK)**
   - Version: JDK 17 or newer
   - Android Studio includes JDK, or download from: https://adoptium.net/

3. **Android SDK**
   - Minimum SDK: API 24 (Android 7.0)
   - Target SDK: API 34 (Android 14)
   - Compile SDK: API 34

4. **Git** (optional, for cloning)
   - Download: https://git-scm.com/

### System Requirements

- **OS**: Windows 10/11, macOS 10.14+, or Linux
- **RAM**: 8 GB minimum, 16 GB recommended
- **Disk Space**: 10 GB free space minimum
- **Display**: 1280x800 minimum resolution

## Building from Source

### 1. Clone the Repository

```bash
git clone https://github.com/rafaelmeloreisnovo/RafGitTools.git
cd RafGitTools
```

Or download the source code as ZIP and extract it.

### 2. Open in Android Studio

1. Launch Android Studio
2. Select **File → Open**
3. Navigate to the RafGitTools directory
4. Click **OK**

Android Studio will:
- Import the project
- Download Gradle wrapper (if needed)
- Sync Gradle files
- Download dependencies

This process may take 5-10 minutes on first run.

### 3. Sync Gradle

If Gradle doesn't sync automatically:

1. Click **File → Sync Project with Gradle Files**
2. Wait for the sync to complete
3. Check the **Build** panel for any errors

### 4. Build Variants

RafGitTools has multiple build variants:

#### Flavors
- **dev**: Development environment with debug tools
- **production**: Production environment

#### Build Types
- **debug**: Debug build with logging
- **release**: Optimized release build

#### Available Variants
- `devDebug` - Development debug build
- `devRelease` - Development release build
- `productionDebug` - Production debug build
- `productionRelease` - Production release build

To select a build variant:
1. Open **Build Variants** panel (View → Tool Windows → Build Variants)
2. Select desired variant from dropdown

### 5. Build the Project

#### Using Android Studio

**Option A: Build APK**
1. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for build to complete
3. APK will be in `app/build/outputs/apk/`

**Option B: Generate Signed Bundle**
1. Select **Build → Generate Signed Bundle / APK**
2. Choose **Android App Bundle**
3. Follow the signing wizard
4. AAB will be in `app/build/outputs/bundle/`

#### Using Command Line

**Build Debug APK:**
```bash
./gradlew assembleDevDebug
```

**Build Release APK:**
```bash
./gradlew assembleProductionRelease
```

**Build App Bundle:**
```bash
./gradlew bundleProductionRelease
```

**Build All Variants:**
```bash
./gradlew assemble
```

### 6. Run on Device/Emulator

#### Using Android Studio

1. Connect Android device via USB (with USB debugging enabled)
   - Or start an Android emulator
2. Select device from device dropdown
3. Click **Run** (▶️) button or press **Shift + F10**

#### Using Command Line

**Install Debug APK:**
```bash
./gradlew installDevDebug
```

**Install and Run:**
```bash
./gradlew installDevDebug
adb shell am start -n com.rafgittools.dev/com.rafgittools.MainActivity
```

## Build Outputs

### APK Location

After building, APKs are located at:
```
app/build/outputs/apk/
├── dev/
│   ├── debug/
│   │   └── app-dev-debug.apk
│   └── release/
│       └── app-dev-release.apk
└── production/
    ├── debug/
    │   └── app-production-debug.apk
    └── release/
        └── app-production-release-unsigned.apk
```

### App Bundle Location

App Bundles (AAB) are located at:
```
app/build/outputs/bundle/
├── devRelease/
│   └── app-dev-release.aab
└── productionRelease/
    └── app-production-release.aab
```

## Signing the Release Build

RafGitTools **não usa mais `keystore.properties`** como fonte oficial de assinatura.
O fluxo atual lê secrets por:
- Variáveis de ambiente, ou
- Propriedades do Gradle (`~/.gradle/gradle.properties` ou `-P...`).

### Trilho 1 — Validação interna (opcional unsigned)

Use este trilho para validação técnica interna (ex.: smoke test de pipeline), **não para distribuição oficial**.

#### Opção A: build de debug (sempre assinado com debug keystore)
```bash
./gradlew assembleDevDebug
```

#### Opção B: release sem chave oficial (somente validação interna)
```bash
./gradlew assembleProductionRelease -PALLOW_UNSIGNED_RELEASE=true
```

> `ALLOW_UNSIGNED_RELEASE=true` permite fallback para assinatura de debug em tarefas `*Release*`.
> Isso existe somente para validação interna e não deve ser usado para release oficial.

### Trilho 2 — Release oficial (assinatura obrigatória)

Para `productionRelease` oficial, configure os 4 secrets:
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Sem esses secrets, o build de release falha (a menos que `ALLOW_UNSIGNED_RELEASE=true` seja ativado explicitamente para validação interna).

### Exemplo mínimo local (`~/.gradle/gradle.properties`)

```properties
# Caminho absoluto recomendado
RELEASE_STORE_FILE=/Users/you/keys/rafgittools-upload.jks
RELEASE_STORE_PASSWORD=change-me
RELEASE_KEY_ALIAS=rafgittools
RELEASE_KEY_PASSWORD=change-me
```

Build oficial local:
```bash
./gradlew bundleProductionRelease
```

### Exemplo mínimo CI (GitHub Actions)

Armazene no GitHub:
- **Secrets**: `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, `RELEASE_STORE_FILE_B64`
- **Vars (opcional)**: `ALLOW_UNSIGNED_RELEASE=false`

Exemplo de job:

```yaml
jobs:
  build-release:
    runs-on: ubuntu-latest
    env:
      RELEASE_STORE_PASSWORD: ${{ secrets.RELEASE_STORE_PASSWORD }}
      RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }}
      RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
      ALLOW_UNSIGNED_RELEASE: ${{ vars.ALLOW_UNSIGNED_RELEASE }}
    steps:
      - uses: actions/checkout@v4
      - name: Decode keystore
        run: |
          echo "${{ secrets.RELEASE_STORE_FILE_B64 }}" | base64 -d > $RUNNER_TEMP/release.jks
          echo "RELEASE_STORE_FILE=$RUNNER_TEMP/release.jks" >> $GITHUB_ENV
      - name: Build signed bundle
        run: ./gradlew bundleProductionRelease
```

### Gerar keystore (se necessário)

```bash
keytool -genkey -v -keystore rafgittools-upload.jks \
  -alias rafgittools -keyalg RSA -keysize 2048 -validity 10000
```

## Testing

### Run Unit Tests

```bash
./gradlew test
```

### Run Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Run Specific Test

```bash
./gradlew test --tests "com.rafgittools.ExampleUnitTest"
```

### Generate Test Coverage Report

```bash
./gradlew jacocoTestReport
```

Report will be in `app/build/reports/jacoco/`

## Linting and Code Quality

### Run Lint

```bash
./gradlew lint
```

Report will be in `app/build/reports/lint-results.html`

### Run Kotlin Lint (ktlint)

```bash
./gradlew ktlintCheck
```

### Auto-format Code

```bash
./gradlew ktlintFormat
```

## Clean Build

If you encounter build issues:

```bash
# Clean build artifacts
./gradlew clean

# Clean and rebuild
./gradlew clean assembleDevDebug

# Invalidate caches in Android Studio
# File → Invalidate Caches → Invalidate and Restart
```

## Troubleshooting

### Common Issues

**1. Gradle sync failed**
- Check internet connection
- Try: `File → Invalidate Caches → Invalidate and Restart`
- Delete `.gradle` folder and sync again

**2. Out of memory error**
- Increase heap size in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4096m
  ```

**3. SDK not found**
- Install required SDK versions via SDK Manager
- Set `ANDROID_HOME` environment variable

**4. Dependency resolution failed**
- Check `repositories` in `build.gradle`
- Clear Gradle cache: `./gradlew clean --refresh-dependencies`

**5. Build takes too long**
- Enable parallel builds: `org.gradle.parallel=true`
- Enable build cache: `org.gradle.caching=true`
- Use daemon: `org.gradle.daemon=true`

### Getting Help

If you encounter issues:
1. Check existing GitHub Issues
2. Create a new issue with:
   - Build error message
   - Gradle version
   - Android Studio version
   - Steps to reproduce

## Performance Tips

### Speed Up Builds

1. **Use Build Cache**
   ```properties
   # gradle.properties
   org.gradle.caching=true
   ```

2. **Enable Parallel Builds**
   ```properties
   org.gradle.parallel=true
   ```

3. **Configure Gradle Daemon**
   ```properties
   org.gradle.daemon=true
   org.gradle.jvmargs=-Xmx4096m
   ```

4. **Use Configuration Cache**
   ```bash
   ./gradlew --configuration-cache assemble
   ```

### Incremental Builds

Gradle automatically uses incremental compilation for Kotlin. Keep builds fast by:
- Only building necessary variants
- Using `assemble` instead of `clean assemble`
- Keeping Android Studio up to date

## CI/CD

For continuous integration, use these commands:

```bash
# Lint check
./gradlew lint

# Unit tests
./gradlew test

# Build all variants
./gradlew assemble

# Generate release bundle
./gradlew bundleProductionRelease
```

## Additional Resources

- [Android Developer Docs](https://developer.android.com/studio/build)
- [Gradle Build Tool](https://gradle.org/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

---

For questions or issues, please open a GitHub Issue or check the [CONTRIBUTING.md](CONTRIBUTING.md) guide.
