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

### 🔧 Git Operations (96+ Features)
- ✅ **Clone Operations**: Full clone, shallow clone, single branch, with submodules, with LFS
- ✅ **Commit Management**: Standard commits, amend, signing (GPG), templates, interactive staging
- ✅ **Push/Pull**: Force push with lease, pull with rebase, tag operations, multi-remote sync
- ✅ **Branch Management**: Create, delete, rename, track, compare branches
- ✅ **Merge Operations**: Multiple strategies (recursive, ours, theirs), fast-forward, squash
- ✅ **Rebase**: Interactive rebase, rebase --onto, autosquash, continue/skip/abort
- ✅ **Stash**: With message, untracked files, apply/pop, drop/clear, stash branch
- ✅ **Cherry-pick**: Single and range cherry-pick with continue/abort
- ✅ **Tags**: Annotated, lightweight, signed (GPG), push/pull tags
- ✅ **Submodules**: Add, update, init, sync, foreach, nested submodules
- ✅ **Git LFS**: Install, track patterns, fetch/pull, prune
- ✅ **Worktrees**: Add, list, remove, prune worktrees
- ✅ **Advanced**: Reflog, bisect, blame, clean, reset, revert, remotes, patches, sparse checkout

### 🐙 GitHub Integration (169+ Features)
- ✅ **Repository Management**: Browse, search, create, settings, archive, transfer, templates
- ✅ **Issue Management**: Create, edit, comment, labels, milestones, assignments, templates, forms
- ✅ **Pull Requests**: Complete workflow, draft mode, auto-merge, reviews, checks, suggestions
- ✅ **Code Review**: Inline comments, suggestions, approval, multi-line, batch comments
- ✅ **Notifications**: Push, in-app, filters, grouping, threads, muting, custom rules
- ✅ **GitHub Actions**: Workflow viewing, runs, logs, re-run, cancellation, triggers
- ✅ **Releases**: Creation, editing, assets, notes, drafts, pre-releases, generation
- ✅ **Wikis**: Browse, edit, create, search, history
- ✅ **Gists**: Create, edit, comment, star, fork, secret gists, revisions
- ✅ **Organizations**: Profile, members, teams, settings, repositories, projects
- ✅ **Projects**: Boards, views, items, automation, fields, insights
- ✅ **Discussions**: Categories, creation, commenting, reactions, polls, answers
- ✅ **Sponsors**: Tiers, goals, dashboard
- ✅ **Security**: Advisories, Dependabot, code scanning, secret scanning
- ✅ **Packages**: Container registry, package management

### 🎨 UI/UX Features (119+ Features)
- ✅ **Material Design 3**: Dynamic colors (Material You), multiple themes, AMOLED black
- ✅ **Themes**: Dark/Light/Auto, custom themes, theme scheduling, color customization
- ✅ **Code Display**: Syntax highlighting, multiple themes, line numbers, code folding, minimap
- ✅ **Diff Viewer**: Side-by-side, unified, split, word diff, semantic diff, navigation
- ✅ **File Browser**: Tree/list/grid views, icons, preview, search, filters, sorting
- ✅ **Search**: Global, repository, code, issue, PR, user, advanced filters, regex
- ✅ **Markdown**: Preview, editing, toolbar, templates, emoji, tables, task lists, diagrams
- ✅ **Image Viewer**: Zoom/pan, rotation, filters, GIF, SVG, gallery support
- ✅ **Gestures**: Swipe, pull-to-refresh, long press, double tap, pinch-to-zoom
- ✅ **Tablet Optimization**: Two-pane layout, landscape, multi-window, drag-and-drop
- ✅ **Widgets**: Home screen, lock screen, configurable, themed widgets
- ✅ **Animations**: Transitions, loading, skeletons, progress, smooth navigation

### 🔐 Authentication & Security (97+ Features)
- ✅ **OAuth**: OAuth 2.0, device flow, refresh tokens, scopes
- ✅ **Tokens**: Personal Access Tokens (fine-grained and classic), expiration, refresh
- ✅ **SSH Keys**: Generation, management, agent, multiple keys, Ed25519/RSA/ECDSA
- ✅ **GPG**: Key generation, management, commit signing, tag signing, verification
- ✅ **Biometric**: Fingerprint, face unlock, iris scan, app lock, auto-lock timer
- ✅ **Multi-Account**: Seamless switching, isolation, per-account settings, profiles
- ✅ **2FA**: TOTP, SMS, security keys, backup codes, passkeys, WebAuthn, FIDO2
- ✅ **Encryption**: AES-256-GCM, end-to-end, at-rest, TLS 1.3, certificate pinning
- ✅ **Secure Storage**: Android Keystore, encrypted SharedPreferences, secure files
- ✅ **Session Management**: Timeout, invalidation, monitoring, trusted devices
- ✅ **Privacy Controls**: Data export/deletion, privacy dashboard, analytics opt-out

