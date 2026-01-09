# RafGitTools 🚀

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/License-GPL--3.0-blue)
![Status](https://img.shields.io/badge/Status-In%20Development-orange)

**A Unified Git/GitHub Android Client**

Combining the best features from FastHub, MGit, PuppyGit, and Termux

[Features](#features) • [Architecture](docs/ARCHITECTURE.md) • [Contributing](#contributing) • [License](#license)

</div>

---

## 📋 Overview

RafGitTools is an ambitious Android application that aims to provide the most comprehensive mobile Git experience by combining:

- 🌐 **GitHub Integration** (inspired by FastHub)
- 📁 **Local Git Operations** (inspired by MGit)
- 🎨 **Modern UI/UX** (inspired by PuppyGit)
- 💻 **Terminal Capabilities** (inspired by Termux)

All while respecting the licenses of these amazing open-source projects and adding unique innovations.

## ✨ Features

### Git Operations
- ✅ Clone repositories (HTTP/HTTPS/SSH)
- ✅ Commit changes with staging
- ✅ Push and pull with conflict resolution
- ✅ Branch creation and management
- ✅ Merge and rebase operations
- ✅ Stash management
- ✅ Tag management

### GitHub Integration
- ✅ Repository browsing and search
- ✅ Issue tracking and management
- ✅ Pull request workflow
- ✅ Code review and comments
- ✅ GitHub Actions monitoring
- ✅ Release management
- ✅ Gist support

### User Experience
- ✅ Material Design 3 (Material You)
- ✅ Dark/Light/Auto theme
- ✅ Intuitive gesture navigation
- ✅ Syntax highlighting
- ✅ File diff viewer
- ✅ Offline-first architecture

### Advanced Features
- ✅ SSH key management
- ✅ Terminal emulation
- ✅ Multi-account support
- ✅ Custom Git server support

### 🔒 Privacy & Security
- ✅ **User Data Control**: Export, view, and delete personal data (GDPR compliant)
- ✅ **Privacy by Design**: Minimal data collection, opt-in analytics only
- ✅ **End-to-End Encryption**: AES-256-GCM encryption for sensitive data
- ✅ **Secure Storage**: Android Keystore for credential protection
- ✅ **TLS 1.3**: Enforced HTTPS with certificate pinning
- ✅ **Biometric Authentication**: Optional fingerprint/face unlock
- ✅ **Privacy Audit Trail**: Complete log of data access and changes
- ✅ **No Third-Party Tracking**: No ads, no analytics by default
- ✅ **Compliance**: ISO 27001, NIST, OWASP, GDPR, CCPA

## 🏗️ Architecture

RafGitTools follows Clean Architecture principles with MVVM pattern:

```
📦 RafGitTools
├── 📱 Presentation Layer (Jetpack Compose + ViewModels)
├── 🎯 Domain Layer (Use Cases + Business Logic)
└── 💾 Data Layer (Repository Pattern + Data Sources)
```

**Tech Stack:**
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt
- **Networking**: Retrofit + OkHttp
- **Database**: Room
- **Git**: JGit
- **Async**: Coroutines + Flow

For detailed architecture information, see [ARCHITECTURE.md](docs/ARCHITECTURE.md)

## 📦 Project Structure

```
RafGitTools/
├── app/                        # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── com/rafgittools/
│   │   │   │       ├── MainActivity.kt
│   │   │   │       ├── RafGitToolsApplication.kt
│   │   │   │       └── ui/theme/
│   │   │   ├── res/            # Resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/               # Unit tests
│   │   └── androidTest/        # Instrumented tests
│   └── build.gradle            # App module build config
├── docs/                       # Documentation
│   ├── PROJECT_OVERVIEW.md    # Detailed project overview
│   ├── ARCHITECTURE.md        # Architecture documentation
│   ├── LICENSE_INFO.md        # License compliance info
│   └── FEATURE_MATRIX.md      # Feature comparison matrix
├── build.gradle                # Root build config
├── settings.gradle            # Project settings
└── README.md                  # This file
```

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or newer
- Android SDK 24+ (Android 7.0+)
- Git command-line tools (optional, for development)

### Building the Project

1. **Clone the repository**
   ```bash
   git clone https://github.com/rafaelmeloreisnovo/RafGitTools.git
   cd RafGitTools
   ```

2. **Open in Android Studio**
   - File → Open → Select the RafGitTools directory

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle files
   - Wait for dependencies to download

4. **Run the app**
   - Select a device/emulator
   - Click Run (▶️) or press Shift+F10

### Build Variants

The project includes multiple build variants:

- **devDebug**: Development build with debug tools
- **devRelease**: Development release build
- **productionDebug**: Production build for testing
- **productionRelease**: Final production build

## 📚 Documentation

- [Project Overview](docs/PROJECT_OVERVIEW.md) - Comprehensive project information
- [Architecture Guide](docs/ARCHITECTURE.md) - Detailed architecture documentation
- [Privacy Policy](docs/PRIVACY.md) - Privacy practices and data protection
- [Security Policy](docs/SECURITY.md) - Security standards and practices
- [Compliance Guide](docs/COMPLIANCE.md) - ISO, NIST, IEEE standards compliance
- [License Information](docs/LICENSE_INFO.md) - License compliance and attribution
- [Feature Matrix](docs/FEATURE_MATRIX.md) - Feature comparison with source projects

## 🤝 Contributing

Contributions are welcome! This project respects the GPL-3.0 license.

### Development Status

🚧 **Currently in active development** 🚧

This project is in the initial development phase. The following components are being built:

- [x] Project structure and architecture
- [x] Documentation
- [x] Android project setup
- [x] Core Git operations
- [x] GitHub API integration
- [x] UI implementation
- [x] Testing infrastructure

### How to Contribute

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the **GNU General Public License v3.0** (GPL-3.0).

This licensing choice ensures compatibility with all source projects and maintains the open-source nature of the combined work.

### Source Project Attribution

This project is inspired by and builds upon concepts from:

- **FastHub** (GPL-3.0) - GitHub client features
- **FastHub-RE** (GPL-3.0) - Modern implementations
- **MGit** (GPL-3.0) - Local Git operations
- **PuppyGit** (Apache-2.0) - UI/UX patterns
- **Termux** (GPL-3.0) - Terminal capabilities

See [LICENSE_INFO.md](docs/LICENSE_INFO.md) for detailed license information and attribution.

## 🙏 Acknowledgments

This project would not be possible without the amazing work of the open-source community. Special thanks to the maintainers and contributors of:

- FastHub and FastHub-RE teams
- MGit developers
- PuppyGit team
- Termux community
- JGit Eclipse Foundation
- Android and Jetpack Compose teams

## 📧 Contact

- **Project Repository**: https://github.com/rafaelmeloreisnovo/RafGitTools
- **Issues**: https://github.com/rafaelmeloreisnovo/RafGitTools/issues

## 🗺️ Roadmap

### Phase 1: Foundation (Weeks 1-4)
- [x] Project architecture
- [ ] Core Git operations
- [ ] Basic repository browsing
- [ ] Authentication system

### Phase 2: GitHub Integration (Weeks 5-8)
- [ ] GitHub API client
- [ ] Issue and PR management
- [ ] Code review features
- [ ] Notifications

### Phase 3: Advanced Features (Weeks 9-12)
- [ ] Terminal emulation
- [ ] Advanced Git operations
- [ ] SSH/GPG key management
- [ ] Multi-account support

### Phase 4: Polish & Release (Weeks 13-16)
- [ ] UI/UX refinement
- [ ] Performance optimization
- [ ] Comprehensive testing
- [ ] Beta release

---

<div align="center">

**Made with ❤️ by the RafGitTools team**

⭐ Star this repo if you find it useful!

</div>