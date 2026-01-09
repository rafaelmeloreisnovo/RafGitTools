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

For distribution, you need to sign the release build.

### Generate Keystore

```bash
keytool -genkey -v -keystore rafgittools.keystore \
  -alias rafgittools -keyalg RSA -keysize 2048 -validity 10000
```

### Configure Signing

1. Create `keystore.properties` in project root:
```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=rafgittools
storeFile=../rafgittools.keystore
```

2. Add to `.gitignore`:
```
keystore.properties
*.keystore
```

3. Update `app/build.gradle` signing config:
```gradle
android {
    signingConfigs {
        release {
            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            // ... other config
        }
    }
}
```

### Build Signed Release

```bash
./gradlew assembleProductionRelease
# or
./gradlew bundleProductionRelease
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