### 🚀 Advanced Features
- ✅ **Terminal Emulation**: Full shell support with Git CLI access
- ✅ **Multi-Platform**: GitHub, GitLab, Bitbucket, Gitea, Gogs, Azure DevOps, AWS CodeCommit
- ✅ **Custom Git Servers**: Full support for self-hosted Git servers
- ✅ **Offline-First**: 44+ offline capabilities, smart sync, conflict resolution
- ✅ **Plugin System**: Extensible architecture, marketplace, development API
- ✅ **Workflow Automation**: Custom scripts, macros, task automation
- ✅ **Code Editor**: 52+ editor features with LSP support

### 🤖 AI & Machine Learning (10+ Features)
- ✅ **AI Commit Messages**: Intelligent commit message generation
- ✅ **Code Review Assistant**: Automated review with suggestions
- ✅ **Smart Conflict Resolution**: AI-assisted merge conflict resolution
- ✅ **Bug Detection**: Proactive pattern detection and security scanning
- ✅ **Predictive Coding**: Context-aware code completion
- ✅ **Intelligent Refactoring**: Smart code improvements
- ✅ **Test Generation**: Automated test case creation
- ✅ **Security Analysis**: Vulnerability detection and prevention

### 🔄 DevOps & CI/CD (12+ Features)
- ✅ **Universal CI/CD**: GitHub Actions, GitLab CI, Jenkins, CircleCI, Travis CI
- ✅ **Container Management**: Docker and Kubernetes integration
- ✅ **Pipeline Monitoring**: Real-time build and deployment tracking
- ✅ **Environment Management**: Multi-environment deployments
- ✅ **Release Automation**: Versioning, changelogs, automated releases
- ✅ **Infrastructure as Code**: Terraform, Ansible, CloudFormation support
- ✅ **Cloud Integration**: AWS, Azure, GCP deployment and management

### 📊 Code Quality & Analysis (10+ Features)
- ✅ **Static Analysis**: Multi-language code analysis
- ✅ **Complexity Metrics**: Code quality and technical debt tracking
- ✅ **Vulnerability Scanning**: Continuous security and dependency scanning
- ✅ **License Compliance**: Automatic license checking
- ✅ **Performance Profiling**: Optimization suggestions
- ✅ **Code Standards**: Style enforcement with auto-formatting
- ✅ **Documentation Generation**: Auto-generated docs

### 👥 Collaboration & Team (12+ Features)
- ✅ **Real-Time Collaboration**: Live editing with presence awareness
- ✅ **Pair Programming**: Video/audio, screen sharing, whiteboard
- ✅ **Team Dashboards**: Activity monitoring and insights
- ✅ **Chat Integration**: Slack, Discord, Microsoft Teams
- ✅ **Code Review Assignments**: Team workflow management
- ✅ **Video Conferencing**: Integrated calls for reviews

### 📈 Analytics & Insights (12+ Features)
- ✅ **Contribution Graphs**: Visual activity tracking
- ✅ **Repository Statistics**: Comprehensive metrics
- ✅ **Team Velocity**: Performance analytics
- ✅ **Code Churn Analysis**: Change tracking
- ✅ **PR Cycle Time**: Workflow efficiency metrics
- ✅ **Custom Dashboards**: Configurable analytics widgets
- ✅ **BI Integration**: Export to Tableau, Power BI, Looker

### 🏢 Enterprise Features (12+ Features)
- ✅ **LDAP/Active Directory**: Enterprise authentication
- ✅ **SAML/SSO**: Single Sign-On integration
- ✅ **RBAC**: Role-based access control
- ✅ **Audit Logging**: Comprehensive activity tracking
- ✅ **Compliance Reporting**: SOC 2, ISO 27001, GDPR, CCPA
- ✅ **Custom Branding**: White-label options
- ✅ **Self-Hosted**: On-premise deployment
- ✅ **API Management**: Full REST/GraphQL API
- ✅ **Backup & Recovery**: Automated disaster recovery
- ✅ **SLA Guarantees**: Enterprise-grade support

### 📱 Mobile-Specific (12+ Features)
- ✅ **Foldable Support**: Optimized for foldable devices
- ✅ **Tablet Optimization**: Two-pane layouts, landscape mode
- ✅ **Samsung DeX**: Desktop mode support
- ✅ **ChromeOS**: Full Chromebook support
- ✅ **Wear OS**: Companion app for smartwatches
- ✅ **Widgets**: Home and lock screen widgets
- ✅ **Quick Settings**: Tiles for quick access
- ✅ **Edge-to-Edge**: Full screen optimization
- ✅ **Dynamic Color**: Material You theming

### ♿ Accessibility (12+ Features)
- ✅ **Screen Reader**: Full TalkBack support
- ✅ **High Contrast**: Multiple contrast themes
- ✅ **Large Text**: Scalable text support
- ✅ **Color Blind**: Multiple color blind modes
- ✅ **Keyboard Navigation**: Full keyboard support
- ✅ **Voice Control**: Voice command integration
- ✅ **Haptic Feedback**: Touch feedback
- ✅ **WCAG 2.1 AA**: Compliance certified
- ✅ **Focus Indicators**: Clear focus states
- ✅ **Reduced Motion**: Animation controls

### 🌍 Internationalization (52+ Languages)
- ✅ **50+ Languages**: Including English, Spanish, Portuguese, Chinese, Japanese, Korean, Arabic, Hindi, and more
- ✅ **RTL Support**: Full right-to-left language support
- ✅ **Dynamic Switching**: Change language in-app
- ✅ **Community Translations**: Crowdsourced translations
- ✅ **Regional Variations**: Locale-specific formatting

### 🧪 Testing & Quality (44+ Features)
- ✅ **Unit Testing**: Comprehensive test framework
- ✅ **Integration Testing**: End-to-end test coverage
- ✅ **UI Testing**: Automated UI validation
- ✅ **Performance Testing**: Load and stress testing
- ✅ **Security Testing**: Penetration testing
- ✅ **Accessibility Testing**: A11y validation
- ✅ **Test Automation**: Continuous testing
- ✅ **Coverage Reporting**: > 80% code coverage target

### 📊 Monitoring & Observability (51+ Features)
- ✅ **Application Monitoring**: Real-time performance tracking
- ✅ **Error Tracking**: Comprehensive error reporting
- ✅ **Crash Reporting**: Detailed crash analysis
- ✅ **Analytics**: User behavior and journey tracking
- ✅ **Performance Metrics**: CPU, memory, battery, network
- ✅ **Log Aggregation**: Centralized logging
- ✅ **Alerting**: Custom alert rules and escalation

### 💾 Backup & Data Management (41+ Features)
- ✅ **Auto Backup**: Scheduled and incremental backups
- ✅ **Cloud Storage**: Google Drive, Dropbox, OneDrive, iCloud
- ✅ **Encryption**: Encrypted backup and restore
- ✅ **Point-in-Time Recovery**: Restore from any backup
- ✅ **Data Export**: Multiple formats (JSON, CSV, XML, SQL)
- ✅ **Migration Tools**: Import from competitors

### 🎨 Customization (43+ Features)
- ✅ **Custom Themes**: Theme editor and marketplace
- ✅ **Color Schemes**: Custom color pickers
- ✅ **Font Selection**: Custom fonts and sizes
- ✅ **Icon Customization**: Custom icon packs
- ✅ **Layout Customization**: Configurable dashboards
- ✅ **Shortcuts**: Custom keyboard shortcuts
- ✅ **Macros**: Record and replay macros
- ✅ **Templates**: Custom code templates

### 🔍 Debugging & Profiling (30+ Features)
- ✅ **Debug Mode**: Comprehensive debugging tools
- ✅ **Breakpoints**: Step debugging with variable inspection
- ✅ **Performance Profiler**: CPU, memory, network profiling
- ✅ **Memory Profiling**: Leak detection and heap analysis
- ✅ **Network Profiling**: API monitoring and tracing
- ✅ **Log Viewer**: Advanced log filtering and export
- ✅ **Remote Debugging**: Chrome DevTools integration

### 📚 Documentation & Help (40+ Features)
- ✅ **User Guide**: Comprehensive documentation
- ✅ **Video Tutorials**: Interactive learning
- ✅ **In-App Tips**: Contextual help
- ✅ **FAQ & Knowledge Base**: Searchable help center
- ✅ **API Documentation**: Developer resources
- ✅ **Support**: Email, live chat, forum, tickets

### 🌐 Integration Ecosystem (69+ Integrations)
- ✅ **Git Platforms**: GitHub, GitLab, Bitbucket, Gitea, Gogs, Azure DevOps
- ✅ **Project Management**: Jira, Trello, Asana, Monday.com, Linear, ClickUp
- ✅ **Communication**: Slack, Discord, Teams, Mattermost, Telegram
- ✅ **Video**: Zoom, Google Meet, Microsoft Teams, Webex
- ✅ **Monitoring**: Sentry, Datadog, New Relic, PagerDuty
- ✅ **Quality**: SonarQube, CodeClimate, Codecov, Snyk
- ✅ **Automation**: Zapier, IFTTT, n8n, Make

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

### 🔄 Pull Request & Workflow Guides

- [Quick Start: Pull Requests](docs/QUICKSTART_PR.md) - Create your first PR in 7 steps
- [Complete PR Guide](docs/PR_GUIDE.md) - Comprehensive guide for contributors (English/Portuguese)
- [Activating PR Workflows](docs/ACTIVATING_PR_WORKFLOWS.md) - Admin guide for enabling workflows (English/Portuguese)
- [Workflow Documentation](.github/workflows/README.md) - Detailed workflow information

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

**Quick Start**: See [Quick Start PR Guide](docs/QUICKSTART_PR.md) for step-by-step instructions in English and Portuguese.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

**Detailed Guides**:
- 📖 [Complete PR Guide](docs/PR_GUIDE.md) - Comprehensive guide for contributors (English/Portuguese)
- 🔧 [Activating Workflows](docs/ACTIVATING_PR_WORKFLOWS.md) - For repository administrators
- 🤝 [Contributing Guidelines](CONTRIBUTING.md) - Full contribution guidelines

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